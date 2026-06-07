package cl.duoc.guias.exception;

/**
 * Se lanza cuando los datos de entrada de una guia no son validos
 * (por ejemplo, numero de guia o nombre de archivo con caracteres no permitidos).
 * Se mapea a HTTP 400 en el GlobalExceptionHandler.
 */
public class DatosGuiaInvalidosException extends RuntimeException {
	public DatosGuiaInvalidosException(String message) {
		super(message);
	}
}
