CREATE TABLE imovel_caracteristica
(
    imovel_id INTEGER NOT NULL,
    caracteristica_id INTEGER NOT NULL,

    CONSTRAINT pk_imovel_caracteristica
        PRIMARY KEY (imovel_id, caracteristica_id),

    CONSTRAINT fk_imovel_caracteristica_imovel
        FOREIGN KEY (imovel_id)
            REFERENCES imovel(id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_imovel_caracteristica_caracteristica
        FOREIGN KEY (caracteristica_id)
            REFERENCES caracteristica(id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);
