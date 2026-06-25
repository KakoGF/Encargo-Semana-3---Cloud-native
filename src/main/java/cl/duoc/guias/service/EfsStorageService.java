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

@Slf4j
@Service
public class EfsStorageService {

	private final Path rutaEfs;

	public EfsStorageService(@Value("${app.efs.path:/app/efs}") String rutaEfs) {
		this.rutaEfs = Paths.get(rutaEfs);
	}

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

	public boolean existeGuiaTemporal(String nombreArchivo) {
		return Files.exists(resolverRutaSegura(nombreArchivo));
	}

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

	private void asegurarDirectorio() throws IOException {
		if (!Files.exists(rutaEfs)) {
			Files.createDirectories(rutaEfs);
			log.info("Directorio EFS creado: {}", rutaEfs.toAbsolutePath());
		}
	}
}
