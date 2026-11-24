package com.br.meuimovel.model;

import com.br.meuimovel.enums.CanalContato;
import com.br.meuimovel.enums.RetornoContato;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "contato_imovel", indexes = {
        @Index(name = "ix_contato_imovel_corretor_criado_em", columnList = "corretor_id, criado_em"),
        @Index(name = "ix_contato_imovel_imovel_criado_em", columnList = "imovel_id, criado_em")
})
@Getter
@Setter
@NoArgsConstructor
public class ContatoImovel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corretor_id", nullable = false)
    private Corretor corretor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalContato canal;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RetornoContato retorno;
}
