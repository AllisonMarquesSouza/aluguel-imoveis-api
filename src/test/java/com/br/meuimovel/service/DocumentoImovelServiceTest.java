package com.br.meuimovel.service;

import com.br.meuimovel.enums.TipoDocumentoImovel;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.DocumentoImovelAlreadyExistsException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.model.DocumentoImovel;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.DocumentoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.storage.DocumentoStorageService;
import com.br.meuimovel.validator.DocumentoImovelValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoImovelServiceTest {
    @Mock private ImovelRepository imovelRepository;
    @Mock private DocumentoImovelRepository documentoRepository;
    @Mock private DocumentoStorageService documentoStorageService;
    @Mock private DocumentoImovelValidator documentoValidator;
    @InjectMocks private DocumentoImovelService documentoImovelService;

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adicionarDocumentoSalvaArquivoEData() {
        Usuario gestor = autenticarGestor();
        when(imovelRepository.findByIdAndEmpresa_Id(12, 7)).thenReturn(Optional.of(imovel(gestor.getEmpresa())));
        when(documentoRepository.existsByImovel_IdAndTipo(12, TipoDocumentoImovel.MATRICULA)).thenReturn(false);
        when(documentoStorageService.salvar(any(), eq(12))).thenReturn("369fd49e-c6e3-45b7-a873-4a8c4635539f");
        doNothing().when(documentoValidator).validar(any());
        when(documentoValidator.nomeArquivo(any())).thenReturn("matricula.pdf");
        when(documentoRepository.save(any(DocumentoImovel.class))).thenAnswer(invocation -> {
            DocumentoImovel documento = invocation.getArgument(0);
            documento.setId(33);
            return documento;
        });

        var response = documentoImovelService.adicionar(12, TipoDocumentoImovel.MATRICULA,
                LocalDate.of(2026, 3, 1), pdf());

        assertEquals(33, response.id());
        assertEquals(TipoDocumentoImovel.MATRICULA, response.tipo());
        assertEquals("matricula.pdf", response.nomeArquivo());
        assertEquals(LocalDate.of(2026, 3, 1), response.dataDocumento());
        assertEquals("/imovel/12/documentos/33/arquivo", response.urlArquivo());
        verify(documentoStorageService).salvar(any(), eq(12));
    }

    @Test
    void rejeitaSegundoDocumentoDoMesmoTipo() {
        Usuario gestor = autenticarGestor();
        when(imovelRepository.findByIdAndEmpresa_Id(12, 7)).thenReturn(Optional.of(imovel(gestor.getEmpresa())));
        when(documentoRepository.existsByImovel_IdAndTipo(12, TipoDocumentoImovel.MATRICULA)).thenReturn(true);
        doNothing().when(documentoValidator).validar(any());

        assertThrows(DocumentoImovelAlreadyExistsException.class,
                () -> documentoImovelService.adicionar(12, TipoDocumentoImovel.MATRICULA,
                        LocalDate.now(), pdf()));

        verifyNoInteractions(documentoStorageService);
        verify(documentoRepository, never()).save(any(DocumentoImovel.class));
    }

    @Test
    void impedeDownloadDeDocumentoDeOutraEmpresa() {
        Usuario gestor = autenticarGestor();
        when(imovelRepository.findByIdAndEmpresa_Id(12, gestor.getEmpresa().getId()))
                .thenReturn(Optional.empty());

        assertThrows(ImovelNotFoundException.class, () -> documentoImovelService.buscarArquivo(12, 33));
        verifyNoInteractions(documentoRepository, documentoStorageService);
    }

    private Usuario autenticarGestor() {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario gestor = new Usuario(empresa, "Gestor", "gestor@example.com", "senha",
                UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, LocalDateTime.now());
        gestor.setId(9);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(gestor, null, gestor.getAuthorities()));
        return gestor;
    }

    private Imovel imovel(Empresa empresa) {
        return Imovel.builder().id(12).empresa(empresa).build();
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("arquivo", "matricula.pdf", "application/pdf",
                "%PDF-1.7 documento de teste".getBytes());
    }
}
