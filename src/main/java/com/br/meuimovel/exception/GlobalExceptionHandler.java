package com.br.meuimovel.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CidadeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCidadeNotFoundException(CidadeNotFoundException exception){
        ErrorResponse errorResponse = new ErrorResponse
                (exception.getMessage(), HttpStatus.NOT_FOUND,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(CidadeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCidadeAlreadyExistsException(CidadeAlreadyExistsException exception){
        ErrorResponse errorResponse = new ErrorResponse
                (exception.getMessage(), HttpStatus.CONFLICT,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
}
//ErroResponse erroResponse = new ErroResponse(exception.getMessage(), HttpStatus.NOT_FOUND,
//        LocalDateTime.now());
//        return new ResponseEntity<>(erroResponse, HttpStatus.NOT_FOUND);