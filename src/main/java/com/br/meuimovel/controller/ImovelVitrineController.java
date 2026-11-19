package com.br.meuimovel.controller;

import com.br.meuimovel.dtos.imovel.ImovelVitrineResponseDto;
import com.br.meuimovel.dtos.imovel.ImovelVitrineFiltro;
import com.br.meuimovel.dtos.CidadeResponseDto;
import com.br.meuimovel.enums.FinalidadeImovel;
import com.br.meuimovel.enums.NaturezaImovel;
import com.br.meuimovel.enums.OrdenacaoVitrine;
import com.br.meuimovel.service.ImovelVitrineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vitrine")
public class ImovelVitrineController {
    private final ImovelVitrineService imovelVitrineService;

    @GetMapping("/cidades")
    public ResponseEntity<List<CidadeResponseDto>> listarCidades() {
        return ResponseEntity.ok(imovelVitrineService.listarCidades());
    }

    @GetMapping("/imoveis")
    public ResponseEntity<Page<ImovelVitrineResponseDto>> buscar(
            @RequestParam Integer cidadeId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) FinalidadeImovel finalidade,
            @RequestParam(required = false) NaturezaImovel natureza,
            @RequestParam(required = false) BigDecimal precoMinimo,
            @RequestParam(required = false) BigDecimal precoMaximo,
            @RequestParam(required = false) Integer quartos,
            @RequestParam(required = false) Integer suites,
            @RequestParam(required = false) Integer vagas,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) BigDecimal areaMinima,
            @RequestParam(required = false) BigDecimal areaMaxima,
            @RequestParam(required = false) List<Integer> caracteristicas,
            @RequestParam(defaultValue = "MAIS_RECENTES") OrdenacaoVitrine ordenacao,
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "12") Integer tamanho
    ) {
        ImovelVitrineFiltro filtro = new ImovelVitrineFiltro(cidadeId, texto, finalidade, natureza,
                precoMinimo, precoMaximo, quartos, suites, vagas, bairro, areaMinima, areaMaxima,
                caracteristicas, ordenacao, pagina, tamanho);
        return ResponseEntity.ok(imovelVitrineService.buscar(filtro));
    }

    @GetMapping("/imoveis/{id}")
    public ResponseEntity<ImovelVitrineResponseDto> buscarDisponivel(@PathVariable Integer id) {
        return ResponseEntity.ok(imovelVitrineService.buscarDisponivel(id));
    }
}
