package cl.duoc.guias.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuiaDespachoMessage implements Serializable {

	private String numeroGuia;
	private String transportista;
	private String fecha;
	private String destinatario;
	private String direccionDespacho;
	private Integer totalProductos;
	private String nombreArchivo;
	private String keyS3;
	private String fechaCreacion;
}
