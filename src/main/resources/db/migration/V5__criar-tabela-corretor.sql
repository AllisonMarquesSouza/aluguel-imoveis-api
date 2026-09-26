CREATE TABLE corretor
(
    usuario_id    INTEGER PRIMARY KEY,
    foto_url      VARCHAR(500) NOT NULL,
    creci         VARCHAR(30) NOT NULL UNIQUE,
    telefone      VARCHAR(20) NOT NULL,
    whatsapp      VARCHAR(20) NOT NULL,
    apresentacao  TEXT NOT NULL,

    CONSTRAINT fk_corretor_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

-- Como gestor da imobiliária, eu quero cadastrar os corretores que trabalham comigo para
-- que cada imóvel tenha um responsável e o interessado saiba com quem está falando.
--
-- O corretor tem nome, CRECI, foto, telefone, WhatsApp, e-mail e uma breve apresentação
-- — são esses os dados que aparecem no perfil público, no site e no aplicativo.
--
-- O gestor ativa e desativa. Corretor desativado some da vitrine e do contato,
-- mas os imóveis dele continuam no acervo da empresa e podem ser passados a outro corretor.
-- Tarefa W02 do documento de histórias.