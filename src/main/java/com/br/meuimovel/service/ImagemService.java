package com.br.meuimovel.service;

import com.br.meuimovel.dtos.imagem.ImagemResponseDto;
import com.br.meuimovel.enums.TipoImagem;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.EmpresaNotFoundException;
import com.br.meuimovel.exception.EmpresaInativaException;
import com.br.meuimovel.exception.ImagemAlreadyExistsException;
import com.br.meuimovel.exception.ImagemInvalidaException;
import com.br.meuimovel.exception.ImagemNotFoundException;
import com.br.meuimovel.exception.ImovelNotFoundException;
import com.br.meuimovel.exception.UnauthorizedOperationException;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.FotoImovel;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CorretorRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.FotoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import com.br.meuimovel.storage.FileStorageService;
import com.br.meuimovel.storage.ImagemUrl;
import com.br.meuimovel.storage.ImagemUrl.ReferenciaImagem;
import com.br.meuimovel.storage.StoredImage;
import com.br.meuimovel.validator.FotoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ImagemService {
    private final FileStorageService fileStorageService;
    private final FotoValidator fotoValidator;
    private final EmpresaRepository empresaRepository;
    private final CorretorRepository corretorRepository;
    private final ImovelRepository imovelRepository;
    private final FotoImovelRepository fotoImovelRepository;

    @Transactional(readOnly = true)
    public StoredImage buscar(TipoImagem tipo, Integer entidadeId, String imagemId) {
        resolverAssociacao(tipo, entidadeId, imagemId);
        return fileStorageService.carregar(tipo, entidadeId, imagemId);
    }

    @Transactional
    public ImagemResponseDto adicionar(TipoImagem tipo, Integer entidadeId, MultipartFile arquivo) {
        Usuario usuario = getUsuarioAutenticado();
        fotoValidator.validar(arquivo);

        return switch (tipo) {
            case EMPRESA -> adicionarLogoEmpresa(entidadeId, arquivo, usuario);
            case CORRETOR -> adicionarFotoCorretor(entidadeId, arquivo, usuario);
            case IMOVEL -> adicionarFotoImovel(entidadeId, arquivo, usuario);
        };
    }

    @Transactional
    public ImagemResponseDto atualizar(TipoImagem tipo, Integer entidadeId, String imagemId, MultipartFile arquivo) {
        ImagemAssociada associada = resolverAssociacao(tipo, entidadeId, imagemId);
        autorizarAlteracao(associada, getUsuarioAutenticado());
        fotoValidator.validar(arquivo);
        fileStorageService.substituir(tipo, entidadeId, imagemId, arquivo);
        return resposta(tipo, entidadeId, imagemId);
    }

    @Transactional
    public void remover(TipoImagem tipo, Integer entidadeId, String imagemId) {
        ImagemAssociada associada = resolverAssociacao(tipo, entidadeId, imagemId);
        autorizarAlteracao(associada, getUsuarioAutenticado());

        switch (tipo) {
            case EMPRESA -> {
                associada.empresa().setLogoMarcaUrl(null);
                empresaRepository.save(associada.empresa());
            }
            case CORRETOR -> throw new ImagemInvalidaException(
                    "A foto do corretor é obrigatória; envie outra imagem para substituí-la.");
            case IMOVEL -> removerFotoImovel(associada.foto());
        }

        fileStorageService.deletar(tipo, entidadeId, imagemId);
    }

    private ImagemResponseDto adicionarLogoEmpresa(Integer empresaId, MultipartFile arquivo, Usuario usuario) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EmpresaNotFoundException("Empresa não encontrada."));
        autorizarEmpresa(usuario, empresa.getId());
        if (!empresa.isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para alterar imagens.");
        }
        if (empresa.getLogoMarcaUrl() != null) {
            throw new ImagemAlreadyExistsException("A empresa já possui uma logomarca; use PUT para atualizá-la.");
        }

        String url = fileStorageService.salvarImagem(arquivo, TipoImagem.EMPRESA, empresaId);
        try {
            empresa.setLogoMarcaUrl(url);
            empresaRepository.save(empresa);
        } catch (RuntimeException exception) {
            tentarExcluir(url);
            throw exception;
        }
        return resposta(TipoImagem.EMPRESA, empresaId, extrairImagemId(url));
    }

    private ImagemResponseDto adicionarFotoCorretor(Integer corretorId, MultipartFile arquivo, Usuario usuario) {
        Corretor corretor = corretorRepository.findById(corretorId)
                .orElseThrow(() -> new ImagemNotFoundException("Corretor não encontrado."));
        Integer empresaId = empresaId(corretor.getUsuario());
        autorizarGestorDaEmpresa(usuario, empresaId);
        if (corretor.getUsuario().getEmpresa() == null || !corretor.getUsuario().getEmpresa().isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para alterar imagens.");
        }
        if (corretor.getFotoUrl() != null) {
            throw new ImagemAlreadyExistsException("O corretor já possui foto; use PUT para atualizá-la.");
        }

        String url = fileStorageService.salvarImagem(arquivo, TipoImagem.CORRETOR, corretorId);
        try {
            corretor.setFotoUrl(url);
            corretorRepository.save(corretor);
        } catch (RuntimeException exception) {
            tentarExcluir(url);
            throw exception;
        }
        return resposta(TipoImagem.CORRETOR, corretorId, extrairImagemId(url));
    }

    private ImagemResponseDto adicionarFotoImovel(Integer imovelId, MultipartFile arquivo, Usuario usuario) {
        Imovel imovel = carregarImovel(imovelId);
        autorizarImovel(usuario, imovel.getEmpresa().getId());
        if (!imovel.getEmpresa().isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para alterar imagens.");
        }
        List<FotoImovel> fotos = fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(imovelId);
        int ordem = fotos.stream().map(FotoImovel::getOrdem).max(Integer::compareTo).orElse(0) + 1;
        boolean capa = fotos.stream().noneMatch(FotoImovel::isCapa);

        String url = fileStorageService.salvarImagem(arquivo, TipoImagem.IMOVEL, imovelId);
        try {
            FotoImovel foto = new FotoImovel();
            foto.setImovel(imovel);
            foto.setUrl(url);
            foto.setOrdem(ordem);
            foto.setCapa(capa);
            fotoImovelRepository.save(foto);
        } catch (RuntimeException exception) {
            tentarExcluir(url);
            throw exception;
        }
        return resposta(TipoImagem.IMOVEL, imovelId, extrairImagemId(url));
    }

    private void removerFotoImovel(FotoImovel foto) {
        List<FotoImovel> fotos = fotoImovelRepository.findAllByImovel_IdOrderByOrdemAsc(foto.getImovel().getId());
        if (fotos.size() <= 1) {
            throw new ImagemInvalidaException("O imóvel deve manter ao menos uma foto.");
        }

        if (foto.isCapa()) {
            fotos.stream()
                    .filter(outra -> !Objects.equals(outra.getId(), foto.getId()))
                    .min(Comparator.comparing(FotoImovel::getOrdem))
                    .ifPresent(novaCapa -> {
                        novaCapa.setCapa(true);
                        fotoImovelRepository.save(novaCapa);
                    });
        }
        fotoImovelRepository.delete(foto);
    }

    private ImagemAssociada resolverAssociacao(TipoImagem tipo, Integer entidadeId, String imagemId) {
        String url = ImagemUrl.criar(tipo, entidadeId, imagemId);
        return switch (tipo) {
            case EMPRESA -> empresaRepository.findByIdAndLogoMarcaUrl(entidadeId, url)
                    .map(empresa -> new ImagemAssociada(tipo, entidadeId, empresa.getId(), empresa, null, null, null))
                    .orElseThrow(() -> new ImagemNotFoundException("Imagem não encontrada."));
            case CORRETOR -> corretorRepository.findByIdAndFotoUrl(entidadeId, url)
                    .map(corretor -> new ImagemAssociada(tipo, entidadeId, empresaId(corretor.getUsuario()),
                            null, corretor, null, null))
                    .orElseThrow(() -> new ImagemNotFoundException("Imagem não encontrada."));
            case IMOVEL -> fotoImovelRepository.findByImovel_IdAndUrl(entidadeId, url)
                    .map(foto -> new ImagemAssociada(tipo, entidadeId, foto.getImovel().getEmpresa().getId(),
                            null, null, foto.getImovel(), foto))
                    .orElseThrow(() -> new ImagemNotFoundException("Imagem não encontrada."));
        };
    }

    private void autorizarAlteracao(ImagemAssociada associada, Usuario usuario) {
        switch (associada.tipo()) {
            case EMPRESA -> autorizarEmpresa(usuario, associada.empresaId());
            case CORRETOR -> autorizarGestorDaEmpresa(usuario, associada.empresaId());
            case IMOVEL -> autorizarImovel(usuario, associada.empresaId());
        }
    }

    private void autorizarEmpresa(Usuario usuario, Integer empresaId) {
        if (usuario.getPerfil() == UsuarioPerfil.ADMINISTRADOR) {
            return;
        }
        autorizarGestorDaEmpresa(usuario, empresaId);
    }

    private void autorizarGestorDaEmpresa(Usuario usuario, Integer empresaId) {
        if (usuario.getPerfil() != UsuarioPerfil.GESTOR
                || !Objects.equals(empresaId(usuario), empresaId)) {
            throw new UnauthorizedOperationException("A operação exige um gestor da empresa proprietária da imagem.");
        }
    }

    private void autorizarImovel(Usuario usuario, Integer empresaId) {
        if (usuario.getPerfil() != UsuarioPerfil.ADMINISTRADOR
                && (usuario.getPerfil() != UsuarioPerfil.CORRETOR
                || !Objects.equals(empresaId(usuario), empresaId))) {
            throw new UnauthorizedOperationException("A operação exige um corretor da empresa proprietária do imóvel.");
        }
    }

    private Imovel carregarImovel(Integer imovelId) {
        return imovelRepository.findById(imovelId)
                .orElseThrow(() -> new ImovelNotFoundException("Imóvel não encontrado."));
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Usuario usuario)
                || usuario.getStatus() != UsuarioStatus.ATIVO) {
            throw new UnauthorizedOperationException("Usuário ativo autenticado é obrigatório.");
        }
        return usuario;
    }

    private Integer empresaId(Usuario usuario) {
        return usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId();
    }

    private ImagemResponseDto resposta(TipoImagem tipo, Integer entidadeId, String imagemId) {
        return new ImagemResponseDto(imagemId, tipo, entidadeId,
                ImagemUrl.criar(tipo, entidadeId, imagemId));
    }

    private String extrairImagemId(String url) {
        ReferenciaImagem referencia = ImagemUrl.analisar(url);
        return referencia.imagemId();
    }

    private void tentarExcluir(String url) {
        try {
            fileStorageService.deletar(url);
        } catch (RuntimeException ignored) {
            // Preserva o erro original da associação.
        }
    }

    private record ImagemAssociada(
            TipoImagem tipo,
            Integer entidadeId,
            Integer empresaId,
            Empresa empresa,
            Corretor corretor,
            Imovel imovel,
            FotoImovel foto
    ) {}
}
