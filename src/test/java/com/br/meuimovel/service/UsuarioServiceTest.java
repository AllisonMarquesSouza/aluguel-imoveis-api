package com.br.meuimovel.service;

import com.br.meuimovel.dtos.usuario.UsuarioSituacaoUpdateDto;
import com.br.meuimovel.dtos.usuario.UsuarioSenhaUpdateDto;
import com.br.meuimovel.dtos.usuario.UsuarioUpdateDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.exception.SenhaInvalidaException;
import com.br.meuimovel.exception.UsuarioNotFoundException;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioService usuarioService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerListsOnlyUsersFromAuthenticatedCompany() {
        autenticar(usuario(5, UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, 7));
        Usuario corretor = usuario(12, UsuarioPerfil.CORRETOR, UsuarioStatus.INATIVO, 7);
        when(usuarioRepository.findAllByEmpresa_IdOrderByNomeAsc(7)).thenReturn(List.of(corretor));

        var response = usuarioService.listar();

        assertEquals(1, response.size());
        assertEquals(12, response.get(0).id());
        assertEquals(7, response.get(0).empresaId());
        assertEquals(UsuarioStatus.INATIVO, response.get(0).status());
        verify(usuarioRepository).findAllByEmpresa_IdOrderByNomeAsc(7);
        verify(usuarioRepository, never()).findAllByOrderByNomeAsc();
    }

    @Test
    void managerCannotReadUserFromAnotherCompany() {
        autenticar(usuario(5, UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, 7));
        when(usuarioRepository.findByIdAndEmpresa_Id(12, 7)).thenReturn(Optional.empty());

        assertThrows(UsuarioNotFoundException.class, () -> usuarioService.buscar(12));

        verify(usuarioRepository).findByIdAndEmpresa_Id(12, 7);
        verify(usuarioRepository, never()).findById(12);
    }

    @Test
    void administratorCanListAllUsers() {
        autenticar(usuario(1, UsuarioPerfil.ADMINISTRADOR, UsuarioStatus.ATIVO, null));
        when(usuarioRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(
                usuario(5, UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, 7)));

        var response = usuarioService.listar();

        assertEquals(1, response.size());
        assertEquals(7, response.get(0).empresaId());
        verify(usuarioRepository).findAllByOrderByNomeAsc();
    }

    @Test
    void updateChangesOnlyNameAndEmailAndChecksEmailUniqueness() {
        Usuario solicitante = usuario(5, UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, 7);
        autenticar(solicitante);
        Usuario alvo = usuario(12, UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, 7);
        when(usuarioRepository.findByIdAndEmpresa_Id(12, 7)).thenReturn(Optional.of(alvo));
        when(usuarioRepository.existsByEmailAndIdNot("novo@example.com", 12)).thenReturn(false);
        when(usuarioRepository.save(alvo)).thenReturn(alvo);

        var response = usuarioService.atualizar(12, new UsuarioUpdateDto("  Ana Souza  ", "novo@example.com"));

        assertEquals("Ana Souza", response.nome());
        assertEquals("novo@example.com", response.email());
        assertEquals(UsuarioPerfil.CORRETOR, response.perfil());
        assertEquals(7, response.empresaId());
        verify(usuarioRepository).save(alvo);
    }

    @Test
    void managerCanChangeSituationOnlyWithinOwnCompany() {
        autenticar(usuario(5, UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, 7));
        Usuario alvo = usuario(12, UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, 7);
        when(usuarioRepository.findByIdAndEmpresa_Id(12, 7)).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(alvo)).thenReturn(alvo);

        var response = usuarioService.atualizarSituacao(12, new UsuarioSituacaoUpdateDto(UsuarioStatus.BLOQUEADO));

        assertEquals(UsuarioStatus.BLOQUEADO, response.status());
        assertFalse(alvo.isEnabled());
        verify(usuarioRepository).save(alvo);
    }

    @Test
    void inactiveManagerCannotManageUsers() {
        autenticar(usuario(5, UsuarioPerfil.GESTOR, UsuarioStatus.INATIVO, 7));

        assertThrows(UnauthorizedOperationException.class, usuarioService::listar);

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void brokerCannotManageUsers() {
        autenticar(usuario(5, UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, 7));

        assertThrows(UnauthorizedOperationException.class, usuarioService::listar);

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void activeUserChangesOwnPasswordAfterVerifyingCurrentPassword() {
        Usuario usuario = usuario(9, UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, 7);
        usuario.setSenha("hash-antiga");
        autenticar(usuario);
        when(usuarioRepository.findById(9)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-atual-segura", "hash-antiga")).thenReturn(true);
        when(passwordEncoder.matches("senha-nova-segura", "hash-antiga")).thenReturn(false);
        when(passwordEncoder.encode("senha-nova-segura")).thenReturn("hash-nova");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        usuarioService.atualizarSenha(new UsuarioSenhaUpdateDto(
                "senha-atual-segura", "senha-nova-segura", "senha-nova-segura"));

        assertEquals("hash-nova", usuario.getSenha());
        verify(passwordEncoder).encode("senha-nova-segura");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void passwordChangeRejectsIncorrectCurrentPassword() {
        Usuario usuario = usuario(9, UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, 7);
        usuario.setSenha("hash-atual");
        autenticar(usuario);
        when(usuarioRepository.findById(9)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-incorreta", "hash-atual")).thenReturn(false);

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> usuarioService.atualizarSenha(new UsuarioSenhaUpdateDto(
                        "senha-incorreta", "senha-nova-segura", "senha-nova-segura")));

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void passwordChangeRejectsConfirmationMismatch() {
        Usuario usuario = usuario(9, UsuarioPerfil.CLIENTE, UsuarioStatus.ATIVO, 7);
        autenticar(usuario);

        assertThrows(SenhaInvalidaException.class,
                () -> usuarioService.atualizarSenha(new UsuarioSenhaUpdateDto(
                        "senha-atual-segura", "senha-nova-segura", "outra-senha-segura")));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void passwordChangeRejectsMissingOrTooShortData() {
        autenticar(usuario(9, UsuarioPerfil.CLIENTE, UsuarioStatus.ATIVO, 7));

        assertThrows(SenhaInvalidaException.class, () -> usuarioService.atualizarSenha(null));
        assertThrows(SenhaInvalidaException.class, () -> usuarioService.atualizarSenha(
                new UsuarioSenhaUpdateDto("senha-atual", "curta", "curta")));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void passwordChangeRejectsPasswordAboveBcryptUtf8ByteLimit() {
        autenticar(usuario(9, UsuarioPerfil.CLIENTE, UsuarioStatus.ATIVO, 7));
        String senhaAcimaDoLimiteEmBytes = "é".repeat(37);

        assertThrows(SenhaInvalidaException.class, () -> usuarioService.atualizarSenha(
                new UsuarioSenhaUpdateDto("senha-atual-segura", senhaAcimaDoLimiteEmBytes,
                        senhaAcimaDoLimiteEmBytes)));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void passwordChangeRejectsReusingCurrentPassword() {
        Usuario usuario = usuario(9, UsuarioPerfil.CLIENTE, UsuarioStatus.ATIVO, 7);
        usuario.setSenha("hash-atual");
        autenticar(usuario);
        when(usuarioRepository.findById(9)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-atual-segura", "hash-atual")).thenReturn(true);

        assertThrows(SenhaInvalidaException.class,
                () -> usuarioService.atualizarSenha(new UsuarioSenhaUpdateDto(
                        "senha-atual-segura", "senha-atual-segura", "senha-atual-segura")));

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void passwordChangeRejectsAccountThatBecameInactive() {
        Usuario principal = usuario(9, UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, 7);
        autenticar(principal);
        Usuario usuarioInativo = usuario(9, UsuarioPerfil.GESTOR, UsuarioStatus.INATIVO, 7);
        when(usuarioRepository.findById(9)).thenReturn(Optional.of(usuarioInativo));

        assertThrows(UnauthorizedOperationException.class,
                () -> usuarioService.atualizarSenha(new UsuarioSenhaUpdateDto(
                        "senha-atual-segura", "senha-nova-segura", "senha-nova-segura")));

        verifyNoInteractions(passwordEncoder);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private Usuario usuario(Integer id, UsuarioPerfil perfil, UsuarioStatus status, Integer empresaId) {
        Empresa empresa = empresaId == null ? null : Empresa.builder().id(empresaId).build();
        Usuario usuario = new Usuario(empresa, "Usuário " + id, "usuario" + id + "@example.com", "hash",
                perfil, status, LocalDateTime.now());
        usuario.setId(id);
        return usuario;
    }
}
