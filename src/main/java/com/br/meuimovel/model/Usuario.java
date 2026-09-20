package com.br.meuimovel.model;

import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(name = "empresa_id")
    @ManyToOne
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true)//modificar esses tamanhos 254 para 255 e remover aqui
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private UsuarioPerfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UsuarioStatus status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
//id    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
//empresa_id    INTEGER,
//nome          VARCHAR(150)             NOT NULL,
//email         VARCHAR(254)             NOT NULL UNIQUE,
//senha_hash    VARCHAR(255)             NOT NULL,
//perfil        VARCHAR(15)              NOT NULL,
//status        VARCHAR(10)              NOT NULL DEFAULT 'ATIVO',
//criado_em     TIMESTAMP NOT NULL,