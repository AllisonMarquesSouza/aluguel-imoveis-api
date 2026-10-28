package com.br.meuimovel.validator;

import com.br.meuimovel.exception.FotoInvalidaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

@Component
public class FotoValidator {

    private static final long TAMANHO_MAXIMO = 5 * 1024 * 1024;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public void validar(MultipartFile foto) {

        if (foto == null || foto.isEmpty()) {
            throw new FotoInvalidaException(
                    "A foto é obrigatória."
            );
        }

        if (foto.getSize() > TAMANHO_MAXIMO) {
            throw new FotoInvalidaException(
                    "A foto deve ter no máximo 5 MB."
            );
        }

        byte[] conteudo;
        try {
            conteudo = foto.getBytes();
        } catch (IOException e) {
            throw new FotoInvalidaException("Não foi possível ler a imagem enviada.");
        }

        String tipoDetectado = detectarTipo(conteudo);
        if (tipoDetectado == null || !TIPOS_PERMITIDOS.contains(tipoDetectado)) {
            throw new FotoInvalidaException(
                    "Tipo de imagem não permitido."
            );
        }

        String tipoInformado = foto.getContentType();
        if (tipoInformado != null && !tipoInformado.equalsIgnoreCase(tipoDetectado)) {
            throw new FotoInvalidaException(
                    "O conteúdo do arquivo não corresponde ao tipo de imagem informado."
            );
        }
    }

    public static String detectarTipo(byte[] conteudo) {
        if (conteudo.length >= 3
                && (conteudo[0] & 0xFF) == 0xFF
                && (conteudo[1] & 0xFF) == 0xD8
                && (conteudo[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        byte[] assinaturaPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (conteudo.length >= assinaturaPng.length
                && Arrays.equals(Arrays.copyOf(conteudo, assinaturaPng.length), assinaturaPng)) {
            return "image/png";
        }

        if (conteudo.length >= 12
                && conteudo[0] == 'R' && conteudo[1] == 'I' && conteudo[2] == 'F' && conteudo[3] == 'F'
                && conteudo[8] == 'W' && conteudo[9] == 'E' && conteudo[10] == 'B' && conteudo[11] == 'P') {
            return "image/webp";
        }

        return null;
    }
}
