package com.br.meuimovel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quadra")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quadra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loteamento_id", nullable = false)
    private Loteamento loteamento;

    @Column(nullable = false, length = 50)
    private String identificacao;
}
