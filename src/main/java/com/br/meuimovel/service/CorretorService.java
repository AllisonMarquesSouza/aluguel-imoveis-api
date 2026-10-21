package com.br.meuimovel.service;

import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import com.br.meuimovel.dtos.corretor.CorretorPublicoResponseDto;
import com.br.meuimovel.dtos.corretor.CorretorResponseDto;
import com.br.meuimovel.dtos.empresa.EmpresaPublicaResponseDto;
import com.br.meuimovel.enums.TipoEmpresa;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CorretorNotFoundException;
import com.br.meuimovel.exception.CorretorStatusAlreadyModifiedException;
import com.br.meuimovel.exception.CreciAlreadyExistsException;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.exception.UsuarioAlreadyExistsException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CorretorRepository;
import com.br.meuimovel.repository.UsuarioRepository;
import com.br.meuimovel.validator.FotoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CorretorService {
    private final UsuarioRepository usuarioRepository;
    private final CorretorRepository corretorRepository;
    private final PasswordEncoder passwordEncoder;
    private final FotoValidator fotoValidator;
    private final FileStorageService fileStorageService;

    private Usuario getGestorAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario gestor)
                || gestor.getPerfil() != UsuarioPerfil.GESTOR
                || gestor.getStatus() != UsuarioStatus.ATIVO
                || gestor.getEmpresa() == null) {
            throw new UnauthorizedOperationException("Para processar essa operacao" +
                    " precisa ser um gestor e estar ativo!");
        }
        return gestor;
    }

    @Transactional
    public CorretorResponseDto create(CorretorCreateDto createDto, MultipartFile foto) {
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

        fotoValidator.validar(foto);
        String encodedSenha = passwordEncoder.encode(createDto.senha());

        Usuario usuario = usuarioRepository
                .save(new Usuario(gestor.getEmpresa(), createDto.nome(), createDto.email(),
                        encodedSenha, UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, LocalDateTime.now()
                ));
        String fotoUrl =
                fileStorageService.salvarFotoCorretor(
                        foto,
                        usuario.getId()
                );
        Corretor corretor = corretorRepository.save(new Corretor
                (usuario, fotoUrl, createDto.creci(), createDto.telefone(),
                        createDto.whatsapp(), createDto.apresentacao()));
        //ajustar a verificacao caso salvar um corretor falhe, a imagem ainda estaria salva
        //logo, dar um rollback na imagem tambem, o transactional nao funciona para ela.

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
    public void ativar(Integer corretorId) {
        Usuario gestor = getGestorAutenticado();
        Empresa empresa = gestor.getEmpresa();
        Corretor corretor = getCorretorDaEmpresa(corretorId, empresa.getId());
        if (corretor.getUsuario().getStatus() != UsuarioStatus.INATIVO) {
            throw new CorretorStatusAlreadyModifiedException("Somente corretores inativos podem ser ativados.");
        }
        corretor.getUsuario().setStatus(UsuarioStatus.ATIVO);
        corretorRepository.save(corretor);
    }

    @Transactional
    public void desativar(Integer corretorId) {
        Usuario gestor = getGestorAutenticado();
        Empresa empresa = gestor.getEmpresa();
        Corretor corretor = getCorretorDaEmpresa(corretorId, empresa.getId());
        if (corretor.getUsuario().getStatus() != UsuarioStatus.ATIVO) {
            throw new CorretorStatusAlreadyModifiedException("Somente corretores ativos podem ser desativados.");
        }
        corretor.getUsuario().setStatus(UsuarioStatus.INATIVO);
        corretorRepository.save(corretor);
    }

    @Transactional(readOnly = true)
    public List<CorretorPublicoResponseDto> listarPublicos() {
        return corretorRepository.findDistinctByUsuario_StatusOrderByUsuario_NomeAsc(UsuarioStatus.ATIVO)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CorretorPublicoResponseDto getPerfilPublico(Integer corretorId) {
        Corretor corretor = corretorRepository.findByIdAndUsuario_Status(corretorId, UsuarioStatus.ATIVO)
                .orElseThrow(() -> new CorretorNotFoundException("Corretor não encontrado."));
        return toPublicResponse(corretor);
    }

    private Corretor getCorretorDaEmpresa(Integer corretorId, Integer empresaId) {
        return corretorRepository.findByIdAndUsuario_Empresa_Id(corretorId, empresaId)
                .orElseThrow(() -> new CorretorNotFoundException("Corretor não encontrado nesta empresa."));
    }

    private CorretorPublicoResponseDto toPublicResponse(Corretor corretor) {
        Usuario usuario = corretor.getUsuario();
        Empresa empresa = usuario.getEmpresa();
        return new CorretorPublicoResponseDto(
                corretor.getId(), usuario.getNome(), corretor.getCreci(), corretor.getFotoUrl(),
                corretor.getTelefone(), corretor.getWhatsapp(), usuario.getEmail(), corretor.getApresentacao(),
                empresa == null ? null : toEmpresaPublica(empresa)
        );
    }

    private EmpresaPublicaResponseDto toEmpresaPublica(Empresa empresa) {
        CidadeResponseDto cidadeBase = empresa.getCidadeBase() == null
                ? null
                : toCidadeResponse(empresa.getCidadeBase());
        List<CidadeResponseDto> cidadesAtuacao = empresa.getCidadesAtuacao() == null
                ? List.of()
                : empresa.getCidadesAtuacao().stream().map(this::toCidadeResponse).toList();
        return new EmpresaPublicaResponseDto(
                empresa.getId(), empresa.getTipo(), empresa.getNome(), empresa.getRazaoSocial(), empresa.getCreci(),
                empresa.getLogoMarcaUrl(), empresa.getDescricao(), empresa.getTelefone(), empresa.getWhatsapp(),
                empresa.getEmail(), empresa.getLogradouro(), empresa.getNumero(), empresa.getBairro(), empresa.getCep(),
                cidadeBase, cidadesAtuacao
        );
    }

    private CidadeResponseDto toCidadeResponse(Cidade cidade) {
        return new CidadeResponseDto(cidade.getId(), cidade.getNome(), cidade.getUf().name());
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
