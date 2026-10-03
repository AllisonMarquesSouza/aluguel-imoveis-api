package com.br.meuimovel.model;

import com.br.meuimovel.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proprietario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Proprietario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(name = "empresa_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 4)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 14)
    private String documento;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(nullable = false)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

}
//tipo_documento VARCHAR(4)   NOT NULL,
//documento      VARCHAR(14)  NOT NULL,
//nome           VARCHAR(150) NOT NULL,
//telefone       VARCHAR(20)  NOT NULL,
//email          VARCHAR(255) NOT NULL,