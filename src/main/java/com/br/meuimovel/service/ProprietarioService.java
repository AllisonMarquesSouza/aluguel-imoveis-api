package com.br.meuimovel.service;

import com.br.meuimovel.dtos.proprietario.ProprietarioCreateDto;
import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ProprietarioAlreadyExistsException;
import com.br.meuimovel.exception.ProprietarioNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Proprietario;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.ProprietarioRepository;
import com.br.meuimovel.validator.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProprietarioService {
    private final ProprietarioRepository proprietarioRepository;
    private final DocumentoValidator documentoValidator;


    private Usuario getCorretorAutenticado(){
        Usuario corretor = (Usuario) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (corretor.getPerfil() != UsuarioPerfil.CORRETOR || corretor.getStatus() != UsuarioStatus.ATIVO) {
            throw new UnauthorizedOperationException("Para processar essa operacao" +
                    " precisa ser um corretor e estar ativo!");
        }
        return corretor;
    }

    public Proprietario getByDocumento(TipoDocumento tipoDocumento, String documento){
        String documentoValid = documentoValidator.validar(tipoDocumento, documento);
        Integer empresaId = getCorretorAutenticado().getEmpresa().getId();
        return proprietarioRepository.findByEmpresaIdAndDocumento(empresaId, documentoValid)
                .orElseThrow(() -> new ProprietarioNotFoundException(
                                "Proprietarico com documento : "+ documento +" nao encontrado!"));
    }

    public List<Proprietario> listByNome(String nome){
        Integer empresaId = getCorretorAutenticado().getEmpresa().getId();
        return proprietarioRepository.findByEmpresaIdAndNomeContainingIgnoreCase(empresaId, nome);
    }

    @Transactional
    public Proprietario create(ProprietarioCreateDto createDto){
        Usuario corretor = getCorretorAutenticado();
        Empresa empresa = corretor.getEmpresa();

        String documento =
                documentoValidator
                .validar(createDto.tipoDocumento(), createDto.documento());
        if(proprietarioRepository.existsByEmpresaIdAndDocumento(empresa.getId(), documento)){
            throw new ProprietarioAlreadyExistsException("Proprietario com esse documento já existe na empresa!");
        }

        Proprietario proprietario = Proprietario.builder()
                .empresa(empresa)
                .tipoDocumento(createDto.tipoDocumento())
                .documento(documento)
                .nome(createDto.nome())
                .telefone(createDto.telefone())
                .email(createDto.email())
                .observacoes(createDto.observacoes())
                .build();

        return proprietarioRepository.save(proprietario);
    }

}
