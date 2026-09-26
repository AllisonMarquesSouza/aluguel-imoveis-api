package com.br.meuimovel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gestor")
@Getter
@Setter
public class Gestor {
    @Id
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Gestor(Usuario usuario) {
        this.usuario = usuario;
    }
}
