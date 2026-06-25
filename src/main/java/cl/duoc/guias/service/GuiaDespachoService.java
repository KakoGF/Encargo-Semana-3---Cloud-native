package cl.duoc.guias.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.guias.dto.GuiaDespachoRequest;
import cl.duoc.guias.dto.GuiaResponse;
import cl.duoc.guias.dto.S3ObjectDto;
import cl.duoc.guias.exception.DatosGuiaInvalidosException;
import cl.duoc.guias.exception.InvalidFileException;
import cl.duoc.guias.exception.S3OperationException;
import cl.duoc.guias.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaDespachoService {

	private final EfsStorageService efsStorageService;
	private final AwsS3Service awsS3Service;
	private final ObjectMapper objectMapper;

	@Value("${aws.s3.bucket}")
	private String bucket;

	private static final Pattern NUMERO_VALIDO = Pattern.compile("^[A-Za-z0-9_-]+$");

	private static final Pattern NOMBRE_ARCHIVO_VALIDO = Pattern.compile("^[A-Za-z0-9._-]+$");

	public GuiaResponse crearGuia(GuiaDespachoRequest request) {

		String numeroGuia;
		if (request.getNumeroGuia() != null && !request.getNumeroGuia().isBlank()) {
			validarNumeroGuia(request.getNumeroGuia());
			numeroGuia = request.getNumeroGuia();
		} else {
			numeroGuia = generarNumeroGuia();
		}

		String nombreArchivo = "guia-" + numeroGuia + ".json";
		byte[] contenido = serializarGuia(numeroGuia, request);
		String keyS3 = construirKey(request.getFecha(), request.getTransportista(), nombreArchivo);

		efsStorageService.guardarGuiaTemporal(nombreArchivo, contenido);
		try {
			awsS3Service.uploadBytes(bucket, keyS3, contenido, "application/json");
		} catch (RuntimeException e) {
			efsStorageService.eliminarGuiaTemporal(nombreArchivo);
			throw e;
		}

		log.info("Guia {} creada en EFS y subida a S3 con key: {}", numeroGuia, keyS3);

		return GuiaResponse.builder()
				.numeroGuia(numeroGuia)
				.transportista(request.getTransportista())
				.fecha(request.getFecha())
				.nombreArchivo(nombreArchivo)
				.keyS3(keyS3)
				.mensaje("Guia generada en el EFS y subida a S3")
				.build();
	}

	public GuiaResponse subirGuiaAS3(String nombreArchivo, String transportista, String fecha) {

		validarNombreArchivo(nombreArchivo);

		byte[] contenido = efsStorageService.leerGuiaTemporal(nombreArchivo);
		String keyS3 = construirKey(fecha, transportista, nombreArchivo);

		awsS3Service.uploadBytes(bucket, keyS3, contenido, "application/json");

		log.info("Guia {} subida a S3 en key: {}", nombreArchivo, keyS3);

		return GuiaResponse.builder()
				.transportista(transportista)
				.fecha(fecha)
				.nombreArchivo(nombreArchivo)
				.keyS3(keyS3)
				.mensaje("Guia subida exitosamente a S3")
				.build();
	}

	public GuiaResponse subirArchivo(MultipartFile file, String transportista, String fecha) {

		if (file == null || file.isEmpty()) {
			throw new InvalidFileException("El archivo esta vacio o es nulo");
		}

		String nombreArchivo = file.getOriginalFilename();
		validarNombreArchivo(nombreArchivo);

		byte[] contenido;
		try {
			contenido = file.getBytes();
		} catch (IOException e) {
			throw new S3UploadException("Error al leer el archivo: " + e.getMessage(), e);
		}

		String keyS3 = construirKey(fecha, transportista, nombreArchivo);
		String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

		efsStorageService.guardarGuiaTemporal(nombreArchivo, contenido);
		try {
			awsS3Service.uploadBytes(bucket, keyS3, contenido, contentType);
		} catch (RuntimeException e) {
			efsStorageService.eliminarGuiaTemporal(nombreArchivo);
			throw e;
		}

		log.info("Archivo {} subido al EFS y a S3 con key: {}", nombreArchivo, keyS3);

		return GuiaResponse.builder()
				.transportista(transportista)
				.fecha(fecha)
				.nombreArchivo(nombreArchivo)
				.keyS3(keyS3)
				.mensaje("Archivo subido al EFS y a S3")
				.build();
	}

	public byte[] descargarGuia(String transportista, String fecha, String nombreArchivo) {
		validarNombreArchivo(nombreArchivo);
		String keyS3 = construirKey(fecha, transportista, nombreArchivo);
		return awsS3Service.downloadAsBytes(bucket, keyS3);
	}

	public GuiaResponse actualizarGuia(GuiaDespachoRequest request) {

		if (request.getNumeroGuia() == null || request.getNumeroGuia().isBlank()) {
			throw new DatosGuiaInvalidosException("Para actualizar es obligatorio indicar el numero de guia");
		}
		validarNumeroGuia(request.getNumeroGuia());

		String nombreArchivo = "guia-" + request.getNumeroGuia() + ".json";
		byte[] contenido = serializarGuia(request.getNumeroGuia(), request);

		efsStorageService.guardarGuiaTemporal(nombreArchivo, contenido);

		String keyS3 = construirKey(request.getFecha(), request.getTransportista(), nombreArchivo);
		awsS3Service.uploadBytes(bucket, keyS3, contenido, "application/json");

		log.info("Guia {} actualizada en EFS y S3 (key: {})", request.getNumeroGuia(), keyS3);

		return GuiaResponse.builder()
				.numeroGuia(request.getNumeroGuia())
				.transportista(request.getTransportista())
				.fecha(request.getFecha())
				.nombreArchivo(nombreArchivo)
				.keyS3(keyS3)
				.mensaje("Guia actualizada exitosamente en el EFS y en S3")
				.build();
	}

	public void eliminarGuia(String transportista, String fecha, String nombreArchivo) {
		validarNombreArchivo(nombreArchivo);
		String keyS3 = construirKey(fecha, transportista, nombreArchivo);

		awsS3Service.deleteObject(bucket, keyS3);
		efsStorageService.eliminarGuiaTemporal(nombreArchivo);

		log.info("Guia eliminada de S3 y del EFS: {} (key S3: {})", nombreArchivo, keyS3);
	}

	public List<S3ObjectDto> consultarGuias(String fecha, String transportista) {
		String prefijo = construirPrefijo(fecha, transportista);
		return awsS3Service.listObjects(bucket, prefijo);
	}

	public List<String> listarGuiasEnEfs() {
		return efsStorageService.listarGuiasTemporales();
	}

	private String construirKey(String fecha, String transportista, String nombreArchivo) {
		return fecha + "/" + transportista + "/" + nombreArchivo;
	}

	private String construirPrefijo(String fecha, String transportista) {
		StringBuilder prefijo = new StringBuilder();
		if (fecha != null && !fecha.isBlank()) {
			prefijo.append(fecha).append("/");
			if (transportista != null && !transportista.isBlank()) {
				prefijo.append(transportista).append("/");
			}
		}
		return prefijo.toString();
	}

	private void validarNumeroGuia(String numeroGuia) {
		if (numeroGuia == null || !NUMERO_VALIDO.matcher(numeroGuia).matches()) {
			throw new DatosGuiaInvalidosException(
					"El numero de guia solo admite letras, numeros, guion y guion bajo: " + numeroGuia);
		}
	}

	private void validarNombreArchivo(String nombreArchivo) {
		if (nombreArchivo == null || nombreArchivo.isBlank()
				|| nombreArchivo.contains("..")
				|| !NOMBRE_ARCHIVO_VALIDO.matcher(nombreArchivo).matches()) {
			throw new DatosGuiaInvalidosException("Nombre de archivo no permitido: " + nombreArchivo);
		}
	}

	private String generarNumeroGuia() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
	}

	private byte[] serializarGuia(String numeroGuia, GuiaDespachoRequest request) {
		Map<String, Object> guia = new LinkedHashMap<>();
		guia.put("numeroGuia", numeroGuia);
		guia.put("transportista", request.getTransportista());
		guia.put("fecha", request.getFecha());
		guia.put("destinatario", request.getDestinatario());
		guia.put("direccionDespacho", request.getDireccionDespacho());
		guia.put("productos", request.getProductos());
		guia.put("fechaGeneracion", LocalDateTime.now().toString());

		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(guia);
		} catch (JsonProcessingException e) {
			throw new S3OperationException("Error al generar el contenido JSON de la guia", e);
		}
	}
}
