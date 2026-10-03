package com.br.meuimovel.service;

import com.br.meuimovel.dtos.CaracteristicaCreateDto;
import com.br.meuimovel.exception.CaracteristicaAlreadyExistsException;
import com.br.meuimovel.exception.CaracteristicaNotFoundException;
import com.br.meuimovel.model.Caracteristica;
import com.br.meuimovel.repository.CaracteristicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaracteristicaService {
    private final CaracteristicaRepository caracteristicaRepository;

    public Caracteristica getByNomeIgnoreCase(String nome){
        String nomeWithoutSpaces = nome.trim();
        return caracteristicaRepository.findByNomeIgnoreCase(nomeWithoutSpaces)
                .orElseThrow(() -> new CaracteristicaNotFoundException("Caracteristica com o nome: "+nome+" nao encontrada!"));
    }
    public List<Caracteristica> listAll(){
        return caracteristicaRepository.findAll();
    }

    @Transactional
    public Caracteristica create(CaracteristicaCreateDto createDto){
        String nome = createDto.nome().trim();
        if(caracteristicaRepository.existsByNomeIgnoreCase(nome)){
            throw new CaracteristicaAlreadyExistsException("Caracteristica com esse nome já existe!!");
        }
        Caracteristica caracteristica = new Caracteristica(nome);
        return caracteristicaRepository.save(caracteristica);
    }

    //pegar por nome ignore case
    //pegar todas
    //criar nova (apenas adm)
}
