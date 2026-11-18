package com.br.meuimovel.service;

import com.br.meuimovel.dtos.loteamento.LoteSituacaoUpdateDto;
import com.br.meuimovel.enums.SituacaoLote;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.LoteNotFoundException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Lote;
import com.br.meuimovel.model.Loteamento;
import com.br.meuimovel.model.Quadra;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.LoteRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteServiceTest {
    @Mock private LoteRepository loteRepository;
    @InjectMocks private LoteService loteService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateSituacaoScopesLotToAuthenticatedCompany() {
        Usuario usuario = autenticarCorretor();
        Lote lote = lote(usuario.getEmpresa());
        when(loteRepository.findByIdAndImovel_Empresa_Id(42, usuario.getEmpresa().getId()))
                .thenReturn(Optional.of(lote));
        when(loteRepository.save(any(Lote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = loteService.updateSituacao(42, new LoteSituacaoUpdateDto(SituacaoLote.VENDIDO));

        assertEquals(SituacaoLote.VENDIDO, response.situacao());
        assertEquals("Residencial das Palmeiras", response.quadra().loteamento().nome());
        verify(loteRepository).findByIdAndImovel_Empresa_Id(42, usuario.getEmpresa().getId());
    }

    @Test
    void updateSituacaoDoesNotExposeLotFromAnotherCompany() {
        Usuario usuario = autenticarCorretor();
        when(loteRepository.findByIdAndImovel_Empresa_Id(42, usuario.getEmpresa().getId()))
                .thenReturn(Optional.empty());

        assertThrows(LoteNotFoundException.class,
                () -> loteService.updateSituacao(42, new LoteSituacaoUpdateDto(SituacaoLote.VENDIDO)));

        verify(loteRepository, never()).save(any(Lote.class));
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

    private Lote lote(Empresa empresa) {
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        Loteamento loteamento = Loteamento.builder()
                .id(70).empresa(empresa).cidade(cidade).nome("Residencial das Palmeiras")
                .incorporadora("Construtora Horizonte").asfalto(true).agua(true).energia(true)
                .esgoto(true).iluminacaoPublica(true).areasLazer("Praça").build();
        Quadra quadra = Quadra.builder().id(90).loteamento(loteamento).identificacao("A").build();
        Imovel imovel = Imovel.builder().id(42).empresa(empresa).build();
        return Lote.builder().id(42).imovel(imovel).quadra(quadra).numero("12")
                .area(BigDecimal.valueOf(250)).frente(BigDecimal.TEN).fundo(BigDecimal.valueOf(25))
                .condicoesParcelamento("À vista").situacao(SituacaoLote.DISPONIVEL).build();
    }
}
