package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imovel.PublicacaoImovelUpdateDto;
import com.br.meuimovel.dtos.imovel.SituacaoImovelUpdateDto;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.EnderecoImovel;
import com.br.meuimovel.model.HistoricoSituacaoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Lote;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.HistoricoSituacaoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.repository.LoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ImovelGestaoServiceTest {
    @Mock private ImovelRepository imovelRepository;
    @Mock private HistoricoSituacaoImovelRepository historicoRepository;
    @Mock private LoteRepository loteRepository;
    @InjectMocks private ImovelGestaoService imovelGestaoService;

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicarRejeitaImovelSemEnderecoCompleto() {
        Usuario gestor = autenticarGestor();
        Imovel imovel = imovel(gestor.getEmpresa());
        when(imovelRepository.findByIdAndEmpresa_Id(12, gestor.getEmpresa().getId()))
                .thenReturn(Optional.of(imovel));

        ImovelInvalidoException exception = assertThrows(ImovelInvalidoException.class,
                () -> imovelGestaoService.atualizarPublicacao(12, new PublicacaoImovelUpdateDto(true)));

        assertTrue(exception.getMessage().contains("CEP"));
        assertTrue(exception.getMessage().contains("latitude"));
        verify(imovelRepository, never()).save(any(Imovel.class));
    }

    @Test
    void publicarImovelComEnderecoCompleto() {
        Usuario gestor = autenticarGestor();
        Imovel imovel = imovel(gestor.getEmpresa());
        imovel.setEndereco(endereco(imovel));
        when(imovelRepository.findByIdAndEmpresa_Id(12, gestor.getEmpresa().getId()))
                .thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any(Imovel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = imovelGestaoService.atualizarPublicacao(12, new PublicacaoImovelUpdateDto(true));

        assertTrue(response.publicado());
        assertEquals(SituacaoImovel.DISPONIVEL, response.situacao());
        verify(imovelRepository).save(imovel);
    }

    @Test
    void mudarSituacaoPersisteHistoricoComAutorEObservacao() {
        Usuario gestor = autenticarGestor();
        Imovel imovel = imovel(gestor.getEmpresa());
        when(imovelRepository.findByIdAndEmpresa_Id(12, gestor.getEmpresa().getId()))
                .thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any(Imovel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoRepository.save(any(HistoricoSituacaoImovel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = imovelGestaoService.atualizarSituacao(12,
                new SituacaoImovelUpdateDto(SituacaoImovel.RESERVADO, "  Proposta em análise  "));

        assertEquals(SituacaoImovel.RESERVADO, response.situacao());
        ArgumentCaptor<HistoricoSituacaoImovel> captor = ArgumentCaptor.forClass(HistoricoSituacaoImovel.class);
        verify(historicoRepository).save(captor.capture());
        assertEquals(SituacaoImovel.DISPONIVEL, captor.getValue().getSituacaoAnterior());
        assertEquals(SituacaoImovel.RESERVADO, captor.getValue().getSituacaoNova());
        assertEquals(gestor, captor.getValue().getUsuario());
        assertEquals("Proposta em análise", captor.getValue().getObservacao());
        assertNotNull(captor.getValue().getAlteradoEm());
    }

    @Test
    void mudarSituacaoDeLoteAtualizaSuaSituacaoDeEstoque() {
        Usuario gestor = autenticarGestor();
        Imovel imovel = imovel(gestor.getEmpresa());
        Lote lote = Lote.builder().id(12).imovel(imovel).situacao(SituacaoImovel.DISPONIVEL).build();
        imovel.setLote(lote);
        when(imovelRepository.findByIdAndEmpresa_Id(12, gestor.getEmpresa().getId()))
                .thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any(Imovel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(loteRepository.save(any(Lote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoRepository.save(any(HistoricoSituacaoImovel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        imovelGestaoService.atualizarSituacao(12,
                new SituacaoImovelUpdateDto(SituacaoImovel.RESERVADO, "Negociação em andamento"));

        assertEquals(SituacaoImovel.RESERVADO, imovel.getSituacao());
        assertEquals(SituacaoImovel.RESERVADO, lote.getSituacao());
        verify(loteRepository).save(lote);
    }

    @Test
    void situacaoIgualNaoCriaHistorico() {
        Usuario gestor = autenticarGestor();
        Imovel imovel = imovel(gestor.getEmpresa());
        when(imovelRepository.findByIdAndEmpresa_Id(12, gestor.getEmpresa().getId()))
                .thenReturn(Optional.of(imovel));

        imovelGestaoService.atualizarSituacao(12,
                new SituacaoImovelUpdateDto(SituacaoImovel.DISPONIVEL, null));

        verify(imovelRepository, never()).save(any(Imovel.class));
        verifyNoInteractions(historicoRepository);
    }

    @Test
    void mudarSituacaoExigeObservacao() {
        Usuario gestor = autenticarGestor();
        Imovel imovel = imovel(gestor.getEmpresa());
        when(imovelRepository.findByIdAndEmpresa_Id(12, gestor.getEmpresa().getId()))
                .thenReturn(Optional.of(imovel));

        assertThrows(ImovelInvalidoException.class, () -> imovelGestaoService.atualizarSituacao(12,
                new SituacaoImovelUpdateDto(SituacaoImovel.RESERVADO, " ")));

        assertEquals(SituacaoImovel.DISPONIVEL, imovel.getSituacao());
        verify(imovelRepository, never()).save(any(Imovel.class));
        verifyNoInteractions(historicoRepository);
    }

    private Usuario autenticarGestor() {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario gestor = new Usuario(empresa, "Gestor", "gestor@example.com", "senha",
                UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO, LocalDateTime.now());
        gestor.setId(9);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(gestor, null, gestor.getAuthorities()));
        return gestor;
    }

    private Imovel imovel(Empresa empresa) {
        return Imovel.builder().id(12).empresa(empresa).situacao(SituacaoImovel.DISPONIVEL).build();
    }

    private EnderecoImovel endereco(Imovel imovel) {
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        return EnderecoImovel.builder().imovel(imovel).cep("50000000").logradouro("Rua do Sol")
                .numero("100").bairro("Centro").cidade(cidade)
                .latitude(BigDecimal.valueOf(-8.05)).longitude(BigDecimal.valueOf(-34.9)).build();
    }
}
