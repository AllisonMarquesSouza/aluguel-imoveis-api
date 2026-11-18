package com.br.meuimovel.model;

import com.br.meuimovel.enums.TipoDocumentoImovel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento_imovel", uniqueConstraints = {
        @UniqueConstraint(name = "uq_documento_imovel_tipo", columnNames = {"imovel_id", "tipo"})
})
@Getter
@Setter
@NoArgsConstructor
public class DocumentoImovel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDocumentoImovel tipo;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "arquivo_id", nullable = false, unique = true, length = 36)
    private String arquivoId;

    @Column(name = "data_documento", nullable = false)
    private LocalDate dataDocumento;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
