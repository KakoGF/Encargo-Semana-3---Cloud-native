package cl.duoc.guias.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class GuiaDespachoRequest {

	private String numeroGuia;

	@NotBlank(message = "El transportista es obligatorio")
	private String transportista;

	@NotBlank(message = "La fecha es obligatoria")
	private String fecha;

	@NotBlank(message = "El destinatario es obligatorio")
	private String destinatario;

	@NotBlank(message = "La direccion de despacho es obligatoria")
	private String direccionDespacho;

	@NotEmpty(message = "La guia debe tener al menos un producto")
	@Valid
	private List<ItemGuia> productos;
}
