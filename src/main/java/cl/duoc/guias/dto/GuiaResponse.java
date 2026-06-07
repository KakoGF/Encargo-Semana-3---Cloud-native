package cl.duoc.guias.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta entregada al cliente tras crear, subir o actualizar una guia.
 */
@Data
@Builder
public class GuiaResponse {

	private String numeroGuia;
	private String transportista;
	private String fecha;

	/** Nombre del archivo generado (por ejemplo "guia-123.json") */
	private String nombreArchivo;

	/** Clave (key) con que se almacena/almacenara en S3 */
	private String keyS3;

	private String mensaje;
}
