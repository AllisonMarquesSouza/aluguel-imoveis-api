package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imovel.EnderecoImovelDto;
import com.br.meuimovel.dtos.imovel.CasaCreateDto;
import com.br.meuimovel.dtos.imovel.ApartamentoCreateDto;
import com.br.meuimovel.dtos.imovel.ImovelCreateDto;
import com.br.meuimovel.dtos.imovel.ImovelResponseDto;
import com.br.meuimovel.dtos.imovel.LoteCreateDto;
import com.br.meuimovel.dtos.imovel.TerrenoCreateDto;
import com.br.meuimovel.dtos.caracteristica.CaracteristicaResponseDto;
import com.br.meuimovel.enums.FinalidadeImovel;
import com.br.meuimovel.enums.NaturezaImovel;
import com.br.meuimovel.enums.SituacaoLote;
import com.br.meuimovel.enums.TopografiaTerreno;
import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ApartamentoAlreadyExistsException;
import com.br.meuimovel.exception.LoteAlreadyExistsException;
import com.br.meuimovel.exception.PredioNotFoundException;
import com.br.meuimovel.exception.QuadraNotFoundException;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.exception.CaracteristicaNotFoundException;
import com.br.meuimovel.model.*;
import com.br.meuimovel.repository.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImovelServiceTest {
    private static final String PRIMEIRA_IMAGEM_ID = "c4bf744d-a873-4ef0-b1f2-b1532bfb31db";
    private static final String SEGUNDA_IMAGEM_ID = "4bbf744d-a873-4ef0-b1f2-b1532bfb31db";

    @Mock private ImovelRepository imovelRepository;
    @Mock private PredioRepository predioRepository;
    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private QuadraRepository quadraRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private ProprietarioRepository proprietarioRepository;
    @Mock private CorretorRepository corretorRepository;
    @Mock private CidadeRepository cidadeRepository;
    @Mock private CaracteristicaRepository caracteristicaRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private FotoImovelRepository fotoImovelRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private FotoValidator fotoValidator;
    @InjectMocks private ImovelService imovelService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsSaleWithoutSalePrice() {
        autenticarCorretor();
        ImovelCreateDto dto = dto(FinalidadeImovel.VENDA, null, null, 1);

        assertThrows(ImovelInvalidoException.class, () -> imovelService.create(dto, List.of()));

        verifyNoInteractions(imovelRepository);
    }

    @Test
    void createRejectsDualPurposeWithoutBothPrices() {
        autenticarCorretor();
        ImovelCreateDto dto = dto(FinalidadeImovel.VENDA_E_ALUGUEL, BigDecimal.valueOf(100_000), null, 1);

        ImovelInvalidoException exception = assertThrows(
                ImovelInvalidoException.class,
                () -> imovelService.create(dto, List.of())
        );

        assertTrue(exception.getMessage().contains("aluguel"));
        verifyNoInteractions(imovelRepository);
    }

    @Test
    void createRejectsHouseWithBuiltAreaLargerThanLandArea() {
        autenticarCorretor();
        ImovelCreateDto base = dto(FinalidadeImovel.VENDA, BigDecimal.valueOf(100_000), null, 1);
        CasaCreateDto casa = casaDto(BigDecimal.valueOf(100), BigDecimal.valueOf(120));
        ImovelCreateDto invalidDto = new ImovelCreateDto(base.codigo(), base.titulo(), base.proprietarioId(),
                base.finalidade(), base.valorVenda(), base.valorAluguel(), base.valorCondominio(), base.valorIptu(),
                base.descricao(), NaturezaImovel.CASA, casa, null, base.endereco(), base.caracteristicasIds(),
                base.fotoCapaOrdem(), null, null);

        ImovelInvalidoException exception = assertThrows(
                ImovelInvalidoException.class,
                () -> imovelService.create(invalidDto, List.of(foto("frente.jpg"))));

        assertEquals("A área construída não pode ser maior que a área do terreno.", exception.getMessage());
        verifyNoInteractions(imovelRepository);
    }

    @Test
    void createRejectsDetailsThatDoNotMatchSelectedNature() {
        autenticarCorretor();
        ImovelCreateDto base = dto(FinalidadeImovel.VENDA, BigDecimal.valueOf(100_000), null, 1);
        ImovelCreateDto casaWithLandDetails = new ImovelCreateDto(base.codigo(), base.titulo(),
                base.proprietarioId(), base.finalidade(), base.valorVenda(), base.valorAluguel(), base.valorCondominio(),
                base.valorIptu(), base.descricao(), NaturezaImovel.CASA,
                casaDto(BigDecimal.valueOf(100), BigDecimal.valueOf(80)), terrenoDto(),
                base.endereco(), base.caracteristicasIds(), base.fotoCapaOrdem(), null, null);

        ImovelInvalidoException exception = assertThrows(
                ImovelInvalidoException.class,
                () -> imovelService.create(casaWithLandDetails, List.of(foto("frente.jpg"))));

        assertTrue(exception.getMessage().contains("apenas os dados de casa"));
        verifyNoInteractions(imovelRepository);
    }

    @Test
    void createAssociatesSelectedCharacteristicsAndSavesPhotosInOrderWithCover() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Proprietario proprietario = Proprietario.builder()
                .id(15)
                .empresa(empresa)
                .tipoDocumento(TipoDocumento.CPF)
                .documento("12345678901")
                .nome("Proprietário")
                .telefone("81999999999")
                .email("proprietario@example.com")
                .build();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        Caracteristica caracteristica = new Caracteristica("Piscina");
        caracteristica.setId(30);

        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "IM-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade));
        when(empresaRepository.atuaNaCidade(empresa.getId(), 20)).thenReturn(true);
        when(caracteristicaRepository.findAllById(Set.of(30))).thenReturn(List.of(caracteristica));
        when(imovelRepository.save(any(Imovel.class))).thenAnswer(invocation -> {
            Imovel imovel = invocation.getArgument(0);
            imovel.setId(42);
            return imovel;
        });
        when(fileStorageService.salvarFotoImovel(any(), eq(42), anyInt()))
                .thenAnswer(invocation -> "/imagens/imovel/42/" + (invocation.getArgument(2).equals(1)
                        ? PRIMEIRA_IMAGEM_ID
                        : SEGUNDA_IMAGEM_ID));

        List<FotoImovel> fotosSalvas = new ArrayList<>();
        when(fotoImovelRepository.save(any(FotoImovel.class))).thenAnswer(invocation -> {
            FotoImovel foto = invocation.getArgument(0);
            foto.setId(fotosSalvas.size() + 1);
            fotosSalvas.add(foto);
            return foto;
        });
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(42))
                .thenAnswer(invocation -> List.copyOf(fotosSalvas));

        List<MockMultipartFile> arquivos = List.of(
                foto("frente.jpg"),
                foto("sala.jpg")
        );
        ImovelCreateDto dto = dto(FinalidadeImovel.VENDA, BigDecimal.valueOf(100_000), null, 2, Set.of(30));

        ImovelResponseDto response = imovelService.create(dto, new ArrayList<>(arquivos));

        assertEquals(Set.of(new CaracteristicaResponseDto(30, "Piscina")), response.caracteristicas());
        assertEquals(NaturezaImovel.CASA, response.natureza());
        assertEquals(BigDecimal.valueOf(100), response.casa().areaLivre());
        assertNull(response.terreno());
        assertEquals(2, response.fotos().size());
        assertEquals(List.of(1, 2), response.fotos().stream().map(foto -> foto.ordem()).toList());
        assertEquals(List.of(false, true), response.fotos().stream().map(foto -> foto.capa()).toList());
        assertEquals(List.of(1, 2), response.fotos().stream().map(foto -> foto.id()).toList());
        assertEquals(List.of(PRIMEIRA_IMAGEM_ID, SEGUNDA_IMAGEM_ID),
                response.fotos().stream().map(foto -> foto.imagemId()).toList());
        verify(fileStorageService).salvarFotoImovel(arquivos.get(0), 42, 1);
        verify(fileStorageService).salvarFotoImovel(arquivos.get(1), 42, 2);
    }

    @Test
    void createStoresAndReturnsTerrainDetailsWithoutHouseFields() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Proprietario proprietario = Proprietario.builder()
                .id(15).empresa(empresa).tipoDocumento(TipoDocumento.CPF).documento("52998224725")
                .nome("Ana Souza").telefone("81999999999").email("ana@example.com").build();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);

        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "IM-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade));
        when(empresaRepository.atuaNaCidade(empresa.getId(), 20)).thenReturn(true);
        when(imovelRepository.save(any(Imovel.class))).thenAnswer(invocation -> {
            Imovel imovel = invocation.getArgument(0);
            imovel.setId(42);
            return imovel;
        });
        when(fileStorageService.salvarFotoImovel(any(), eq(42), eq(1)))
                .thenReturn("/imagens/imovel/42/" + PRIMEIRA_IMAGEM_ID);
        when(fotoImovelRepository.save(any(FotoImovel.class))).thenAnswer(invocation -> {
            FotoImovel foto = invocation.getArgument(0);
            foto.setId(1);
            return foto;
        });
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(42))
                .thenAnswer(invocation -> List.of(fotoEntity(42)));

        ImovelCreateDto base = dto(FinalidadeImovel.VENDA, BigDecimal.valueOf(100_000), null, 1);
        ImovelCreateDto terrenoDto = new ImovelCreateDto(base.codigo(), base.titulo(), base.proprietarioId(),
                base.finalidade(), base.valorVenda(), base.valorAluguel(), base.valorCondominio(), base.valorIptu(),
                base.descricao(), NaturezaImovel.TERRENO, null, terrenoDto(), base.endereco(),
                base.caracteristicasIds(), base.fotoCapaOrdem(), null, null);

        ImovelResponseDto response = imovelService.create(terrenoDto, List.of(foto("frente.jpg")));

        assertEquals(NaturezaImovel.TERRENO, response.natureza());
        assertNotNull(response.terreno());
        assertEquals("Uso residencial", response.terreno().usoPermitido());
        assertNull(response.casa());
    }

    @Test
    void createLotLinksSubdivisionAndBlockAndReturnsTheirData() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Proprietario proprietario = Proprietario.builder()
                .id(15).empresa(empresa).tipoDocumento(TipoDocumento.CPF).documento("52998224725")
                .nome("Ana Souza").telefone("81999999999").email("ana@example.com").build();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        Quadra quadra = quadra(empresa, cidade);

        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "LO-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(quadraRepository.findByIdAndLoteamento_Empresa_Id(90, empresa.getId()))
                .thenReturn(Optional.of(quadra));
        when(loteRepository.existsByQuadra_IdAndNumeroIgnoreCase(90, "12A")).thenReturn(false);
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade));
        when(empresaRepository.atuaNaCidade(empresa.getId(), 20)).thenReturn(true);
        when(imovelRepository.save(any(Imovel.class))).thenAnswer(invocation -> {
            Imovel imovel = invocation.getArgument(0);
            imovel.setId(42);
            imovel.getLote().setId(42);
            return imovel;
        });
        when(fileStorageService.salvarFotoImovel(any(), eq(42), eq(1)))
                .thenReturn("/imagens/imovel/42/" + PRIMEIRA_IMAGEM_ID);
        when(fotoImovelRepository.save(any(FotoImovel.class))).thenAnswer(invocation -> {
            FotoImovel foto = invocation.getArgument(0);
            foto.setId(1);
            return foto;
        });
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(42))
                .thenAnswer(invocation -> List.of(fotoEntity(42)));

        ImovelResponseDto response = imovelService.create(loteDto(), List.of(foto("lote.jpg")));

        assertEquals(NaturezaImovel.LOTE, response.natureza());
        assertEquals("12A", response.lote().numero());
        assertEquals(SituacaoLote.DISPONIVEL, response.lote().situacao());
        assertEquals("Quadra A", response.lote().quadra().identificacao());
        assertEquals("Residencial das Palmeiras", response.lote().quadra().loteamento().nome());
        assertEquals("Recife", response.lote().quadra().loteamento().cidade());
        assertEquals("Rua Exemplo", response.endereco().logradouro());
        assertNull(response.apartamento());
        verify(loteRepository).existsByQuadra_IdAndNumeroIgnoreCase(90, "12A");
    }

    @Test
    void createLotRejectsDuplicateNumberInTheSameBlock() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Proprietario proprietario = Proprietario.builder()
                .id(15).empresa(empresa).tipoDocumento(TipoDocumento.CPF).documento("52998224725")
                .nome("Ana Souza").telefone("81999999999").email("ana@example.com").build();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        Quadra quadra = quadra(empresa, cidade);

        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "LO-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(quadraRepository.findByIdAndLoteamento_Empresa_Id(90, empresa.getId()))
                .thenReturn(Optional.of(quadra));
        when(loteRepository.existsByQuadra_IdAndNumeroIgnoreCase(90, "12A")).thenReturn(true);

        assertThrows(LoteAlreadyExistsException.class,
                () -> imovelService.create(loteDto(), List.of(foto("lote.jpg"))));

        verify(imovelRepository, never()).save(any(Imovel.class));
    }

    @Test
    void createLotRejectsBlockFromAnotherCompany() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Proprietario proprietario = Proprietario.builder()
                .id(15).empresa(empresa).tipoDocumento(TipoDocumento.CPF).documento("52998224725")
                .nome("Ana Souza").telefone("81999999999").email("ana@example.com").build();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());

        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "LO-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(quadraRepository.findByIdAndLoteamento_Empresa_Id(90, empresa.getId())).thenReturn(Optional.empty());

        assertThrows(QuadraNotFoundException.class,
                () -> imovelService.create(loteDto(), List.of(foto("lote.jpg"))));

        verify(imovelRepository, never()).save(any(Imovel.class));
    }

    @Test
    void createLotRequiresSalePrice() {
        autenticarCorretor();
        ImovelCreateDto base = loteDto();
        ImovelCreateDto semPrecoDeVenda = new ImovelCreateDto(
                base.codigo(), base.titulo(), base.proprietarioId(), FinalidadeImovel.ALUGUEL,
                null, BigDecimal.valueOf(1200), base.valorCondominio(), base.valorIptu(), base.descricao(),
                NaturezaImovel.LOTE, null, null, base.endereco(), base.caracteristicasIds(),
                base.fotoCapaOrdem(), null, base.lote());

        ImovelInvalidoException exception = assertThrows(ImovelInvalidoException.class,
                () -> imovelService.create(semPrecoDeVenda, List.of(foto("lote.jpg"))));

        assertEquals("Informe o preço de venda para o lote.", exception.getMessage());
        verifyNoInteractions(imovelRepository);
    }

    @Test
    void createApartmentUsesBuildingAddressAndReturnsSharedBuildingDetails() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Proprietario proprietario = Proprietario.builder()
                .id(15).empresa(empresa).tipoDocumento(TipoDocumento.CPF).documento("52998224725")
                .nome("Ana Souza").telefone("81999999999").email("ana@example.com").build();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        Predio predio = predio(empresa, cidade);

        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "AP-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(predioRepository.findByIdAndEmpresa_Id(55, empresa.getId())).thenReturn(Optional.of(predio));
        when(apartamentoRepository.existsByPredio_IdAndAndarAndNumeroIgnoreCase(55, 4, "401A"))
                .thenReturn(false);
        when(empresaRepository.atuaNaCidade(empresa.getId(), 20)).thenReturn(true);
        when(imovelRepository.save(any(Imovel.class))).thenAnswer(invocation -> {
            Imovel imovel = invocation.getArgument(0);
            imovel.setId(42);
            imovel.getApartamento().setId(42);
            return imovel;
        });
        when(fileStorageService.salvarFotoImovel(any(), eq(42), eq(1)))
                .thenReturn("/imagens/imovel/42/" + PRIMEIRA_IMAGEM_ID);
        when(fotoImovelRepository.save(any(FotoImovel.class))).thenAnswer(invocation -> {
            FotoImovel foto = invocation.getArgument(0);
            foto.setId(1);
            return foto;
        });
        when(fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(42))
                .thenAnswer(invocation -> List.of(fotoEntity(42)));

        ImovelCreateDto base = dto(FinalidadeImovel.VENDA, BigDecimal.valueOf(300_000), null, 1);
        ImovelCreateDto createDto = new ImovelCreateDto(
                "AP-1", base.titulo(), base.proprietarioId(), base.finalidade(), base.valorVenda(),
                base.valorAluguel(), base.valorCondominio(), base.valorIptu(), base.descricao(),
                NaturezaImovel.APARTAMENTO, null, null, null, Set.of(), 1,
                new ApartamentoCreateDto(55, 4, " 401a ", BigDecimal.valueOf(80), BigDecimal.valueOf(95),
                        3, 1, 2, 1, true, "Leste"), null);

        ImovelResponseDto response = imovelService.create(createDto, List.of(foto("sala.jpg")));

        assertEquals(NaturezaImovel.APARTAMENTO, response.natureza());
        assertNull(response.casa());
        assertNull(response.terreno());
        assertEquals("401A", response.apartamento().numero());
        assertEquals("Edifício Atlântico", response.apartamento().predio().nome());
        assertEquals("Rua do Sol", response.endereco().logradouro());
        assertNull(response.endereco().id());
        verify(apartamentoRepository).existsByPredio_IdAndAndarAndNumeroIgnoreCase(55, 4, "401A");
    }

    @Test
    void createRejectsDuplicateApartmentWithinBuilding() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Proprietario proprietario = Proprietario.builder()
                .id(15).empresa(empresa).tipoDocumento(TipoDocumento.CPF).documento("52998224725")
                .nome("Ana Souza").telefone("81999999999").email("ana@example.com").build();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        Predio predio = predio(empresa, cidade);
        ImovelCreateDto createDto = apartamentoDto();

        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "AP-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(predioRepository.findByIdAndEmpresa_Id(55, empresa.getId())).thenReturn(Optional.of(predio));
        when(apartamentoRepository.existsByPredio_IdAndAndarAndNumeroIgnoreCase(55, 4, "401A"))
                .thenReturn(true);

        assertThrows(ApartamentoAlreadyExistsException.class,
                () -> imovelService.create(createDto, List.of(foto("sala.jpg"))));

        verify(imovelRepository, never()).save(any(Imovel.class));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void createRejectsBuildingOutsideAuthenticatedCompany() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Proprietario proprietario = Proprietario.builder()
                .id(15).empresa(empresa).tipoDocumento(TipoDocumento.CPF).documento("52998224725")
                .nome("Ana Souza").telefone("81999999999").email("ana@example.com").build();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());
        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "AP-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(predioRepository.findByIdAndEmpresa_Id(55, empresa.getId())).thenReturn(Optional.empty());

        assertThrows(PredioNotFoundException.class,
                () -> imovelService.create(apartamentoDto(), List.of(foto("sala.jpg"))));

        verify(imovelRepository, never()).save(any(Imovel.class));
    }

    @Test
    void createRejectsCharacteristicIdsThatDoNotExist() {
        Usuario usuario = autenticarCorretor();
        Empresa empresa = usuario.getEmpresa();
        Corretor corretor = new Corretor(usuario, "/foto-corretor.jpg", "CRECI-1", "81999999999",
                "81999999999", "Apresentação");
        corretor.setId(usuario.getId());
        Proprietario proprietario = Proprietario.builder()
                .id(15).empresa(empresa).tipoDocumento(TipoDocumento.CPF).documento("52998224725")
                .nome("Ana Souza").telefone("81999999999").email("ana@example.com").build();
        Cidade cidade = new Cidade("Recife", Uf.PE);
        cidade.setId(20);
        when(imovelRepository.existsByEmpresa_IdAndCodigo(empresa.getId(), "IM-1")).thenReturn(false);
        when(corretorRepository.findById(usuario.getId())).thenReturn(Optional.of(corretor));
        when(proprietarioRepository.findByIdAndEmpresa_Id(15, empresa.getId())).thenReturn(Optional.of(proprietario));
        when(cidadeRepository.findById(20)).thenReturn(Optional.of(cidade));
        when(empresaRepository.atuaNaCidade(empresa.getId(), 20)).thenReturn(true);
        when(caracteristicaRepository.findAllById(Set.of(999))).thenReturn(List.of());

        assertThrows(CaracteristicaNotFoundException.class, () -> imovelService.create(
                dto(FinalidadeImovel.VENDA, BigDecimal.valueOf(100_000), null, 1, Set.of(999)),
                List.of(foto("frente.jpg"))));

        verify(imovelRepository, never()).save(any(Imovel.class));
    }

    @Test
    void getByIdScopesLookupToAuthenticatedCompany() {
        Usuario usuario = autenticarCorretor();
        when(imovelRepository.findByIdAndEmpresa_Id(42, usuario.getEmpresa().getId()))
                .thenReturn(Optional.empty());

        assertThrows(ImovelNotFoundException.class, () -> imovelService.getById(42));

        verify(imovelRepository).findByIdAndEmpresa_Id(42, usuario.getEmpresa().getId());
    }

    @Test
    void listByEmpresaUsesAuthenticatedCompany() {
        Usuario usuario = autenticarCorretor();
        when(imovelRepository.findAllByEmpresa_Id(usuario.getEmpresa().getId())).thenReturn(List.of());

        assertTrue(imovelService.listByEmpresa().isEmpty());

        verify(imovelRepository).findAllByEmpresa_Id(usuario.getEmpresa().getId());
    }

    private Usuario autenticarCorretor() {
        Empresa empresa = Empresa.builder().id(7).ativa(true).build();
        Usuario usuario = new Usuario(empresa, "Corretor", "corretor@example.com", "senha",
                UsuarioPerfil.CORRETOR, UsuarioStatus.ATIVO, LocalDateTime.now());
        usuario.setId(9);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
        return usuario;
    }

    private ImovelCreateDto dto(FinalidadeImovel finalidade, BigDecimal valorVenda,
                                BigDecimal valorAluguel, Integer fotoCapaOrdem) {
        return dto(finalidade, valorVenda, valorAluguel, fotoCapaOrdem, Set.of());
    }

    private ImovelCreateDto dto(FinalidadeImovel finalidade, BigDecimal valorVenda,
                                BigDecimal valorAluguel, Integer fotoCapaOrdem, Set<Integer> caracteristicasIds) {
        EnderecoImovelDto endereco = new EnderecoImovelDto(
                "50000000", "Rua Exemplo", "10", null, "Centro", 20,
                BigDecimal.valueOf(-8.05), BigDecimal.valueOf(-34.9));
        return new ImovelCreateDto("IM-1", "Imóvel de teste", 15, finalidade,
                valorVenda, valorAluguel, null, null, "Descrição", NaturezaImovel.CASA,
                casaDto(BigDecimal.valueOf(150), BigDecimal.valueOf(50)), null,
                endereco, caracteristicasIds, fotoCapaOrdem, null, null);
    }

    private ImovelCreateDto loteDto() {
        EnderecoImovelDto endereco = new EnderecoImovelDto(
                "50000000", "Rua Exemplo", "10", null, "Centro", 20,
                BigDecimal.valueOf(-8.05), BigDecimal.valueOf(-34.9));
        return new ImovelCreateDto("LO-1", "Lote residencial", 15, FinalidadeImovel.VENDA,
                BigDecimal.valueOf(150_000), null, null, null, "Descrição", NaturezaImovel.LOTE,
                null, null, endereco, Set.of(), 1, null,
                new LoteCreateDto(90, " 12a ", BigDecimal.valueOf(250), BigDecimal.TEN,
                        BigDecimal.valueOf(25), "Entrada de 30% e saldo em 24 parcelas"));
    }

    private Quadra quadra(Empresa empresa, Cidade cidade) {
        Loteamento loteamento = Loteamento.builder()
                .id(70).empresa(empresa).cidade(cidade).nome("Residencial das Palmeiras")
                .incorporadora("Construtora Horizonte").asfalto(true).agua(true).energia(true)
                .esgoto(true).iluminacaoPublica(true).areasLazer("Praça e playground").build();
        return Quadra.builder().id(90).loteamento(loteamento).identificacao("Quadra A").build();
    }

    private Predio predio(Empresa empresa, Cidade cidade) {
        return Predio.builder()
                .id(55)
                .empresa(empresa)
                .nome("Edifício Atlântico")
                .cep("50000000").logradouro("Rua do Sol").numero("10").bairro("Centro")
                .cidade(cidade).latitude(BigDecimal.valueOf(-8.05)).longitude(BigDecimal.valueOf(-34.9))
                .numeroAndares(10).unidadesPorAndar(4).anoConstrucao(2010).elevadores(2)
                .portaria24h(true).piscina(true).academia(false).salaoFestas(true).playground(false)
                .valorCondominioBase(BigDecimal.valueOf(650)).build();
    }

    private ImovelCreateDto apartamentoDto() {
        return new ImovelCreateDto("AP-1", "Apartamento de teste", 15, FinalidadeImovel.VENDA,
                BigDecimal.valueOf(300_000), null, null, null, "Descrição", NaturezaImovel.APARTAMENTO,
                null, null, null, Set.of(), 1,
                new ApartamentoCreateDto(55, 4, " 401a ", BigDecimal.valueOf(80), BigDecimal.valueOf(95),
                        3, 1, 2, 1, true, "Leste"), null);
    }

    private CasaCreateDto casaDto(BigDecimal areaTerreno, BigDecimal areaConstruida) {
        return new CasaCreateDto(areaTerreno, areaConstruida, 3, 1, 2, 1, 2, 1, 2010,
                true, true, true, false);
    }

    private TerrenoCreateDto terrenoDto() {
        return new TerrenoCreateDto(BigDecimal.valueOf(300), BigDecimal.valueOf(10), BigDecimal.valueOf(10),
                BigDecimal.valueOf(30), BigDecimal.valueOf(30), TopografiaTerreno.PLANO,
                false, false, "Uso residencial");
    }

    private FotoImovel fotoEntity(Integer imovelId) {
        FotoImovel foto = new FotoImovel();
        foto.setId(1);
        foto.setImovel(Imovel.builder().id(imovelId).build());
        foto.setUrl("/imagens/imovel/" + imovelId + "/" + PRIMEIRA_IMAGEM_ID);
        foto.setOrdem(1);
        foto.setCapa(true);
        return foto;
    }

    private MockMultipartFile foto(String nome) {
        return new MockMultipartFile("fotos", nome, "image/jpeg", new byte[]{1, 2, 3});
    }
}
