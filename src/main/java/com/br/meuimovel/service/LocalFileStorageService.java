package com.br.meuimovel.service;

import com.br.meuimovel.exception.FileStorageException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation = Paths.get("uploads");

    @Override
    public String salvarLogoEmpresa(
            MultipartFile arquivo,
            Integer empresaId
    ) {

        return salvar(
                arquivo,
                "empresas/" + empresaId,
                "logomarca"
        );
    }

    @Override
    public String salvarFotoCorretor(
            MultipartFile arquivo,
            Integer usuarioId
    ) {

        return salvar(
                arquivo,
                "corretores/" + usuarioId,
                "foto"
        );
    }

    private String salvar(
            MultipartFile arquivo,
            String diretorio,
            String nomeArquivo
    ) {

        try {

            Path diretorioDestino =
                    rootLocation.resolve(diretorio);

            Files.createDirectories(diretorioDestino);

            String extensao = obterExtensao(arquivo);

            Path caminhoArquivo =
                    diretorioDestino.resolve(
                            nomeArquivo + extensao
                    );

            arquivo.transferTo(caminhoArquivo);

            return "/" + rootLocation
                    .resolve(diretorio)
                    .resolve(nomeArquivo + extensao)
                    .toString()
                    .replace("\\", "/");

        } catch (IOException e) {

            throw new FileStorageException(
                    "Não foi possível salvar o arquivo."
            );
        }
    }

    private String obterExtensao(MultipartFile arquivo) {

        String nomeOriginal =
                arquivo.getOriginalFilename();

        if (nomeOriginal == null ||
                !nomeOriginal.contains(".")) {

            throw new FileStorageException(
                    "O arquivo não possui uma extensão válida."
            );
        }

        return nomeOriginal.substring(
                nomeOriginal.lastIndexOf(".")
        ).toLowerCase();
    }

    @Override
    public void deletar(String caminho) {

        try {

            Path arquivo = Paths.get(
                    caminho.substring(1)
            );

            Files.deleteIfExists(arquivo);

        } catch (IOException e) {

            throw new FileStorageException(
                    "Não foi possível excluir o arquivo."
            );
        }
    }
}