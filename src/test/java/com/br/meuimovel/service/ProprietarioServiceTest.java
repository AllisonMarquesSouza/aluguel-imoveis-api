package com.br.meuimovel.service;

import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.ProprietarioRepository;
import com.br.meuimovel.validator.DocumentoValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProprietarioServiceTest {
    @Mock private ProprietarioRepository proprietarioRepository;
    @Mock private DocumentoValidator documentoValidator;
    @InjectMocks private ProprietarioService proprietarioService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nameSearchUsesAuthenticatedCompany() {
        autenticarCorretor(7);
        when(proprietarioRepository.findByEmpresaIdAndNomeContainingIgnoreCase(7, "Ana"))
                .thenReturn(List.of());

        assertTrue(proprietarioService.listByNome("Ana").isEmpty());

        verify(proprietarioRepository).findByEmpresaIdAndNomeContainingIgnoreCase(7, "Ana");
    }

    @Test
    void documentSearchUsesAuthenticatedCompany() {
        autenticarCorretor(12);
        when(documentoValidator.validar(TipoDocumento.CPF, "12345678901")).thenReturn("12345678901");
        when(proprietarioRepository.findByEmpresaIdAndDocumento(12, "12345678901"))
                .thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                com.br.meuimovel.exception.ProprietarioNotFoundException.class,
                () -> proprietarioService.getByDocumento(TipoDocumento.CPF, "12345678901")
        );

        verify(proprietarioRepository).findByEmpresaIdAndDocumento(12, "12345678901");
    }

    private void autenticarCorretor(Integer empresaId) {
        Empresa empresa = Empresa.builder().id(empresaId).build();
        Usuario usuario = new Usuario(empresa, "Corretor", "corretor@example.com", "hash",
                UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, LocalDateTime.now());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }
}
