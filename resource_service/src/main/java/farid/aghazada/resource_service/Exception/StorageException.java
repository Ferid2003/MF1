package farid.aghazada.resource_service.Exception;

import org.springframework.http.HttpStatus;

public class StorageException extends AppException {

    public StorageException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }

}
