package com.br.meuimovel.model;

import com.br.meuimovel.enums.SituacaoImovel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lote")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lote {
    @Id
    @Column(name = "imovel_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quadra_id", nullable = false)
    private Quadra quadra;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal area;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal frente;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal fundo;

    @Column(name = "condicoes_parcelamento", nullable = false, columnDefinition = "TEXT")
    private String condicoesParcelamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SituacaoImovel situacao = SituacaoImovel.DISPONIVEL;
}
