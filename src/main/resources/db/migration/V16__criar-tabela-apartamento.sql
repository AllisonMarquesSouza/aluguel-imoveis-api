CREATE TABLE apartamento
(
    imovel_id      INTEGER        PRIMARY KEY,
    predio_id      INTEGER        NOT NULL,
    andar          INTEGER        NOT NULL,
    numero         VARCHAR(30)    NOT NULL,
    area_privativa NUMERIC(15, 2) NOT NULL,
    area_total     NUMERIC(15, 2) NOT NULL,
    quartos        INTEGER        NOT NULL,
    suites         INTEGER        NOT NULL,
    banheiros      INTEGER        NOT NULL,
    vagas          INTEGER        NOT NULL,
    varanda        BOOLEAN        NOT NULL,
    posicao_solar  VARCHAR(40)    NOT NULL,

    CONSTRAINT fk_apartamento_imovel
        FOREIGN KEY (imovel_id)
            REFERENCES imovel (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_apartamento_predio
        FOREIGN KEY (predio_id)
            REFERENCES predio (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT,

    CONSTRAINT uq_apartamento_predio_andar_numero
        UNIQUE (predio_id, andar, numero),

    CONSTRAINT ck_apartamento_localizacao
        CHECK (andar >= 0 AND length(trim(numero)) > 0),

    CONSTRAINT ck_apartamento_areas
        CHECK (area_privativa > 0 AND area_total >= area_privativa),

    CONSTRAINT ck_apartamento_quantidades
        CHECK (quartos >= 0 AND suites >= 0 AND banheiros >= 0 AND vagas >= 0),

    CONSTRAINT ck_apartamento_posicao_solar
        CHECK (length(trim(posicao_solar)) > 0)
);
