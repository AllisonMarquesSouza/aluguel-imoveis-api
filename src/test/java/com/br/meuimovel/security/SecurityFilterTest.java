package com.br.meuimovel.security;

import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {
    @Mock private JWTService jwtService;
    @Mock private UsuarioRepository usuarioRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledUserTokenDoesNotAuthenticateRequest() throws Exception {
        Usuario usuario = new Usuario(Empresa.builder().id(7).build(), "Gestor", "gestor@example.com", "hash",
                UsuarioPerfil.GESTOR, UsuarioStatus.BLOQUEADO, LocalDateTime.now());
        when(jwtService.validateToken("token")).thenReturn("gestor@example.com");
        when(usuarioRepository.findByEmail("gestor@example.com")).thenReturn(usuario);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        new SecurityFilter(jwtService, usuarioRepository).doFilter(
                request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) ->
                        assertNull(SecurityContextHolder.getContext().getAuthentication()));

        verify(usuarioRepository).findByEmail("gestor@example.com");
    }
}
