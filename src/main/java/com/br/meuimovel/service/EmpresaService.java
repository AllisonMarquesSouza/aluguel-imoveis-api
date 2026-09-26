package com.br.meuimovel.service;

import com.br.meuimovel.dtos.empresa.EmpresaAutonomaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaImobiliariaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaUpdateDto;
import com.br.meuimovel.enums.TipoEmpresa;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CreciAlreadyExistsException;
import com.br.meuimovel.exception.EmpresaAlreadyExistsException;
import com.br.meuimovel.exception.EmpresaNotFoundException;
import com.br.meuimovel.exception.UsuarioAlreadyExistsException;
import com.br.meuimovel.model.*;
import com.br.meuimovel.repository.CorretorRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.GestorRepository;
import com.br.meuimovel.repository.UsuarioRepository;
import com.br.meuimovel.validator.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

    public Empresa getById(Integer id) {
        Usuario usuario = (Usuario) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Empresa empresa = empresaRepository
                .findById(id)
                .orElseThrow(() ->
                        new EmpresaNotFoundException("Empresa não encontrada!"));

        if (!empresa.getId().equals(usuario.getEmpresa().getId())
                && usuario.getPerfil() != UsuarioPerfil.ADMINISTRADOR) {
            throw new RuntimeException(
                    "Você não tem acesso a esta empresa."
            );
        }
        //a ideia é que o ADMINISTRADOR consegue acessar dados de qualquer empresa
        return empresa;
    }

    public List<Empresa> listAll() {
        return empresaRepository.findAll();
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
                .logoMarcaUrl(empresa.logoMarcaUrl()).descricao(empresa.descricao())
                .telefone(empresa.telefone()).whatsapp(empresa.whatsapp())
                .email(empresa.email()).logradouro(empresa.logradouro())
                .numero(empresa.numero()).bairro(empresa.bairro())
                .cep(empresa.cep()).cidadeBase(cidadeBase).cidadesAtuacao(cidades)
                .ativa(true).criadoEm(LocalDateTime.now()).build());
    }

    @Transactional
    public Empresa createImobiliaria(EmpresaImobiliariaCreateDto createDto) {
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

        String encodedSenha = passwordEncoder.encode(createDto.senhaGestor());
        Empresa empresa = criarEmpresa(documento, createDto.empresa(), TipoEmpresa.IMOBILIARIA);

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
        return empresa;
    }

    @Transactional
    public Empresa createAutonoma(EmpresaAutonomaCreateDto createDto) {
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
        String encodedSenha = passwordEncoder.encode(createDto.corretor().senha());
        Empresa empresa = criarEmpresa(documento, createDto.empresa(), TipoEmpresa.AUTONOMO);

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

        corretorRepository.save(new Corretor
                (usuario, createDto.corretor().fotoUrl(), createDto.corretor().creci(), createDto.corretor().telefone(),
                        createDto.corretor().whatsapp(), createDto.corretor().apresentacao()));
        return empresa;
    }

    //criar outras versoes de updates individuais?
    @Transactional
    public void update(Integer id, EmpresaUpdateDto updateDto) {
        Empresa empresa = getById(id);

        if (empresaRepository.existsByDocumento(updateDto.documento())
                && !empresa.getDocumento().equals(updateDto.documento())) {
            throw new EmpresaAlreadyExistsException(
                    "Empresa com esse documento já existe, cheque o CNPJ OU CPF"
            );
        }

        Cidade cidadeBase = updateDto.cidadeBaseId() == null
                ? null
                : cidadeService.getById(updateDto.cidadeBaseId());
        Set<Cidade> cidades = cidadeService.getAllById(
                updateDto.cidadesAtuacaoIds()
        );

        empresa.setTipo(updateDto.tipo());
        empresa.setNome(updateDto.nome());
        empresa.setRazaoSocial(updateDto.razaoSocial());
        empresa.setTipoDocumento(updateDto.tipoDocumento());
        empresa.setDocumento(updateDto.documento());
        empresa.setCreci(updateDto.creci());
        empresa.setLogoMarcaUrl(updateDto.logoMarcaUrl());
        empresa.setDescricao(updateDto.descricao());
        empresa.setTelefone(updateDto.telefone());
        empresa.setWhatsapp(updateDto.whatsapp());
        empresa.setEmail(updateDto.email());
        empresa.setLogradouro(updateDto.logradouro());
        empresa.setNumero(updateDto.numero());
        empresa.setBairro(updateDto.bairro());
        empresa.setCep(updateDto.cep());
        empresa.setCidadeBase(cidadeBase);
        empresa.getCidadesAtuacao().clear();
        empresa.getCidadesAtuacao().addAll(cidades);

        empresaRepository.save(empresa);
    }


    @Transactional
    public void activate(Integer id) {
        Empresa empresa = getById(id);
        if (empresa.isAtiva()) throw new RuntimeException("Empresa ja ativa");
        empresa.setAtiva(true);
    }

    @Transactional
    public void inactivate(Integer id) {
        Empresa empresa = getById(id);
        if (!empresa.isAtiva()) throw new RuntimeException("Empresa ja desativada");
        empresa.setAtiva(false);
    }

    @Transactional
    public void deleteById(Integer id) {
        getById(id);
        empresaRepository.deleteById(id);
    }
}
