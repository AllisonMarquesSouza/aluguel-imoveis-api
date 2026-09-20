CREATE TABLE usuario
(
    id    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id    INTEGER,
    nome          VARCHAR(150)             NOT NULL,
    email         VARCHAR(255)             NOT NULL UNIQUE,
    senha         VARCHAR(255)             NOT NULL,
    perfil        VARCHAR(15)              NOT NULL,
    status        VARCHAR(10)              NOT NULL DEFAULT 'ATIVO',
    criado_em     TIMESTAMP NOT NULL,

    CONSTRAINT fk_usuario_empresa
        FOREIGN KEY (empresa_id)
            REFERENCES empresa (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT,

    CONSTRAINT ck_usuario_perfil
        CHECK (perfil IN ('ADMINISTRADOR', 'GESTOR', 'CORRETOR', 'CLIENTE')),

    CONSTRAINT ck_usuario_status
        CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADO')),

    CONSTRAINT ck_usuario_empresa_por_perfil
        CHECK (
            (perfil = 'ADMINISTRADOR' AND empresa_id IS NULL)
                OR
            (perfil <> 'ADMINISTRADOR' AND empresa_id IS NOT NULL)
            )
);
