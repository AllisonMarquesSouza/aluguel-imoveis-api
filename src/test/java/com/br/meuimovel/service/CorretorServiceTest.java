package com.br.meuimovel.service;

import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import com.br.meuimovel.dtos.corretor.CorretorPublicoResponseDto;
import com.br.meuimovel.dtos.empresa.EmpresaPublicaResponseDto;
import com.br.meuimovel.enums.TipoEmpresa;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.CorretorNotFoundException;
import com.br.meuimovel.exception.CorretorStatusAlreadyModifiedException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CorretorRepository;
import com.br.meuimovel.repository.UsuarioRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorretorServiceTest {
    private static final String FOTO_IMAGEM_ID = "84c8304e-9e5d-4b81-b8d5-90a53c6171bc";
    private static final String LOGO_IMAGEM_ID = "c4bf744d-a873-4ef0-b1f2-b1532bfb31db";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CorretorRepository corretorRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FotoValidator fotoValidator;
    @Mock private FileStorageService fileStorageService;
    @InjectMocks private CorretorService corretorService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAssociatesCorretorWithAuthenticatedGestorCompany() {
        Empresa empresa = empresa(7);
        autenticar(gestor(empresa));
        CorretorCreateDto dto = createDto();
        MockMultipartFile foto = foto();
        when(corretorRepository.existsByCreci(dto.creci())).thenReturn(false);
        when(usuarioRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(dto.senha())).thenReturn("senha-hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(21);
            return usuario;
        });
        when(fileStorageService.salvarFotoCorretor(foto, 21))
                .thenReturn("/imagens/corretor/21/" + FOTO_IMAGEM_ID);
        when(corretorRepository.save(any(Corretor.class))).thenAnswer(invocation -> {
            Corretor corretor = invocation.getArgument(0);
            corretor.setId(21);
            return corretor;
        });

        var response = corretorService.create(dto, foto);

        assertEquals(21, response.id());
        assertEquals(dto.nome(), response.nome());
        assertEquals(dto.email(), response.email());
        assertEquals("/imagens/corretor/21/" + FOTO_IMAGEM_ID, response.fotoUrl());
        assertEquals(FOTO_IMAGEM_ID, response.fotoImagemId());
        var usuarioCaptor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertSame(empresa, usuarioCaptor.getValue().getEmpresa());
        assertEquals(UsuarioPerfil.CORRETOR, usuarioCaptor.getValue().getPerfil());
        assertEquals(UsuarioStatus.ATIVO, usuarioCaptor.getValue().getStatus());
        assertEquals("senha-hash", usuarioCaptor.getValue().getSenha());
        verify(fotoValidator).validar(foto);
    }

    @Test
    void createRejectsNonGestor() {
        autenticar(usuario(empresa(7), UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO));

        assertThrows(UnauthorizedOperationException.class, () -> corretorService.create(createDto(), foto()));

        verifyNoInteractions(usuarioRepository, corretorRepository, fotoValidator, fileStorageService);
    }

    @Test
    void createRejectsAutonomousCompany() {
        autenticar(gestor(empresa(7, TipoEmpresa.AUTONOMO)));

        assertThrows(UnauthorizedOperationException.class, () -> corretorService.create(createDto(), foto()));

        verifyNoInteractions(usuarioRepository, corretorRepository, fotoValidator, fileStorageService);
    }

    @Test
    void activateChangesOnlyCorretorFromAuthenticatedCompany() {
        autenticar(gestor(empresa(7)));
        Corretor corretor = corretor(21, 7, UsuarioStatus.INATIVO);
        when(corretorRepository.findByIdAndUsuario_Empresa_Id(21, 7)).thenReturn(Optional.of(corretor));

        corretorService.ativar(21);

        assertEquals(UsuarioStatus.ATIVO, corretor.getUsuario().getStatus());
        verify(corretorRepository).save(corretor);
    }

    @Test
    void activateRejectsCorretorFromAnotherCompany() {
        autenticar(gestor(empresa(7)));
        when(corretorRepository.findByIdAndUsuario_Empresa_Id(21, 7)).thenReturn(Optional.empty());

        assertThrows(CorretorNotFoundException.class, () -> corretorService.ativar(21));

        verify(corretorRepository, never()).save(any());
    }

    @Test
    void activateRejectsCorretorThatIsNotInactive() {
        autenticar(gestor(empresa(7)));
        Corretor corretor = corretor(21, 7, UsuarioStatus.ATIVO);
        when(corretorRepository.findByIdAndUsuario_Empresa_Id(21, 7)).thenReturn(Optional.of(corretor));

        assertThrows(CorretorStatusAlreadyModifiedException.class, () -> corretorService.ativar(21));

        verify(corretorRepository, never()).save(any());
    }

    @Test
    void inactivateChangesActiveCorretorStatus() {
        autenticar(gestor(empresa(7)));
        Corretor corretor = corretor(21, 7, UsuarioStatus.ATIVO);
        when(corretorRepository.findByIdAndUsuario_Empresa_Id(21, 7)).thenReturn(Optional.of(corretor));

        corretorService.desativar(21);

        assertEquals(UsuarioStatus.INATIVO, corretor.getUsuario().getStatus());
        verify(corretorRepository).save(corretor);
    }

    @Test
    void inactivateRejectsCorretorThatIsNotActive() {
        autenticar(gestor(empresa(7)));
        Corretor corretor = corretor(21, 7, UsuarioStatus.BLOQUEADO);
        when(corretorRepository.findByIdAndUsuario_Empresa_Id(21, 7)).thenReturn(Optional.of(corretor));

        assertThrows(CorretorStatusAlreadyModifiedException.class, () -> corretorService.desativar(21));

        verify(corretorRepository, never()).save(any());
    }

    @Test
    void activateRejectsInactiveGestor() {
        autenticar(usuario(empresa(7), UsuarioPerfil.GESTOR, UsuarioStatus.INATIVO));

        assertThrows(UnauthorizedOperationException.class, () -> corretorService.ativar(21));

        verifyNoInteractions(corretorRepository);
    }

    @Test
    void listPublicProfilesMapsCorretorAndCompanyAndRequestsOnlyActiveCorretores() {
        Corretor corretor = corretor(21, 7, UsuarioStatus.ATIVO);
        when(corretorRepository.findDistinctByUsuario_StatusOrderByUsuario_NomeAsc(UsuarioStatus.ATIVO))
                .thenReturn(List.of(corretor));

        List<CorretorPublicoResponseDto> response = corretorService.listarPublicos();

        assertEquals(1, response.size());
        assertEquals("Corretor 21", response.get(0).nome());
        assertEquals("corretor21@example.com", response.get(0).email());
        assertEquals(FOTO_IMAGEM_ID, response.get(0).fotoImagemId());
        assertEquals("81999999999", response.get(0).telefone());
        EmpresaPublicaResponseDto empresaResponse = response.get(0).empresa();
        assertEquals(7, empresaResponse.id());
        assertEquals("Imobiliária 7", empresaResponse.nome());
        assertEquals(LOGO_IMAGEM_ID, empresaResponse.logoImagemId());
        assertEquals(new CidadeResponseDto(4, "Recife", "PE"), empresaResponse.cidadeBase());
        assertEquals(List.of(new CidadeResponseDto(4, "Recife", "PE")), empresaResponse.cidadesAtuacao());
        verify(corretorRepository).findDistinctByUsuario_StatusOrderByUsuario_NomeAsc(UsuarioStatus.ATIVO);
    }

    @Test
    void getPublicProfileReturnsActiveCorretorWithCompany() {
        Corretor corretor = corretor(21, 7, UsuarioStatus.ATIVO);
        when(corretorRepository.findByIdAndUsuario_Status(21, UsuarioStatus.ATIVO))
                .thenReturn(Optional.of(corretor));

        CorretorPublicoResponseDto response = corretorService.getPerfilPublico(21);

        assertEquals(21, response.id());
        assertEquals("Atendimento imobiliário", response.apresentacao());
        assertEquals("81988888888", response.whatsapp());
        assertEquals(FOTO_IMAGEM_ID, response.fotoImagemId());
        assertNotNull(response.empresa());
        assertEquals(LOGO_IMAGEM_ID, response.empresa().logoImagemId());
        assertEquals("81444444444", response.empresa().whatsapp());
    }

    @Test
    void getPublicProfileHidesInactiveCorretorAndContactDetails() {
        when(corretorRepository.findByIdAndUsuario_Status(21, UsuarioStatus.ATIVO)).thenReturn(Optional.empty());

        assertThrows(CorretorNotFoundException.class, () -> corretorService.getPerfilPublico(21));

        verify(corretorRepository).findByIdAndUsuario_Status(21, UsuarioStatus.ATIVO);
    }

    private Usuario gestor(Empresa empresa) {
        return usuario(empresa, UsuarioPerfil.GESTOR, UsuarioStatus.ATIVO);
    }

    private Usuario usuario(Empresa empresa, UsuarioPerfil perfil, UsuarioStatus status) {
        return new Usuario(empresa, "Usuário", "usuario@example.com", "senha-hash",
                perfil, status, LocalDateTime.now());
    }

    private void autenticar(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private Corretor corretor(Integer id, Integer empresaId, UsuarioStatus status) {
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(4);
        Empresa empresa = empresa(empresaId);
        empresa.setLogoMarcaUrl("/imagens/empresa/" + empresaId + "/" + LOGO_IMAGEM_ID);
        empresa.setCidadeBase(cidade);
        empresa.setCidadesAtuacao(new HashSet<>(Set.of(cidade)));
        Usuario usuario = new Usuario(empresa, "Corretor " + id, "corretor" + id + "@example.com", "senha-hash",
                UsuarioPerfil.CORRETOR, status, LocalDateTime.now());
        usuario.setId(id);
        Corretor corretor = new Corretor(usuario, "/imagens/corretor/" + id + "/" + FOTO_IMAGEM_ID, "CRECI-" + id,
                "81999999999", "81988888888", "Atendimento imobiliário");
        corretor.setId(id);
        return corretor;
    }

    private Empresa empresa(Integer id) {
        return empresa(id, TipoEmpresa.IMOBILIARIA);
    }

    private Empresa empresa(Integer id, TipoEmpresa tipo) {
        return Empresa.builder()
                .id(id)
                .tipo(tipo)
                .nome("Imobiliária " + id)
                .ativa(true)
                .email("empresa" + id + "@example.com")
                .telefone("81333333333")
                .whatsapp("81444444444")
                .cidadesAtuacao(new HashSet<>())
                .build();
    }

    private CorretorCreateDto createDto() {
        return new CorretorCreateDto("Corretor", "corretor@example.com", "senha-segura-456", "CRECI-21",
                "81999999999", "81988888888", "Atendimento imobiliário");
    }

    private MockMultipartFile foto() {
        return new MockMultipartFile("foto", "corretor.png", "image/png", new byte[]{1, 2, 3});
    }
}
