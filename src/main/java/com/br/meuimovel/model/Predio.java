package com.br.meuimovel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "predio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Predio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 180)
    private String nome;

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false, length = 180)
    private String logradouro;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(nullable = false, length = 100)
    private String bairro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cidade_id", nullable = false)
    private Cidade cidade;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "numero_andares", nullable = false)
    private Integer numeroAndares;

    @Column(name = "unidades_por_andar", nullable = false)
    private Integer unidadesPorAndar;

    @Column(name = "ano_construcao", nullable = false)
    private Integer anoConstrucao;

    @Column(nullable = false)
    private Integer elevadores;

    @Column(name = "portaria_24h", nullable = false)
    private Boolean portaria24h;

    @Column(nullable = false)
    private Boolean piscina;

    @Column(nullable = false)
    private Boolean academia;

    @Column(name = "salao_festas", nullable = false)
    private Boolean salaoFestas;

    @Column(nullable = false)
    private Boolean playground;

    @Column(name = "valor_condominio_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCondominioBase;
}
