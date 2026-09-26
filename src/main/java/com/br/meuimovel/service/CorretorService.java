package com.br.meuimovel.service;

import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import com.br.meuimovel.dtos.corretor.CorretorResponseDto;
import com.br.meuimovel.enums.TipoEmpresa;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CreciAlreadyExistsException;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.exception.UsuarioAlreadyExistsException;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.UsuarioRepository;
import com.br.meuimovel.repository.CorretorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CorretorService {
    private final UsuarioRepository usuarioRepository;
    private final CorretorRepository corretorRepository;
    private final PasswordEncoder passwordEncoder;

    private Usuario getGestorAutenticado(){
        Usuario gestor = (Usuario) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (gestor.getPerfil() != UsuarioPerfil.GESTOR || gestor.getStatus() != UsuarioStatus.ATIVO) {
            throw new UnauthorizedOperationException("Para processar essa operacao" +
                    " precisa ser um gestor e estar ativo!");
        }

        return gestor;
    }
    @Transactional
    public CorretorResponseDto create(CorretorCreateDto createDto) {
        Usuario gestor = getGestorAutenticado();

        if (!gestor.getEmpresa().isAtiva()) {
            throw new EmpresaInativaException("Empresa deve estar ativa para cadastrar corretor!");
        }
        if(gestor.getEmpresa().getTipo() == TipoEmpresa.AUTONOMO){
            throw new UnauthorizedOperationException
                    ("Empresa autonoma nao pode ter corretores, o único corretor é o dono!");
        }
        if(corretorRepository.existsByCreci(createDto.creci())){
            throw new CreciAlreadyExistsException("Corretor com esse creci ja existe, verifique novamente!");
        }
        if (usuarioRepository.existsByEmail(createDto.email())) {
            throw new UsuarioAlreadyExistsException("Usuário já cadastrado!");
        }

        String encodedSenha = passwordEncoder.encode(createDto.senha());

        Usuario usuario = usuarioRepository
                .save(new Usuario(gestor.getEmpresa(), createDto.nome(), createDto.email(),
                        encodedSenha, UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, LocalDateTime.now()
                ));
        Corretor corretor = corretorRepository.save(new Corretor
                (usuario, createDto.fotoUrl(), createDto.creci(), createDto.telefone(),
                        createDto.whatsapp(), createDto.apresentacao()));
        //validar fotoUrl, por tipo e tamanho antes de gravar?
        //perguntar e heldon sobre essa validaçao

        return CorretorResponseDto.builder()
                .id(corretor.getId())
                .nome(usuario.getNome())
                .creci(corretor.getCreci())
                .fotoUrl(corretor.getFotoUrl())
                .telefone(corretor.getTelefone())
                .whatsapp(corretor.getWhatsapp())
                .email(usuario.getEmail())
                .apresentacao(corretor.getApresentacao())
                .build();
    }

    @Transactional
    public void ativar(Integer corretorId){
        Usuario gestor = getGestorAutenticado();
        Empresa empresa = gestor.getEmpresa();
        Corretor corretor = corretorRepository.findByIdAndUsuario_Empresa_Id(corretorId, empresa.getId())
                .orElseThrow(() -> new EntityNotFoundException("Corretor nao encontrado nessa empresa"));

        corretor.getUsuario().setStatus(UsuarioStatus.ATIVO);
        corretorRepository.save(corretor);
    }

    @Transactional
    public void desativar(Integer corretorId){
        //criar método em userService para pegar usuário logado.
        Usuario gestor = getGestorAutenticado();
        Empresa empresa = gestor.getEmpresa();
        Corretor corretor = corretorRepository.findByIdAndUsuario_Empresa_Id(corretorId, empresa.getId())
                .orElseThrow(() -> new EntityNotFoundException("Corretor nao encontrado nessa empresa"));

        corretor.getUsuario().setStatus(UsuarioStatus.INATIVO);
        corretorRepository.save(corretor);
    }
}

//create()
// │
// ├── obtém gestor autenticado
// ├── valida gestor
// ├── valida empresa
// ├── valida tipo da empresa
// ├── verifica email
// ├── verifica CRECI
// ├── cria usuário
// ├── cria corretor
// └── monta resposta