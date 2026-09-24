package com.br.meuimovel.service;

import com.br.meuimovel.dtos.empresa.EmpresaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaUpdateDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.exception.EmpresaAlreadyExistsException;
import com.br.meuimovel.exception.EmpresaNotFoundException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.validator.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
                && usuario.getPerfil() != UsuarioPerfil.ADMINISTRADOR ) {
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


    @Transactional
    public Empresa create(EmpresaCreateDto createDto) {
        String documento = documentoValidator.validar(
                createDto.tipoDocumento(),
                createDto.documento()
        );
        if (empresaRepository.existsByDocumento(documento)) {
            throw new EmpresaAlreadyExistsException("Empresa com esse documento já existe, cheque o CNPJ OU CPF");
            //ver essa verificacao do CNPJ e CPF
        }
        Cidade cidadeBase = createDto.cidadeBaseId() == null
                ? null
                : cidadeService.getById(createDto.cidadeBaseId());
        Set<Cidade> cidades = cidadeService.getAllById(createDto.cidadesAtuacaoIds());

        Empresa empresa = Empresa.builder()
                .tipo(createDto.tipo()).nome(createDto.nome()).razaoSocial(createDto.razaoSocial())
                .tipoDocumento(createDto.tipoDocumento())
                .documento(documento).creci(createDto.creci())
                .logoMarcaUrl(createDto.logoMarcaUrl()).descricao(createDto.descricao())
                .telefone(createDto.telefone()).whatsapp(createDto.whatsapp())
                .email(createDto.email()).logradouro(createDto.logradouro())
                .numero(createDto.numero()).bairro(createDto.bairro())
                .cep(createDto.cep()).cidadeBase(cidadeBase).cidadesAtuacao(cidades)
                .ativa(true).criadoEm(LocalDateTime.now()).build();
        return empresaRepository.save(empresa);
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
    public void activate(Integer id){
        Empresa empresa = getById(id);
        if(empresa.isAtiva()) throw new RuntimeException("Empresa ja ativa");
        empresa.setAtiva(true);
    }

    @Transactional
    public void inactivate(Integer id){
        Empresa empresa = getById(id);
        if(!empresa.isAtiva()) throw new RuntimeException("Empresa ja desativada");
        empresa.setAtiva(false);
    }

    @Transactional
    public void deleteById(Integer id){
        getById(id);
        empresaRepository.deleteById(id);
    }
}
