package com.br.meuimovel.service;

import com.br.meuimovel.dtos.contato.ContatoImovelCreateDto;
import com.br.meuimovel.dtos.contato.ContatoImovelRetornoUpdateDto;
import com.br.meuimovel.enums.CanalContato;
import com.br.meuimovel.enums.RetornoContato;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ContatoImovelInvalidoException;
import com.br.meuimovel.exception.ContatoImovelNotFoundException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.ContatoImovel;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.ContatoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContatoImovelServiceTest {
    @Mock private ContatoImovelRepository contatoRepository;
    @Mock private ImovelRepository imovelRepository;
    @InjectMocks private ContatoImovelService contatoImovelService;

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraContatoWhatsappEConstroiLinkComCodigoETitulo() {
        Imovel imovel = imovelPublicado();
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)).thenReturn(Optional.of(imovel));
        when(contatoRepository.save(any(ContatoImovel.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        var response = contatoImovelService.registrarContato(12, new ContatoImovelCreateDto(CanalContato.WHATSAPP));

        assertEquals(CanalContato.WHATSAPP, response.canal());
        assertTrue(response.whatsappUrl().startsWith("https://wa.me/5581999998888?text="));
        String mensagem = URLDecoder.decode(response.whatsappUrl().substring(response.whatsappUrl().indexOf("text=") + 5),
                StandardCharsets.UTF_8);
        assertEquals("Olá, tenho interesse no imóvel IM-12 - Casa & Sol.", mensagem);

        ArgumentCaptor<ContatoImovel> captor = ArgumentCaptor.forClass(ContatoImovel.class);
        verify(contatoRepository).save(captor.capture());
        assertSame(imovel, captor.getValue().getImovel());
        assertSame(imovel.getCorretor(), captor.getValue().getCorretor());
        assertEquals(CanalContato.WHATSAPP, captor.getValue().getCanal());
        assertNotNull(captor.getValue().getCriadoEm());
        assertNull(captor.getValue().getRetorno());
    }

    @Test
    void registraTelefoneSemLinkWhatsapp() {
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)).thenReturn(Optional.of(imovelPublicado()));
        when(contatoRepository.save(any(ContatoImovel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = contatoImovelService.registrarContato(12, new ContatoImovelCreateDto(CanalContato.TELEFONE));

        assertEquals(CanalContato.TELEFONE, response.canal());
        assertNull(response.whatsappUrl());
    }

    @Test
    void recusaRegistroWhatsappSemNumeroValido() {
        Imovel imovel = imovelPublicado();
        imovel.getCorretor().setWhatsapp("telefone inválido");
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)).thenReturn(Optional.of(imovel));

        assertThrows(ContatoImovelInvalidoException.class, () -> contatoImovelService.registrarContato(
                12, new ContatoImovelCreateDto(CanalContato.WHATSAPP)));

        verifyNoInteractions(contatoRepository);
    }

    @Test
    void contatoPublicoRecusaImovelForaDaVitrine() {
        when(imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                12, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)).thenReturn(Optional.empty());

        assertThrows(ImovelNotFoundException.class, () -> contatoImovelService.registrarContato(
                12, new ContatoImovelCreateDto(CanalContato.WHATSAPP)));

        verifyNoInteractions(contatoRepository);
    }

    @Test
    void caixaDoCorretorFiltraPorSuaEmpresaImovelEPeriodoInclusivo() {
        Usuario corretorUsuario = usuario(UsuarioPerfil.CORRETOR, 9);
        autenticar(corretorUsuario);
        ContatoImovel contato = contato(imovelPublicado());
        when(contatoRepository.listarCaixa(7, 9, 12,
                LocalDate.of(2026, 11, 1).atStartOfDay(), LocalDate.of(2026, 11, 8).atStartOfDay()))
                .thenReturn(List.of(contato));

        var response = contatoImovelService.listar(12, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7));

        assertEquals(1, response.size());
        assertEquals("IM-12", response.get(0).imovelCodigo());
        assertEquals("Casa & Sol", response.get(0).imovelTitulo());
        verify(contatoRepository).listarCaixa(7, 9, 12,
                LocalDate.of(2026, 11, 1).atStartOfDay(), LocalDate.of(2026, 11, 8).atStartOfDay());
    }

    @Test
    void caixaDoGestorListaContatosDaEmpresaSemRestringirAUmCorretor() {
        autenticar(usuario(UsuarioPerfil.GESTOR, 15));
        when(contatoRepository.listarCaixa(7, null, null, null, null)).thenReturn(List.of());

        assertTrue(contatoImovelService.listar(null, null, null).isEmpty());

        verify(contatoRepository).listarCaixa(7, null, null, null, null);
    }

    @Test
    void filtroRecusaPeriodoInvertidoAntesDeConsultarRepositorio() {
        autenticar(usuario(UsuarioPerfil.CORRETOR, 9));

        assertThrows(ContatoImovelInvalidoException.class,
                () -> contatoImovelService.listar(null, LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 1)));

        verifyNoInteractions(contatoRepository);
    }

    @Test
    void corretorAtualizaRetornoSomenteDeContatoDaSuaEmpresa() {
        Usuario corretorUsuario = usuario(UsuarioPerfil.CORRETOR, 9);
        autenticar(corretorUsuario);
        ContatoImovel contato = contato(imovelPublicado());
        contato.setId(81);
        when(contatoRepository.findByIdAndCorretor_IdAndImovel_Empresa_Id(81, 9, 7))
                .thenReturn(Optional.of(contato));
        when(contatoRepository.save(contato)).thenReturn(contato);

        var response = contatoImovelService.atualizarRetorno(81,
                new ContatoImovelRetornoUpdateDto(RetornoContato.VISITA_AGENDADA));

        assertEquals(RetornoContato.VISITA_AGENDADA, response.retorno());
        verify(contatoRepository).save(contato);
    }

    @Test
    void corretorNaoAtualizaContatoDeOutraCaixa() {
        autenticar(usuario(UsuarioPerfil.CORRETOR, 9));
        when(contatoRepository.findByIdAndCorretor_IdAndImovel_Empresa_Id(81, 9, 7))
                .thenReturn(Optional.empty());

        assertThrows(ContatoImovelNotFoundException.class, () -> contatoImovelService.atualizarRetorno(81,
                new ContatoImovelRetornoUpdateDto(RetornoContato.FALEI)));

        verify(contatoRepository, never()).save(any(ContatoImovel.class));
    }

    @Test
    void gestorNaoAnotaRetornoNoLugarDoCorretor() {
        autenticar(usuario(UsuarioPerfil.GESTOR, 15));

        assertThrows(UnauthorizedOperationException.class, () -> contatoImovelService.atualizarRetorno(81,
                new ContatoImovelRetornoUpdateDto(RetornoContato.FALEI)));

        verifyNoInteractions(contatoRepository);
    }

    private Usuario usuario(UsuarioPerfil perfil, Integer id) {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario usuario = new Usuario(empresa, "Usuário", "usuario@example.com", "senha",
                perfil, UsuarioStatus.ATIVO, LocalDateTime.now());
        usuario.setId(id);
        return usuario;
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private Imovel imovelPublicado() {
        Usuario usuario = usuario(UsuarioPerfil.CORRETOR, 9);
        Corretor corretor = new Corretor(usuario, "/foto.jpg", "CRECI-9", "81999998888", "81999998888", "Apresentação");
        corretor.setId(9);
        return Imovel.builder().id(12).codigo("IM-12").titulo("Casa & Sol")
                .empresa(usuario.getEmpresa()).corretor(corretor)
                .situacao(SituacaoImovel.DISPONIVEL).publicado(true).build();
    }

    private ContatoImovel contato(Imovel imovel) {
        ContatoImovel contato = new ContatoImovel();
        contato.setId(81);
        contato.setImovel(imovel);
        contato.setCorretor(imovel.getCorretor());
        contato.setCanal(CanalContato.EMAIL);
        contato.setCriadoEm(LocalDateTime.of(2026, 11, 5, 10, 30));
        return contato;
    }
}
