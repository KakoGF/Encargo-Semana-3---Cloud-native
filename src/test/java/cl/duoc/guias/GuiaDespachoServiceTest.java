package cl.duoc.guias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.guias.dto.GuiaDespachoRequest;
import cl.duoc.guias.dto.GuiaResponse;
import cl.duoc.guias.dto.ItemGuia;
import cl.duoc.guias.exception.DatosGuiaInvalidosException;
import cl.duoc.guias.service.AwsS3Service;
import cl.duoc.guias.service.EfsStorageService;
import cl.duoc.guias.service.GuiaDespachoService;

/**
 * Tests de la logica de negocio de {@link GuiaDespachoService} usando mocks de
 * la capa EFS y S3 (no se requieren credenciales ni AWS real).
 */
@ExtendWith(MockitoExtension.class)
class GuiaDespachoServiceTest {

	@Mock
	EfsStorageService efs;

	@Mock
	AwsS3Service s3;

	GuiaDespachoService service;

	@BeforeEach
	void setUp() {
		service = new GuiaDespachoService(efs, s3, new ObjectMapper());
		ReflectionTestUtils.setField(service, "bucket", "test-bucket");
	}

	private GuiaDespachoRequest requestValido(String numero) {
		GuiaDespachoRequest req = new GuiaDespachoRequest();
		req.setNumeroGuia(numero);
		req.setTransportista("transportistaX");
		req.setFecha("20211");
		req.setDestinatario("Comercial Los Andes");
		req.setDireccionDespacho("Av. Siempre Viva 742");
		req.setProductos(List.of(new ItemGuia("Caja de tornillos", 10)));
		return req;
	}

	@Test
	void crearGuia_conNumeroIndicado_armaNombreYKeyCorrectos() {
		GuiaResponse resp = service.crearGuia(requestValido("123"));

		assertEquals("guia-123.json", resp.getNombreArchivo());
		assertEquals("20211/transportistaX/guia-123.json", resp.getKeyS3());
		// Sincronizado: crear escribe en EFS y sube a S3 en la misma operacion
		verify(efs).guardarGuiaTemporal(eq("guia-123.json"), any(byte[].class));
		verify(s3).uploadBytes(eq("test-bucket"),
				eq("20211/transportistaX/guia-123.json"), any(byte[].class), eq("application/json"));
	}

	@Test
	void crearGuia_sinNumero_generaNumeroAutomatico() {
		GuiaResponse resp = service.crearGuia(requestValido(null));

		assertNotNull(resp.getNumeroGuia());
		assertTrue(resp.getNombreArchivo().matches("guia-\\d+\\.json"),
				"El nombre deberia ser guia-<digitos>.json pero fue " + resp.getNombreArchivo());
		verify(efs).guardarGuiaTemporal(anyString(), any(byte[].class));
	}

	@Test
	void crearGuia_conNumeroMalicioso_lanza400_yNoTocaElEfs() {
		assertThrows(DatosGuiaInvalidosException.class,
				() -> service.crearGuia(requestValido("../../etc/passwd")));
		verifyNoInteractions(efs);
	}

	@Test
	void subirGuiaAS3_subeConKeyYContentTypeCorrectos() {
		when(efs.leerGuiaTemporal("guia-123.json")).thenReturn("{}".getBytes());

		service.subirGuiaAS3("guia-123.json", "transportistaX", "20211");

		verify(s3).uploadBytes(eq("test-bucket"),
				eq("20211/transportistaX/guia-123.json"), any(byte[].class), eq("application/json"));
	}

	@Test
	void descargarGuia_conNombreMalicioso_lanza400_yNoLlamaAS3() {
		// Cubre a la vez path traversal y header injection: el nombre no llega
		// ni a la key de S3 ni a la cabecera Content-Disposition.
		assertThrows(DatosGuiaInvalidosException.class,
				() -> service.descargarGuia("transportistaX", "20211", "../../secret.txt"));
		verifyNoInteractions(s3);
	}

	@Test
	void actualizarGuia_sinNumero_lanza400() {
		assertThrows(DatosGuiaInvalidosException.class,
				() -> service.actualizarGuia(requestValido(null)));
	}

	@Test
	void consultarGuias_conFechaYTransportista_armaPrefijoCompleto() {
		service.consultarGuias("20211", "transportistaX");
		verify(s3).listObjects("test-bucket", "20211/transportistaX/");
	}

	@Test
	void consultarGuias_soloFecha_armaPrefijoParcial() {
		service.consultarGuias("20211", null);
		verify(s3).listObjects("test-bucket", "20211/");
	}

	@Test
	void eliminarGuia_borraDeS3YDelEfs() {
		service.eliminarGuia("transportistaX", "20211", "guia-1001.json");
		// Sincronizado: el borrado afecta a ambos almacenamientos
		verify(s3).deleteObject("test-bucket", "20211/transportistaX/guia-1001.json");
		verify(efs).eliminarGuiaTemporal("guia-1001.json");
	}
}
