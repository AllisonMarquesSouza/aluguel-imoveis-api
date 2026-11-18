package com.br.meuimovel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loteamento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loteamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cidade_id", nullable = false)
    private Cidade cidade;

    @Column(nullable = false, length = 180)
    private String nome;

    @Column(nullable = false, length = 180)
    private String incorporadora;

    @Column(nullable = false)
    private Boolean asfalto;

    @Column(nullable = false)
    private Boolean agua;

    @Column(nullable = false)
    private Boolean energia;

    @Column(nullable = false)
    private Boolean esgoto;

    @Column(name = "iluminacao_publica", nullable = false)
    private Boolean iluminacaoPublica;

    @Column(name = "areas_lazer", columnDefinition = "TEXT")
    private String areasLazer;
}
