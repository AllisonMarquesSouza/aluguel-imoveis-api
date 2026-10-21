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

O token expira em duas horas. As rotas `GET /corretor` e `GET /corretor/{id}` e `POST /auth/login` são públicas. As demais exigem autenticação, além das permissões por perfil descritas nas seções abaixo.

### Perfis e escopo

| Perfil | Acesso relevante |
| --- | --- |
| `ADMINISTRADOR` | Registra usuários; lista, cria, atualiza, ativa, desativa e remove empresas; cria e remove cidades; cria características. |
| `GESTOR` | Cadastra e ativa/desativa corretores da própria empresa. A empresa precisa estar ativa para cadastrar corretores. |
| `CORRETOR` | Cadastra e consulta proprietários da própria empresa e cria/consulta os imóveis da empresa. O corretor precisa estar ativo. |
| Não autenticado | Consulta a listagem e o perfil público de corretores ativos. |

As consultas de empresa por ID permitem ao administrador consultar qualquer empresa e aos demais usuários consultar apenas a empresa associada à própria conta. Proprietários e imóveis também são limitados à empresa do corretor autenticado. Uma tentativa de consultar um recurso de outra empresa pode retornar `404`.

As consultas `GET /cidade`, `GET /cidade/{id}`, `GET /caracteristica` e `GET /caracteristica/nome` exigem autenticação. A resposta de acesso negado depende de onde a permissão foi recusada: as regras do serviço retornam `401`; o Spring Security também pode retornar `403`.

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
  "token": "<jwt>"
}
```

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
  "logoMarcaUrl": "/logos/12.png",
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

Lista características. Exige autenticação. Resposta `200 OK`, com itens no formato:

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
  "fotoUrl": "/corretores/7.png",
  "telefone": "81999990000",
  "whatsapp": "81999990000",
  "email": "joao@example.com",
  "apresentacao": "Atendimento residencial",
  "empresa": {
    "id": 12,
    "tipo": "IMOBILIARIA",
    "nome": "Imobiliária Recife",
    "logoMarcaUrl": "/logos/12.png",
    "cidadesAtuacao": [{ "id": 1, "nome": "Recife", "uf": "PE" }]
  }
}
```

O objeto público da empresa inclui também seus dados de contato e endereço disponíveis no DTO.

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

## Imóveis

As rotas de imóveis exigem `CORRETOR` ativo associado a uma empresa ativa. Os IDs de proprietário, cidade e características são validados no contexto do cadastro; o cliente não escolhe a empresa nem o corretor responsável, que vêm do usuário autenticado.

### `POST /imovel`

Cadastra imóvel. Consome `multipart/form-data` com `dto` (JSON) e uma ou mais partes `fotos` (arquivos). A ordem das partes repetidas `fotos` determina a ordem das fotos; `fotoCapaOrdem` é um índice começando em `1`.

**Parte `dto` (exemplo):**

```json
{
  "codigo": "IM-2026-001",
  "titulo": "Apartamento próximo ao parque",
  "proprietarioId": 31,
  "finalidade": "VENDA",
  "valorVenda": 450000.00,
  "valorAluguel": null,
  "valorCondominio": 650.00,
  "valorIptu": 1200.00,
  "descricao": "Apartamento com varanda e duas vagas.",
  "endereco": {
    "cep": "50000000",
    "logradouro": "Rua Exemplo",
    "numero": "100",
    "complemento": "Apto 502",
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

Envie cada arquivo com o mesmo nome de parte:

```bash
curl -X POST http://localhost:8080/imovel \
  -H 'Authorization: Bearer <jwt>' \
  -F 'dto={"codigo":"IM-2026-001","titulo":"Apartamento próximo ao parque","proprietarioId":31,"finalidade":"VENDA","valorVenda":450000,"endereco":{"cep":"50000000","logradouro":"Rua Exemplo","numero":"100","bairro":"Centro","cidadeId":1,"latitude":-8.05,"longitude":-34.9},"caracteristicasIds":[3,8],"fotoCapaOrdem":1};type=application/json' \
  -F 'fotos=@frente.jpg;type=image/jpeg' \
  -F 'fotos=@sala.webp;type=image/webp'
```

É obrigatório enviar ao menos uma foto e escolher uma ordem de capa existente. Cada foto deve ser JPEG, PNG ou WebP. O validador da aplicação aceita arquivos de até 5 MiB, mas a configuração multipart atual não define limites próprios e usa o padrão do Spring Boot: 1 MiB por arquivo e 10 MiB por requisição. Assim, na configuração atual o limite efetivo é 1 MiB por arquivo. Resposta `201 Created` no formato resumido:

```json
{
  "id": 88,
  "codigo": "IM-2026-001",
  "titulo": "Apartamento próximo ao parque",
  "empresaId": 12,
  "corretorId": 7,
  "nomeCorretor": "João Silva",
  "proprietarioId": 31,
  "nomeProprietario": "Ana Souza",
  "finalidade": "VENDA",
  "valorVenda": 450000.00,
  "valorAluguel": null,
  "valorCondominio": 650.00,
  "valorIptu": 1200.00,
  "descricao": "Apartamento com varanda e duas vagas.",
  "endereco": {
    "id": 90,
    "cep": "50000000",
    "logradouro": "Rua Exemplo",
    "numero": "100",
    "complemento": "Apto 502",
    "bairro": "Centro",
    "cidadeId": 1,
    "cidade": "Recife",
    "uf": "PE",
    "latitude": -8.05,
    "longitude": -34.9
  },
  "caracteristicas": [{ "id": 3, "nome": "Piscina" }],
  "fotos": [
    { "id": 101, "url": "/imoveis/88/1.jpg", "ordem": 1, "capa": true }
  ]
}
```

Código duplicado retorna `409`; preço, área de atuação, ordem de capa ou outra regra de negócio inválida retorna `400`; proprietário, cidade ou característica inexistente retorna `404`.

### `GET /imovel` e `GET /imovel/{id}`

Listam imóveis da empresa do corretor autenticado ou consultam um imóvel por ID dentro dessa mesma empresa. Resposta `200 OK` com `ImovelResponseDto` (mesmo formato do cadastro). Imóvel inexistente ou de outra empresa retorna `404`.

O código atual não expõe filtro de imóveis por cidade nem rotas para atualizar ou remover imóveis.

## Regras comuns de arquivos

As fotos de empresa, corretor e imóvel aceitam `image/jpeg`, `image/png` ou `image/webp`. O validador da aplicação permite até 5 MiB por foto. Como `spring.servlet.multipart.max-file-size` e `spring.servlet.multipart.max-request-size` não estão definidos em `application.properties`, a configuração padrão do Spring Boot limita cada arquivo a 1 MiB e cada requisição multipart a 10 MiB; esse é o limite efetivo na configuração atual. Nos endpoints multipart, envie a parte `dto` com `Content-Type: application/json` e os arquivos usando exatamente os nomes documentados para cada rota.
