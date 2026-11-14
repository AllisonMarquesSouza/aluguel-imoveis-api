CREATE TABLE terreno
(
    imovel_id        INTEGER        PRIMARY KEY,
    area_total       NUMERIC(15, 2) NOT NULL,
    frente           NUMERIC(15, 2) NOT NULL,
    fundo            NUMERIC(15, 2) NOT NULL,
    lateral_esquerda NUMERIC(15, 2) NOT NULL,
    lateral_direita  NUMERIC(15, 2) NOT NULL,
    topografia       VARCHAR(20)    NOT NULL,
    muro             BOOLEAN        NOT NULL,
    esquina          BOOLEAN        NOT NULL,
    uso_permitido    TEXT           NOT NULL,

    CONSTRAINT fk_terreno_imovel
        FOREIGN KEY (imovel_id)
            REFERENCES imovel (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    CONSTRAINT ck_terreno_medidas
        CHECK (area_total > 0 AND frente > 0 AND fundo > 0
            AND lateral_esquerda > 0 AND lateral_direita > 0),
    CONSTRAINT ck_terreno_topografia
        CHECK (topografia IN ('PLANO', 'ACLIVE', 'DECLIVE')),
    CONSTRAINT ck_terreno_uso_permitido
        CHECK (length(trim(uso_permitido)) > 0)
);
