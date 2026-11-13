package by.lobacevich.analyzer.exceptionhandler;

import by.lobacevich.analyzer.dto.ErrorDto;
import by.lobacevich.analyzer.exception.RecordsNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorDto> handleRecordsNotFoundException(RecordsNotFoundException e) {
        log.error("{}, {}", e.getMessage(), e.getStackTrace());
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorDto> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("{}, {}", e.getMessage(), e.getStackTrace());
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("{}, {}", e.getMessage(), e.getStackTrace());
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDto> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.error("{}, {}", e.getMessage(), e.getStackTrace());
        return new ResponseEntity<>(new ErrorDto(msg), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorDto> handleException(Exception e) {
        log.error("{}, {}, {}", e.getMessage(), e.getClass().getSimpleName(), e.getStackTrace());
        return new ResponseEntity<>(new ErrorDto("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
