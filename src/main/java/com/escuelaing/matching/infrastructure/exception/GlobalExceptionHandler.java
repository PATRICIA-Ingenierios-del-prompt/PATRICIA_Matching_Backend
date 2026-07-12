package com.escuelaing.matching.infrastructure.exception;

import com.escuelaing.matching.domain.exception.MatchYaExisteException;
import com.escuelaing.matching.domain.exception.SugerenciaNoEncontradaException;
import com.escuelaing.matching.domain.exception.UsuarioNoElegibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SugerenciaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleSugerenciaNoEncontrada(SugerenciaNoEncontradaException ex) {
        return notFound(ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoElegibleException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioNoElegible(UsuarioNoElegibleException ex) {
        return notFound(ex.getMessage());
    }

    @ExceptionHandler(MatchYaExisteException.class)
    public ResponseEntity<ErrorResponse> handleMatchYaExiste(MatchYaExisteException ex) {
        return conflict(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(400)
                        .error("BAD_REQUEST")
                        .message(message)
                        .build()
        );
    }

    private ResponseEntity<ErrorResponse> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(404)
                        .error("NOT_FOUND")
                        .message(message)
                        .build()
        );
    }

    private ResponseEntity<ErrorResponse> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(409)
                        .error("CONFLICT")
                        .message(message)
                        .build()
        );
    }
}
