package com.br.meuimovel.model;

import com.br.meuimovel.enums.TopografiaTerreno;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "terreno")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Terreno {

    @Id
    @Column(name = "imovel_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @Column(name = "area_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal areaTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal frente;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal fundo;

    @Column(name = "lateral_esquerda", nullable = false, precision = 15, scale = 2)
    private BigDecimal lateralEsquerda;

    @Column(name = "lateral_direita", nullable = false, precision = 15, scale = 2)
    private BigDecimal lateralDireita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TopografiaTerreno topografia;

    @Column(nullable = false)
    private Boolean muro;

    @Column(nullable = false)
    private Boolean esquina;

    @Column(name = "uso_permitido", nullable = false, columnDefinition = "TEXT")
    private String usoPermitido;
}
