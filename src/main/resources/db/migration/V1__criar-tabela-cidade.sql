CREATE TABLE cidade
(
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome     VARCHAR(120) NOT NULL,
    uf       VARCHAR(2)   NOT NULL,

    CONSTRAINT uq_cidade_nome_uf
        UNIQUE (nome, uf),

    CONSTRAINT ck_cidade_uf
        CHECK (
            uf IN (
                'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES',
                'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR',
                'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC',
                'SP', 'SE', 'TO'
            )
        )
);
--seria bom ter esses UF ja no banco de dados por padrao??
