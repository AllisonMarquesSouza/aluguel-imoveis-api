package com.br.meuimovel.storage;

import com.br.meuimovel.exception.DocumentoImovelNotFoundException;
import com.br.meuimovel.exception.FileStorageException;
import com.br.meuimovel.validator.DocumentoImovelValidator;
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
import java.util.UUID;

@Service
public class LocalDocumentoStorageService implements DocumentoStorageService {
    private final Path rootLocation;

    public LocalDocumentoStorageService(@Value("${app.storage.location:uploads}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @Override
    public String salvar(MultipartFile arquivo, Integer imovelId) {
        String arquivoId = UUID.randomUUID().toString();
        Path destino = caminho(imovelId, arquivoId);
        try {
            Files.createDirectories(destino.getParent());
            arquivo.transferTo(destino);
            return arquivoId;
        } catch (IOException exception) {
            tentarExcluir(destino);
            throw new FileStorageException("Não foi possível salvar o documento do imóvel.");
        }
    }

    @Override
    public StoredDocument carregar(Integer imovelId, String arquivoId) {
        Path arquivo = caminho(imovelId, arquivoId);
        if (!Files.isRegularFile(arquivo)) {
            throw new DocumentoImovelNotFoundException("Arquivo do documento não encontrado.");
        }

        try {
            Resource resource = new UrlResource(arquivo.toUri());
            String contentType = DocumentoImovelValidator.detectarTipo(Files.readAllBytes(arquivo));
            if (contentType == null) {
                throw new FileStorageException("O documento armazenado não possui um formato permitido.");
            }
            return new StoredDocument(resource, contentType);
        } catch (IOException exception) {
            throw new FileStorageException("Não foi possível ler o documento do imóvel.");
        }
    }

    @Override
    public void substituir(Integer imovelId, String arquivoId, MultipartFile arquivo) {
        Path destino = caminho(imovelId, arquivoId);
        if (!Files.isRegularFile(destino)) {
            throw new DocumentoImovelNotFoundException("Arquivo do documento não encontrado.");
        }

        Path temporario = null;
        try {
            temporario = Files.createTempFile(destino.getParent(), "documento-", ".tmp");
            arquivo.transferTo(temporario);
            try {
                Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new FileStorageException("Não foi possível atualizar o documento do imóvel.");
        } finally {
            if (temporario != null) {
                tentarExcluir(temporario);
            }
        }
    }

    @Override
    public void deletar(Integer imovelId, String arquivoId) {
        tentarExcluir(caminho(imovelId, arquivoId));
    }

    private Path caminho(Integer imovelId, String arquivoId) {
        if (imovelId == null || imovelId < 1) {
            throw new FileStorageException("Referência de documento inválida.");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(arquivoId);
            if (!uuid.toString().equals(arquivoId)) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new DocumentoImovelNotFoundException("Arquivo do documento não encontrado.");
        }

        Path arquivo = rootLocation.resolve("documentos-imovel")
                .resolve(imovelId.toString()).resolve(uuid.toString()).normalize();
        if (!arquivo.startsWith(rootLocation)) {
            throw new DocumentoImovelNotFoundException("Arquivo do documento não encontrado.");
        }
        return arquivo;
    }

    private void tentarExcluir(Path arquivo) {
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException exception) {
            throw new FileStorageException("Não foi possível excluir o arquivo do documento.");
        }
    }
}
