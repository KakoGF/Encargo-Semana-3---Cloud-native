package cl.duoc.guias;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cl.duoc.guias.exception.EfsFileNotFoundException;
import cl.duoc.guias.exception.EfsStorageException;
import cl.duoc.guias.service.EfsStorageService;

/**
 * Tests de {@link EfsStorageService} sobre un directorio temporal real.
 * Incluye la verificacion del fix de path traversal.
 */
class EfsStorageServiceTest {

	@TempDir
	Path tempDir;

	EfsStorageService service;

	@BeforeEach
	void setUp() {
		service = new EfsStorageService(tempDir.toString());
	}

	@Test
	void guardarYLeer_devuelveElMismoContenido() {
		byte[] contenido = "contenido de prueba".getBytes();
		service.guardarGuiaTemporal("guia-1.json", contenido);

		byte[] leido = service.leerGuiaTemporal("guia-1.json");
		assertArrayEquals(contenido, leido);
	}

	@Test
	void listar_devuelveLosArchivosGuardados() {
		service.guardarGuiaTemporal("guia-1.json", "a".getBytes());
		service.guardarGuiaTemporal("guia-2.json", "b".getBytes());

		List<String> archivos = service.listarGuiasTemporales();
		assertTrue(archivos.contains("guia-1.json"));
		assertTrue(archivos.contains("guia-2.json"));
	}

	@Test
	void eliminar_dejaElArchivoInexistente() {
		service.guardarGuiaTemporal("guia-1.json", "a".getBytes());
		service.eliminarGuiaTemporal("guia-1.json");

		assertFalse(service.existeGuiaTemporal("guia-1.json"));
	}

	@Test
	void leerInexistente_lanzaEfsFileNotFound() {
		assertThrows(EfsFileNotFoundException.class,
				() -> service.leerGuiaTemporal("no-existe.json"));
	}

	@Test
	void pathTraversal_conDoblePunto_esRechazado() {
		assertThrows(EfsStorageException.class,
				() -> service.guardarGuiaTemporal("../evil.txt", "x".getBytes()));
	}

	@Test
	void pathTraversal_conRutaAbsoluta_esRechazado() {
		assertThrows(EfsStorageException.class,
				() -> service.leerGuiaTemporal("/etc/passwd"));
	}
}
