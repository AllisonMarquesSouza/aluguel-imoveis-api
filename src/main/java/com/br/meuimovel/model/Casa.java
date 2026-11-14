package com.br.meuimovel.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "casa")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Casa {

    @Id
    @Column(name = "imovel_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @Column(name = "area_terreno", nullable = false, precision = 15, scale = 2)
    private BigDecimal areaTerreno;

    @Column(name = "area_construida", nullable = false, precision = 15, scale = 2)
    private BigDecimal areaConstruida;

    @Column(nullable = false)
    private Integer quartos;

    @Column(nullable = false)
    private Integer suites;

    @Column(nullable = false)
    private Integer banheiros;

    @Column(nullable = false)
    private Integer salas;

    @Column(nullable = false)
    private Integer vagas;

    @Column(nullable = false)
    private Integer pavimentos;

    @Column(name = "ano_construcao", nullable = false)
    private Integer anoConstrucao;

    @Column(nullable = false)
    private Boolean piscina;

    @Column(nullable = false)
    private Boolean quintal;

    @Column(nullable = false)
    private Boolean churrasqueira;

    @Column(nullable = false)
    private Boolean mobiliada;
}
