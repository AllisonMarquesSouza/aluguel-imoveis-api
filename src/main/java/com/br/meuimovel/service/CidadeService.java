package com.br.meuimovel.service;

import com.br.meuimovel.dtos.CidadeCreateDto;
import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.exception.CidadeAlreadyExistsException;
import com.br.meuimovel.exception.CidadeNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CidadeService {
    private final CidadeRepository cidadeRepository;

    public List<CidadeResponseDto> listAll() {
        return cidadeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public Set<Cidade> getAllById(Set<Integer> ids) {
        List<Cidade> cidades = cidadeRepository.findAllById(ids);
        if (cidades.size() != ids.size()) {
            throw new CidadeNotFoundException("Cidades não encontradas, verifque os ids ");
        }
        return new HashSet<>(cidades);
    }

    public Cidade getById(Integer id) {
        return cidadeRepository.findById(id).orElseThrow(() ->
                new CidadeNotFoundException("Cidade nao encontrada"));
    }
    public CidadeResponseDto getResponseById(Integer id) {
        return toResponse(getById(id));
    }

    @Transactional
    public CidadeResponseDto create(CidadeCreateDto createDto) {
        exigirAdministrador();
        String nome = createDto.nome().trim();
        if (cidadeRepository.existsByNomeIgnoreCaseAndUf(nome, createDto.uf())) {
            throw new CidadeAlreadyExistsException("Cidade já cadastrada!");
        }
        Cidade cidade = cidadeRepository.save(new Cidade(nome, createDto.uf()));
        return toResponse(cidade);
    }

    @Transactional
    public void delete(Integer id) {
        exigirAdministrador();
        getById(id);
        //fazer uma verificao se tem algumas empresa com essa cidade,
        // se estiver, nao pode excluir.
        //user o EmpresaService ou EmpresaRepositoryAqui aqui
        cidadeRepository.deleteById(id);
    }

    private void exigirAdministrador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)
                || usuario.getPerfil() != UsuarioPerfil.ADMINISTRADOR) {
            throw new UnauthorizedOperationException("A operação exige perfil de administrador.");
        }
    }

    private CidadeResponseDto toResponse(Cidade cidade) {
        return new CidadeResponseDto(cidade.getId(), cidade.getNome(), cidade.getUf().name());
    }
}
