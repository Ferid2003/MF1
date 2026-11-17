package farid.aghazada.storage_service.Exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("errorMessage", "Validation error");
        result.put("details", details);
        result.put("errorCode", "400");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    // malformed JSON or an invalid storageType value (not STAGING/PERMANENT)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedBody(HttpMessageNotReadableException ex) {
        return buildError("Malformed request body or invalid storageType (allowed: STAGING, PERMANENT)",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildError("Missing required parameter '" + ex.getParameterName() + "'", HttpStatus.BAD_REQUEST);
    }

    // thrown by StorageService for CSV length/format errors
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        return buildError(ex.getReason(), HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    // duplicate (bucket, path)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DataIntegrityViolationException ex) {
        return buildError("Storage with this bucket and path already exists", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedError(Exception ex) {
        return buildError("An error occurred on the server", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, String>> buildError(String message, HttpStatus status) {
        Map<String, String> result = new HashMap<>();
        result.put("errorMessage", message);
        result.put("errorCode", String.valueOf(status.value()));
        return ResponseEntity.status(status).body(result);
    }
}
