package com.br.meuimovel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "corretor")
@Getter
@Setter
public class Corretor {

    @Id
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "foto_url", nullable = false)
    private String fotoUrl;

    @Column(nullable = false, unique = true)
    private String creci;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String whatsapp;

    @Column(nullable = false)
    private String apresentacao;

    public Corretor(Usuario usuario, String fotoUrl, String creci, String telefone, String whatsapp, String apresentacao) {
        this.usuario = usuario;
        this.fotoUrl = fotoUrl;
        this.creci = creci;
        this.telefone = telefone;
        this.whatsapp = whatsapp;
        this.apresentacao = apresentacao;
    }
}