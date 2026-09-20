CREATE TABLE empresa (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo VARCHAR(12) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    razao_social VARCHAR(180),
    documento VARCHAR(18) NOT NULL UNIQUE, --CPF OU CNPJ
    creci VARCHAR(30),
    logomarca_url VARCHAR(500),
    descricao TEXT,
    telefone VARCHAR(20),
    whatsapp VARCHAR(20),
    email VARCHAR(255) NOT NULL,
    logradouro VARCHAR(180),
    numero VARCHAR(20),
    bairro VARCHAR(100),
    cep VARCHAR(8),
    cidade_id INTEGER,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,


    CONSTRAINT fk_empresa_cidade_endereco
        FOREIGN KEY (cidade_id)
            REFERENCES cidade (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT,
    --nao permitre remover a cidade, tem que remover a
    -- associacao na empresa primeiro

    CONSTRAINT ck_empresas_tipo
        CHECK (tipo IN ('IMOBILIARIA', 'AUTONOMO'))
);

