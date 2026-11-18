CREATE TABLE lote
(
    imovel_id                INTEGER        PRIMARY KEY,
    quadra_id                INTEGER        NOT NULL,
    numero                   VARCHAR(30)    NOT NULL,
    area                     NUMERIC(15, 2) NOT NULL,
    frente                   NUMERIC(15, 2) NOT NULL,
    fundo                    NUMERIC(15, 2) NOT NULL,
    condicoes_parcelamento   TEXT           NOT NULL,
    situacao                 VARCHAR(20)    NOT NULL DEFAULT 'DISPONIVEL',

    CONSTRAINT fk_lote_imovel
        FOREIGN KEY (imovel_id)
            REFERENCES imovel (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_lote_quadra
        FOREIGN KEY (quadra_id)
            REFERENCES quadra (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT,

    CONSTRAINT uq_lote_quadra_numero
        UNIQUE (quadra_id, numero),

    CONSTRAINT ck_lote_numero
        CHECK (length(trim(numero)) > 0),

    CONSTRAINT ck_lote_medidas
        CHECK (area > 0 AND frente > 0 AND fundo > 0),

    CONSTRAINT ck_lote_condicoes_parcelamento
        CHECK (length(trim(condicoes_parcelamento)) > 0),

    CONSTRAINT ck_lote_situacao
        CHECK (situacao IN ('DISPONIVEL', 'VENDIDO'))
);
