package com.br.meuimovel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "apartamento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Apartamento {
    @Id
    @Column(name = "imovel_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @Column(nullable = false)
    private Integer andar;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(name = "area_privativa", nullable = false, precision = 15, scale = 2)
    private BigDecimal areaPrivativa;

    @Column(name = "area_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal areaTotal;

    @Column(nullable = false)
    private Integer quartos;

    @Column(nullable = false)
    private Integer suites;

    @Column(nullable = false)
    private Integer banheiros;

    @Column(nullable = false)
    private Integer vagas;

    @Column(nullable = false)
    private Boolean varanda;

    @Column(name = "posicao_solar", nullable = false, length = 40)
    private String posicaoSolar;
}
