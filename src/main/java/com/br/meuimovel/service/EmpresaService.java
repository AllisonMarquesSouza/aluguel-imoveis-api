package com.br.meuimovel.service;

import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.dtos.empresa.EmpresaAutonomaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaImobiliariaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaResponseDto;
import com.br.meuimovel.dtos.empresa.EmpresaUpdateDto;
import com.br.meuimovel.enums.TipoEmpresa;
import com.br.meuimovel.enums.TipoImagem;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.*;
import com.br.meuimovel.model.*;
import com.br.meuimovel.repository.CorretorRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.GestorRepository;
import com.br.meuimovel.repository.UsuarioRepository;
import com.br.meuimovel.storage.FileStorageService;
import com.br.meuimovel.storage.ImagemUrl;
import com.br.meuimovel.validator.DocumentoValidator;
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
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmpresaService {
    private final EmpresaRepository empresaRepository;
    private final CidadeService cidadeService;
    private final DocumentoValidator documentoValidator;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final CorretorRepository corretorRepository;
    private final GestorRepository gestorRepository;
    private final FotoValidator fotoValidator;
    private final FileStorageService fileStorageService;

    public Empresa getById(Integer id) {
        Usuario usuario = getUsuarioAutenticado();

        if (usuario.getPerfil() != UsuarioPerfil.ADMINISTRADOR) {
            Integer empresaId = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId();
            if (!Objects.equals(id, empresaId)) {
                throw new EmpresaNotFoundException("Empresa não encontrada!");
            }
        }

        return empresaRepository.findById(id)
                .orElseThrow(() -> new EmpresaNotFoundException("Empresa não encontrada!"));
    }

    @Transactional(readOnly = true)
    public EmpresaResponseDto getResponseById(Integer id) {
        return toResponse(getById(id));
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponseDto> listAll() {
        getAdministradorAutenticado();
        return empresaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private Empresa criarEmpresa(String documento, EmpresaCreateDto empresa, TipoEmpresa tipoEmpresa) {
        Cidade cidadeBase = empresa.cidadeBaseId() == null
                ? null
                : cidadeService.getById(empresa.cidadeBaseId());

        Set<Cidade> cidades = cidadeService.getAllById(empresa.cidadesAtuacaoIds());

        return empresaRepository.save(Empresa.builder()
                .tipo(tipoEmpresa).nome(empresa.nome()).razaoSocial(empresa.razaoSocial())
                .tipoDocumento(empresa.tipoDocumento())
                .documento(documento).creci(empresa.creci())
                .descricao(empresa.descricao())
                .telefone(empresa.telefone()).whatsapp(empresa.whatsapp())
                .email(empresa.email()).logradouro(empresa.logradouro())
                .numero(empresa.numero()).bairro(empresa.bairro())
                .cep(empresa.cep()).cidadeBase(cidadeBase).cidadesAtuacao(cidades)
                .ativa(true).criadoEm(LocalDateTime.now()).build());
    }


    @Transactional
    public EmpresaResponseDto createImobiliaria(EmpresaImobiliariaCreateDto createDto, MultipartFile logoImg) {
        getAdministradorAutenticado();
        String documento = documentoValidator.validar(
                createDto.empresa().tipoDocumento(),
                createDto.empresa().documento()
        );
        if (empresaRepository.existsByDocumento(documento)) {
            throw new EmpresaAlreadyExistsException("Empresa com esse documento já existe, cheque o CNPJ OU CPF");
        }
        if (usuarioRepository.existsByEmail(createDto.emailGestor())) {
            throw new UsuarioAlreadyExistsException(
                    "Email já cadastrado, cheque novamente!"
            );
        }
        if(empresaRepository.existsByCreci(createDto.empresa().creci())){
            throw new CreciAlreadyExistsException(
                    "Creci já foi cadastrado, cheque novamente!"
            );
        }

        fotoValidator.validar(logoImg);
        String encodedSenha = passwordEncoder.encode(createDto.senhaGestor());
        Empresa empresa = criarEmpresa(documento, createDto.empresa(), TipoEmpresa.IMOBILIARIA);
        String fotoUrl =
                fileStorageService.salvarLogoEmpresa(
                        logoImg,
                        empresa.getId()
                );
        empresa.setLogoMarcaUrl(fotoUrl);

        Usuario usuario = usuarioRepository.save(
                new Usuario(
                        empresa,
                        createDto.nomeGestor(),
                        createDto.emailGestor(),
                        encodedSenha,
                        UsuarioPerfil.GESTOR,
                        UsuarioStatus.ATIVO,
                        LocalDateTime.now()
                )
        );
        gestorRepository.save(new Gestor(usuario));
        return toResponse(empresa);
    }

    @Transactional
    public EmpresaResponseDto createAutonoma(EmpresaAutonomaCreateDto createDto, MultipartFile logoImg,
                                             MultipartFile fotoCorretor) {
        getAdministradorAutenticado();
        String documento = documentoValidator.validar(
                createDto.empresa().tipoDocumento(),
                createDto.empresa().documento()
        );
        if (empresaRepository.existsByDocumento(documento)) {
            throw new EmpresaAlreadyExistsException("Empresa com esse documento já existe, cheque o CNPJ OU CPF");
        }
        if (usuarioRepository.existsByEmail(createDto.corretor().email())) {
            throw new UsuarioAlreadyExistsException(
                    "Email já cadastrado, cheque novamente!"
            );
        }
        if(corretorRepository.existsByCreci(createDto.corretor().creci())){
            throw new CreciAlreadyExistsException(
                    "Creci já foi cadastrado, cheque novamente!"
            );
        }
        fotoValidator.validar(logoImg);
        fotoValidator.validar(fotoCorretor);
        String encodedSenha = passwordEncoder.encode(createDto.corretor().senha());
        Empresa empresa = criarEmpresa(documento, createDto.empresa(), TipoEmpresa.AUTONOMO);
        String logoUrlEmpresa =
                fileStorageService.salvarLogoEmpresa(
                        logoImg,
                        empresa.getId()
                );

        empresa.setLogoMarcaUrl(logoUrlEmpresa);

        Usuario usuario = usuarioRepository.save(
                new Usuario(
                        empresa,
                        createDto.corretor().nome(),
                        createDto.corretor().email(),
                        encodedSenha,
                        UsuarioPerfil.CORRETOR,
                        UsuarioStatus.ATIVO,
                        LocalDateTime.now()
                )
        );
        String fotoCorretorUrl =
                fileStorageService.salvarFotoCorretor(
                        fotoCorretor,
                        usuario.getId()
                );
        corretorRepository.save(new Corretor
                (usuario, fotoCorretorUrl, createDto.corretor().creci(), createDto.corretor().telefone(),
                        createDto.corretor().whatsapp(), createDto.corretor().apresentacao()));
        return toResponse(empresa);
    }


    @Transactional
    public void update(
            Integer id,
            EmpresaUpdateDto updateDto,
            MultipartFile logoImg
    ) {
        Empresa empresa = getById(id);

        if (updateDto.tipo() != null) {
            empresa.setTipo(updateDto.tipo());
        }

        if (updateDto.nome() != null) {
            empresa.setNome(updateDto.nome());
        }

        if (updateDto.razaoSocial() != null) {
            empresa.setRazaoSocial(updateDto.razaoSocial());
        }

        if (updateDto.tipoDocumento() != null) {
            empresa.setTipoDocumento(updateDto.tipoDocumento());
        }

        if (updateDto.documento() != null) {
            if (empresaRepository.existsByDocumento(updateDto.documento())
                    && !empresa.getDocumento().equals(updateDto.documento())) {

                throw new EmpresaAlreadyExistsException(
                        "Empresa com esse documento já existe, cheque o CNPJ OU CPF"
                );
            }

            empresa.setDocumento(updateDto.documento());
        }

        if (updateDto.creci() != null) {
            empresa.setCreci(updateDto.creci());
        }

        if (updateDto.descricao() != null) {
            empresa.setDescricao(updateDto.descricao());
        }

        if (updateDto.telefone() != null) {
            empresa.setTelefone(updateDto.telefone());
        }

        if (updateDto.whatsapp() != null) {
            empresa.setWhatsapp(updateDto.whatsapp());
        }

        if (updateDto.email() != null) {
            empresa.setEmail(updateDto.email());
        }

        if (updateDto.logradouro() != null) {
            empresa.setLogradouro(updateDto.logradouro());
        }

        if (updateDto.numero() != null) {
            empresa.setNumero(updateDto.numero());
        }

        if (updateDto.bairro() != null) {
            empresa.setBairro(updateDto.bairro());
        }

        if (updateDto.cep() != null) {
            empresa.setCep(updateDto.cep());
        }

        if (updateDto.cidadeBaseId() != null) {
            Cidade cidadeBase = cidadeService.getById(
                    updateDto.cidadeBaseId()
            );

            empresa.setCidadeBase(cidadeBase);
        }

        if (updateDto.cidadesAtuacaoIds() != null) {
            Set<Cidade> cidades = cidadeService.getAllById(
                    updateDto.cidadesAtuacaoIds()
            );

            empresa.getCidadesAtuacao().clear();
            empresa.getCidadesAtuacao().addAll(cidades);
        }

        String logoAnterior = null;
        String logoNova = null;
        if (logoImg != null && !logoImg.isEmpty()) {
            fotoValidator.validar(logoImg);

            logoAnterior = empresa.getLogoMarcaUrl();
            logoNova = fileStorageService.salvarLogoEmpresa(
                    logoImg,
                    empresa.getId()
            );

            empresa.setLogoMarcaUrl(logoNova);
        }

        try {
            empresaRepository.save(empresa);
        } catch (RuntimeException exception) {
            if (logoNova != null) {
                tentarExcluirArquivo(logoNova);
            }
            throw exception;
        }
        if (logoNova != null && logoAnterior != null && !logoAnterior.equals(logoNova)) {
            tentarExcluirArquivo(logoAnterior);
        }
    }

    @Transactional
    public void activate(Integer id) {
        getAdministradorAutenticado();
        Empresa empresa = getById(id);
        if (empresa.isAtiva()) throw new EmpresaStatusAlreadyModifiedException("Empresa ja ativa");
        empresa.setAtiva(true);
    }

    @Transactional
    public void inactivate(Integer id) {
        getAdministradorAutenticado();
        Empresa empresa = getById(id);
        if (!empresa.isAtiva()) throw new EmpresaStatusAlreadyModifiedException("Empresa ja desativada");
        empresa.setAtiva(false);
    }

    @Transactional
    public void deleteById(Integer id) {
        getAdministradorAutenticado();
        getById(id);
        empresaRepository.deleteById(id);
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new UnauthorizedOperationException("Usuário não autorizado.");
        }
        return usuario;
    }

    private Usuario getAdministradorAutenticado() {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario.getPerfil() != UsuarioPerfil.ADMINISTRADOR) {
            throw new UnauthorizedOperationException("A operação exige perfil de administrador.");
        }
        return usuario;
    }

    private void tentarExcluirArquivo(String caminho) {
        try {
            fileStorageService.deletar(caminho);
        } catch (RuntimeException ignored) {
            // A falha na limpeza não deve ocultar a falha original da atualização.
        }
    }

    private EmpresaResponseDto toResponse(Empresa empresa) {
        CidadeResponseDto cidadeBase = empresa.getCidadeBase() == null
                ? null
                : toCidadeResponse(empresa.getCidadeBase());

        List<CidadeResponseDto> cidadesAtuacao = empresa.getCidadesAtuacao() == null
                ? List.of()
                : empresa.getCidadesAtuacao().stream()
                        .map(this::toCidadeResponse)
                        .toList();

        return new EmpresaResponseDto(
                empresa.getId(), empresa.getTipo(), empresa.getNome(), empresa.getRazaoSocial(),
                empresa.getTipoDocumento(), empresa.getDocumento(), empresa.getCreci(),
                empresa.getLogoMarcaUrl(),
                ImagemUrl.extrairImagemId(empresa.getLogoMarcaUrl(), TipoImagem.EMPRESA, empresa.getId()),
                empresa.getDescricao(), empresa.getTelefone(),
                empresa.getWhatsapp(), empresa.getEmail(), empresa.getLogradouro(), empresa.getNumero(),
                empresa.getBairro(), empresa.getCep(), cidadeBase, cidadesAtuacao,
                empresa.isAtiva(), empresa.getCriadoEm()
        );
    }

    private CidadeResponseDto toCidadeResponse(Cidade cidade) {
        return new CidadeResponseDto(cidade.getId(), cidade.getNome(), cidade.getUf().name());
    }
}
