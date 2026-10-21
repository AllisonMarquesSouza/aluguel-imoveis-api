package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imovel.EnderecoImovelDto;
import com.br.meuimovel.dtos.imovel.ImovelCreateDto;
import com.br.meuimovel.dtos.imovel.ImovelResponseDto;
import com.br.meuimovel.enums.FinalidadeImovel;
import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.ImovelInvalidoException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.model.*;
import com.br.meuimovel.repository.*;
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
    @Mock private ImovelRepository imovelRepository;
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
    void createSavesPhotosInOrderAndMarksSelectedCover() {
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
        when(fileStorageService.salvarFotoImovel(any(), eq(42), anyInt()))
                .thenAnswer(invocation -> "/uploads/imoveis/42/foto-" + invocation.getArgument(2) + ".jpg");

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
        ImovelCreateDto dto = dto(FinalidadeImovel.VENDA, BigDecimal.valueOf(100_000), null, 2);

        ImovelResponseDto response = imovelService.create(dto, new ArrayList<>(arquivos));

        assertEquals(2, response.fotos().size());
        assertEquals(List.of(1, 2), response.fotos().stream().map(foto -> foto.ordem()).toList());
        assertEquals(List.of(false, true), response.fotos().stream().map(foto -> foto.capa()).toList());
        verify(fileStorageService).salvarFotoImovel(arquivos.get(0), 42, 1);
        verify(fileStorageService).salvarFotoImovel(arquivos.get(1), 42, 2);
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
        EnderecoImovelDto endereco = new EnderecoImovelDto(
                "50000000", "Rua Exemplo", "10", null, "Centro", 20,
                BigDecimal.valueOf(-8.05), BigDecimal.valueOf(-34.9));
        return new ImovelCreateDto("IM-1", "Imóvel de teste", 15, finalidade,
                valorVenda, valorAluguel, null, null, "Descrição", endereco, Set.of(), fotoCapaOrdem);
    }

    private MockMultipartFile foto(String nome) {
        return new MockMultipartFile("fotos", nome, "image/jpeg", new byte[]{1, 2, 3});
    }
}
