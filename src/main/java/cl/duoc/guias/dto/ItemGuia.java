package cl.duoc.guias.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Linea de detalle de una guia de despacho (producto + cantidad).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemGuia {

	@NotBlank(message = "El nombre del producto es obligatorio")
	private String producto;

	@NotNull(message = "La cantidad es obligatoria")
	@Min(value = 1, message = "La cantidad debe ser al menos 1")
	private Integer cantidad;
}
