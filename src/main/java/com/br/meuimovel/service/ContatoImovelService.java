package com.br.meuimovel.service;

import com.br.meuimovel.dtos.contato.ContatoImovelCreateDto;
import com.br.meuimovel.dtos.contato.ContatoImovelRegistroResponseDto;
import com.br.meuimovel.dtos.contato.ContatoImovelResponseDto;
import com.br.meuimovel.dtos.contato.ContatoImovelRetornoUpdateDto;
import com.br.meuimovel.enums.CanalContato;
import com.br.meuimovel.enums.SituacaoImovel;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.exception.*;
import com.br.meuimovel.model.ContatoImovel;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Imovel;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.ContatoImovelRepository;
import com.br.meuimovel.repository.ImovelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContatoImovelService {
    private final ContatoImovelRepository contatoRepository;
    private final ImovelRepository imovelRepository;

    @Transactional
    public ContatoImovelRegistroResponseDto registrarContato(Integer imovelId, ContatoImovelCreateDto dto) {
        if (dto == null || dto.canal() == null) {
            throw new ContatoImovelInvalidoException("Informe o canal do contato.");
        }
        Imovel imovel = imovelRepository.findByIdAndPublicadoTrueAndSituacaoAndCorretor_Usuario_Status(
                        imovelId, SituacaoImovel.DISPONIVEL, UsuarioStatus.ATIVO)
                .orElseThrow(() -> new ImovelNotFoundException("Imóvel não encontrado na vitrine."));
        String whatsappUrl = dto.canal() == CanalContato.WHATSAPP
                ? criarLinkWhatsApp(imovel.getCorretor(), imovel) : null;

        ContatoImovel contato = new ContatoImovel();
        contato.setImovel(imovel);
        contato.setCorretor(imovel.getCorretor());
        contato.setCanal(dto.canal());
        contato.setCriadoEm(LocalDateTime.now());
        ContatoImovel salvo = contatoRepository.save(contato);

        return new ContatoImovelRegistroResponseDto(salvo.getCanal(), whatsappUrl);
    }

    @Transactional(readOnly = true)
    public List<ContatoImovelResponseDto> listar(Integer imovelId, LocalDate dataInicio, LocalDate dataFim) {
        Usuario usuario = getUsuarioComAcessoCaixa();
        validarFiltros(imovelId, dataInicio, dataFim);

        Integer corretorId = usuario.getPerfil() == UsuarioPerfil.CORRETOR ? usuario.getId() : null;
        LocalDateTime inicio = dataInicio == null ? null : dataInicio.atStartOfDay();
        LocalDateTime fimExclusivo = dataFim == null ? null : dataFim.plusDays(1).atStartOfDay();
        return contatoRepository.listarCaixa(usuario.getEmpresa().getId(), corretorId,
                        imovelId, inicio, fimExclusivo)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ContatoImovelResponseDto atualizarRetorno(Integer contatoId, ContatoImovelRetornoUpdateDto dto) {
        Usuario corretor = getCorretorAutenticado();
        if (dto == null || dto.retorno() == null) {
            throw new ContatoImovelInvalidoException("Informe o retorno do contato.");
        }
        ContatoImovel contato = contatoRepository.findByIdAndCorretor_IdAndImovel_Empresa_Id(
                        contatoId, corretor.getId(), corretor.getEmpresa().getId())
                .orElseThrow(() -> new ContatoImovelNotFoundException("Contato não encontrado nesta caixa."));
        contato.setRetorno(dto.retorno());
        return toResponse(contatoRepository.save(contato));
    }

    private void validarFiltros(Integer imovelId, LocalDate dataInicio, LocalDate dataFim) {
        if (imovelId != null && imovelId < 1) {
            throw new ContatoImovelInvalidoException("Informe um imóvel válido para filtrar os contatos.");
        }
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new ContatoImovelInvalidoException("A data inicial não pode ser posterior à data final.");
        }
    }

    private Usuario getUsuarioComAcessoCaixa() {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario.getPerfil() != UsuarioPerfil.CORRETOR && usuario.getPerfil() != UsuarioPerfil.GESTOR) {
            throw new UnauthorizedOperationException("A operação exige um corretor ou gestor ativo.");
        }
        validarEmpresaAtiva(usuario);
        return usuario;
    }

    private Usuario getCorretorAutenticado() {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario.getPerfil() != UsuarioPerfil.CORRETOR) {
            throw new UnauthorizedOperationException("A operação exige o corretor responsável pelo contato.");
        }
        validarEmpresaAtiva(usuario);
        return usuario;
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof Usuario usuario)
                || usuario.getStatus() != UsuarioStatus.ATIVO
                || usuario.getEmpresa() == null) {
            throw new UnauthorizedOperationException("A operação exige um usuário ativo vinculado a uma empresa.");
        }
        return usuario;
    }

    private void validarEmpresaAtiva(Usuario usuario) {
        if (!usuario.getEmpresa().isAtiva()) {
            throw new EmpresaInativaException("A empresa deve estar ativa para acessar os contatos.");
        }
    }

    private String criarLinkWhatsApp(Corretor corretor, Imovel imovel) {
        String telefone = corretor.getWhatsapp().replaceAll("\\D", "");
        if (telefone.isBlank()) {
            throw new ContatoImovelInvalidoException("O corretor não possui um número de WhatsApp válido.");
        }
        if (telefone.length() == 10 || telefone.length() == 11) {
            telefone = "55" + telefone;
        }
        if (telefone.length() < 12 || telefone.length() > 15) {
            throw new ContatoImovelInvalidoException("O corretor não possui um número de WhatsApp válido.");
        }
        String mensagem = "Olá, tenho interesse no imóvel " + imovel.getCodigo() + " - " + imovel.getTitulo() + ".";
        return "https://wa.me/" + telefone + "?text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
    }

    private ContatoImovelResponseDto toResponse(ContatoImovel contato) {
        return new ContatoImovelResponseDto(
                contato.getId(), contato.getImovel().getId(), contato.getImovel().getCodigo(),
                contato.getImovel().getTitulo(), contato.getCorretor().getId(),
                contato.getCorretor().getUsuario().getNome(), contato.getCanal(),
                contato.getCriadoEm(), contato.getRetorno());
    }
}
