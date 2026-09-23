package com.br.meuimovel.service;

import com.br.meuimovel.dtos.autenticacao.AutenticacaoDto;
import com.br.meuimovel.dtos.autenticacao.RegisterDto;
import com.br.meuimovel.dtos.autenticacao.TokenDto;
import com.br.meuimovel.dtos.autenticacao.UsuarioResponseDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.UsuarioAlreadyExistsException;
import com.br.meuimovel.exception.UsuarioNaoPodePossuirEmpresaException;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.UsuarioRepository;
import com.br.meuimovel.security.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AutenticacaoService{
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager manager;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaService empresaService;

    public TokenDto login(AutenticacaoDto authDto) {

        var userPassword =
                new UsernamePasswordAuthenticationToken(
                        authDto.email(),
                        authDto.senha()
                );

        Authentication authenticate =
                manager.authenticate(userPassword);
        //verificar login e possíveis exceptions a serem lancadas

        Usuario usuario = (Usuario) authenticate.getPrincipal();
        String token = jwtService.generateToken(usuario);

        return new TokenDto(token);
    }

    @Transactional
    public UsuarioResponseDto register(RegisterDto dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new UsuarioAlreadyExistsException(
                    "Usuário já cadastrado"
            );
        }

        Empresa empresa = null;

        if (dto.perfil() != UsuarioPerfil.ADMINISTRADOR) {
            empresa = empresaService.getById(dto.empresaId());
        }

        String encodedSenha = passwordEncoder.encode(dto.senha());
        Usuario usuario = usuarioRepository.save(
                new Usuario(
                        empresa,
                        dto.nome(),
                        dto.email(),
                        encodedSenha,
                        dto.perfil(),
                        UsuarioStatus.ATIVO,
                        LocalDateTime.now()
                )
        );

        return UsuarioResponseDto.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .empresa(usuario.getEmpresa())
                .perfil(usuario.getPerfil())
                .status(usuario.getStatus())
                .criadoEm(usuario.getCriadoEm())
                .build();
    }

    //criar metodo para mudar senha de um usuário...? api externa para enviar email?
    //criar um método, um userService talvez para alterar o status no caso ativar, inativar, bloquear usuarios
    //criar um métod para mudar o perfil do usuário? se ... apenas adminstradores fazem mudanças.
}

