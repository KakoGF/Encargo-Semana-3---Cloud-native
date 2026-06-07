package cl.duoc.guias.exception;

public class EfsFileNotFoundException extends RuntimeException {
	public EfsFileNotFoundException(String nombreArchivo) {
		super("La guia '" + nombreArchivo + "' no existe en el EFS");
	}
}
