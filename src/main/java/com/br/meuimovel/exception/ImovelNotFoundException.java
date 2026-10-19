package com.br.meuimovel.exception;

public class ImovelNotFoundException extends RuntimeException {
    public ImovelNotFoundException(String message) {
        super(message);
    }
}
