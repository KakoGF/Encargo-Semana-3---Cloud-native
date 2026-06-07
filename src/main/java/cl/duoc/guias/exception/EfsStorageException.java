package cl.duoc.guias.exception;

public class EfsStorageException extends RuntimeException {
	public EfsStorageException(String message) {
		super(message);
	}
	public EfsStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
