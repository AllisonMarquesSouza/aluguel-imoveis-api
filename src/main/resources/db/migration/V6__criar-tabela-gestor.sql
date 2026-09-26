CREATE TABLE gestor
(
    usuario_id    INTEGER PRIMARY KEY,
    CONSTRAINT fk_gestor_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);
-- Se o tiver mais de um gestor por empresa? isolei as tabelas para melhor design