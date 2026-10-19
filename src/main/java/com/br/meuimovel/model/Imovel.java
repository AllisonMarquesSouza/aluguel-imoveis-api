package com.br.meuimovel.model;

import com.br.meuimovel.enums.FinalidadeImovel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "imovel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Imovel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 180)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corretor_id", nullable = false)
    private Corretor corretor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietario_id", nullable = false)
    private Proprietario proprietario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FinalidadeImovel finalidade;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "imovel_caracteristica",
            joinColumns = @JoinColumn(name = "imovel_id"),
            inverseJoinColumns = @JoinColumn(name = "caracteristica_id")
    )
    @Builder.Default
    private Set<Caracteristica> caracteristicas = new HashSet<>();

    @OneToOne(
            mappedBy = "imovel",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private EnderecoImovel endereco;

    @OneToMany(
            mappedBy = "imovel",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<FotoImovel> fotos = new ArrayList<>();


    @Column(name = "valor_venda", precision = 15, scale = 2)
    private BigDecimal valorVenda;

    @Column(name = "valor_aluguel", precision = 15, scale = 2)
    private BigDecimal valorAluguel;

    @Column(name = "valor_condominio", precision = 15, scale = 2)
    private BigDecimal valorCondominio;

    @Column(name = "valor_iptu", precision = 15, scale = 2)
    private BigDecimal valorIptu;

    @Column(columnDefinition = "TEXT")
    private String descricao;

}
