package com.br.meuimovel.validator;

import com.br.meuimovel.exception.DocumentoImovelInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Component
public class DocumentoImovelValidator {
    private static final long TAMANHO_MAXIMO = 5 * 1024 * 1024;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp");

    public void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new DocumentoImovelInvalidoException("O arquivo do documento é obrigatório.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new DocumentoImovelInvalidoException("O documento deve ter no máximo 5 MB.");
        }

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException exception) {
            throw new DocumentoImovelInvalidoException("Não foi possível ler o documento enviado.");
        }

        String tipoDetectado = detectarTipo(conteudo);
        if (tipoDetectado == null || !TIPOS_PERMITIDOS.contains(tipoDetectado)) {
            throw new DocumentoImovelInvalidoException("Envie um documento PDF, JPEG, PNG ou WebP válido.");
        }

        String tipoInformado = arquivo.getContentType();
        if (tipoInformado != null && !tipoInformado.isBlank()
                && !tipoInformado.toLowerCase(Locale.ROOT).equals(tipoDetectado)) {
            throw new DocumentoImovelInvalidoException(
                    "O conteúdo do arquivo não corresponde ao tipo informado.");
        }
    }

    public String nomeArquivo(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank()) {
            return "documento";
        }
        int separador = Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\'));
        nome = nome.substring(separador + 1).replaceAll("[\\p{Cntrl}]", "_").trim();
        if (nome.isBlank()) {
            return "documento";
        }
        return nome.length() > 255 ? nome.substring(0, 255) : nome;
    }

    public static String detectarTipo(byte[] conteudo) {
        if (conteudo.length >= 5
                && new String(conteudo, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            return "application/pdf";
        }
        return FotoValidator.detectarTipo(conteudo);
    }
}
