package com.br.meuimovel.service;

import com.br.meuimovel.dtos.CidadeCreateDto;
import com.br.meuimovel.exception.CidadeAlreadyExistsException;
import com.br.meuimovel.exception.CidadeNotFoundException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.repository.CidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CidadeService {
    private final CidadeRepository cidadeRepository;


    public List<Cidade> listAll(){
        return cidadeRepository.findAll();
    }

    public Cidade getById(Integer id){
        return cidadeRepository.findById(id)
                .orElseThrow(() -> new CidadeNotFoundException("Cidade nao encontrada"));

    }

    @Transactional
    public Cidade create(CidadeCreateDto createDto){
        if(cidadeRepository.existsByNomeIgnoreCaseAndUf(createDto.nome(), createDto.uf())){
            throw new CidadeAlreadyExistsException("Cidade já cadastrada!");
        }
        Cidade cidade = new Cidade(createDto.nome().trim(), createDto.uf());
        return cidadeRepository.save(cidade);
    }

    public void delete(Integer id){
        getById(id);
        //fazer uma verificao se tem algumas empresa com essa cidade,
        // se estiver, nao pode excluir.
        //user o EmpresaService ou EmpresaRepositoryAqui aqui
        cidadeRepository.deleteById(id);
    }
}
