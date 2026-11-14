CREATE TABLE casa
(
    imovel_id       INTEGER        PRIMARY KEY,
    area_terreno    NUMERIC(15, 2) NOT NULL,
    area_construida NUMERIC(15, 2) NOT NULL,
    quartos         INTEGER        NOT NULL,
    suites          INTEGER        NOT NULL,
    banheiros       INTEGER        NOT NULL,
    salas           INTEGER        NOT NULL,
    vagas           INTEGER        NOT NULL,
    pavimentos      INTEGER        NOT NULL,
    ano_construcao  INTEGER        NOT NULL,
    piscina         BOOLEAN        NOT NULL,
    quintal         BOOLEAN        NOT NULL,
    churrasqueira   BOOLEAN        NOT NULL,
    mobiliada       BOOLEAN        NOT NULL,

    CONSTRAINT fk_casa_imovel
        FOREIGN KEY (imovel_id)
            REFERENCES imovel (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    CONSTRAINT ck_casa_areas
        CHECK (area_terreno > 0 AND area_construida > 0 AND area_construida <= area_terreno),
    CONSTRAINT ck_casa_quantidades
        CHECK (quartos >= 0 AND suites >= 0 AND banheiros >= 0 AND salas >= 0
            AND vagas >= 0 AND pavimentos >= 0 AND ano_construcao >= 0)
);
