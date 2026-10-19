package com.br.meuimovel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "foto_imovel")
@Getter
@Setter
public class FotoImovel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false)
    private boolean capa;
}
