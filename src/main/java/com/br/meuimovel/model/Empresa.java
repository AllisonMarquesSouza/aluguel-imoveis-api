package com.br.meuimovel.model;

import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.TipoEmpresa;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "empresa")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoEmpresa tipo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "razao_social", length = 180)
    private String razaoSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 4)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, unique = true, length = 14)
    private String documento;

    @Column(length = 30)
    private String creci;

    @Column(name = "logomarca_url", length = 500)
    private String logoMarcaUrl;

    @Column
    private String descricao;

    @Column(length = 20)
    private String telefone;

    @Column(length = 20)
    private String whatsapp;

    @Column(nullable = false)
    private String email;

    @Column(length = 180)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 100)
    private String bairro;

    @Column(length = 8)
    private String cep;

    @JoinColumn(name = "cidade_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Cidade cidadeBase;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "empresa_cidade",
            joinColumns = @JoinColumn(
                    name = "empresa_id",
                    referencedColumnName = "id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "cidade_id",
                    referencedColumnName = "id"
            )
    )
    private Set<Cidade> cidadesAtuacao = new HashSet<>();

    @Column(nullable = false)
    private boolean ativa;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

}
