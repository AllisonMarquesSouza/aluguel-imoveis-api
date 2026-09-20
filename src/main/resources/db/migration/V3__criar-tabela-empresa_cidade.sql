create table empresa_cidade(
    empresa_id INTEGER NOT NULL,
    cidade_id INTEGER NOT NULL,

    CONSTRAINT pk_empresa_cidade
        PRIMARY KEY (empresa_id, cidade_id),
--os dois vao ser chaves primarias/ composta e unicos
-- A ideia é que uma empresa nao pode ser associada duas vezes a uma cidade

    CONSTRAINT fk_empresa_cidade_empresa
        FOREIGN KEY (empresa_id)
            REFERENCES empresa (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_empresa_cidade_cidade
        FOREIGN KEY (cidade_id)
            REFERENCES cidade (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);
