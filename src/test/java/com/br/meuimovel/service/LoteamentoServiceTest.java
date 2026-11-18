package com.br.meuimovel.service;

import com.br.meuimovel.dtos.loteamento.LoteamentoCreateDto;
import com.br.meuimovel.dtos.loteamento.LoteamentoResponseDto;
import com.br.meuimovel.dtos.loteamento.QuadraCreateDto;
import com.br.meuimovel.dtos.loteamento.QuadraResponseDto;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.QuadraAlreadyExistsException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Loteamento;
import com.br.meuimovel.model.Quadra;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CidadeRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.LoteRepository;
import com.br.meuimovel.repository.LoteamentoRepository;
import com.br.meuimovel.repository.QuadraRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteamentoServiceTest {
    @Mock private LoteamentoRepository loteamentoRepository;
    @Mock private QuadraRepository quadraRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private CidadeRepository cidadeRepository;
    @Mock private EmpresaRepository empresaRepository;
    @InjectMocks private LoteamentoService loteamentoService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUsesAuthenticatedCompanyAndReturnsInfrastructure() {
        Usuario usuario = autenticarCorretor();
        Cidade cidade = cidade(20);
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade));
        when(empresaRepository.atuaNaCidade(usuario.getEmpresa().getId(), 20)).thenReturn(true);
        when(loteamentoRepository.save(any(Loteamento.class))).thenAnswer(invocation -> {
            Loteamento loteamento = invocation.getArgument(0);
            loteamento.setId(40);
            return loteamento;
        });
        when(quadraRepository.findAllByLoteamento_IdOrderByIdentificacaoAsc(40)).thenReturn(List.of());

        LoteamentoResponseDto response = loteamentoService.create(dto("  Residencial das Palmeiras  "));

        assertEquals(40, response.id());
        assertEquals(usuario.getEmpresa().getId(), response.empresaId());
        assertEquals("Residencial das Palmeiras", response.nome());
        assertEquals("Recife", response.cidade());
        assertTrue(response.asfalto());
        assertEquals("Praça e playground", response.areasLazer());
        verify(empresaRepository).atuaNaCidade(usuario.getEmpresa().getId(), 20);
    }

    @Test
    void createRejectsCityOutsideAuthenticatedCompanyServiceArea() {
        Usuario usuario = autenticarCorretor();
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade(20)));
        when(empresaRepository.atuaNaCidade(usuario.getEmpresa().getId(), 20)).thenReturn(false);

        assertThrows(ImovelInvalidoException.class, () -> loteamentoService.create(dto("Residencial")));

        verify(loteamentoRepository, never()).save(any(Loteamento.class));
    }

    @Test
    void getByIdReturnsEachBlockWithAvailableAndSoldCounts() {
        Usuario usuario = autenticarCorretor();
        Loteamento loteamento = loteamento(usuario.getEmpresa(), cidade(20));
        Quadra quadra = Quadra.builder().id(90).loteamento(loteamento).identificacao("A").build();
        when(loteamentoRepository.findByIdAndEmpresa_Id(40, usuario.getEmpresa().getId()))
                .thenReturn(Optional.of(loteamento));
        when(quadraRepository.findAllByLoteamento_IdOrderByIdentificacaoAsc(40)).thenReturn(List.of(quadra));
        when(loteRepository.countByQuadra_IdAndSituacao(90, SituacaoImovel.DISPONIVEL)).thenReturn(8L);
        when(loteRepository.countByQuadra_IdAndSituacao(90, SituacaoImovel.VENDIDO)).thenReturn(3L);

        LoteamentoResponseDto response = loteamentoService.getById(40);

        assertEquals(1, response.quadras().size());
        assertEquals("A", response.quadras().get(0).identificacao());
        assertEquals(8L, response.quadras().get(0).lotesDisponiveis());
        assertEquals(3L, response.quadras().get(0).lotesVendidos());
    }

    @Test
    void createQuadraNormalizesIdentifierAndReturnsCounts() {
        Usuario usuario = autenticarCorretor();
        Loteamento loteamento = loteamento(usuario.getEmpresa(), cidade(20));
        when(loteamentoRepository.findByIdAndEmpresa_Id(40, usuario.getEmpresa().getId()))
                .thenReturn(Optional.of(loteamento));
        when(quadraRepository.existsByLoteamento_IdAndIdentificacaoIgnoreCase(40, "QUADRA A"))
                .thenReturn(false);
        when(quadraRepository.save(any(Quadra.class))).thenAnswer(invocation -> {
            Quadra quadra = invocation.getArgument(0);
            quadra.setId(90);
            return quadra;
        });
        when(loteRepository.countByQuadra_IdAndSituacao(90, SituacaoImovel.DISPONIVEL)).thenReturn(0L);
        when(loteRepository.countByQuadra_IdAndSituacao(90, SituacaoImovel.VENDIDO)).thenReturn(0L);

        QuadraResponseDto response = loteamentoService.createQuadra(40, new QuadraCreateDto(" quadra a "));

        assertEquals("QUADRA A", response.identificacao());
        assertEquals(0L, response.lotesDisponiveis());
        verify(quadraRepository).save(any(Quadra.class));
    }

    @Test
    void createQuadraRejectsDuplicateIdentifierInTheSameSubdivision() {
        Usuario usuario = autenticarCorretor();
        when(loteamentoRepository.findByIdAndEmpresa_Id(40, usuario.getEmpresa().getId()))
                .thenReturn(Optional.of(loteamento(usuario.getEmpresa(), cidade(20))));
        when(quadraRepository.existsByLoteamento_IdAndIdentificacaoIgnoreCase(40, "A"))
                .thenReturn(true);

        assertThrows(QuadraAlreadyExistsException.class,
                () -> loteamentoService.createQuadra(40, new QuadraCreateDto("A")));

        verify(quadraRepository, never()).save(any(Quadra.class));
    }

    private Usuario autenticarCorretor() {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario usuario = new Usuario(empresa, "Corretor", "corretor@example.com", "senha",
                UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, java.time.LocalDateTime.now());
        usuario.setId(9);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
        return usuario;
    }

    private LoteamentoCreateDto dto(String nome) {
        return new LoteamentoCreateDto(nome, "Construtora Horizonte", 20,
                true, true, true, true, true, "Praça e playground");
    }

    private Cidade cidade(Integer id) {
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(id);
        return cidade;
    }

    private Loteamento loteamento(Empresa empresa, Cidade cidade) {
        return Loteamento.builder()
                .id(40).empresa(empresa).cidade(cidade).nome("Residencial das Palmeiras")
                .incorporadora("Construtora Horizonte").asfalto(true).agua(true).energia(true)
                .esgoto(true).iluminacaoPublica(true).areasLazer("Praça e playground").build();
    }
}
