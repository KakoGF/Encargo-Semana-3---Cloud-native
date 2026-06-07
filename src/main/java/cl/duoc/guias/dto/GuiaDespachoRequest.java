package cl.duoc.guias.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Datos de entrada para generar o actualizar una guia de despacho.
 */
@Data
public class GuiaDespachoRequest {

	/**
	 * Numero de guia. Es opcional al CREAR (si viene vacio se genera uno);
	 * es obligatorio al ACTUALIZAR para identificar la guia existente.
	 */
	private String numeroGuia;

	@NotBlank(message = "El transportista es obligatorio")
	private String transportista;

	/**
	 * Fecha usada para organizar la carpeta en S3 (por ejemplo "20211" o
	 * "2026-06-02"). Define el primer nivel de la ruta: fecha/transportista/.
	 */
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
