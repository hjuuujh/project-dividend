package zerobase.projectdividend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(value = AbstractException.class)
    protected ResponseEntity<ErrorResponse> handleException(AbstractException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getStatusCode())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.resolve(ex.getStatusCode()));
    }
}
