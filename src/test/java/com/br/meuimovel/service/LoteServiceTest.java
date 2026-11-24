package com.br.meuimovel.service;

import com.br.meuimovel.dtos.loteamento.LoteSituacaoUpdateDto;
import com.br.meuimovel.enums.SituacaoImovel;
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
import com.br.meuimovel.repository.QuadraRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.repository.HistoricoSituacaoImovelRepository;
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
    @Mock private QuadraRepository quadraRepository;
    @Mock private ImovelRepository imovelRepository;
    @Mock private HistoricoSituacaoImovelRepository historicoRepository;
    @InjectMocks private LoteService loteService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateSituacaoScopesLotToAuthenticatedCompany() {
        Usuario usuario = autenticarGestor();
        Lote lote = lote(usuario.getEmpresa());
        when(loteRepository.findByIdAndImovel_Empresa_Id(42, usuario.getEmpresa().getId()))
                .thenReturn(Optional.of(lote));
        when(loteRepository.save(any(Lote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imovelRepository.save(any(Imovel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = loteService.updateSituacao(42,
                new LoteSituacaoUpdateDto(SituacaoImovel.VENDIDO, "Venda concluída"));

        assertEquals(SituacaoImovel.VENDIDO, response.situacao());
        assertEquals(SituacaoImovel.VENDIDO, lote.getImovel().getSituacao());
        assertEquals("Residencial das Palmeiras", response.quadra().loteamento().nome());
        verify(loteRepository).findByIdAndImovel_Empresa_Id(42, usuario.getEmpresa().getId());
        verify(imovelRepository).save(lote.getImovel());
        verify(historicoRepository).save(argThat(historico ->
                historico.getSituacaoAnterior() == SituacaoImovel.DISPONIVEL
                        && historico.getSituacaoNova() == SituacaoImovel.VENDIDO
                        && historico.getUsuario().equals(usuario)
                        && "Venda concluída".equals(historico.getObservacao())));
    }

    @Test
    void updateSituacaoDoesNotExposeLotFromAnotherCompany() {
        Usuario usuario = autenticarGestor();
        when(loteRepository.findByIdAndImovel_Empresa_Id(42, usuario.getEmpresa().getId()))
                .thenReturn(Optional.empty());

        assertThrows(LoteNotFoundException.class,
                () -> loteService.updateSituacao(42,
                        new LoteSituacaoUpdateDto(SituacaoImovel.VENDIDO, "Venda concluída")));

        verify(loteRepository, never()).save(any(Lote.class));
    }

    @Test
    void listByQuadraReturnsLotIdsAndSituationsWithinTenant() {
        Usuario usuario = autenticarGestor();
        Lote lote = lote(usuario.getEmpresa());
        when(quadraRepository.findByIdAndLoteamento_IdAndLoteamento_Empresa_Id(90, 70, 7))
                .thenReturn(Optional.of(lote.getQuadra()));
        when(loteRepository.findAllByQuadra_IdAndImovel_Empresa_IdOrderByNumeroAsc(90, 7))
                .thenReturn(java.util.List.of(lote));

        var response = loteService.listarPorQuadra(70, 90);

        assertEquals(1, response.size());
        assertEquals(42, response.get(0).id());
        assertEquals(SituacaoImovel.DISPONIVEL, response.get(0).situacao());
        assertEquals(90, response.get(0).quadra().id());
        verify(loteRepository).findAllByQuadra_IdAndImovel_Empresa_IdOrderByNumeroAsc(90, 7);
    }

    @Test
    void listByQuadraRejectsQuadraOutsideRequestedSubdivisionOrTenant() {
        autenticarGestor();
        when(quadraRepository.findByIdAndLoteamento_IdAndLoteamento_Empresa_Id(90, 70, 7))
                .thenReturn(Optional.empty());

        assertThrows(com.br.meuimovel.exception.QuadraNotFoundException.class,
                () -> loteService.listarPorQuadra(70, 90));

        verifyNoInteractions(loteRepository);
    }

    private Usuario autenticarGestor() {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario usuario = new Usuario(empresa, "Gestor", "gestor@example.com", "senha",
                UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, LocalDateTime.now());
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
                .condicoesParcelamento("À vista").situacao(SituacaoImovel.DISPONIVEL).build();
    }
}
