package cl.duoc.guias.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import cl.duoc.guias.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NoSuchBucketException.class)
	public ResponseEntity<ErrorResponse> handleNoSuchBucketException(NoSuchBucketException ex, WebRequest request) {
		log.error("Bucket no encontrado: {}", ex.getMessage());
		return build(HttpStatus.NOT_FOUND, "Bucket Not Found",
				"El bucket especificado no existe en S3", ex.getMessage(), request);
	}

	@ExceptionHandler(NoSuchKeyException.class)
	public ResponseEntity<ErrorResponse> handleNoSuchKeyException(NoSuchKeyException ex, WebRequest request) {
		log.error("Objeto no encontrado: {}", ex.getMessage());
		return build(HttpStatus.NOT_FOUND, "Object Not Found",
				"El objeto especificado no existe en el bucket", ex.getMessage(), request);
	}

	@ExceptionHandler(S3BucketNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleS3BucketNotFoundException(S3BucketNotFoundException ex, WebRequest request) {
		log.error("Bucket no encontrado: {}", ex.getMessage());
		return build(HttpStatus.NOT_FOUND, "Bucket Not Found", ex.getMessage(), null, request);
	}

	@ExceptionHandler(S3ObjectNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleS3ObjectNotFoundException(S3ObjectNotFoundException ex, WebRequest request) {
		log.error("Objeto no encontrado: {}", ex.getMessage());
		return build(HttpStatus.NOT_FOUND, "Object Not Found", ex.getMessage(), null, request);
	}

	@ExceptionHandler(S3AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleS3AccessDeniedException(S3AccessDeniedException ex, WebRequest request) {
		log.error("Acceso denegado: {}", ex.getMessage());
		return build(HttpStatus.FORBIDDEN, "Access Denied", ex.getMessage(),
				"Verifique las credenciales y permisos de IAM en AWS", request);
	}

	@ExceptionHandler(S3UploadException.class)
	public ResponseEntity<ErrorResponse> handleS3UploadException(S3UploadException ex, WebRequest request) {
		log.error("Error al subir archivo: {}", ex.getMessage());
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Upload Failed", ex.getMessage(), null, request);
	}

	@ExceptionHandler(S3OperationException.class)
	public ResponseEntity<ErrorResponse> handleS3OperationException(S3OperationException ex, WebRequest request) {
		log.error("Error en operacion S3: {}", ex.getMessage());
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "S3 Operation Failed", ex.getMessage(), null, request);
	}

	@ExceptionHandler(InvalidFileException.class)
	public ResponseEntity<ErrorResponse> handleInvalidFileException(InvalidFileException ex, WebRequest request) {
		log.error("Archivo invalido: {}", ex.getMessage());
		return build(HttpStatus.BAD_REQUEST, "Invalid File", ex.getMessage(), null, request);
	}

	@ExceptionHandler(DatosGuiaInvalidosException.class)
	public ResponseEntity<ErrorResponse> handleDatosGuiaInvalidosException(DatosGuiaInvalidosException ex, WebRequest request) {
		log.error("Datos de guia invalidos: {}", ex.getMessage());
		return build(HttpStatus.BAD_REQUEST, "Datos de guia invalidos", ex.getMessage(), null, request);
	}

	@ExceptionHandler(EfsFileNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleEfsFileNotFoundException(EfsFileNotFoundException ex, WebRequest request) {
		log.error("Guia no encontrada en EFS: {}", ex.getMessage());
		return build(HttpStatus.NOT_FOUND, "EFS File Not Found", ex.getMessage(), null, request);
	}

	@ExceptionHandler(EfsStorageException.class)
	public ResponseEntity<ErrorResponse> handleEfsStorageException(EfsStorageException ex, WebRequest request) {
		log.error("Error en operacion EFS: {}", ex.getMessage());
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "EFS Storage Error", ex.getMessage(),
				"Verifique que el EFS este montado correctamente en la ruta configurada", request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
		String detalles = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.collect(Collectors.joining("; "));
		log.error("Error de validacion: {}", detalles);
		return build(HttpStatus.BAD_REQUEST, "Validation Error",
				"Los datos enviados no son validos", detalles, request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, WebRequest request) {
		log.error("Archivo excede tamano maximo: {}", ex.getMessage());
		return build(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large",
				"El archivo excede el tamano maximo permitido", ex.getMessage(), request);
	}

	@ExceptionHandler(S3Exception.class)
	public ResponseEntity<ErrorResponse> handleS3Exception(S3Exception ex, WebRequest request) {
		log.error("Error de S3: {} - Codigo: {}", ex.getMessage(), ex.statusCode());
		HttpStatus status = HttpStatus.valueOf(ex.statusCode());
		String detalle = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage();
		return build(status, "S3 Error", "Error al realizar la operacion en S3", detalle, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
		log.error("Error inesperado: {}", ex.getMessage(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
				"Ha ocurrido un error inesperado en el servidor", ex.getMessage(), request);
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message,
			String details, WebRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(status.value())
				.error(error)
				.message(message)
				.details(details)
				.path(request.getDescription(false).replace("uri=", ""))
				.build();
		return new ResponseEntity<>(body, status);
	}
}
