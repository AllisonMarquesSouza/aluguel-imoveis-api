package com.br.meuimovel.model;

import com.br.meuimovel.enums.SituacaoImovel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_situacao_imovel")
@Getter
@Setter
@NoArgsConstructor
public class HistoricoSituacaoImovel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_anterior", nullable = false, length = 20)
    private SituacaoImovel situacaoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_nova", nullable = false, length = 20)
    private SituacaoImovel situacaoNova;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "alterado_em", nullable = false)
    private LocalDateTime alteradoEm;
}
