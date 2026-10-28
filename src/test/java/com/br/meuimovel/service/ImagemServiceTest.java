package com.br.meuimovel.service;

import com.br.meuimovel.enums.TipoImagem;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.FotoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CorretorRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.FotoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.storage.FileStorageService;
import com.br.meuimovel.validator.FotoValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImagemServiceTest {
    private static final String ID_IMAGEM = "c4bf744d-a873-4ef0-b1f2-b1532bfb31db";

    @Mock private FileStorageService fileStorageService;
    @Mock private FotoValidator fotoValidator;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private CorretorRepository corretorRepository;
    @Mock private ImovelRepository imovelRepository;
    @Mock private FotoImovelRepository fotoImovelRepository;
    @InjectMocks private ImagemService imagemService;

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adicionaFotoAoImovelEDefineCapaQuandoAindaNaoExiste() {
        Empresa empresa = empresa(7);
        Usuario corretor = usuario(7, UsuarioPerfil.CORRETOR, empresa);
        Imovel imovel = new Imovel();
        imovel.setId(42);
        imovel.setEmpresa(empresa);
        autenticar(corretor);
        when(imovelRepository.findById(42)).thenReturn(Optional.of(imovel));
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(42)).thenReturn(List.of());
        when(fileStorageService.salvarImagem(any(), eq(TipoImagem.IMOVEL), eq(42)))
                .thenReturn("/imagens/imovel/42/" + ID_IMAGEM);

        var response = imagemService.adicionar(TipoImagem.IMOVEL, 42, arquivo());

        assertEquals(ID_IMAGEM, response.id());
        assertEquals("/imagens/imovel/42/" + ID_IMAGEM, response.url());
        var captor = org.mockito.ArgumentCaptor.forClass(FotoImovel.class);
        verify(fotoImovelRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOrdem());
        assertTrue(captor.getValue().isCapa());
    }

    @Test
    void impedeAtualizacaoDeImagemDeImovelDeOutraEmpresa() {
        Empresa outraEmpresa = empresa(9);
        Imovel imovel = new Imovel();
        imovel.setId(42);
        imovel.setEmpresa(outraEmpresa);
        FotoImovel foto = foto(imovel, 1, "/imagens/imovel/42/" + ID_IMAGEM, true);
        when(fotoImovelRepository.findByImovel_IdAndUrl(42, foto.getUrl())).thenReturn(Optional.of(foto));
        autenticar(usuario(7, UsuarioPerfil.CORRETOR, empresa(7)));

        assertThrows(UnauthorizedOperationException.class,
                () -> imagemService.atualizar(TipoImagem.IMOVEL, 42, ID_IMAGEM, arquivo()));
        verify(fileStorageService, never()).substituir(any(), anyInt(), anyString(), any());
    }

    @Test
    void aoRemoverFotoDeCapaPromoveOutraFotoEApagaArquivo() {
        Empresa empresa = empresa(7);
        Imovel imovel = new Imovel();
        imovel.setId(42);
        imovel.setEmpresa(empresa);
        FotoImovel capa = foto(imovel, 1, "/imagens/imovel/42/" + ID_IMAGEM, true);
        FotoImovel outra = foto(imovel, 2, "/imagens/imovel/42/4bbf744d-a873-4ef0-b1f2-b1532bfb31db", false);
        when(fotoImovelRepository.findByImovel_IdAndUrl(42, capa.getUrl())).thenReturn(Optional.of(capa));
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(42)).thenReturn(List.of(capa, outra));
        autenticar(usuario(7, UsuarioPerfil.CORRETOR, empresa));

        imagemService.remover(TipoImagem.IMOVEL, 42, ID_IMAGEM);

        assertTrue(outra.isCapa());
        verify(fotoImovelRepository).delete(capa);
        verify(fileStorageService).deletar(TipoImagem.IMOVEL, 42, ID_IMAGEM);
    }

    private Empresa empresa(int id) {
        Empresa empresa = new Empresa();
        empresa.setId(id);
        empresa.setAtiva(true);
        return empresa;
    }

    private Usuario usuario(int empresaId, UsuarioPerfil perfil, Empresa empresa) {
        Usuario usuario = new Usuario();
        usuario.setId(empresaId);
        usuario.setEmpresa(empresa);
        usuario.setPerfil(perfil);
        usuario.setStatus(UsuarioStatus.ATIVO);
        return usuario;
    }

    private FotoImovel foto(Imovel imovel, int ordem, String url, boolean capa) {
        FotoImovel foto = new FotoImovel();
        foto.setId(ordem);
        foto.setImovel(imovel);
        foto.setOrdem(ordem);
        foto.setUrl(url);
        foto.setCapa(capa);
        return foto;
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private MockMultipartFile arquivo() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01};
        return new MockMultipartFile("arquivo", "foto.png", "image/png", png);
    }
}
