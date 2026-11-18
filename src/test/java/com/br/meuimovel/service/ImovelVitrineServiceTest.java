package com.br.meuimovel.service;

import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.EnderecoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.FotoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImovelVitrineServiceTest {
    @Mock private ImovelRepository imovelRepository;
    @Mock private FotoImovelRepository fotoImovelRepository;
    @InjectMocks private ImovelVitrineService imovelVitrineService;

    @Test
    void listaSomenteImoveisDisponiveisEPublicados() {
        Imovel imovel = imovelPublicado();
        when(imovelRepository.findAllByPublicadoTrueAndSituacaoAndCorretor_Usuario_StatusOrderByIdDesc(
                SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO))
                .thenReturn(List.of(imovel));
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(12)).thenReturn(List.of());

        var resposta = imovelVitrineService.listarDisponiveis();

        assertEquals(1, resposta.size());
        assertEquals(12, resposta.get(0).id());
        assertEquals("Casa ensolarada", resposta.get(0).titulo());
        verify(imovelRepository).findAllByPublicadoTrueAndSituacaoAndCorretor_Usuario_StatusOrderByIdDesc(
                SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO);
    }

    @Test
    void buscaDetalheSomenteQuandoDisponivelEPublicado() {
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO))
                .thenReturn(Optional.of(imovelPublicado()));
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(12)).thenReturn(List.of());

        var resposta = imovelVitrineService.buscarDisponivel(12);

        assertEquals("50000000", resposta.endereco().cep());
        verify(imovelRepository).findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO);
    }

    @Test
    void naoEncontradoQuandoNaoEstaDisponivelOuPublicado() {
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)).thenReturn(Optional.empty());

        assertThrows(ImovelNotFoundException.class, () -> imovelVitrineService.buscarDisponivel(12));
        verifyNoInteractions(fotoImovelRepository);
    }

    private Imovel imovelPublicado() {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario usuario = new Usuario(empresa, "Corretor", "corretor@example.com", "senha",
                UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, java.time.LocalDateTime.now());
        usuario.setId(9);
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1",
                "81999999999", "81999999999", "Corretor de imóveis");
        Imovel imovel = Imovel.builder().id(12).empresa(empresa).codigo("IM-12").titulo("Casa ensolarada")
                .corretor(corretor).situacao(SituacaoImovel.DISPONIVEL).publicado(true).build();
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        imovel.setEndereco(EnderecoImovel.builder().imovel(imovel).cep("50000000").logradouro("Rua do Sol")
                .numero("100").bairro("Centro").cidade(cidade)
                .latitude(BigDecimal.valueOf(-8.05)).longitude(BigDecimal.valueOf(-34.9)).build());
        return imovel;
    }
}
