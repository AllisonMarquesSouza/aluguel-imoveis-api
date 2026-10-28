package com.br.meuimovel.storage;

import com.br.meuimovel.enums.TipoImagem;
import com.br.meuimovel.exception.FileStorageException;
import com.br.meuimovel.exception.ImagemNotFoundException;
import com.br.meuimovel.validator.FotoValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.storage.location:uploads}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @Override
    public String salvarImagem(MultipartFile arquivo, TipoImagem tipo, Integer entidadeId) {
        validarReferencia(tipo, entidadeId);
        String imagemId = UUID.randomUUID().toString();
        Path destino = caminho(tipo, entidadeId, imagemId);

        try {
            Files.createDirectories(destino.getParent());
            arquivo.transferTo(destino);
            return ImagemUrl.criar(tipo, entidadeId, imagemId);
        } catch (IOException e) {
            throw new FileStorageException("Não foi possível salvar a imagem.");
        }
    }

    @Override
    public StoredImage carregar(TipoImagem tipo, Integer entidadeId, String imagemId) {
        Path arquivo = caminho(tipo, entidadeId, imagemId);
        if (!Files.isRegularFile(arquivo)) {
            throw new ImagemNotFoundException("Imagem não encontrada.");
        }

        try {
            Resource resource = new UrlResource(arquivo.toUri());
            String contentType = FotoValidator.detectarTipo(Files.readAllBytes(arquivo));
            if (contentType == null) {
                throw new FileStorageException("O arquivo armazenado não é uma imagem permitida.");
            }
            return new StoredImage(resource, contentType);
        } catch (IOException e) {
            throw new FileStorageException("Não foi possível ler a imagem armazenada.");
        }
    }

    @Override
    public void substituir(TipoImagem tipo, Integer entidadeId, String imagemId, MultipartFile arquivo) {
        Path destino = caminho(tipo, entidadeId, imagemId);
        if (!Files.isRegularFile(destino)) {
            throw new ImagemNotFoundException("Imagem não encontrada.");
        }

        Path temporario = null;
        try {
            temporario = Files.createTempFile(destino.getParent(), "imagem-", ".tmp");
            arquivo.transferTo(temporario);
            try {
                Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new FileStorageException("Não foi possível atualizar a imagem.");
        } finally {
            if (temporario != null) {
                try {
                    Files.deleteIfExists(temporario);
                } catch (IOException ignored) {
                    // A falha de limpeza não deve ocultar a falha original.
                }
            }
        }
    }

    @Override
    public void deletar(TipoImagem tipo, Integer entidadeId, String imagemId) {
        try {
            Files.deleteIfExists(caminho(tipo, entidadeId, imagemId));
        } catch (IOException e) {
            throw new FileStorageException("Não foi possível excluir a imagem.");
        }
    }

    @Override
    public void deletar(String url) {
        try {
            ImagemUrl.ReferenciaImagem referencia = ImagemUrl.analisar(url);
            deletar(referencia.tipo(), referencia.entidadeId(), referencia.imagemId());
        } catch (IllegalArgumentException exception) {
            deletarCaminhoLegado(url);
        }
    }

    private void deletarCaminhoLegado(String url) {
        String prefixo = "/uploads/";
        if (url == null || !url.startsWith(prefixo)) {
            throw new FileStorageException("Referência de imagem inválida.");
        }

        Path arquivo = rootLocation.resolve(url.substring(prefixo.length())).normalize();
        if (!arquivo.startsWith(rootLocation)) {
            throw new FileStorageException("Referência de imagem inválida.");
        }
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            throw new FileStorageException("Não foi possível excluir a imagem.");
        }
    }

    private Path caminho(TipoImagem tipo, Integer entidadeId, String imagemId) {
        validarReferencia(tipo, entidadeId);
        UUID uuid;
        try {
            uuid = UUID.fromString(imagemId);
            if (!uuid.toString().equals(imagemId)) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new ImagemNotFoundException("Imagem não encontrada.");
        }

        Path arquivo = rootLocation.resolve(tipo.name().toLowerCase(Locale.ROOT))
                .resolve(entidadeId.toString())
                .resolve(uuid.toString())
                .normalize();
        if (!arquivo.startsWith(rootLocation)) {
            throw new ImagemNotFoundException("Imagem não encontrada.");
        }
        return arquivo;
    }

    private void validarReferencia(TipoImagem tipo, Integer entidadeId) {
        if (tipo == null || entidadeId == null || entidadeId < 1) {
            throw new FileStorageException("Referência de imagem inválida.");
        }
    }
}
