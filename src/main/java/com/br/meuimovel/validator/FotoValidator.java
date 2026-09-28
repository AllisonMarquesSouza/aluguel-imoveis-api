package com.br.meuimovel.validator;

import com.br.meuimovel.exception.FotoInvalidaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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

        if (!TIPOS_PERMITIDOS.contains(foto.getContentType())) {
            throw new FotoInvalidaException(
                    "Tipo de imagem não permitido."
            );
        }
    }
}