package com.br.meuimovel.storage;

import com.br.meuimovel.enums.TipoImagem;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class ImagemUrl {
    private ImagemUrl() {}

    public static String criar(TipoImagem tipo, Integer entidadeId, String imagemId) {
        return "/imagens/" + tipo.name().toLowerCase(Locale.ROOT) + "/" + entidadeId + "/" + imagemId;
    }

    public static ReferenciaImagem analisar(String url) {
        if (url == null) {
            throw new IllegalArgumentException("URL de imagem ausente.");
        }

        String[] partes = url.split("/");
        if (partes.length != 5 || !partes[1].equals("imagens")) {
            throw new IllegalArgumentException("URL de imagem inválida.");
        }

        try {
            TipoImagem tipo = TipoImagem.valueOf(partes[2].toUpperCase(Locale.ROOT));
            Integer entidadeId = Integer.valueOf(partes[3]);
            String imagemId = partes[4];
            UUID uuid = UUID.fromString(imagemId);
            if (!uuid.toString().equals(imagemId) || entidadeId < 1) {
                throw new IllegalArgumentException("URL de imagem inválida.");
            }
            return new ReferenciaImagem(tipo, entidadeId, imagemId);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("URL de imagem inválida.", exception);
        }
    }

    public static String extrairImagemId(String url, TipoImagem tipoEsperado, Integer entidadeIdEsperada) {
        try {
            ReferenciaImagem referencia = analisar(url);
            if (referencia.tipo() != tipoEsperado
                    || !Objects.equals(referencia.entidadeId(), entidadeIdEsperada)) {
                return null;
            }
            return referencia.imagemId();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public record ReferenciaImagem(TipoImagem tipo, Integer entidadeId, String imagemId) {}
}
