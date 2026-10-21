package com.br.meuimovel.service;

import com.br.meuimovel.dtos.caracteristica.CaracteristicaCreateDto;
import com.br.meuimovel.dtos.caracteristica.CaracteristicaResponseDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.exception.CaracteristicaAlreadyExistsException;
import com.br.meuimovel.exception.CaracteristicaNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Caracteristica;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CaracteristicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaracteristicaService {
    private final CaracteristicaRepository caracteristicaRepository;

    public CaracteristicaResponseDto getByNomeIgnoreCase(String nome){
        String nomeWithoutSpaces = nome.trim();
        Caracteristica caracteristica = caracteristicaRepository.findByNomeIgnoreCase(nomeWithoutSpaces)
                .orElseThrow(() -> new CaracteristicaNotFoundException
                        ("Caracteristica com o nome: " + nome + " nao encontrada!"));
        return new CaracteristicaResponseDto(caracteristica.getId(), caracteristica.getNome());
    }

    public List<CaracteristicaResponseDto> listAll(){
        return caracteristicaRepository.findAll()
                .stream()
                .map(caracteristica -> new CaracteristicaResponseDto(
                        caracteristica.getId(),
                        caracteristica.getNome()
                ))
                .toList();
    }

    @Transactional
    public CaracteristicaResponseDto create(CaracteristicaCreateDto createDto){
        exigirAdministrador();
        String nome = createDto.nome().trim();
        if(caracteristicaRepository.existsByNomeIgnoreCase(nome)){
            throw new CaracteristicaAlreadyExistsException("Caracteristica com esse nome já existe!!");
        }
        Caracteristica caracteristica = caracteristicaRepository.save(new Caracteristica(nome));
        return new CaracteristicaResponseDto(caracteristica.getId(), caracteristica.getNome());
    }

    private void exigirAdministrador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)
                || usuario.getPerfil() != UsuarioPerfil.ADMINISTRADOR) {
            throw new UnauthorizedOperationException("A operação exige perfil de administrador.");
        }
    }

    //pegar por nome ignore case
    //pegar todas
    //criar nova (apenas adm)
}
