package cl.duoc.guias.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GuiaResponse {

	private String numeroGuia;
	private String transportista;
	private String fecha;

	private String nombreArchivo;

	private String keyS3;

	private String mensaje;
}
