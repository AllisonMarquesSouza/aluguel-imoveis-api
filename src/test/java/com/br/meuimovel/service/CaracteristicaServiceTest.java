package com.br.meuimovel.service;

import com.br.meuimovel.dtos.caracteristica.CaracteristicaCreateDto;
import com.br.meuimovel.dtos.caracteristica.CaracteristicaResponseDto;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CaracteristicaAlreadyExistsException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Caracteristica;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CaracteristicaRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaracteristicaServiceTest {
    @Mock private CaracteristicaRepository caracteristicaRepository;
    @InjectMocks private CaracteristicaService caracteristicaService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listAllMapsCatalogEntriesToResponseDtos() {
        when(caracteristicaRepository.findAll()).thenReturn(List.of(
                caracteristica(1, "Ar-condicionado"), caracteristica(2, "Portão eletrônico")));

        List<CaracteristicaResponseDto> response = caracteristicaService.listAll();

        assertEquals(List.of(
                new CaracteristicaResponseDto(1, "Ar-condicionado"),
                new CaracteristicaResponseDto(2, "Portão eletrônico")), response);
    }

    @Test
    void getByNameTrimsAndUsesCaseInsensitiveSearch() {
        when(caracteristicaRepository.findByNomeIgnoreCase("AR-CONDICIONADO"))
                .thenReturn(Optional.of(caracteristica(1, "Ar-condicionado")));

        CaracteristicaResponseDto response = caracteristicaService.getByNomeIgnoreCase(" AR-CONDICIONADO ");

        assertEquals(new CaracteristicaResponseDto(1, "Ar-condicionado"), response);
        verify(caracteristicaRepository).findByNomeIgnoreCase("AR-CONDICIONADO");
    }

    @Test
    void createTrimsNameAndSavesForAdministrator() {
        autenticar(UsuarioPerfil.ADMINISTRADOR);
        when(caracteristicaRepository.existsByNomeIgnoreCase("Piscina")).thenReturn(false);
        when(caracteristicaRepository.save(any(Caracteristica.class))).thenAnswer(invocation -> {
            Caracteristica caracteristica = invocation.getArgument(0);
            caracteristica.setId(3);
            return caracteristica;
        });

        CaracteristicaResponseDto response = caracteristicaService.create(new CaracteristicaCreateDto(" Piscina "));

        assertEquals(new CaracteristicaResponseDto(3, "Piscina"), response);
        verify(caracteristicaRepository).existsByNomeIgnoreCase("Piscina");
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        autenticar(UsuarioPerfil.ADMINISTRADOR);
        when(caracteristicaRepository.existsByNomeIgnoreCase("Piscina")).thenReturn(true);

        assertThrows(CaracteristicaAlreadyExistsException.class,
                () -> caracteristicaService.create(new CaracteristicaCreateDto(" Piscina ")));

        verify(caracteristicaRepository, never()).save(any(Caracteristica.class));
    }

    @Test
    void createRequiresAdministrator() {
        autenticar(UsuarioPerfil.CORRETOR);

        assertThrows(UnauthorizedOperationException.class,
                () -> caracteristicaService.create(new CaracteristicaCreateDto("Piscina")));

        verifyNoInteractions(caracteristicaRepository);
    }

    private void autenticar(UsuarioPerfil perfil) {
        Usuario usuario = new Usuario(null, "Usuário", "usuario@example.com", "hash", perfil,
                UsuarioStatus.ATIVO, LocalDateTime.now());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private Caracteristica caracteristica(Integer id, String nome) {
        Caracteristica caracteristica = new Caracteristica(nome);
        caracteristica.setId(id);
        return caracteristica;
    }
}
