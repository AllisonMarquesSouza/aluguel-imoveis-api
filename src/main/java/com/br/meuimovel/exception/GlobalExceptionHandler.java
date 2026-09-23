package com.br.meuimovel.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException exception) {
        ErrorResponse errorResponse = new ErrorResponse
                ("Email ou senha inválidos", HttpStatus.UNAUTHORIZED,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException exception) {
        ErrorResponse errorResponse = new ErrorResponse
                ("Usuario inativo ou bloqueado", HttpStatus.UNAUTHORIZED,
                        LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(CidadeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCidadeNotFoundException(CidadeNotFoundException exception){
        ErrorResponse errorResponse = new ErrorResponse
                (exception.getMessage(), HttpStatus.NOT_FOUND,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmpresaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmpresaNotFoundException(EmpresaNotFoundException exception){
        ErrorResponse errorResponse = new ErrorResponse
                (exception.getMessage(), HttpStatus.NOT_FOUND,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(UsuarioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioNotFoundException(UsuarioNotFoundException exception){
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
    @ExceptionHandler(UsuarioAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioAlreadyExistsException(UsuarioAlreadyExistsException exception){
        ErrorResponse errorResponse = new ErrorResponse
                (exception.getMessage(), HttpStatus.CONFLICT,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(EmpresaAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmpresaAlreadyExistsException(EmpresaAlreadyExistsException exception){
        ErrorResponse errorResponse = new ErrorResponse
                (exception.getMessage(), HttpStatus.CONFLICT,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsuarioNaoPodePossuirEmpresaException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioNaoPodePossuirEmpresaException
            (UsuarioNaoPodePossuirEmpresaException exception){
        ErrorResponse errorResponse = new ErrorResponse
                (exception.getMessage(), HttpStatus.CONFLICT,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DocumentoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleDocumentoInvalidoException(DocumentoInvalidoException exception){
        ErrorResponse errorResponse = new ErrorResponse
                (exception.getMessage(), HttpStatus.BAD_REQUEST,
                        LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
