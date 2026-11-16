package com.br.meuimovel.service;

import com.br.meuimovel.dtos.predio.EnderecoPredioDto;
import com.br.meuimovel.dtos.predio.PredioCreateDto;
import com.br.meuimovel.dtos.predio.PredioResponseDto;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.PredioNotFoundException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Predio;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CidadeRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.PredioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredioServiceTest {
    @Mock private PredioRepository predioRepository;
    @Mock private CidadeRepository cidadeRepository;
    @Mock private EmpresaRepository empresaRepository;
    @InjectMocks private PredioService predioService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUsesAuthenticatedCompanyAndItsServiceArea() {
        Usuario usuario = autenticarCorretor();
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade));
        when(empresaRepository.atuaNaCidade(usuario.getEmpresa().getId(), 20)).thenReturn(true);
        when(predioRepository.save(any(Predio.class))).thenAnswer(invocation -> {
            Predio predio = invocation.getArgument(0);
            predio.setId(30);
            return predio;
        });

        PredioResponseDto response = predioService.create(dto("  Edifício Atlântico  ", "Rua do Sol"));

        assertEquals(30, response.id());
        assertEquals(usuario.getEmpresa().getId(), response.empresaId());
        assertEquals("Edifício Atlântico", response.nome());
        assertEquals("Rua do Sol", response.endereco().logradouro());
        assertEquals("Recife", response.endereco().cidade());
        verify(empresaRepository).atuaNaCidade(usuario.getEmpresa().getId(), 20);
    }

    @Test
    void createRejectsCityOutsideAuthenticatedCompanyServiceArea() {
        Usuario usuario = autenticarCorretor();
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade));
        when(empresaRepository.atuaNaCidade(usuario.getEmpresa().getId(), 20)).thenReturn(false);

        assertThrows(ImovelInvalidoException.class,
                () -> predioService.create(dto("Edifício Atlântico", "Rua do Sol")));

        verify(predioRepository, never()).save(any(Predio.class));
    }

    @Test
    void updateScopesBuildingLookupToAuthenticatedCompanyAndChangesSharedData() {
        Usuario usuario = autenticarCorretor();
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        Predio predio = Predio.builder().id(30).empresa(usuario.getEmpresa()).build();
        when(predioRepository.findByIdAndEmpresa_Id(30, usuario.getEmpresa().getId()))
                .thenReturn(Optional.of(predio));
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade));
        when(empresaRepository.atuaNaCidade(usuario.getEmpresa().getId(), 20)).thenReturn(true);
        when(predioRepository.save(predio)).thenReturn(predio);

        PredioResponseDto response = predioService.update(30, dto("Edifício Atlântico", "Avenida Nova"));

        assertEquals("Avenida Nova", response.endereco().logradouro());
        verify(predioRepository).findByIdAndEmpresa_Id(30, usuario.getEmpresa().getId());
    }

    @Test
    void getByIdDoesNotReturnAnotherCompanyBuilding() {
        Usuario usuario = autenticarCorretor();
        when(predioRepository.findByIdAndEmpresa_Id(30, usuario.getEmpresa().getId()))
                .thenReturn(Optional.empty());

        assertThrows(PredioNotFoundException.class, () -> predioService.getById(30));
    }

    @Test
    void listByEmpresaUsesAuthenticatedCompany() {
        Usuario usuario = autenticarCorretor();
        when(predioRepository.findAllByEmpresa_IdOrderByNomeAsc(usuario.getEmpresa().getId()))
                .thenReturn(List.of());

        assertTrue(predioService.listByEmpresa().isEmpty());

        verify(predioRepository).findAllByEmpresa_IdOrderByNomeAsc(usuario.getEmpresa().getId());
    }

    private Usuario autenticarCorretor() {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario usuario = new Usuario(empresa, "Corretor", "corretor@example.com", "senha",
                UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, LocalDateTime.now());
        usuario.setId(9);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
        return usuario;
    }

    private PredioCreateDto dto(String nome, String logradouro) {
        return new PredioCreateDto(nome,
                new EnderecoPredioDto("50000000", logradouro, "10", null, "Centro", 20,
                        BigDecimal.valueOf(-8.05), BigDecimal.valueOf(-34.9)),
                10, 4, 2010, 2, true, true, false, true, false, BigDecimal.valueOf(650));
    }
}
