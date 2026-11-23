# Meu Imóvel API

API REST para gestão de empresas imobiliárias, corretores, proprietários, cidades, características e imóveis.

## Ambiente de desenvolvimento com Docker

Requisitos: Docker Engine e Docker Compose v2.

1. Crie o arquivo local de variáveis de ambiente e substitua os valores de exemplo por valores próprios:

   ```bash
   cp .env.example .env
   ```

   `POSTGRES_PASSWORD` define a senha local do banco; `API_SECURITY_TOKEN_SECRET` deve ser uma chave aleatória forte. O `.env` é ignorado pelo Git. Não use os valores de exemplo em ambientes compartilhados ou de produção.

2. Valide a configuração e construa/inicie os serviços:

   ```bash
   docker compose config --quiet
   docker compose up --build -d
   ```

3. Acompanhe a inicialização da API:

   ```bash
   docker compose ps
   docker compose logs -f app
   ```

A API fica disponível em `http://localhost:8080` por padrão. O PostgreSQL fica acessível pela rede Docker como `postgres:5432` e, a partir da máquina host, em `localhost:5433` por padrão. As portas podem ser alteradas por `APP_PORT` e `POSTGRES_PORT` no `.env`.

O serviço da aplicação só inicia depois que o healthcheck `pg_isready` indicar que o PostgreSQL está pronto. O Flyway já está habilitado em `application.properties` e executa as migrations de `src/main/resources/db/migration` durante a inicialização da aplicação; depois, o Hibernate valida o schema (`ddl-auto=validate`). Não é necessário executar migrations manualmente.

O Compose ativa o perfil Spring `demo` por padrão. Na primeira inicialização da API, esse perfil cria, se ainda não existirem, uma cidade em Recife, uma imobiliária ativa, um administrador e um corretor vinculado à imobiliária. O corretor usa um avatar SVG genérico servido pela aplicação. A carga não altera registros encontrados e não roda nos demais perfis Spring. Se o volume do PostgreSQL já tiver dados, os registros existentes são preservados e apenas dados de demonstração ausentes são criados.

Credenciais locais de demonstração:

| Perfil | E-mail | Senha |
| --- | --- | --- |
| Administrador | `admin@demo.meu-imovel.test` | `AdminDemo123!` |
| Corretor | `corretor@demo.meu-imovel.test` | `CorretorDemo123!` |

Autentique em `POST /auth/login` com `email` e `senha`, conforme o formato documentado abaixo. As senhas são codificadas com BCrypt antes de serem persistidas. Para personalizar as credenciais, defina `DEMO_ADMIN_EMAIL`, `DEMO_ADMIN_PASSWORD`, `DEMO_BROKER_EMAIL` e `DEMO_BROKER_PASSWORD` antes da primeira inicialização desses usuários. Alterar as variáveis depois que os usuários já existem não troca suas senhas.

Os volumes nomeados `postgres_data` e `uploads_data` preservam, respectivamente, o banco e os arquivos enviados quando os containers são removidos. Para parar os serviços sem apagar os dados:

```bash
docker compose down
```

Para apagar também os volumes persistentes, use `docker compose down -v`; isso remove os dados do PostgreSQL e os uploads locais.

## Acesso e autenticação

Localmente, a aplicação usa a porta padrão do Spring Boot (`http://localhost:8080`). Substitua a URL pela base da implantação quando consumir outro ambiente. Não há prefixo global de rotas.

As rotas protegidas usam JWT. Envie o token retornado por `POST /auth/login` no cabeçalho:

```http
Authorization: Bearer <token>
```

O token expira em duas horas. As rotas `GET /caracteristica`, `GET /corretor`, `GET /corretor/{id}`, `GET /imagens/{tipo}/{entidadeId}/{imagemId}`, `GET /vitrine/cidades`, `GET /vitrine/imoveis`, `GET /vitrine/imoveis/{id}` e `POST /auth/login` são públicas. `GET /corretor/empresa/ativos` e `GET /corretor/empresa/inativos` exigem autenticação de um gestor ativo e listam os corretores da empresa vinculada ao token, conforme o status da rota. As demais rotas exigem autenticação, além das permissões por perfil descritas nas seções abaixo.

### Perfis e escopo

| Perfil | Acesso relevante |
| --- | --- |
| `ADMINISTRADOR` | Registra usuários; lista, cria, atualiza, ativa, desativa e remove empresas; cria e remove cidades; cria características. |
| `GESTOR` | Cadastra, lista corretores ativos e inativos e ativa/desativa corretores da própria empresa; gerencia documentos, publicação e situação dos imóveis da empresa. O cadastro de corretores exige empresa ativa. |
| `CORRETOR` | Cadastra e consulta proprietários da própria empresa e cria/consulta os imóveis da empresa. O corretor precisa estar ativo. |
| Não autenticado | Consulta características, corretores ativos, cidades e imóveis publicados na vitrine e busca imagens pela API. |

As consultas de empresa por ID permitem ao administrador consultar qualquer empresa e aos demais usuários consultar apenas a empresa associada à própria conta. Proprietários e imóveis também são limitados à empresa do corretor autenticado. Uma tentativa de consultar um recurso de outra empresa pode retornar `404`.

As consultas `GET /cidade`, `GET /cidade/{id}` e `GET /caracteristica/nome` exigem autenticação. `GET /caracteristica` é público para fornecer os IDs do filtro da vitrine. A resposta de acesso negado depende de onde a permissão foi recusada: as regras do serviço retornam `401`; o Spring Security também pode retornar `403`.

## Formato de erros

Os erros tratados pela API seguem este formato:

```json
{
  "message": "Já existe um imóvel com esse código na empresa.",
  "httpStatus": "CONFLICT",
  "timestamp": "2026-10-21T12:30:00"
}
```

| HTTP | Situações comuns |
| --- | --- |
| `400 Bad Request` | Campos inválidos, documento inválido, foto inválida, regra de preço/endereço/foto do imóvel não atendida ou falha de armazenamento de arquivo. |
| `401 Unauthorized` | Credenciais inválidas, conta inativa/bloqueada ou operação recusada pelo serviço. |
| `403 Forbidden` | Perfil sem permissão para a rota, conforme a regra aplicada pelo Spring Security. |
| `404 Not Found` | Cidade, empresa, corretor, proprietário, característica ou imóvel não encontrado; inclui recursos fora do escopo da empresa do usuário. |
| `409 Conflict` | Documento, e-mail, CRECI, código de imóvel ou nome de cidade/característica duplicado; empresa inativa ou transição de status inválida. |
| `413 Content Too Large` | Arquivo maior que o limite multipart configurado ou requisição multipart acima do limite total. |
| `415 Unsupported Media Type` | Requisição multipart enviada com tipo de conteúdo incompatível. |

Nas respostas de validação de DTO, `message` contém os campos inválidos e suas mensagens. O `httpStatus` é o nome do status HTTP; o status também vem na linha de resposta.

## Autenticação

### `POST /auth/login`

Autentica um usuário. Não exige token.

**Requisição (`application/json`):**

```json
{
  "email": "gestor@example.com",
  "senha": "senha-com-12-caracteres"
}
```

`email` deve ter formato válido e `email` e `senha` não podem estar vazios.

**Resposta `200 OK`:**

```json
{
  "token": "<jwt>",
  "perfil": "CORRETOR"
}
```
Retorna o token + perfil do usuário, se o login for executado com sucesso.

Credenciais incorretas ou usuário inativo/bloqueado retornam `401`.

### `POST /auth/register`

Registra um usuário. Exige `ADMINISTRADOR`.

**Requisição (`application/json`):**

```json
{
  "empresaId": 12,
  "nome": "Ana Souza",
  "email": "ana@example.com",
  "senha": "senha-com-no-minimo-12",
  "perfil": "GESTOR"
}
```

`nome` tem limite de 150 caracteres; `email` deve ser válido e ter até 255; `senha` deve ter de 12 a 255 caracteres; `perfil` é obrigatório. Para perfis diferentes de `ADMINISTRADOR`, `empresaId` é obrigatório. Para `ADMINISTRADOR`, `empresaId` deve ser omitido ou nulo. Os perfis aceitos são `ADMINISTRADOR`, `GESTOR`, `CORRETOR` e `CLIENTE`. A conta criada recebe status `ATIVO`.

**Resposta `200 OK` (exemplo resumido):**

```json
{
  "id": 42,
  "email": "ana@example.com",
  "empresa": { "id": 12 },
  "perfil": "GESTOR",
  "status": "ATIVO",
  "criadoEm": "2026-10-21T12:30:00"
}
```

O campo `empresa` é nulo para um administrador e contém a entidade `Empresa` associada para os demais perfis; o exemplo acima está resumido. E-mail já cadastrado ou associação de perfil/empresa incompatível retorna `409`.

## Empresas

### `GET /empresa`

Lista empresas. Exige `ADMINISTRADOR`.

**Resposta `200 OK`:** lista de `EmpresaResponseDto`, incluindo dados da empresa, cidade-base, cidades de atuação, status e data de criação.

### `GET /empresa/{id}`

Consulta uma empresa por ID. Exige autenticação; o administrador pode consultar qualquer empresa e os demais usuários apenas a própria empresa.

**Resposta `200 OK` (exemplo resumido):**

```json
{
  "id": 12,
  "tipo": "IMOBILIARIA",
  "nome": "Imobiliária Recife",
  "razaoSocial": "Imobiliária Recife Ltda.",
  "tipoDocumento": "CNPJ",
  "documento": "11222333000181",
  "creci": "12345-J",
  "logoMarcaUrl": "/imagens/empresa/12/c4bf744d-a873-4ef0-b1f2-b1532bfb31db",
  "logoImagemId": "c4bf744d-a873-4ef0-b1f2-b1532bfb31db",
  "email": "contato@example.com",
  "cidadeBase": { "id": 1, "nome": "Recife", "uf": "PE" },
  "cidadesAtuacao": [{ "id": 1, "nome": "Recife", "uf": "PE" }],
  "ativa": true,
  "criadoEm": "2026-10-21T12:30:00"
}
```

O DTO também pode conter `descricao`, `telefone`, `whatsapp`, `logradouro`, `numero`, `bairro` e `cep`. `cidadeBase` pode ser nula.

### `POST /empresa/imobiliaria`

Cadastra uma imobiliária e seu gestor. Exige `ADMINISTRADOR`; consome `multipart/form-data` com as partes `dto` (JSON) e `logoEmpresa` (arquivo).

**Parte `dto` (exemplo):**

```json
{
  "nomeGestor": "Ana Souza",
  "emailGestor": "ana@example.com",
  "senhaGestor": "senha-com-no-minimo-12",
  "empresa": {
    "nome": "Imobiliária Recife",
    "razaoSocial": "Imobiliária Recife Ltda.",
    "tipoDocumento": "CNPJ",
    "documento": "11222333000181",
    "creci": "12345-J",
    "descricao": "Imóveis na região metropolitana",
    "telefone": "8133334444",
    "whatsapp": "81999998888",
    "email": "contato@example.com",
    "logradouro": "Rua Exemplo",
    "numero": "100",
    "bairro": "Centro",
    "cep": "50000000",
    "cidadeBaseId": 1,
    "cidadesAtuacaoIds": [1, 2]
  }
}
```

`cidadeBaseId` é opcional; os IDs de cidade informados são consultados no cadastro e um ID inexistente retorna `404`. `tipoDocumento` aceita `CPF` ou `CNPJ`. Os campos `nomeGestor`, `emailGestor` e `senhaGestor` são obrigatórios; a senha deve ter de 12 a 255 caracteres. A estrutura aninhada `empresa` é obrigatória. No código atual, as validações de campo da estrutura aninhada não são propagadas automaticamente pelo controller; por isso a anotação `@NotEmpty` de `cidadesAtuacaoIds` não é aplicada nessa rota e uma lista vazia é aceita pelo serviço.

**Exemplo multipart:**

```bash
curl -X POST http://localhost:8080/empresa/imobiliaria \
  -H 'Authorization: Bearer <jwt>' \
  -F 'dto={"nomeGestor":"Ana Souza","emailGestor":"ana@example.com","senhaGestor":"senha-com-no-minimo-12","empresa":{"nome":"Imobiliária Recife","tipoDocumento":"CNPJ","documento":"11222333000181","email":"contato@example.com","cidadesAtuacaoIds":[1]}};type=application/json' \
  -F 'logoEmpresa=@logo.png;type=image/png'
```

**Resposta `201 Created`:** `EmpresaResponseDto` da empresa criada.

### `POST /empresa/autonoma`

Cadastra uma empresa autônoma junto com seu único corretor. Exige `ADMINISTRADOR`; consome `multipart/form-data` com `dto` (JSON), `logoEmpresa` e `fotoCorretor`.

**Parte `dto` (estrutura):**

```json
{
  "corretor": {
    "nome": "João Silva",
    "email": "joao@example.com",
    "senha": "senha-com-no-minimo-12",
    "creci": "54321-F",
    "telefone": "81999990000",
    "whatsapp": "81999990000",
    "apresentacao": "Atendimento residencial"
  },
  "empresa": {
    "nome": "João Silva Imóveis",
    "tipoDocumento": "CPF",
    "documento": "52998224725",
    "email": "contato@example.com",
    "cidadesAtuacaoIds": [1]
  }
}
```

As partes `corretor` e `empresa` são obrigatórias. Envie ambos os arquivos como imagens aceitas. As validações de campo dos objetos aninhados não são propagadas automaticamente pelo controller atual; a lista vazia de `cidadesAtuacaoIds` também é aceita pelo serviço. A resposta `201 Created` é `EmpresaResponseDto`.

### `PUT /empresa/{id}`

Atualiza dados de uma empresa. Exige autenticação; usuários não administradores só podem atualizar a empresa associada à própria conta. Consome `multipart/form-data` com `dto` (JSON) e a parte opcional `logoEmpresa`.

Os campos não nulos de `EmpresaUpdateDto` são aplicados. Exemplo de parte `dto`:

```json
{
  "nome": "Imobiliária Recife Atualizada",
  "telefone": "8133335555",
  "cidadesAtuacaoIds": [1, 2]
}
```

Enviar `cidadesAtuacaoIds: []` remove todas as cidades de atuação. Campos nulos são ignorados, portanto não limpam valores existentes. A logo é atualizada enviando `logoEmpresa`; o campo `logoMarcaUrl` do DTO não é aplicado pelo serviço atual. Resposta `204 No Content`.

### Ativar, desativar e remover empresa

Todas as rotas exigem `ADMINISTRADOR` e retornam `204 No Content` quando concluídas:

| Método e rota | Ação |
| --- | --- |
| `PATCH /empresa/activate/{id}` | Ativa uma empresa inativa. |
| `PATCH /empresa/inactivate/{id}` | Inativa uma empresa ativa. |
| `DELETE /empresa/{id}` | Remove a empresa. |

Empresa inexistente retorna `404`; tentar repetir o estado atual retorna `409`.

## Cidades

### `GET /cidade` e `GET /cidade/{id}`

Listam cidades ou consultam uma cidade por ID. Exigem autenticação.

**Resposta `200 OK` (lista ou item):**

```json
[{ "id": 1, "nome": "Recife", "uf": "PE" }]
```

### `POST /cidade`

Cria cidade. Exige `ADMINISTRADOR`.

```json
{
  "nome": "Recife",
  "uf": "PE"
}
```

`nome` é obrigatório e tem limite de 120 caracteres; `uf` é obrigatório e deve ser uma sigla de estado aceita pelo enum `Uf`. Resposta `201 Created` com a cidade criada. Nome e UF duplicados retornam `409`.

### `DELETE /cidade/{id}`

Remove cidade. Exige `ADMINISTRADOR`. Resposta `204 No Content`; cidade inexistente retorna `404`.

## Características

### `GET /caracteristica`

Lista características. É pública para preencher os filtros da vitrine. Resposta `200 OK`, com itens no formato:

```json
[{ "id": 3, "nome": "Piscina" }]
```

### `GET /caracteristica/nome?nome={nome}`

Busca uma característica por nome, ignorando maiúsculas/minúsculas e espaços externos. Exige autenticação. Retorna `200 OK` ou `404` se não encontrada.

### `POST /caracteristica`

Cria característica. Exige `ADMINISTRADOR`.

```json
{
  "nome": "Piscina"
}
```

`nome` não pode estar vazio e aceita até 50 caracteres. O serviço remove espaços externos; duplicidade ignorando maiúsculas/minúsculas retorna `409`. Resposta `201 Created` com `{ "id": 3, "nome": "Piscina" }`.

## Corretores

### `GET /corretor` e `GET /corretor/{id}`

Rotas públicas. Listam corretores ativos ou consultam o perfil público de um corretor ativo. Corretores inativos não aparecem e seus perfis retornam `404`.

**Resposta `200 OK` (item resumido):**

```json
{
  "id": 7,
  "nome": "João Silva",
  "creci": "54321-F",
  "fotoUrl": "/imagens/corretor/7/84c8304e-9e5d-4b81-b8d5-90a53c6171bc",
  "fotoImagemId": "84c8304e-9e5d-4b81-b8d5-90a53c6171bc",
  "telefone": "81999990000",
  "whatsapp": "81999990000",
  "email": "joao@example.com",
  "apresentacao": "Atendimento residencial",
  "empresa": {
    "id": 12,
    "tipo": "IMOBILIARIA",
    "nome": "Imobiliária Recife",
    "logoMarcaUrl": "/imagens/empresa/12/c4bf744d-a873-4ef0-b1f2-b1532bfb31db",
    "logoImagemId": "c4bf744d-a873-4ef0-b1f2-b1532bfb31db",
    "cidadesAtuacao": [{ "id": 1, "nome": "Recife", "uf": "PE" }]
  }
}
```

O objeto público da empresa inclui também seus dados de contato e endereço disponíveis no DTO.

### `GET /corretor/empresa/ativos` e `GET /corretor/empresa/inativos`

Exigem `GESTOR` ativo. Listam, em ordem alfabética, os corretores da empresa associada ao gestor autenticado; a empresa é obtida do token e a requisição não aceita um ID de empresa. Cada rota retorna `200 OK` com uma lista de `CorretorResponseDto`: `/ativos` retorna usuários com status `ATIVO`, e `/inativos` retorna status `INATIVO`. A resposta contém os dados de contato e os IDs de imagem do corretor.

### `POST /corretor`

Cadastra corretor para a empresa do gestor autenticado. Exige `GESTOR` ativo, empresa ativa do tipo imobiliária e `multipart/form-data` com `dto` (JSON) e `foto`.

**Parte `dto`:**

```json
{
  "nome": "João Silva",
  "email": "joao@example.com",
  "senha": "senha-com-no-minimo-12",
  "creci": "54321-F",
  "telefone": "81999990000",
  "whatsapp": "81999990000",
  "apresentacao": "Atendimento residencial"
}
```

Nome, e-mail, senha, CRECI, telefone, WhatsApp e apresentação são obrigatórios; e-mail deve ser válido; senha deve ter de 12 a 255 caracteres. Resposta `201 Created` com os dados do corretor, sem a senha.

### Ativar e desativar corretor

Exigem `GESTOR` ativo e o corretor precisa pertencer à mesma empresa do gestor. Ambos retornam `204 No Content`.

| Método e rota | Regra |
| --- | --- |
| `PATCH /corretor/ativar/{id}` | Só ativa corretor inativo. |
| `PATCH /corretor/desativar/{id}` | Só desativa corretor ativo. |

Corretor de outra empresa ou inexistente retorna `404`; transição de status inválida retorna `409`.

## Proprietários

Todas as rotas exigem um `CORRETOR` ativo associado a uma empresa e operam somente nos proprietários dessa empresa.

### `GET /proprietario?nome={nome}`

Busca proprietários por trecho do nome, sem diferenciar maiúsculas/minúsculas. Resposta `200 OK`, mesmo quando a lista não contém resultados.

### `GET /proprietario/documento?tipoDocumento={CPF|CNPJ}&documento={documento}`

Busca proprietário pelo tipo e documento. O documento é normalizado removendo pontuação e convertido para maiúsculas. CPF aceita 11 dígitos; CNPJ aceita caracteres alfanuméricos no corpo e dois dígitos verificadores. Documento inválido retorna `400`; não encontrado na empresa do corretor retorna `404`.

### `POST /proprietario`

Cadastra proprietário na empresa do corretor autenticado.

```json
{
  "tipoDocumento": "CPF",
  "documento": "529.982.247-25",
  "nome": "Ana Souza",
  "telefone": "81999990000",
  "email": "ana@example.com",
  "observacoes": "Prefere contato à tarde"
}
```

`tipoDocumento`, documento, nome, telefone e e-mail são obrigatórios; e-mail deve ser válido. Documento é validado e salvo sem pontuação. Repetição dentro da mesma empresa retorna `409`. Resposta `201 Created` no formato:

```json
{
  "id": 31,
  "tipoDocumento": "CPF",
  "documento": "52998224725",
  "nome": "Ana Souza",
  "telefone": "81999990000",
  "email": "ana@example.com",
  "observacoes": "Prefere contato à tarde"
}
```

## Prédios

As rotas de prédios exigem `CORRETOR` ativo associado a uma empresa ativa. A empresa é obtida do usuário autenticado, e o prédio é listado, consultado e alterado somente dentro desse tenant. O endereço completo inclui CEP, logradouro, número, bairro, cidade e coordenadas; a cidade deve estar na área de atuação da empresa.

### `POST /predio`

Cadastra um prédio para reutilização nas unidades. Resposta `201 Created` com `PredioResponseDto`.

```json
{
  "nome": "Edifício Atlântico",
  "endereco": {
    "cep": "50000000",
    "logradouro": "Rua do Sol",
    "numero": "120",
    "complemento": null,
    "bairro": "Boa Vista",
    "cidadeId": 1,
    "latitude": -8.05,
    "longitude": -34.9
  },
  "numeroAndares": 12,
  "unidadesPorAndar": 4,
  "anoConstrucao": 2018,
  "elevadores": 2,
  "portaria24h": true,
  "piscina": true,
  "academia": true,
  "salaoFestas": true,
  "playground": false,
  "valorCondominioBase": 650.00
}
```

### `GET /predio`, `GET /predio/{id}` e `PUT /predio/{id}`

Lista os prédios da empresa, consulta um prédio ou substitui seus dados. A atualização grava no mesmo prédio associado às unidades, então o próximo detalhe de cada apartamento já apresenta os dados atualizados. IDs de outra empresa retornam `404`. O `PUT` recebe o mesmo formato do `POST` e responde `200 OK`.

## Imóveis

As rotas de imóveis exigem `CORRETOR` ativo associado a uma empresa ativa. Os IDs de proprietário, cidade e características são validados no contexto do cadastro; o cliente não escolhe a empresa nem o corretor responsável, que vêm do usuário autenticado.

### `POST /imovel`

Cadastra imóvel. Consome `multipart/form-data` com `dto` (JSON) e uma ou mais partes `fotos` (arquivos). A ordem das partes repetidas `fotos` determina a ordem das fotos; `fotoCapaOrdem` é um índice começando em `1`.

**Parte `dto` (exemplo):**

```json
{
  "codigo": "IM-2026-001",
  "titulo": "Casa próxima ao parque",
  "proprietarioId": 31,
  "finalidade": "VENDA",
  "valorVenda": 450000.00,
  "valorAluguel": null,
  "valorCondominio": 650.00,
  "valorIptu": 1200.00,
  "descricao": "Casa com quintal e duas vagas.",
  "natureza": "CASA",
  "casa": {
    "areaTerreno": 250.00,
    "areaConstruida": 120.00,
    "quartos": 3,
    "suites": 1,
    "banheiros": 2,
    "salas": 2,
    "vagas": 2,
    "pavimentos": 2,
    "anoConstrucao": 2018,
    "piscina": false,
    "quintal": true,
    "churrasqueira": true,
    "mobiliada": false
  },
  "endereco": {
    "cep": "50000000",
    "logradouro": "Rua Exemplo",
    "numero": "100",
    "complemento": "Casa 1",
    "bairro": "Centro",
    "cidadeId": 1,
    "latitude": -8.05,
    "longitude": -34.9
  },
  "caracteristicasIds": [3, 8],
  "fotoCapaOrdem": 1
}
```

`codigo` aceita até 50 caracteres e `titulo` até 180. `finalidade` aceita `VENDA`, `ALUGUEL` ou `VENDA_E_ALUGUEL`. O preço de venda é obrigatório para `VENDA` e `VENDA_E_ALUGUEL`; o de aluguel é obrigatório para `ALUGUEL` e `VENDA_E_ALUGUEL`. Preços não podem ser negativos e aceitam até duas casas decimais. CEP deve ter oito dígitos; latitude deve estar entre `-90` e `90`, e longitude entre `-180` e `180`. Cidade e proprietário precisam existir; proprietário e corretor devem pertencer à empresa autenticada e a empresa precisa atuar na cidade informada. Todas as características informadas devem existir. O código é único dentro da empresa.

`natureza` aceita `CASA`, `TERRENO`, `APARTAMENTO` ou `LOTE`. Envie somente o bloco correspondente à natureza escolhida: `casa`, `terreno`, `apartamento` ou `lote`. Para casa, terreno e lote, `endereco` é obrigatório. Para apartamento, omita `endereco`: o endereço vem do prédio vinculado.

Para casa e terreno, os campos dos blocos são obrigatórios; áreas e medidas usam metros quadrados e metros, respectivamente. Contagens são inteiros não negativos. Para casa, `areaConstruida` não pode superar `areaTerreno`; `areaLivre` é calculada pela API como `areaTerreno - areaConstruida` e aparece somente na resposta. Para terreno, `topografia` aceita `PLANO`, `ACLIVE` ou `DECLIVE`; `lateralEsquerda` e `lateralDireita` representam as duas laterais, e `usoPermitido` é texto livre.

No apartamento, `apartamento.predioId` deve apontar para um prédio da empresa autenticada. Informe `andar`, `numero`, `areaPrivativa`, `areaTotal`, quartos, suítes, banheiros, vagas, `varanda` e `posicaoSolar`. As áreas são positivas e a área total deve ser maior ou igual à privativa; contagens são não negativas. `posicaoSolar` é texto livre. O mesmo número normalizado não pode aparecer no mesmo andar e prédio; a API remove espaços nas pontas e converte o número para maiúsculas antes de salvar.

Exemplo do bloco `apartamento`:

```json
{
  "predioId": 44,
  "andar": 4,
  "numero": "401A",
  "areaPrivativa": 80.00,
  "areaTotal": 95.00,
  "quartos": 3,
  "suites": 1,
  "banheiros": 2,
  "vagas": 1,
  "varanda": true,
  "posicaoSolar": "Leste"
}
```

O detalhe do imóvel inclui `apartamento` com os dados da unidade e um `predio` aninhado com o cadastro compartilhado. O campo comum `endereco` também reflete o endereço atual do prédio; seu `id` vem nulo porque não há uma cópia do endereço na tabela `endereco_imovel` para apartamentos. Exemplo da parte específica da resposta:

```json
{
  "natureza": "APARTAMENTO",
  "casa": null,
  "terreno": null,
  "apartamento": {
    "andar": 4,
    "numero": "401A",
    "areaPrivativa": 80.00,
    "areaTotal": 95.00,
    "quartos": 3,
    "suites": 1,
    "banheiros": 2,
    "vagas": 1,
    "varanda": true,
    "posicaoSolar": "Leste",
    "predio": {
      "id": 44,
      "empresaId": 12,
      "nome": "Edifício Atlântico",
      "endereco": {
        "cep": "50000000",
        "logradouro": "Rua do Sol",
        "numero": "120",
        "complemento": null,
        "bairro": "Boa Vista",
        "cidadeId": 1,
        "cidade": "Recife",
        "uf": "PE",
        "latitude": -8.05,
        "longitude": -34.9
      },
      "numeroAndares": 12,
      "unidadesPorAndar": 4,
      "anoConstrucao": 2018,
      "elevadores": 2,
      "portaria24h": true,
      "piscina": true,
      "academia": true,
      "salaoFestas": true,
      "playground": false,
      "valorCondominioBase": 650.00
    }
  },
  "endereco": {
    "id": null,
    "cep": "50000000",
    "logradouro": "Rua do Sol",
    "numero": "120",
    "complemento": null,
    "bairro": "Boa Vista",
    "cidadeId": 1,
    "cidade": "Recife",
    "uf": "PE",
    "latitude": -8.05,
    "longitude": -34.9
  }
}
```

Exemplo do bloco `terreno`:

```json
{
  "areaTotal": 300.00,
  "frente": 10.00,
  "fundo": 10.00,
  "lateralEsquerda": 30.00,
  "lateralDireita": 30.00,
  "topografia": "PLANO",
  "muro": false,
  "esquina": false,
  "usoPermitido": "Residencial"
}
```

Envie cada arquivo com o mesmo nome de parte:

```bash
curl -X POST http://localhost:8080/imovel \
  -H 'Authorization: Bearer <jwt>' \
  -F 'dto={"codigo":"IM-2026-001","titulo":"Casa próxima ao parque","proprietarioId":31,"finalidade":"VENDA","valorVenda":450000,"natureza":"CASA","casa":{"areaTerreno":250,"areaConstruida":120,"quartos":3,"suites":1,"banheiros":2,"salas":2,"vagas":2,"pavimentos":2,"anoConstrucao":2018,"piscina":false,"quintal":true,"churrasqueira":true,"mobiliada":false},"endereco":{"cep":"50000000","logradouro":"Rua Exemplo","numero":"100","bairro":"Centro","cidadeId":1,"latitude":-8.05,"longitude":-34.9},"caracteristicasIds":[3,8],"fotoCapaOrdem":1};type=application/json' \
  -F 'fotos=@frente.jpg;type=image/jpeg' \
  -F 'fotos=@sala.webp;type=image/webp'
```

É obrigatório enviar ao menos uma foto e escolher uma ordem de capa existente. Cada foto deve ser JPEG, PNG ou WebP. O validador da aplicação aceita arquivos de até 5 MiB, mas a configuração multipart atual não define limites próprios e usa o padrão do Spring Boot: 1 MiB por arquivo e 10 MiB por requisição. Assim, na configuração atual o limite efetivo é 1 MiB por arquivo. Resposta `201 Created` no formato resumido:

```json
{
  "id": 88,
  "codigo": "IM-2026-001",
  "titulo": "Casa próxima ao parque",
  "empresaId": 12,
  "corretorId": 7,
  "nomeCorretor": "João Silva",
  "proprietarioId": 31,
  "nomeProprietario": "Ana Souza",
  "finalidade": "VENDA",
  "natureza": "CASA",
  "casa": {
    "areaTerreno": 250.00,
    "areaConstruida": 120.00,
    "areaLivre": 130.00,
    "quartos": 3,
    "suites": 1,
    "banheiros": 2,
    "salas": 2,
    "vagas": 2,
    "pavimentos": 2,
    "anoConstrucao": 2018,
    "piscina": false,
    "quintal": true,
    "churrasqueira": true,
    "mobiliada": false
  },
  "terreno": null,
  "valorVenda": 450000.00,
  "valorAluguel": null,
  "valorCondominio": 650.00,
  "valorIptu": 1200.00,
  "descricao": "Casa com quintal e duas vagas.",
  "endereco": {
    "id": 90,
    "cep": "50000000",
    "logradouro": "Rua Exemplo",
    "numero": "100",
    "complemento": "Casa 1",
    "bairro": "Centro",
    "cidadeId": 1,
    "cidade": "Recife",
    "uf": "PE",
    "latitude": -8.05,
    "longitude": -34.9
  },
  "caracteristicas": [{ "id": 3, "nome": "Piscina" }],
  "fotos": [
    {
      "id": 101,
      "imagemId": "369fd49e-c6e3-45b7-a873-4a8c4635539f",
      "url": "/imagens/imovel/88/369fd49e-c6e3-45b7-a873-4a8c4635539f",
      "ordem": 1,
      "capa": true
    }
  ]
}
```

Código duplicado retorna `409`; preço, área de atuação, ordem de capa ou outra regra de negócio inválida retorna `400`; proprietário, cidade ou característica inexistente retorna `404`.

### `GET /imovel` e `GET /imovel/{id}`

Listam imóveis da empresa do corretor autenticado ou consultam um imóvel por ID dentro dessa mesma empresa. Resposta `200 OK` com `ImovelResponseDto` (mesmo formato do cadastro). Imóvel inexistente ou de outra empresa retorna `404`.

O código atual não expõe filtros por cidade nem rotas para atualizar os dados cadastrais ou remover imóveis. A situação e a publicação possuem operações próprias descritas abaixo.

## Documentos, situação e publicação

As operações de gestão abaixo exigem um gestor ativo da mesma empresa do imóvel. A situação inicial é `DISPONIVEL` e a publicação começa desativada. A publicação é independente da situação: somente imóveis publicados e disponíveis aparecem na vitrine pública. Imóveis cujo corretor foi desativado também ficam ocultos, conforme a regra da W02.

### `PATCH /imovel/{id}/publicacao`

Ativa ou desativa a publicação sem alterar a situação. Para publicar, o endereço precisa ter CEP, logradouro, número, bairro, cidade, latitude e longitude; a resposta `400` informa os campos ausentes. Apartamentos usam o endereço do prédio. Exemplo:

```json
{
  "publicado": true
}
```

A resposta contém o ID do imóvel, a situação atual e o valor de `publicado`.

### `PATCH /imovel/{id}/situacao`

Atualiza a situação para `DISPONIVEL`, `RESERVADO`, `VENDIDO` ou `ALUGADO`. Cada mudança exige observação e registra autor, data, situação anterior e nova:

```json
{
  "situacao": "RESERVADO",
  "observacao": "Proposta em análise"
}
```

`GET /imovel/{id}/historico-situacao` retorna as mudanças em ordem cronológica. Reenviar a situação atual não cria um registro de histórico.

### Documentos do imóvel

É permitido um documento de cada tipo por imóvel: `MATRICULA`, `CARNE_IPTU` e `AUTORIZACAO_PROPRIETARIO`. O arquivo e `dataDocumento` são obrigatórios. Os documentos aceitam PDF, JPEG, PNG ou WebP, com até 5 MB; os bytes são validados e armazenados fora do banco. A listagem e o download são privados e exigem gestor da empresa do imóvel.

`POST /imovel/{id}/documentos` recebe `multipart/form-data` com os campos `tipo`, `dataDocumento` (formato `AAAA-MM-DD`) e a parte `arquivo`. `GET /imovel/{id}/documentos` lista os anexos. Se já houver um documento daquele tipo, use `PUT /imovel/{id}/documentos/{documentoId}` para substituir o arquivo e atualizar a data. O download usa `GET /imovel/{id}/documentos/{documentoId}/arquivo`.

### Vitrine pública

`GET /vitrine/cidades` lista as cidades da área de atuação das empresas ativas, com `id`, nome e UF. A busca sempre exige `cidadeId`, que pode ser obtido nesse endpoint:

```http
GET /vitrine/imoveis?cidadeId=1&texto=sol&finalidade=VENDA&natureza=CASA&quartos=3&ordenacao=MENOR_PRECO&pagina=0&tamanho=12
```

Filtros opcionais: `texto` procura sem diferenciar maiúsculas/minúsculas em título, bairro e descrição; `finalidade` aceita `VENDA`, `ALUGUEL` ou `VENDA_E_ALUGUEL` (a busca por venda ou aluguel também inclui anúncios das duas finalidades); `natureza` aceita `CASA`, `TERRENO`, `APARTAMENTO` ou `LOTE`; `precoMinimo` e `precoMaximo` limitam a faixa de preço; `quartos`, `suites` e `vagas` definem quantidades mínimas; `bairro` procura o bairro sem diferenciar maiúsculas/minúsculas; `areaMinima` e `areaMaxima` limitam a área; e `caracteristicas` pode ser repetido para exigir todas as características informadas, por exemplo `&caracteristicas=2&caracteristicas=5`. Use `GET /caracteristica` para consultar o catálogo público e obter os IDs.

A área usada no filtro é a construída para casa, privativa para apartamento, total para terreno e a área do lote. Para faixa e ordenação de preço, `finalidade=ALUGUEL` usa o aluguel; as demais finalidades usam o preço de venda. Sem finalidade informada, usa o preço de venda quando existe e, caso contrário, o aluguel. A ordenação aceita `MAIS_RECENTES` (padrão, usando o ID decrescente), `MENOR_PRECO`, `MAIOR_PRECO`, `MENOR_AREA` ou `MAIOR_AREA`. A paginação começa em zero, usa 12 itens por padrão e aceita até 50 por página. A resposta é uma página com `content`, `totalElements`, `totalPages`, `number` e os demais metadados do Spring Data.

Todos os resultados são publicados, disponíveis e atribuídos a corretores ativos. `GET /vitrine/imoveis/{id}` consulta o detalhe com fotos, endereço e coordenadas, dados da natureza e informações públicas do corretor responsável. Imóveis reservados, vendidos, alugados, não publicados ou atribuídos a corretor inativo retornam `404` no detalhe. Os DTOs públicos não incluem proprietário, documentos, código interno do imóvel, identificador da empresa do prédio nem observações internas.

## Loteamentos, quadras e lotes

As rotas exigem um corretor ativo vinculado a uma empresa ativa. O loteamento pertence à empresa autenticada e à cidade informada, que deve fazer parte da área de atuação. Os campos `asfalto`, `agua`, `energia`, `esgoto` e `iluminacaoPublica` descrevem a infraestrutura; `areasLazer` é texto livre e pode ser omitido.

### `POST /loteamento`

Cadastra um loteamento. Exemplo de corpo:

```json
{
  "nome": "Residencial das Palmeiras",
  "incorporadora": "Construtora Horizonte",
  "cidadeId": 1,
  "asfalto": true,
  "agua": true,
  "energia": true,
  "esgoto": true,
  "iluminacaoPublica": true,
  "areasLazer": "Praça, playground e quadra esportiva"
}
```

`GET /loteamento` lista os loteamentos da empresa e `GET /loteamento/{id}` consulta um deles. A resposta inclui as quadras e a contagem de lotes disponíveis e vendidos em cada uma. IDs de outra empresa retornam `404`.

### `POST /loteamento/{id}/quadras`

Cadastra uma quadra dentro do loteamento. O corpo contém `identificacao`, por exemplo `{"identificacao":"A"}`. A identificação é normalizada com trim e maiúsculas e não pode se repetir no mesmo loteamento. A resposta inclui as quantidades atuais de lotes por situação.

### Cadastro do lote por `POST /imovel`

Envie `natureza: "LOTE"`, o bloco `lote` e o endereço normal do imóvel. A cidade do endereço precisa ser a mesma do loteamento da quadra. O preço usa o campo comum `valorVenda`; número, área, frente, fundo e condições de parcelamento ficam no bloco específico:

```json
{
  "codigo": "LT-2026-001",
  "titulo": "Lote residencial na Quadra A",
  "proprietarioId": 31,
  "finalidade": "VENDA",
  "valorVenda": 150000.00,
  "natureza": "LOTE",
  "lote": {
    "quadraId": 8,
    "numero": "12A",
    "area": 250.00,
    "frente": 10.00,
    "fundo": 25.00,
    "condicoesParcelamento": "Entrada de 30% e saldo em 24 parcelas"
  },
  "endereco": {
    "cep": "50000000",
    "logradouro": "Rua do Residencial",
    "numero": "12A",
    "bairro": "Centro",
    "cidadeId": 1,
    "latitude": -8.05,
    "longitude": -34.9
  },
  "fotoCapaOrdem": 1
}
```

O lote é gravado como imóvel da empresa autenticada, exige preço de venda e fica inicialmente `DISPONIVEL`. O detalhe do imóvel retorna o lote, sua quadra e os dados atuais do loteamento. O número é normalizado com trim e maiúsculas; repetição na mesma quadra retorna `409`.

### `PATCH /lote/{id}/situacao`

Atualiza a situação para `DISPONIVEL`, `RESERVADO`, `VENDIDO` ou `ALUGADO`, permitindo que a consulta do loteamento reflita os totais por quadra. A mudança também atualiza a situação comum do imóvel e exige observação para o histórico. Exige gestor ativo da empresa autenticada. Exemplo:

```json
{
  "situacao": "VENDIDO",
  "observacao": "Venda concluída"
}
```

## Regras comuns de arquivos

As fotos de empresa, corretor e imóvel aceitam `image/jpeg`, `image/png` ou `image/webp`. O conteúdo do arquivo é identificado pela assinatura da imagem e comparado ao `Content-Type` informado quando ele estiver presente. O limite configurado é de 5 MB por arquivo e 50 MB por requisição multipart; o validador também limita cada foto a 5 MiB. Nos endpoints multipart, envie a parte `dto` com `Content-Type: application/json` e os arquivos usando exatamente os nomes documentados para cada rota.

## Imagens

Os endpoints abaixo permitem consultar e gerenciar imagens associadas a uma empresa, corretor ou imóvel. `tipo` aceita `empresa`, `corretor` ou `imovel`, sem diferenciar maiúsculas de minúsculas; `entidadeId` é o ID da entidade correspondente e `imagemId` é o UUID retornado no upload. As respostas incluem URLs relativas neste formato e IDs explícitos (`logoImagemId`, `fotoImagemId` e `imagemId` em cada foto de imóvel). Combine a URL com a origem da API para usá-la, por exemplo, em `img src`.

No DTO de imóvel, `fotos[].id` continua sendo o ID do registro da foto no banco; `fotos[].imagemId` é o UUID usado nas rotas de imagem. Para URLs antigas ou ausentes, os campos de ID da imagem podem vir nulos.

Os arquivos são armazenados fora do banco, em `APP_STORAGE_LOCATION` (padrão `uploads`, relativo ao diretório de execução), dentro de pastas por tipo e ID da entidade. O nome enviado pelo cliente não é usado; cada upload recebe um UUID. O campo da entidade no banco guarda a URL da API.

Os uploads dos endpoints existentes continuam disponíveis: `POST /empresa/imobiliaria` e `POST /empresa/autonoma` recebem `logoEmpresa`; a rota autônoma também recebe `fotoCorretor`; `PUT /empresa/{id}` aceita a parte opcional `logoEmpresa`; `POST /corretor` recebe `foto`; e `POST /imovel` recebe uma ou mais partes `fotos`. As URLs retornadas por esses fluxos agora apontam para `/imagens/{tipo}/{entidadeId}/{imagemId}` e podem ser consultadas pelo `GET` descrito abaixo.

### `GET /imagens/{tipo}/{entidadeId}/{imagemId}`

Busca a imagem e devolve os bytes com `Content-Type` detectado (`image/jpeg`, `image/png` ou `image/webp`) e disposição `inline`. É público e não exige token. A imagem precisa estar associada à entidade; uma referência inexistente ou sem associação retorna `404`.

```bash
curl -i http://localhost:8080/imagens/imovel/88/369fd49e-c6e3-45b7-a873-4a8c4635539f
```

### `POST /imagens/{tipo}/{entidadeId}`

Adiciona uma imagem. Exige token e `multipart/form-data` com a parte `arquivo`.

```bash
curl -X POST http://localhost:8080/imagens/imovel/88 \
  -H 'Authorization: Bearer <jwt>' \
  -F 'arquivo=@sala.webp;type=image/webp'
```

**Resposta `201 Created`:**

```json
{
  "id": "369fd49e-c6e3-45b7-a873-4a8c4635539f",
  "tipo": "IMOVEL",
  "entidadeId": 88,
  "url": "/imagens/imovel/88/369fd49e-c6e3-45b7-a873-4a8c4635539f"
}
```

Para empresas, só é possível adicionar logo se a empresa ainda não tiver um; para corretores, só se ainda não houver foto. Imóveis podem receber fotos adicionais. Ao adicionar a primeira foto de um imóvel sem capa, ela se torna a capa. A empresa precisa estar ativa para receber imagens.

### `PUT /imagens/{tipo}/{entidadeId}/{imagemId}`

Substitui o conteúdo da imagem existente sem alterar seu ID ou URL. Exige token e `multipart/form-data` com a parte `arquivo`.

```bash
curl -X PUT http://localhost:8080/imagens/imovel/88/369fd49e-c6e3-45b7-a873-4a8c4635539f \
  -H 'Authorization: Bearer <jwt>' \
  -F 'arquivo=@sala-atualizada.jpg;type=image/jpeg'
```

**Resposta `200 OK`:** o mesmo formato JSON do `POST`, preservando `id` e `url`.

### `DELETE /imagens/{tipo}/{entidadeId}/{imagemId}`

Desassocia e remove uma imagem, retornando `204 No Content` quando concluído. Exige token. A foto do corretor é obrigatória e não pode ser removida; use `PUT` para substituí-la. Um imóvel deve manter ao menos uma foto. Ao remover a capa de um imóvel, a foto restante de menor `ordem` passa a ser a nova capa. O logo da empresa pode ser removido.

```bash
curl -X DELETE http://localhost:8080/imagens/empresa/12/c4bf744d-a873-4ef0-b1f2-b1532bfb31db \
  -H 'Authorization: Bearer <jwt>'
```

### Autenticação e permissões

Envie `Authorization: Bearer <jwt>` para `POST`, `PUT` e `DELETE`. As rotas aceitam os perfis `ADMINISTRADOR`, `GESTOR` e `CORRETOR`, mas o serviço também confere o tipo da imagem e a empresa associada:

| Imagem | Pode adicionar/atualizar/remover |
| --- | --- |
| Logo da empresa | `ADMINISTRADOR` em qualquer empresa ou `GESTOR` da própria empresa. |
| Foto do corretor | `GESTOR` da empresa do corretor. O próprio corretor e o administrador não podem alterá-la por estes endpoints. A remoção é vedada por ser obrigatória. |
| Foto do imóvel | `ADMINISTRADOR` ou `CORRETOR` da empresa do imóvel. O serviço não exige que o corretor seja o responsável específico pelo imóvel. |

O usuário precisa estar ativo e autenticado. Para adicionar imagens, a empresa associada precisa estar ativa. O `GET` é público.

### Validações e erros de imagem

| HTTP | Situação |
| --- | --- |
| `400 Bad Request` | Tipo de entidade inválido; arquivo vazio, maior que 5 MiB, conteúdo que não é JPEG/PNG/WebP, divergência entre conteúdo e `Content-Type`, ou erro de armazenamento. |
| `401 Unauthorized` / `403 Forbidden` | Token ou usuário ativo ausente/inválido, perfil sem acesso à rota ou usuário sem autorização para aquela empresa, conforme a camada que rejeitou a operação. |
| `404 Not Found` | Entidade ou imagem inexistente, imagem não associada à entidade indicada, ou arquivo não encontrado no armazenamento. |
| `409 Conflict` | Tentativa de adicionar logo/foto que já existe, empresa inativa ao adicionar imagem, tentativa de remover foto obrigatória de corretor ou última foto do imóvel. |
| `413 Content Too Large` | Arquivo acima de 5 MB ou requisição multipart acima de 50 MB. |
| `415 Unsupported Media Type` | Upload enviado com tipo de conteúdo incompatível com `multipart/form-data`. |
