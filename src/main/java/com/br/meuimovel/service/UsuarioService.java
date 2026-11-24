package com.br.meuimovel.service;

import com.br.meuimovel.dtos.usuario.UsuarioResponseDto;
import com.br.meuimovel.dtos.usuario.UsuarioSenhaUpdateDto;
import com.br.meuimovel.dtos.usuario.UsuarioSituacaoUpdateDto;
import com.br.meuimovel.dtos.usuario.UsuarioUpdateDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.SenhaInvalidaException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.exception.UsuarioAlreadyExistsException;
import com.br.meuimovel.exception.UsuarioNotFoundException;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {
    private static final int LIMITE_SENHA_BCRYPT_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = usuarioRepository.findByEmail(username);
        if(user == null){
            throw new UsuarioNotFoundException("Usuario nao encontrado");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listar() {
        Usuario solicitante = getAdministradorOuGestorAutenticado();
        List<Usuario> usuarios = solicitante.getPerfil() == UsuarioPerfil.ADMINISTRADOR
                ? usuarioRepository.findAllByOrderByNomeAsc()
                : usuarioRepository.findAllByEmpresa_IdOrderByNomeAsc(solicitante.getEmpresa().getId());
        return usuarios.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto buscar(Integer id) {
        Usuario solicitante = getAdministradorOuGestorAutenticado();
        return toResponse(buscarUsuario(id, solicitante));
    }

    @Transactional
    public UsuarioResponseDto atualizar(Integer id, UsuarioUpdateDto dto) {
        Usuario solicitante = getAdministradorOuGestorAutenticado();
        Usuario usuario = buscarUsuario(id, solicitante);
        String email = dto.email().trim();
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw new UsuarioAlreadyExistsException("Já existe um usuário com esse e-mail.");
        }
        usuario.setNome(dto.nome().trim());
        usuario.setEmail(email);
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDto atualizarSituacao(Integer id, UsuarioSituacaoUpdateDto dto) {
        Usuario solicitante = getAdministradorOuGestorAutenticado();
        Usuario usuario = buscarUsuario(id, solicitante);
        usuario.setStatus(dto.status());
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void atualizarSenha(UsuarioSenhaUpdateDto dto) {
        Usuario principal = getUsuarioAutenticado();
        if (dto == null || dto.senhaAtual() == null || dto.senhaAtual().isBlank()
                || excedeLimiteBcrypt(dto.senhaAtual())
                || dto.novaSenha() == null || dto.novaSenha().length() < 12 || dto.novaSenha().length() > 255
                || excedeLimiteBcrypt(dto.novaSenha())
                || dto.confirmacaoNovaSenha() == null || dto.confirmacaoNovaSenha().length() < 12
                || dto.confirmacaoNovaSenha().length() > 255 || excedeLimiteBcrypt(dto.confirmacaoNovaSenha())) {
            throw new SenhaInvalidaException(
                    "Informe uma senha nova de 12 a 255 caracteres, com no máximo 72 bytes em UTF-8.");
        }
        if (!dto.novaSenha().equals(dto.confirmacaoNovaSenha())) {
            throw new SenhaInvalidaException("A confirmação da nova senha não confere.");
        }

        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário autenticado não encontrado."));
        if (!usuario.isEnabled()) {
            throw new UnauthorizedOperationException("A conta precisa estar ativa para alterar a senha.");
        }
        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new BadCredentialsException("Senha atual inválida.");
        }
        if (passwordEncoder.matches(dto.novaSenha(), usuario.getSenha())) {
            throw new SenhaInvalidaException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    private boolean excedeLimiteBcrypt(String senha) {
        return senha.getBytes(StandardCharsets.UTF_8).length > LIMITE_SENHA_BCRYPT_BYTES;
    }

    private Usuario buscarUsuario(Integer id, Usuario solicitante) {
        if (solicitante.getPerfil() == UsuarioPerfil.ADMINISTRADOR) {
            return usuarioRepository.findById(id)
                    .orElseThrow(() -> new UsuarioNotFoundException("Usuário não encontrado."));
        }
        return usuarioRepository.findByIdAndEmpresa_Id(id, solicitante.getEmpresa().getId())
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário não encontrado nesta empresa."));
    }

    private Usuario getAdministradorOuGestorAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)
                || !usuario.isEnabled()) {
            throw new UnauthorizedOperationException("É necessário estar autenticado com um usuário ativo.");
        }
        if (usuario.getPerfil() == UsuarioPerfil.ADMINISTRADOR) {
            return usuario;
        }
        if (usuario.getPerfil() == UsuarioPerfil.GESTOR && usuario.getEmpresa() != null) {
            return usuario;
        }
        throw new UnauthorizedOperationException("A operação exige perfil de administrador ou gestor.");
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)
                || !usuario.isEnabled()) {
            throw new UnauthorizedOperationException("É necessário estar autenticado com um usuário ativo.");
        }
        return usuario;
    }

    private UsuarioResponseDto toResponse(Usuario usuario) {
        return new UsuarioResponseDto(
                usuario.getId(),
                usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId(),
                usuario.getNome(), usuario.getEmail(), usuario.getPerfil(), usuario.getStatus(), usuario.getCriadoEm());
    }
}
