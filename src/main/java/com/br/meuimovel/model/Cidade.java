package com.br.meuimovel.model;

import com.br.meuimovel.enums.Uf;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cidade")
@Getter
@Setter
@NoArgsConstructor
public class Cidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Uf uf;

    public Cidade(String nome, Uf uf){
        this.nome = nome;
        this.uf = uf;
    }

}
