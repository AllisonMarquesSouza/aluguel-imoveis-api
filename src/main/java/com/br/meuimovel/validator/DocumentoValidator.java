package com.br.meuimovel.validator;

import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.exception.DocumentoInvalidoException;
import org.springframework.stereotype.Component;

@Component
public class DocumentoValidator {

    public String validar(TipoDocumento tipoDocumento, String documento) {

        if (tipoDocumento == null) {
            throw new DocumentoInvalidoException(
                    "Tipo de documento não informado."
            );
        }

        if (documento == null || documento.isBlank()) {
            throw new DocumentoInvalidoException(
                    "Documento não informado."
            );
        }

        String documentoLimpo = documento
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase();

        switch (tipoDocumento) {
            case CPF -> validarCpf(documentoLimpo);
            case CNPJ -> validarCnpj(documentoLimpo);
        }

        return documentoLimpo;
    }

    private void validarCpf(String cpf) {

        if (!cpf.matches("\\d{11}")) {
            throw new DocumentoInvalidoException(
                    "CPF deve possuir 11 dígitos."
            );
        }

        if (todosDigitosIguais(cpf)) {
            throw new DocumentoInvalidoException(
                    "CPF inválido."
            );
        }

        int primeiroDigito = calcularDigitoCpf(cpf, 9);
        int segundoDigito = calcularDigitoCpf(cpf, 10);

        if (Character.getNumericValue(cpf.charAt(9)) != primeiroDigito ||
                Character.getNumericValue(cpf.charAt(10)) != segundoDigito) {

            throw new DocumentoInvalidoException(
                    "CPF inválido."
            );
        }
    }

    private int calcularDigitoCpf(String cpf, int quantidadeDigitos) {

        int soma = 0;
        int peso = quantidadeDigitos + 1;

        for (int i = 0; i < quantidadeDigitos; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * peso--;
        }

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    private void validarCnpj(String cnpj) {

        if (!cnpj.matches("[A-Z0-9]{12}\\d{2}")) {
            throw new DocumentoInvalidoException(
                    "CNPJ deve possuir 12 caracteres alfanuméricos " +
                            "e 2 dígitos verificadores."
            );
        }

        if (todosCaracteresIguais(cnpj)) {
            throw new DocumentoInvalidoException(
                    "CNPJ inválido."
            );
        }

        int primeiroDigito = calcularDigitoCnpj(cnpj, 12);
        int segundoDigito = calcularDigitoCnpj(cnpj, 13);

        if (Character.getNumericValue(cnpj.charAt(12)) != primeiroDigito ||
                Character.getNumericValue(cnpj.charAt(13)) != segundoDigito) {

            throw new DocumentoInvalidoException(
                    "CNPJ inválido."
            );
        }
    }

    private int calcularDigitoCnpj(String cnpj, int quantidadeDigitos) {

        int[] pesos = quantidadeDigitos == 12
                ? new int[]{
                5, 4, 3, 2, 9, 8,
                7, 6, 5, 4, 3, 2
        }
                : new int[]{
                6, 5, 4, 3, 2, 9,
                8, 7, 6, 5, 4, 3, 2
        };

        int soma = 0;

        for (int i = 0; i < quantidadeDigitos; i++) {
            soma += valorCnpj(cnpj.charAt(i)) * pesos[i];
        }

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    private int valorCnpj(char caractere) {

        if (Character.isDigit(caractere)) {
            return caractere - '0';
        }

        return caractere - 'A' + 17;
    }

    private boolean todosDigitosIguais(String documento) {

        return documento.chars()
                .allMatch(digito -> digito == documento.charAt(0));
    }

    private boolean todosCaracteresIguais(String documento) {

        return documento.chars()
                .allMatch(caractere -> caractere == documento.charAt(0));
    }
}