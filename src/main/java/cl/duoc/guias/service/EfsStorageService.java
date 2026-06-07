package cl.duoc.guias.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.duoc.guias.exception.EfsFileNotFoundException;
import cl.duoc.guias.exception.EfsStorageException;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio encargado del almacenamiento TEMPORAL de las guias en el sistema de
 * archivos compartido Amazon EFS, montado en el contenedor sobre la ruta
 * indicada por la propiedad {@code app.efs.path} (por defecto /app/efs).
 *
 * Sigue el mismo patron que {@link AwsS3Service}, pero opera sobre el disco
 * montado (EFS) en lugar de sobre el bucket de S3.
 */
@Slf4j
@Service
public class EfsStorageService {

	private final Path rutaEfs;

	public EfsStorageService(@Value("${app.efs.path:/app/efs}") String rutaEfs) {
		this.rutaEfs = Paths.get(rutaEfs);
	}

	/**
	 * Guarda una guia de forma temporal en el EFS.
	 * (Analogo a AwsS3Service.upload, pero sobre el disco montado)
	 *
	 * @param nombreArchivo Nombre del archivo (por ejemplo "guia-123.json")
	 * @param contenido     Contenido del archivo en bytes
	 * @return Ruta absoluta donde quedo guardado el archivo
	 */
	public String guardarGuiaTemporal(String nombreArchivo, byte[] contenido) {

		Path destino = resolverRutaSegura(nombreArchivo);

		try {
			asegurarDirectorio();
			Files.write(destino, contenido);

			log.info("Guia guardada temporalmente en EFS: {}", destino.toAbsolutePath());

			return destino.toAbsolutePath().toString();

		} catch (IOException e) {
			throw new EfsStorageException("Error al guardar la guia en el EFS: " + nombreArchivo, e);
		}
	}

	/**
	 * Lee una guia almacenada temporalmente en el EFS.
	 * (Analogo a AwsS3Service.downloadAsBytes)
	 */
	public byte[] leerGuiaTemporal(String nombreArchivo) {

		Path origen = resolverRutaSegura(nombreArchivo);

		if (!Files.exists(origen)) {
			throw new EfsFileNotFoundException(nombreArchivo);
		}

		try {
			log.info("Leyendo guia desde el EFS: {}", origen.toAbsolutePath());
			return Files.readAllBytes(origen);
		} catch (IOException e) {
			throw new EfsStorageException("Error al leer la guia desde el EFS: " + nombreArchivo, e);
		}
	}

	/**
	 * Elimina una guia temporal del EFS.
	 * (Analogo a AwsS3Service.deleteObject)
	 */
	public void eliminarGuiaTemporal(String nombreArchivo) {

		Path objetivo = resolverRutaSegura(nombreArchivo);

		try {
			boolean eliminado = Files.deleteIfExists(objetivo);
			if (eliminado) {
				log.info("Guia temporal eliminada del EFS: {}", objetivo.toAbsolutePath());
			} else {
				log.warn("No se encontro la guia para eliminar en EFS: {}", objetivo.toAbsolutePath());
			}
		} catch (IOException e) {
			throw new EfsStorageException("Error al eliminar la guia del EFS: " + nombreArchivo, e);
		}
	}

	/**
	 * Lista los nombres de las guias almacenadas temporalmente en el EFS.
	 * (Analogo a AwsS3Service.listObjects)
	 */
	public List<String> listarGuiasTemporales() {

		try {
			asegurarDirectorio();

			try (var stream = Files.list(rutaEfs)) {
				return stream.filter(Files::isRegularFile)
						.map(p -> p.getFileName().toString())
						.collect(Collectors.toList());
			}

		} catch (IOException e) {
			throw new EfsStorageException("Error al listar las guias del EFS", e);
		}
	}

	/**
	 * Indica si una guia existe en el EFS.
	 */
	public boolean existeGuiaTemporal(String nombreArchivo) {
		return Files.exists(resolverRutaSegura(nombreArchivo));
	}

	/**
	 * Resuelve la ruta de un archivo DENTRO del directorio del EFS, evitando
	 * ataques de path traversal (por ejemplo "../../etc/passwd" o rutas
	 * absolutas). 
	 */
	private Path resolverRutaSegura(String nombreArchivo) {

		if (nombreArchivo == null || nombreArchivo.isBlank()) {
			throw new EfsStorageException("El nombre de archivo no puede estar vacio");
		}
		if (nombreArchivo.contains("/") || nombreArchivo.contains("\\") || nombreArchivo.contains("..")) {
			throw new EfsStorageException("Nombre de archivo no permitido: " + nombreArchivo);
		}

		Path base = rutaEfs.toAbsolutePath().normalize();
		Path destino = base.resolve(nombreArchivo).normalize();

		if (!destino.startsWith(base)) {
			throw new EfsStorageException("Ruta fuera del directorio EFS: " + nombreArchivo);
		}

		return destino;
	}

	/**
	 * Crea el directorio del EFS si no existe (util en entorno local y en el
	 * primer arranque del contenedor).
	 */
	private void asegurarDirectorio() throws IOException {
		if (!Files.exists(rutaEfs)) {
			Files.createDirectories(rutaEfs);
			log.info("Directorio EFS creado: {}", rutaEfs.toAbsolutePath());
		}
	}
}
