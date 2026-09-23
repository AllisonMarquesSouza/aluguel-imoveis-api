package com.br.meuimovel.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    String message;
    HttpStatus httpStatus;
    LocalDateTime timestamp;
}
