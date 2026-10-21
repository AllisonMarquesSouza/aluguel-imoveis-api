package com.br.meuimovel.service;

import com.br.meuimovel.dtos.proprietario.ProprietarioCreateDto;
import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.exception.ProprietarioAlreadyExistsException;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Proprietario;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.ProprietarioRepository;
import com.br.meuimovel.validator.DocumentoValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProprietarioServiceTest {
    @Mock private ProprietarioRepository proprietarioRepository;
    @Mock private DocumentoValidator documentoValidator;
    @InjectMocks private ProprietarioService proprietarioService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nameSearchUsesAuthenticatedCompany() {
        autenticarCorretor(7);
        Proprietario proprietario = proprietario(31, 7);
        when(proprietarioRepository.findByEmpresaIdAndNomeContainingIgnoreCase(7, "Ana"))
                .thenReturn(List.of(proprietario));

        var response = proprietarioService.listByNome("Ana");

        assertEquals(1, response.size());
        assertEquals(31, response.get(0).id());
        assertEquals("Ana Souza", response.get(0).nome());
        verify(proprietarioRepository).findByEmpresaIdAndNomeContainingIgnoreCase(7, "Ana");
    }

    @Test
    void documentSearchUsesAuthenticatedCompany() {
        autenticarCorretor(12);
        when(documentoValidator.validar(TipoDocumento.CPF, "12345678901")).thenReturn("12345678901");
        when(proprietarioRepository.findByEmpresaIdAndDocumento(12, "12345678901"))
                .thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                com.br.meuimovel.exception.ProprietarioNotFoundException.class,
                () -> proprietarioService.getByDocumento(TipoDocumento.CPF, "12345678901")
        );

        verify(proprietarioRepository).findByEmpresaIdAndDocumento(12, "12345678901");
    }

    @Test
    void documentSearchMapsOwnerFromAuthenticatedCompany() {
        autenticarCorretor(12);
        when(documentoValidator.validar(TipoDocumento.CPF, "529.982.247-25"))
                .thenReturn("52998224725");
        when(proprietarioRepository.findByEmpresaIdAndDocumento(12, "52998224725"))
                .thenReturn(Optional.of(proprietario(31, 12)));

        var response = proprietarioService.getByDocumento(TipoDocumento.CPF, "529.982.247-25");

        assertEquals("52998224725", response.documento());
        assertEquals("Ana Souza", response.nome());
        verify(proprietarioRepository).findByEmpresaIdAndDocumento(12, "52998224725");
    }

    @Test
    void createValidatesAndAssociatesOwnerWithAuthenticatedCompany() {
        Usuario corretor = autenticarCorretor(7);
        ProprietarioCreateDto createDto = new ProprietarioCreateDto(
                TipoDocumento.CPF, "529.982.247-25", "Ana Souza", "81999999999",
                "ana@example.com", "Prefere contato à tarde");
        when(documentoValidator.validar(TipoDocumento.CPF, createDto.documento())).thenReturn("52998224725");
        when(proprietarioRepository.existsByEmpresaIdAndDocumento(7, "52998224725")).thenReturn(false);
        when(proprietarioRepository.save(any(Proprietario.class))).thenAnswer(invocation -> {
            Proprietario saved = invocation.getArgument(0);
            saved.setId(31);
            return saved;
        });

        var response = proprietarioService.create(createDto);

        assertEquals(31, response.id());
        assertEquals(TipoDocumento.CPF, response.tipoDocumento());
        assertEquals("52998224725", response.documento());
        assertEquals("Prefere contato à tarde", response.observacoes());
        ArgumentCaptor<Proprietario> captor = ArgumentCaptor.forClass(Proprietario.class);
        verify(proprietarioRepository).save(captor.capture());
        assertSame(corretor.getEmpresa(), captor.getValue().getEmpresa());
        assertEquals("ana@example.com", captor.getValue().getEmail());
        verify(documentoValidator).validar(TipoDocumento.CPF, createDto.documento());
    }

    @Test
    void createRejectsDocumentAlreadyRegisteredInAuthenticatedCompany() {
        autenticarCorretor(7);
        ProprietarioCreateDto createDto = new ProprietarioCreateDto(
                TipoDocumento.CNPJ, "11.222.333/0001-81", "Empresa Proprietária", "81333333333",
                "contato@example.com", null);
        when(documentoValidator.validar(TipoDocumento.CNPJ, createDto.documento()))
                .thenReturn("11222333000181");
        when(proprietarioRepository.existsByEmpresaIdAndDocumento(7, "11222333000181")).thenReturn(true);

        assertThrows(ProprietarioAlreadyExistsException.class, () -> proprietarioService.create(createDto));

        verify(proprietarioRepository, never()).save(any(Proprietario.class));
    }

    @Test
    void createRejectsUserWithoutBrokerProfile() {
        autenticarUsuario(UsuarioPerfil.GESTOR, 7);

        assertThrows(UnauthorizedOperationException.class, () -> proprietarioService.create(
                new ProprietarioCreateDto(TipoDocumento.CPF, "529.982.247-25", "Ana Souza",
                        "81999999999", "ana@example.com", null)));

        verifyNoInteractions(proprietarioRepository, documentoValidator);
    }

    private Usuario autenticarCorretor(Integer empresaId) {
        Usuario usuario = criarUsuario(UsuarioPerfil.CORRETOR, empresaId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
        return usuario;
    }

    private void autenticarUsuario(UsuarioPerfil perfil, Integer empresaId) {
        Usuario usuario = criarUsuario(perfil, empresaId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private Usuario criarUsuario(UsuarioPerfil perfil, Integer empresaId) {
        Empresa empresa = Empresa.builder().id(empresaId).build();
        return new Usuario(empresa, "Usuário", "usuario@example.com", "hash",
                perfil, UsuarioStatus.ATIVO, LocalDateTime.now());
    }

    private Proprietario proprietario(Integer id, Integer empresaId) {
        return Proprietario.builder()
                .id(id)
                .empresa(Empresa.builder().id(empresaId).build())
                .tipoDocumento(TipoDocumento.CPF)
                .documento("52998224725")
                .nome("Ana Souza")
                .telefone("81999999999")
                .email("ana@example.com")
                .observacoes("Prefere contato à tarde")
                .build();
    }
}
