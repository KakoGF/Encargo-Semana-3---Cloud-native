package cl.duoc.guias.exception;

public class S3AccessDeniedException extends RuntimeException {
	public S3AccessDeniedException(String operation) {
		super("Acceso denegado al intentar realizar la operacion: " + operation);
	}
	public S3AccessDeniedException(String operation, Throwable cause) {
		super("Acceso denegado al intentar realizar la operacion: " + operation, cause);
	}
}
