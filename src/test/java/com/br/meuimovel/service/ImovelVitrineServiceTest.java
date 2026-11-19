package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imovel.ImovelVitrineFiltro;
import com.br.meuimovel.enums.FinalidadeImovel;
import com.br.meuimovel.enums.NaturezaImovel;
import com.br.meuimovel.enums.OrdenacaoVitrine;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CidadeNotFoundException;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Apartamento;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.EnderecoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Predio;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CidadeRepository;
import com.br.meuimovel.repository.FotoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImovelVitrineServiceTest {
    @Mock private ImovelRepository imovelRepository;
    @Mock private FotoImovelRepository fotoImovelRepository;
    @Mock private CidadeRepository cidadeRepository;
    @InjectMocks private ImovelVitrineService imovelVitrineService;

    @Test
    void listaCidadesQuePossuemImoveisVisiveisComEstado() {
        when(cidadeRepository.listarCidadesAtendidas())
                .thenReturn(List.of(new Cidade("Recife", Uf.PE)));

        var cidades = imovelVitrineService.listarCidades();

        assertEquals(1, cidades.size());
        assertEquals("Recife", cidades.get(0).nome());
        assertEquals("PE", cidades.get(0).uf());
    }

    @Test
    void buscaDentroDaCidadeComPaginacaoEDevolveCorretorPublico() {
        Imovel imovel = imovelPublicado();
        when(cidadeRepository.existsById(20)).thenReturn(true);
        when(imovelRepository.buscarVitrine(
                eq(20), eq("Rua do Sol"), eq("VENDA"), eq("CASA"), eq("Centro"),
                eq(new BigDecimal("100000")), eq(new BigDecimal("500000")), eq(2), eq(1), eq(1),
                eq(new BigDecimal("60")), eq(new BigDecimal("300")), eq("4,5"), eq(2),
                eq("MENOR_PRECO"), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(imovel), invocation.getArgument(15), 6));
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(12)).thenReturn(List.of());

        ImovelVitrineFiltro filtro = new ImovelVitrineFiltro(
                20, "  Rua do Sol  ", FinalidadeImovel.VENDA, NaturezaImovel.CASA,
                new BigDecimal("100000"), new BigDecimal("500000"), 2, 1, 1,
                "Centro", new BigDecimal("60"), new BigDecimal("300"), List.of(4, 5),
                OrdenacaoVitrine.MENOR_PRECO, 1, 5);

        var pagina = imovelVitrineService.buscar(filtro);

        assertEquals(6, pagina.getTotalElements());
        assertEquals(1, pagina.getNumber());
        assertEquals(5, pagina.getSize());
        assertEquals("Casa ensolarada", pagina.getContent().get(0).titulo());
        assertEquals(BigDecimal.valueOf(-8.05), pagina.getContent().get(0).endereco().latitude());
        assertEquals("Corretor responsável", pagina.getContent().get(0).corretor().nome());
        assertEquals("CRECI-1", pagina.getContent().get(0).corretor().creci());
        assertNull(pagina.getContent().get(0).endereco().id());
        verify(imovelRepository).buscarVitrine(
                eq(20), eq("Rua do Sol"), eq("VENDA"), eq("CASA"), eq("Centro"),
                eq(new BigDecimal("100000")), eq(new BigDecimal("500000")), eq(2), eq(1), eq(1),
                eq(new BigDecimal("60")), eq(new BigDecimal("300")), eq("4,5"), eq(2), eq("MENOR_PRECO"),
                eq(PageRequest.of(1, 5)));
    }

    @Test
    void buscaSemFiltrosOpcionaisUsaValoresPadraoEListaVaziaDeCaracteristicas() {
        when(cidadeRepository.existsById(20)).thenReturn(true);
        when(imovelRepository.buscarVitrine(
                eq(20), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(""), eq(0), eq("MAIS_RECENTES"), eq(PageRequest.of(0, 12))))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        var pagina = imovelVitrineService.buscar(filtro(20, 0, 12));

        assertTrue(pagina.isEmpty());
        verify(imovelRepository).buscarVitrine(
                eq(20), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(""), eq(0), eq("MAIS_RECENTES"), eq(PageRequest.of(0, 12)));
    }

    @Test
    void cidadeInexistenteEhRecusadaAntesDaBusca() {
        when(cidadeRepository.existsById(20)).thenReturn(false);

        assertThrows(CidadeNotFoundException.class,
                () -> imovelVitrineService.buscar(filtro(20, 0, 12)));

        verifyNoInteractions(imovelRepository);
    }

    @Test
    void rejeitaPaginacaoAcimaDoLimite() {
        assertThrows(ImovelInvalidoException.class,
                () -> imovelVitrineService.buscar(filtro(20, 0, 51)));

        verifyNoInteractions(cidadeRepository, imovelRepository);
    }

    @Test
    void rejeitaFaixaDePrecoInvertida() {
        ImovelVitrineFiltro filtro = new ImovelVitrineFiltro(
                20, null, null, null, new BigDecimal("500000"), new BigDecimal("100000"),
                null, null, null, null, null, null, null, OrdenacaoVitrine.MAIS_RECENTES, 0, 12);

        assertThrows(ImovelInvalidoException.class, () -> imovelVitrineService.buscar(filtro));
        verifyNoInteractions(cidadeRepository, imovelRepository);
    }

    @Test
    void buscaDetalheSomenteQuandoDisponivelEPublicado() {
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO))
                .thenReturn(Optional.of(imovelPublicado()));
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(12)).thenReturn(List.of());

        var resposta = imovelVitrineService.buscarDisponivel(12);

        assertEquals("50000000", resposta.endereco().cep());
        assertEquals("Recife", resposta.endereco().cidade());
        assertEquals("Corretor responsável", resposta.corretor().nome());
        verify(imovelRepository).findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO);
    }

    @Test
    void detalheNaoEncontradoQuandoImovelNaoEstaDisponivelOuPublicado() {
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)).thenReturn(Optional.empty());

        assertThrows(ImovelNotFoundException.class, () -> imovelVitrineService.buscarDisponivel(12));
        verifyNoInteractions(fotoImovelRepository);
    }

    @Test
    void detalheDeApartamentoIncluiPredioSemIdentificadorDaEmpresa() {
        Imovel imovel = imovelPublicado();
        imovel.setEndereco(null);
        imovel.setNatureza(NaturezaImovel.APARTAMENTO);
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        Predio predio = Predio.builder().empresa(imovel.getEmpresa()).nome("Edifício Atlântico")
                .cep("50000000").logradouro("Rua do Sol").numero("200").bairro("Centro").cidade(cidade)
                .latitude(new BigDecimal("-8.05")).longitude(new BigDecimal("-34.9"))
                .numeroAndares(10).unidadesPorAndar(4).anoConstrucao(2020).elevadores(2)
                .portaria24h(true).piscina(true).academia(true).salaoFestas(true).playground(false)
                .valorCondominioBase(new BigDecimal("500.00")).build();
        Apartamento apartamento = Apartamento.builder().imovel(imovel).predio(predio).andar(3).numero("301")
                .areaPrivativa(new BigDecimal("80.00")).areaTotal(new BigDecimal("95.00"))
                .quartos(3).suites(1).banheiros(2).vagas(1).varanda(true).posicaoSolar("NASCENTE").build();
        imovel.setApartamento(apartamento);
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)).thenReturn(Optional.of(imovel));
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(12)).thenReturn(List.of());

        var resposta = imovelVitrineService.buscarDisponivel(12);

        assertEquals("Edifício Atlântico", resposta.apartamento().predio().nome());
        assertEquals("50000000", resposta.endereco().cep());
        assertEquals("Recife", resposta.endereco().cidade());
        assertNull(resposta.endereco().id());
    }

    private ImovelVitrineFiltro filtro(Integer cidadeId, int pagina, int tamanho) {
        return new ImovelVitrineFiltro(cidadeId, null, null, null, null, null,
                null, null, null, null, null, null, null, OrdenacaoVitrine.MAIS_RECENTES, pagina, tamanho);
    }

    private Imovel imovelPublicado() {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario usuario = new Usuario(empresa, "Corretor responsável", "corretor@example.com", "senha",
                UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, LocalDateTime.now());
        usuario.setId(9);
        Corretor corretor = new Corretor(usuario, "/imagens/CORRETOR/9/3c27fae7-a7c3-4f27-90d0-465da93c4e76",
                "CRECI-1", "81999999999", "81999999999", "Atendimento residencial");
        Imovel imovel = Imovel.builder().id(12).empresa(empresa).codigo("INTERNO-12").titulo("Casa ensolarada")
                .corretor(corretor).situacao(SituacaoImovel.DISPONIVEL).publicado(true).build();
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        imovel.setEndereco(EnderecoImovel.builder().imovel(imovel).cep("50000000").logradouro("Rua do Sol")
                .numero("100").bairro("Centro").cidade(cidade)
                .latitude(new BigDecimal("-8.05")).longitude(new BigDecimal("-34.9")).build());
        return imovel;
    }
}
