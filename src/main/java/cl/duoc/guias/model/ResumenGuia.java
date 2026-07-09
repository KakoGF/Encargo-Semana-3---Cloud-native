package cl.duoc.guias.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resumen_guias")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumenGuia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "numero_guia", nullable = false, unique = true)
	private String numeroGuia;

	@Column(nullable = false)
	private String transportista;

	@Column(nullable = false)
	private String fecha;

	@Column(nullable = false)
	private String destinatario;

	@Column(name = "direccion_despacho", nullable = false)
	private String direccionDespacho;

	@Column(name = "total_productos", nullable = false)
	private Integer totalProductos;

	@Column(name = "nombre_archivo")
	private String nombreArchivo;

	@Column(name = "key_s3")
	private String keyS3;

	@Column(name = "fecha_creacion")
	private String fechaCreacion;

	@Column(name = "fecha_procesado", nullable = false)
	private LocalDateTime fechaProcesado;
}
