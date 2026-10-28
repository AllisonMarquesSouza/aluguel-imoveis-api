package com.br.meuimovel.service;

import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.dtos.corretor.CorretorCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaAutonomaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaImobiliariaCreateDto;
import com.br.meuimovel.dtos.empresa.EmpresaUpdateDto;
import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.TipoEmpresa;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.exception.CreciAlreadyExistsException;
import com.br.meuimovel.exception.EmpresaAlreadyExistsException;
import com.br.meuimovel.exception.EmpresaNotFoundException;
import com.br.meuimovel.exception.EmpresaStatusAlreadyModifiedException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.exception.UsuarioAlreadyExistsException;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Gestor;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CorretorRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.GestorRepository;
import com.br.meuimovel.repository.UsuarioRepository;
import com.br.meuimovel.storage.FileStorageService;
import com.br.meuimovel.validator.DocumentoValidator;
import com.br.meuimovel.validator.FotoValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {
    private static final String LOGO_IMAGEM_ID = "c4bf744d-a873-4ef0-b1f2-b1532bfb31db";
    private static final String LOGO_URL_50 = "/imagens/empresa/50/" + LOGO_IMAGEM_ID;

    @Mock private EmpresaRepository empresaRepository;
    @Mock private CidadeService cidadeService;
    @Mock private DocumentoValidator documentoValidator;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CorretorRepository corretorRepository;
    @Mock private GestorRepository gestorRepository;
    @Mock private FotoValidator fotoValidator;
    @Mock private FileStorageService fileStorageService;
    @InjectMocks private EmpresaService empresaService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administratorCanReadCompanyWithoutBeingAssignedToOne() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        Empresa empresa = Empresa.builder().id(5).build();
        when(empresaRepository.findById(5)).thenReturn(Optional.of(empresa));

        assertSame(empresa, empresaService.getById(5));
    }

    @Test
    void tenantCannotReadAnotherCompanyByChangingTheId() {
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());

        assertThrows(EmpresaNotFoundException.class, () -> empresaService.getById(8));

        verifyNoInteractions(empresaRepository);
    }

    @Test
    void onlyAdministratorCanListAllCompanies() {
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());

        assertThrows(UnauthorizedOperationException.class, empresaService::listAll);

        verifyNoInteractions(empresaRepository);
    }

    @Test
    void getResponseMapsCompanyCitiesToCityResponseDtos() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(4);
        Empresa empresa = Empresa.builder()
                .id(5)
                .cidadeBase(cidade)
                .cidadesAtuacao(Set.of(cidade))
                .build();
        when(empresaRepository.findById(5)).thenReturn(Optional.of(empresa));

        var response = empresaService.getResponseById(5);

        CidadeResponseDto cidadeResponse = new CidadeResponseDto(4, "Recife", "PE");
        assertEquals(cidadeResponse, response.cidadeBase());
        assertEquals(Set.of(cidadeResponse), Set.copyOf(response.cidadesAtuacao()));
    }

    @Test
    void listAllReturnsResponseDtosForAdministrator() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        Empresa empresa = Empresa.builder().id(5).nome("Imobiliária Recife").build();
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        var response = empresaService.listAll();

        assertEquals(1, response.size());
        assertEquals(5, response.get(0).id());
        assertEquals("Imobiliária Recife", response.get(0).nome());
        verify(empresaRepository).findAll();
    }

    @Test
    void getByIdRejectsUnauthenticatedCaller() {
        assertThrows(UnauthorizedOperationException.class, () -> empresaService.getById(5));

        verifyNoInteractions(empresaRepository);
    }

    @Test
    void getByIdThrowsNotFoundWhenCompanyDoesNotExist() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        when(empresaRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(EmpresaNotFoundException.class, () -> empresaService.getById(5));
    }

    @Test
    void createImobiliariaSavesCompanyManagerAndLogo() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        EmpresaCreateDto empresaDto = empresaCreateDto(10, Set.of(11));
        EmpresaImobiliariaCreateDto createDto = new EmpresaImobiliariaCreateDto(
                "Gestora", "gestora@example.com", "senha-segura-123", empresaDto);
        MockMultipartFile logo = imagem("logo.png");
        Cidade cidadeBase = cidade(10, "Recife");
        Cidade cidadeAtuacao = cidade(11, "Olinda");
        when(documentoValidator.validar(TipoDocumento.CNPJ, empresaDto.documento())).thenReturn("11222333000181");
        when(empresaRepository.existsByDocumento("11222333000181")).thenReturn(false);
        when(usuarioRepository.existsByEmail("gestora@example.com")).thenReturn(false);
        when(empresaRepository.existsByCreci(empresaDto.creci())).thenReturn(false);
        when(passwordEncoder.encode("senha-segura-123")).thenReturn("senha-hash");
        when(cidadeService.getById(10)).thenReturn(cidadeBase);
        when(cidadeService.getAllById(Set.of(11))).thenReturn(Set.of(cidadeAtuacao));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa saved = invocation.getArgument(0);
            saved.setId(50);
            return saved;
        });
        when(fileStorageService.salvarLogoEmpresa(logo, 50)).thenReturn(LOGO_URL_50);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario saved = invocation.getArgument(0);
            saved.setId(80);
            return saved;
        });

        var response = empresaService.createImobiliaria(createDto, logo);

        assertEquals(50, response.id());
        assertEquals(TipoEmpresa.IMOBILIARIA, response.tipo());
        assertEquals(LOGO_URL_50, response.logoMarcaUrl());
        assertEquals(LOGO_IMAGEM_ID, response.logoImagemId());
        assertEquals("Recife", response.cidadeBase().nome());
        assertEquals(List.of(new CidadeResponseDto(11, "Olinda", "PE")), response.cidadesAtuacao());

        var empresaCaptor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(empresaCaptor.capture());
        assertEquals("11222333000181", empresaCaptor.getValue().getDocumento());
        assertTrue(empresaCaptor.getValue().isAtiva());

        var usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertSame(empresaCaptor.getValue(), usuarioCaptor.getValue().getEmpresa());
        assertEquals(UsuarioPerfil.GESTOR, usuarioCaptor.getValue().getPerfil());
        assertEquals("senha-hash", usuarioCaptor.getValue().getSenha());
        verify(gestorRepository).save(any(Gestor.class));
        verify(fotoValidator).validar(logo);
        verify(fileStorageService).salvarLogoEmpresa(logo, 50);
    }

    @Test
    void createImobiliariaRejectsDuplicateCompanyDocument() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        EmpresaCreateDto empresaDto = empresaCreateDto(null, Set.of(11));
        when(documentoValidator.validar(TipoDocumento.CNPJ, empresaDto.documento())).thenReturn("11222333000181");
        when(empresaRepository.existsByDocumento("11222333000181")).thenReturn(true);

        assertThrows(EmpresaAlreadyExistsException.class, () ->
                empresaService.createImobiliaria(
                        new EmpresaImobiliariaCreateDto("Gestora", "gestora@example.com", "senha-segura-123", empresaDto),
                        imagem("logo.png")));

        verifyNoInteractions(fotoValidator, passwordEncoder, fileStorageService, cidadeService, gestorRepository);
    }

    @Test
    void createImobiliariaRejectsDuplicateManagerEmail() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        EmpresaCreateDto empresaDto = empresaCreateDto(null, Set.of(11));
        when(documentoValidator.validar(TipoDocumento.CNPJ, empresaDto.documento())).thenReturn("11222333000181");
        when(empresaRepository.existsByDocumento("11222333000181")).thenReturn(false);
        when(usuarioRepository.existsByEmail("gestora@example.com")).thenReturn(true);

        assertThrows(UsuarioAlreadyExistsException.class, () ->
                empresaService.createImobiliaria(
                        new EmpresaImobiliariaCreateDto("Gestora", "gestora@example.com", "senha-segura-123", empresaDto),
                        imagem("logo.png")));

        verify(empresaRepository, never()).save(any(Empresa.class));
        verifyNoInteractions(fotoValidator, passwordEncoder, fileStorageService, cidadeService, gestorRepository);
    }

    @Test
    void createImobiliariaRejectsDuplicateCreci() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        EmpresaCreateDto empresaDto = empresaCreateDto(null, Set.of(11));
        when(documentoValidator.validar(TipoDocumento.CNPJ, empresaDto.documento())).thenReturn("11222333000181");
        when(empresaRepository.existsByDocumento("11222333000181")).thenReturn(false);
        when(usuarioRepository.existsByEmail("gestora@example.com")).thenReturn(false);
        when(empresaRepository.existsByCreci(empresaDto.creci())).thenReturn(true);

        assertThrows(CreciAlreadyExistsException.class, () ->
                empresaService.createImobiliaria(
                        new EmpresaImobiliariaCreateDto("Gestora", "gestora@example.com", "senha-segura-123", empresaDto),
                        imagem("logo.png")));

        verifyNoInteractions(fotoValidator, passwordEncoder, fileStorageService, cidadeService, gestorRepository);
    }

    @Test
    void createImobiliariaRejectsNonAdministrator() {
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());

        assertThrows(UnauthorizedOperationException.class, () ->
                empresaService.createImobiliaria(
                        new EmpresaImobiliariaCreateDto(
                                "Gestora", "gestora@example.com", "senha-segura-123", empresaCreateDto(null, Set.of(11))),
                        imagem("logo.png")));

        verifyNoInteractions(empresaRepository, usuarioRepository, fotoValidator, passwordEncoder,
                fileStorageService, cidadeService, gestorRepository);
    }

    @Test
    void createAutonomaSavesCompanyUserBrokerAndBothPhotos() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        EmpresaCreateDto empresaDto = empresaCreateDto(null, Set.of(11));
        CorretorCreateDto corretorDto = new CorretorCreateDto(
                "Corretor", "corretor@example.com", "senha-segura-456", "CRECI-2",
                "81999999999", "81988888888", "Atendimento imobiliário");
        EmpresaAutonomaCreateDto createDto = new EmpresaAutonomaCreateDto(corretorDto, empresaDto);
        MockMultipartFile logo = imagem("logo.png");
        MockMultipartFile fotoCorretor = imagem("corretor.png");
        Cidade cidadeAtuacao = cidade(11, "Olinda");
        when(documentoValidator.validar(TipoDocumento.CNPJ, empresaDto.documento())).thenReturn("11222333000181");
        when(empresaRepository.existsByDocumento("11222333000181")).thenReturn(false);
        when(usuarioRepository.existsByEmail(corretorDto.email())).thenReturn(false);
        when(corretorRepository.existsByCreci(corretorDto.creci())).thenReturn(false);
        when(passwordEncoder.encode(corretorDto.senha())).thenReturn("senha-hash");
        when(cidadeService.getAllById(Set.of(11))).thenReturn(Set.of(cidadeAtuacao));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa saved = invocation.getArgument(0);
            saved.setId(50);
            return saved;
        });
        when(fileStorageService.salvarLogoEmpresa(logo, 50)).thenReturn(LOGO_URL_50);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario saved = invocation.getArgument(0);
            saved.setId(80);
            return saved;
        });
        when(fileStorageService.salvarFotoCorretor(fotoCorretor, 80))
                .thenReturn("/imagens/corretor/80/84c8304e-9e5d-4b81-b8d5-90a53c6171bc");

        var response = empresaService.createAutonoma(createDto, logo, fotoCorretor);

        assertEquals(50, response.id());
        assertEquals(TipoEmpresa.AUTONOMO, response.tipo());
        assertEquals(LOGO_URL_50, response.logoMarcaUrl());
        assertEquals(LOGO_IMAGEM_ID, response.logoImagemId());
        assertEquals(List.of(new CidadeResponseDto(11, "Olinda", "PE")), response.cidadesAtuacao());

        var usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals(UsuarioPerfil.CORRETOR, usuarioCaptor.getValue().getPerfil());
        assertEquals("senha-hash", usuarioCaptor.getValue().getSenha());
        assertEquals(response.id(), usuarioCaptor.getValue().getEmpresa().getId());

        var corretorCaptor = ArgumentCaptor.forClass(Corretor.class);
        verify(corretorRepository).save(corretorCaptor.capture());
        assertEquals("/imagens/corretor/80/84c8304e-9e5d-4b81-b8d5-90a53c6171bc",
                corretorCaptor.getValue().getFotoUrl());
        assertEquals("CRECI-2", corretorCaptor.getValue().getCreci());
        verify(fotoValidator, times(2)).validar(any());
        verify(fileStorageService).salvarLogoEmpresa(logo, 50);
        verify(fileStorageService).salvarFotoCorretor(fotoCorretor, 80);
        verifyNoInteractions(gestorRepository);
    }

    @Test
    void createAutonomaRejectsDuplicateBrokerCreci() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        EmpresaCreateDto empresaDto = empresaCreateDto(null, Set.of(11));
        CorretorCreateDto corretorDto = new CorretorCreateDto(
                "Corretor", "corretor@example.com", "senha-segura-456", "CRECI-2",
                "81999999999", "81988888888", "Atendimento imobiliário");
        when(documentoValidator.validar(TipoDocumento.CNPJ, empresaDto.documento())).thenReturn("11222333000181");
        when(empresaRepository.existsByDocumento("11222333000181")).thenReturn(false);
        when(usuarioRepository.existsByEmail(corretorDto.email())).thenReturn(false);
        when(corretorRepository.existsByCreci(corretorDto.creci())).thenReturn(true);

        assertThrows(CreciAlreadyExistsException.class, () ->
                empresaService.createAutonoma(
                        new EmpresaAutonomaCreateDto(corretorDto, empresaDto), imagem("logo.png"), imagem("corretor.png")));

        verify(empresaRepository, never()).save(any(Empresa.class));
        verifyNoInteractions(fotoValidator, passwordEncoder, fileStorageService, cidadeService, gestorRepository);
    }

    @Test
    void createAutonomaRejectsNonAdministrator() {
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());

        assertThrows(UnauthorizedOperationException.class, () ->
                empresaService.createAutonoma(
                        new EmpresaAutonomaCreateDto(corretorCreateDto(), empresaCreateDto(null, Set.of(11))),
                        imagem("logo.png"), imagem("corretor.png")));

        verifyNoInteractions(empresaRepository, usuarioRepository, corretorRepository, gestorRepository,
                fotoValidator, passwordEncoder, fileStorageService, cidadeService);
    }

    @Test
    void createAutonomaRejectsDuplicateCompanyDocument() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        EmpresaCreateDto empresaDto = empresaCreateDto(null, Set.of(11));
        CorretorCreateDto corretorDto = corretorCreateDto();
        when(documentoValidator.validar(TipoDocumento.CNPJ, empresaDto.documento())).thenReturn("11222333000181");
        when(empresaRepository.existsByDocumento("11222333000181")).thenReturn(true);

        assertThrows(EmpresaAlreadyExistsException.class, () ->
                empresaService.createAutonoma(
                        new EmpresaAutonomaCreateDto(corretorDto, empresaDto), imagem("logo.png"), imagem("corretor.png")));

        verifyNoInteractions(fotoValidator, passwordEncoder, fileStorageService, cidadeService, gestorRepository);
    }

    @Test
    void createAutonomaRejectsDuplicateBrokerEmail() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        EmpresaCreateDto empresaDto = empresaCreateDto(null, Set.of(11));
        CorretorCreateDto corretorDto = corretorCreateDto();
        when(documentoValidator.validar(TipoDocumento.CNPJ, empresaDto.documento())).thenReturn("11222333000181");
        when(empresaRepository.existsByDocumento("11222333000181")).thenReturn(false);
        when(usuarioRepository.existsByEmail(corretorDto.email())).thenReturn(true);

        assertThrows(UsuarioAlreadyExistsException.class, () ->
                empresaService.createAutonoma(
                        new EmpresaAutonomaCreateDto(corretorDto, empresaDto), imagem("logo.png"), imagem("corretor.png")));

        verify(empresaRepository, never()).save(any(Empresa.class));
        verifyNoInteractions(fotoValidator, passwordEncoder, fileStorageService, cidadeService, gestorRepository);
    }

    @Test
    void updateAppliesCompanyFieldsCitiesAndNewLogo() {
        Empresa empresa = empresaTenant(7, "documento-antigo");
        empresa.setLogoMarcaUrl("/uploads/empresas/7/logomarca-antiga.png");
        autenticar(UsuarioPerfil.GESTOR, empresaTenant(7, "outro-documento"));
        when(empresaRepository.findById(7)).thenReturn(Optional.of(empresa));
        when(empresaRepository.existsByDocumento("documento-novo")).thenReturn(false);
        Cidade cidadeBase = cidade(12, "Recife");
        Cidade cidadeAtuacao1 = cidade(13, "Olinda");
        Cidade cidadeAtuacao2 = cidade(14, "Jaboatão");
        when(cidadeService.getById(12)).thenReturn(cidadeBase);
        when(cidadeService.getAllById(Set.of(13, 14))).thenReturn(Set.of(cidadeAtuacao1, cidadeAtuacao2));
        MockMultipartFile logo = imagem("logo-nova.png");
        when(fileStorageService.salvarLogoEmpresa(logo, 7))
                .thenReturn("/imagens/empresa/7/0f17a05b-95f3-49fc-8730-4fbb5345e840");
        EmpresaUpdateDto updateDto = new EmpresaUpdateDto(
                TipoEmpresa.AUTONOMO, "Nome atualizado", "Razão atualizada", TipoDocumento.CNPJ,
                "documento-novo", "CRECI-ATUALIZADO", null, "Descrição atualizada",
                "81999999999", "81988888888", "nova@example.com", "Rua nova", "100",
                "Centro", "50000000", 12, Set.of(13, 14));

        empresaService.update(7, updateDto, logo);

        assertEquals(TipoEmpresa.AUTONOMO, empresa.getTipo());
        assertEquals("Nome atualizado", empresa.getNome());
        assertEquals("Razão atualizada", empresa.getRazaoSocial());
        assertEquals(TipoDocumento.CNPJ, empresa.getTipoDocumento());
        assertEquals("documento-novo", empresa.getDocumento());
        assertEquals("CRECI-ATUALIZADO", empresa.getCreci());
        assertEquals("Descrição atualizada", empresa.getDescricao());
        assertEquals("81999999999", empresa.getTelefone());
        assertEquals("81988888888", empresa.getWhatsapp());
        assertEquals("nova@example.com", empresa.getEmail());
        assertEquals("Rua nova", empresa.getLogradouro());
        assertEquals("100", empresa.getNumero());
        assertEquals("Centro", empresa.getBairro());
        assertEquals("50000000", empresa.getCep());
        assertSame(cidadeBase, empresa.getCidadeBase());
        assertEquals(Set.of(cidadeAtuacao1, cidadeAtuacao2), empresa.getCidadesAtuacao());
        assertEquals("/imagens/empresa/7/0f17a05b-95f3-49fc-8730-4fbb5345e840", empresa.getLogoMarcaUrl());
        verify(empresaRepository).save(empresa);
        verify(fotoValidator).validar(logo);
        verify(fileStorageService).salvarLogoEmpresa(logo, 7);
        verify(fileStorageService).deletar("/uploads/empresas/7/logomarca-antiga.png");
    }

    @Test
    void updateRejectsDuplicateDocument() {
        Empresa empresa = empresaTenant(7, "documento-antigo");
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());
        when(empresaRepository.findById(7)).thenReturn(Optional.of(empresa));
        when(empresaRepository.existsByDocumento("documento-duplicado")).thenReturn(true);
        EmpresaUpdateDto updateDto = updateDto("documento-duplicado");

        assertThrows(EmpresaAlreadyExistsException.class, () -> empresaService.update(7, updateDto, null));

        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    void updateAllowsUnchangedDocumentEvenWhenItAlreadyExists() {
        Empresa empresa = empresaTenant(7, "documento-existente");
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());
        when(empresaRepository.findById(7)).thenReturn(Optional.of(empresa));
        when(empresaRepository.existsByDocumento("documento-existente")).thenReturn(true);

        empresaService.update(7, updateDto("documento-existente"), null);

        assertEquals("documento-existente", empresa.getDocumento());
        verify(empresaRepository).save(empresa);
    }

    @Test
    void updateRejectsIdFromAnotherTenantBeforeRepositoryLookup() {
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());

        assertThrows(EmpresaNotFoundException.class,
                () -> empresaService.update(8, updateDto(null), null));

        verifyNoInteractions(empresaRepository);
    }

    @Test
    void activateChangesInactiveCompanyToActive() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        Empresa empresa = Empresa.builder().id(5).ativa(false).build();
        when(empresaRepository.findById(5)).thenReturn(Optional.of(empresa));

        empresaService.activate(5);

        assertTrue(empresa.isAtiva());
    }

    @Test
    void activateRejectsCompanyThatIsAlreadyActive() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        when(empresaRepository.findById(5)).thenReturn(Optional.of(Empresa.builder().id(5).ativa(true).build()));

        assertThrows(EmpresaStatusAlreadyModifiedException.class, () -> empresaService.activate(5));
    }

    @Test
    void activateRejectsNonAdministrator() {
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());

        assertThrows(UnauthorizedOperationException.class, () -> empresaService.activate(5));

        verifyNoInteractions(empresaRepository);
    }

    @Test
    void inactivateChangesActiveCompanyToInactive() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        Empresa empresa = Empresa.builder().id(5).ativa(true).build();
        when(empresaRepository.findById(5)).thenReturn(Optional.of(empresa));

        empresaService.inactivate(5);

        assertFalse(empresa.isAtiva());
    }

    @Test
    void inactivateRejectsCompanyThatIsAlreadyInactive() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        when(empresaRepository.findById(5)).thenReturn(Optional.of(Empresa.builder().id(5).ativa(false).build()));

        assertThrows(EmpresaStatusAlreadyModifiedException.class, () -> empresaService.inactivate(5));
    }

    @Test
    void inactivateRejectsNonAdministrator() {
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());

        assertThrows(UnauthorizedOperationException.class, () -> empresaService.inactivate(5));

        verifyNoInteractions(empresaRepository);
    }

    @Test
    void deleteByIdDeletesCompanyAfterFindingIt() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        when(empresaRepository.findById(5)).thenReturn(Optional.of(Empresa.builder().id(5).build()));

        empresaService.deleteById(5);

        verify(empresaRepository).deleteById(5);
    }

    @Test
    void deleteByIdDoesNotDeleteMissingCompany() {
        autenticar(UsuarioPerfil.ADMINISTRADOR, null);
        when(empresaRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(EmpresaNotFoundException.class, () -> empresaService.deleteById(5));

        verify(empresaRepository, never()).deleteById(5);
    }

    @Test
    void deleteByIdRejectsNonAdministrator() {
        autenticar(UsuarioPerfil.GESTOR, Empresa.builder().id(7).build());

        assertThrows(UnauthorizedOperationException.class, () -> empresaService.deleteById(5));

        verifyNoInteractions(empresaRepository);
    }

    private void autenticar(UsuarioPerfil perfil, Empresa empresa) {
        Usuario usuario = new Usuario(empresa, "Usuário", "usuario@example.com", "hash",
                perfil, UsuarioStatus.ATIVO, LocalDateTime.now());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private EmpresaCreateDto empresaCreateDto(Integer cidadeBaseId, Set<Integer> cidadesAtuacaoIds) {
        return new EmpresaCreateDto(
                "Imobiliária Recife", "Imobiliária Recife LTDA", TipoDocumento.CNPJ,
                "11.222.333/0001-81", "CRECI-1", "Descrição", "81999999999", "81988888888",
                "empresa@example.com", "Rua Principal", "10", "Centro", "50000000",
                cidadeBaseId, cidadesAtuacaoIds);
    }

    private CorretorCreateDto corretorCreateDto() {
        return new CorretorCreateDto(
                "Corretor", "corretor@example.com", "senha-segura-456", "CRECI-2",
                "81999999999", "81988888888", "Atendimento imobiliário");
    }

    private EmpresaUpdateDto updateDto(String documento) {
        return new EmpresaUpdateDto(null, null, null, null, documento, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    private Empresa empresaTenant(Integer id, String documento) {
        return Empresa.builder().id(id).documento(documento).cidadesAtuacao(new HashSet<>()).build();
    }

    private Cidade cidade(Integer id, String nome) {
        Cidade cidade = new Cidade(nome, Uf.PE);
        cidade.setId(id);
        return cidade;
    }

    private MockMultipartFile imagem(String nome) {
        return new MockMultipartFile("arquivo", nome, "image/png", new byte[]{1, 2, 3});
    }
}
