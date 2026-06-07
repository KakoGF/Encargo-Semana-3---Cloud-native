package cl.duoc.guias.controllers;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.guias.dto.GuiaDespachoRequest;
import cl.duoc.guias.dto.GuiaResponse;
import cl.duoc.guias.dto.S3ObjectDto;
import cl.duoc.guias.service.GuiaDespachoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API REST de guias de despacho. 
 */
@RestController
@RequestMapping("/guias")
@RequiredArgsConstructor
public class GuiaDespachoController {

	private final GuiaDespachoService guiaDespachoService;

	/** Crear guia de despacho: genera el archivo y lo guarda temporalmente en EFS. */
	@PostMapping
	public ResponseEntity<GuiaResponse> crearGuia(@Valid @RequestBody GuiaDespachoRequest request) {
		GuiaResponse response = guiaDespachoService.crearGuia(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/** Subir a S3 una guia ya generada en el EFS. */
	@PostMapping("/subir")
	public ResponseEntity<GuiaResponse> subirGuia(@RequestParam String nombreArchivo,
			@RequestParam String transportista, @RequestParam String fecha) {
		GuiaResponse response = guiaDespachoService.subirGuiaAS3(nombreArchivo, transportista, fecha);
		return ResponseEntity.ok(response);
	}

	/**
	 * Subir un archivo cualquiera (multipart) al EFS y a S3 a la vez.
	 * Habilita el CRUD de archivos arbitrarios; la descarga, eliminacion y
	 * consulta se hacen con los endpoints existentes usando el mismo nombre.
	 */
	@PostMapping(value = "/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<GuiaResponse> subirArchivo(@RequestParam("file") MultipartFile file,
			@RequestParam String transportista, @RequestParam String fecha) {
		GuiaResponse response = guiaDespachoService.subirArchivo(file, transportista, fecha);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/** Descargar una guia desde S3. */
	@GetMapping("/descargar")
	public ResponseEntity<byte[]> descargarGuia(@RequestParam String transportista,
			@RequestParam String fecha, @RequestParam String nombreArchivo) {

		byte[] contenido = guiaDespachoService.descargarGuia(transportista, fecha, nombreArchivo);

		// El nombreArchivo ya fue validado en la capa de servicio; ademas se arma
		// la cabecera con el builder de Spring para un escapado correcto y evitar
		// inyeccion de cabeceras (header injection).
		ContentDisposition contentDisposition = ContentDisposition.attachment()
				.filename(nombreArchivo)
				.build();

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(contenido);
	}

	/** Modificar / actualizar una guia (regenera en EFS y sobreescribe en S3). */
	@PutMapping
	public ResponseEntity<GuiaResponse> actualizarGuia(@Valid @RequestBody GuiaDespachoRequest request) {
		GuiaResponse response = guiaDespachoService.actualizarGuia(request);
		return ResponseEntity.ok(response);
	}

	/** Eliminar una guia especifica desde S3. */
	@DeleteMapping
	public ResponseEntity<Void> eliminarGuia(@RequestParam String transportista,
			@RequestParam String fecha, @RequestParam String nombreArchivo) {
		guiaDespachoService.eliminarGuia(transportista, fecha, nombreArchivo);
		return ResponseEntity.noContent().build();
	}

	/** Consultar el historial de guias por transportista y fecha. */
	@GetMapping
	public ResponseEntity<List<S3ObjectDto>> consultarGuias(
			@RequestParam(required = false) String fecha,
			@RequestParam(required = false) String transportista) {
		List<S3ObjectDto> guias = guiaDespachoService.consultarGuias(fecha, transportista);
		return ResponseEntity.ok(guias);
	}

	/** Listar las guias que estan actualmente en el EFS (apoyo para la demo). */
	@GetMapping("/temporales")
	public ResponseEntity<List<String>> listarTemporales() {
		return ResponseEntity.ok(guiaDespachoService.listarGuiasEnEfs());
	}
}