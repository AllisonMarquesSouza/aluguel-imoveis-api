package com.br.meuimovel.service;

import com.br.meuimovel.dtos.CidadeCreateDto;
import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CidadeAlreadyExistsException;
import com.br.meuimovel.exception.CidadeNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CidadeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CidadeServiceTest {
    @Mock private CidadeRepository cidadeRepository;
    @InjectMocks private CidadeService cidadeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTrimsNameAndSavesState() {
        autenticar(UsuarioPerfil.ADMINISTRADOR);
        when(cidadeRepository.existsByNomeIgnoreCaseAndUf("Recife", Uf.PE)).thenReturn(false);
        when(cidadeRepository.save(any(Cidade.class))).thenAnswer(invocation -> {
            Cidade cidade = invocation.getArgument(0);
            cidade.setId(4);
            return cidade;
        });

        CidadeResponseDto response = cidadeService.create(new CidadeCreateDto(" Recife ", Uf.PE));

        assertEquals(4, response.id());
        assertEquals("Recife", response.nome());
        assertEquals("PE", response.uf());
        verify(cidadeRepository).existsByNomeIgnoreCaseAndUf("Recife", Uf.PE);
    }

    @Test
    void getByIdReturnsCityEntity() {
        Cidade cidade = cidade(4, "Recife");
        when(cidadeRepository.findById(4)).thenReturn(Optional.of(cidade));

        Cidade response = cidadeService.getById(4);

        assertSame(cidade, response);
    }

    @Test
    void getResponseByIdMapsCityToDto() {
        when(cidadeRepository.findById(4)).thenReturn(Optional.of(cidade(4, "Recife")));

        CidadeResponseDto response = cidadeService.getResponseById(4);

        assertEquals(new CidadeResponseDto(4, "Recife", "PE"), response);
    }

    @Test
    void createRejectsExistingNameAndState() {
        autenticar(UsuarioPerfil.ADMINISTRADOR);
        when(cidadeRepository.existsByNomeIgnoreCaseAndUf("Recife", Uf.PE)).thenReturn(true);

        assertThrows(CidadeAlreadyExistsException.class,
                () -> cidadeService.create(new CidadeCreateDto(" Recife ", Uf.PE)));

        verify(cidadeRepository, never()).save(any(Cidade.class));
    }

    @Test
    void createRequiresAdministrator() {
        autenticar(UsuarioPerfil.CORRETOR);

        assertThrows(UnauthorizedOperationException.class,
                () -> cidadeService.create(new CidadeCreateDto("Recife", Uf.PE)));

        verifyNoInteractions(cidadeRepository);
    }

    @Test
    void getAllByIdRejectsMissingCityIds() {
        when(cidadeRepository.findAllById(Set.of(4, 5))).thenReturn(List.of(cidade(4, "Recife")));

        assertThrows(CidadeNotFoundException.class, () -> cidadeService.getAllById(Set.of(4, 5)));
    }

    private void autenticar(UsuarioPerfil perfil) {
        Usuario usuario = new Usuario(null, "Usuário", "usuario@example.com", "hash", perfil,
                UsuarioStatus.ATIVO, LocalDateTime.now());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private Cidade cidade(Integer id, String nome) {
        Cidade cidade = new Cidade(nome, Uf.PE);
        cidade.setId(id);
        return cidade;
    }
}
