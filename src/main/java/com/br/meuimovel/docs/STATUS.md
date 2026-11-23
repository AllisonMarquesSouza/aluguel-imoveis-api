# Status do projeto

> Atualizado em 19/11/2026. Consulte os requisitos e a arquitetura para o detalhamento das regras de negócio e do banco.

## W01 — Empresa e usuários

- [x] Implementação-base existente: cadastro, login/JWT, perfis e empresas
- [x] Isolar consultas por empresa e impedir leitura de empresa de outro tenant por ID
- [x] Limitar listagem de empresas e cadastro de usuários a administradores
- [ ] Revisar autenticação conforme status ativo/inativo/bloqueado (adiado)
- [x] Cobrir isolamento por empresa com testes unitários

### Verificação

- [x] Testes de EmpresaService (30), ProprietarioService e ImovelService (7): 37 testes passaram
- [x] `git diff --check`
- [ ] Teste integrado de contexto: pendente nesta validação; a configuração local do PostgreSQL rejeita as credenciais (`SQLSTATE 28P01`, ver verificação da W04)

## W02 — Cadastro de corretores

- [x] Implementação-base existente: cadastro, vínculo com empresa e validação de foto
- [x] Expor ativação e desativação de corretores pela API, restritas a gestores da própria empresa
- [x] Disponibilizar listagens autenticadas dos corretores ativos e inativos da empresa do gestor, derivando o tenant do token
- [x] Disponibilizar listagem e perfil público do corretor com dados públicos da empresa
- [x] Ocultar corretores inativos da vitrine e do perfil público, incluindo seus contatos
- [x] Cobrir cadastro, autorização, isolamento por empresa, transições de status e perfil público com testes unitários

### Verificação

- [x] `CorretorServiceTest`: 15 testes passaram; suíte unitária do projeto, excluindo `MeuImovelApplicationTests`: 125 testes passaram
- [x] `git diff --check`
- [ ] `MeuImovelApplicationTests.contextLoads`: não inicializou porque a conexão ao PostgreSQL local foi bloqueada no ambiente (`SQLState 08001`, `SocketException: Operation not permitted`)

## W03 — Proprietários e características

- [x] Implementação-base existente: proprietários, busca por nome/documento, cidades e características
- [x] Validar documento e evitar duplicidade de proprietário dentro da empresa
- [x] Restringir operações de proprietário à empresa do corretor e retornar DTOs nas respostas
- [x] Manter cidades e características por administradores e retornar cidades por DTO
- [x] Associar características selecionadas ao cadastro do imóvel
- [ ] Associar características à atualização do imóvel (não há endpoint de atualização; fora do escopo atual)
- [x] Cobrir validação documental, duplicidade, autorização, catálogos e associação de características com testes unitários

### Verificação

- [x] Testes unitários de ProprietarioService (6), CidadeService (4), CaracteristicaService (5), DocumentoValidator (4), EmpresaService (30) e ImovelService (6): 55 testes passaram
- [x] `git diff --check`
- [ ] Teste de contexto do Spring não executado nesta validação

## W04 — Cadastro comum de imóveis

### Estrutura de dados

- [x] Migration do imóvel (V9)
- [x] Migration do endereço (V10)
- [x] Migration da tabela intermediária de características (V11)
- [x] Migration das fotos (V12)

### Camada de persistência

- [x] Entidade Imovel
- [x] Entidade EnderecoImovel
- [x] Entidade FotoImovel
- [x] Relacionamentos com as entidades existentes
- [x] Repositories

### DTOs

- [x] ImovelCreateDto
- [x] EnderecoImovelDto
- [x] EnderecoImovelResponseDto
- [x] ImovelResponseDto
- [x] FotoImovelResponseDto
- [x] CaracteristicaResponseDto

### Serviço e API

- [x] Revisar manualmente a consistência entre migrations e entidades
- [x] Implementar ImovelService e validações de negócio
- [x] Validar preço conforme a finalidade; `VENDA_E_ALUGUEL` exige ambos
- [x] Restringir corretor, proprietário e imóvel à empresa do corretor autenticado
- [x] Validar cidade na área de atuação da empresa; a UF é derivada da cidade cadastrada
- [x] Exigir endereço, cidade e coordenadas no cadastro
- [x] Armazenar várias fotos, ordenar pela lista enviada e selecionar exatamente uma capa
- [x] Implementar `POST /imovel`, `GET /imovel` e `GET /imovel/{id}`, com isolamento por empresa
- [x] Criar testes unitários para preços, cadastro, características e fotos
- [x] Cobrir os critérios de cadastro da W04

### Verificação

- [x] Compilação Maven e testes unitários de `ImovelService` (6 testes)
- [ ] Teste de contexto (`MeuImovelApplicationTests.contextLoads`) — o PostgreSQL local rejeitou as credenciais configuradas (SQLSTATE 28P01)

### Fora do escopo atual

- Atualização e remoção de imóveis
- Publicação formal do imóvel (implementada posteriormente na W08)
- Naturezas fora do escopo original W04: apartamento (implementado na W06) e rural
- Loteamentos, quadras e lotes
- Vitrine pública completa e filtros do aplicativo
- Favoritos e funcionalidades previstas para etapas posteriores

## W05 — Casa e terreno livre

- [x] Incluir a natureza `CASA` ou `TERRENO` na migration do imóvel; criar tabelas `casa` (V13) e `terreno` (V14)
- [x] Cadastrar dados de casa: áreas, cômodos, vagas, pavimentos, ano e características
- [x] Calcular `areaLivre` como área do terreno menos área construída, sem aceitar ou persistir esse campo
- [x] Recusar área construída superior à área do terreno com mensagem clara
- [x] Cadastrar dados de terreno: área, frente, fundo, laterais, topografia, muro, esquina e uso permitido
- [x] Retornar os dados do bloco correspondente à natureza no DTO do imóvel (o outro bloco fica nulo)
- [ ] Adaptar os campos da tela conforme a natureza escolhida — este repositório contém a API, sem interface web
- [x] Cobrir criação das duas naturezas, cálculo e validação com testes de serviço

### Verificação

- [x] `./mvnw -q -Dtest=ImovelServiceTest test` — 9 testes passaram
- [x] `./mvnw -q -Dtest='!MeuImovelApplicationTests' test` — 92 testes passaram
- [x] `git diff --check`
- [ ] `MeuImovelApplicationTests.contextLoads` — não inicializou: PostgreSQL local indisponível em `localhost:5432` (conexão recusada, SQLState `08001`); a migration ainda precisa ser validada com o banco acessível

## W06 — Prédio e apartamento

- [x] Incluir `APARTAMENTO` na natureza do imóvel em V9; criar prédio (V15) e apartamento (V16) em migrations separadas
- [x] Cadastrar, listar, consultar e atualizar prédios dentro da empresa do corretor autenticado
- [x] Validar a cidade do endereço do prédio na área de atuação da empresa
- [x] Cadastrar apartamento como natureza de imóvel e vinculá-lo somente a prédio da mesma empresa
- [x] Expor os dados do prédio atualizado no detalhe das unidades, incluindo o endereço compartilhado
- [x] Recusar duplicidade de andar e número no serviço e no banco
- [x] Documentar os endpoints de prédio e os dados de apartamento no `README.md`
- [ ] Adaptar os campos da tela conforme a natureza — este repositório contém a API, sem interface web
- [x] Cobrir cadastro e consulta de prédio, isolamento por empresa, atualização, cidade e cadastro/duplicidade de apartamento com testes unitários

### Verificação

- [x] `./mvnw -q -DskipTests compile`
- [x] `./mvnw -q -Dtest=ImovelServiceTest,PredioServiceTest test` — 17 testes passaram
- [x] `./mvnw -q -Dtest='!MeuImovelApplicationTests' test` — 100 testes passaram
- [x] `git diff --check`
- [ ] `MeuImovelApplicationTests.contextLoads` e validação de V15/V16 — não inicializaram: conexão ao PostgreSQL bloqueada no ambiente (`SQLState 08001`, `SocketException: Operation not permitted`)

## W07 — Loteamento, quadras e lotes

- [x] Incluir `LOTE` no constraint de natureza da migration V9 de `imovel`
- [x] Criar as tabelas `loteamento` (V18), `quadra` (V19) e `lote` (V20), uma por migration
- [x] Cadastrar e consultar loteamentos da empresa autenticada, com cidade, incorporadora, infraestrutura e áreas de lazer
- [x] Cadastrar quadras por loteamento e impedir identificação duplicada dentro dele
- [x] Cadastrar lote como imóvel, com endereço na cidade do loteamento, preço de venda, medidas e condições de parcelamento
- [x] Impedir repetição do número do lote dentro da quadra no serviço e no banco
- [x] Retornar no detalhe do imóvel os dados atuais do lote, da quadra e do loteamento
- [x] Expor por quadra os totais disponíveis e vendidos; permitir atualizar a situação do lote
- [x] Documentar os recursos e payloads no `README.md` e atualizar este documento e a arquitetura
- [ ] Adaptar a tela para loteamentos, quadras e lotes — este repositório contém a API, sem interface web
- [x] Cobrir cadastro e isolamento de loteamentos, contagens, criação de lotes e duplicidade com testes de serviço

### Verificação

- [x] `./mvnw -q -Dtest=ImovelServiceTest,PredioServiceTest,LoteamentoServiceTest,LoteServiceTest test` — 28 testes passaram
- [x] `./mvnw -q -Dtest='!MeuImovelApplicationTests' test` — 111 testes passaram
- [x] `git diff --check`
- [ ] `MeuImovelApplicationTests.contextLoads` e validação de V9 e V18–V20 — não inicializaram: a conexão local ao PostgreSQL foi bloqueada pelo ambiente (`SQLState 08001`, `SocketException: Operation not permitted`)

O lote é integrado ao cadastro e ao detalhe comum de imóveis. A consulta pública básica da vitrine foi criada na W08 e ganhou busca e filtros na W09.

## W08 — Documentos, publicação, situação e histórico

- [x] Adicionar situação inicial `DISPONIVEL` e publicação inicialmente desativada ao imóvel na migration V9
- [x] Criar migrations separadas para documentos do imóvel (V21) e histórico de situação (V22)
- [x] Anexar um arquivo por tipo (`MATRICULA`, `CARNE_IPTU`, `AUTORIZACAO_PROPRIETARIO`) com data e download privado
- [x] Permitir ao gestor ativar e desativar a publicação, exigindo endereço completo para publicar
- [x] Permitir ao gestor alterar a situação para disponível, reservado, vendido ou alugado
- [x] Registrar cada mudança efetiva com autor, data, situação anterior, nova situação e observação
- [x] Sincronizar situação de lote com a situação do imóvel e registrar histórico também pela rota de lotes
- [x] Expor na vitrine somente imóveis publicados e disponíveis; ocultar também imóveis de corretores inativos conforme a W02
- [x] Cobrir upload e validação de documentos, publicação, histórico e filtro da vitrine com testes unitários
- [x] Documentar endpoints e atualizar arquitetura e status

### Verificação

- [x] `./mvnw -q -DskipTests compile`
- [x] `./mvnw -q -Dtest=ImovelGestaoServiceTest,DocumentoImovelServiceTest,ImovelVitrineServiceTest,DocumentoImovelValidatorTest,LocalDocumentoStorageServiceTest,ImovelServiceTest,LoteServiceTest,LoteamentoServiceTest,PredioServiceTest test` — 45 testes passaram
- [x] `./mvnw -q -Dtest='!MeuImovelApplicationTests' test` — 128 testes passaram
- [x] `git diff --check`
- [ ] `MeuImovelApplicationTests.contextLoads` e validação das migrations V9, V20–V22 — não inicializaram porque a conexão local ao PostgreSQL foi bloqueada pelo ambiente (`SQLState 08001`, `SocketException: Operation not permitted`)

Os documentos usam armazenamento local configurável por `APP_STORAGE_LOCATION`, separado das imagens e acessível apenas pelas rotas autenticadas de gestão.

## W09 — API pública da vitrine

- [x] Listar cidades da área de atuação das empresas ativas, incluindo nome, ID e UF
- [x] Exigir cidade na busca pública e manter a consulta limitada à cidade informada
- [x] Filtrar por texto em título, bairro e descrição; finalidade; natureza; faixa de preço; quartos; suítes; vagas; bairro; faixa de área; e características
- [x] Centralizar os filtros em uma consulta nativa paginada no `ImovelRepository`, com consulta de contagem correspondente
- [x] Ordenar por preço, área ou mais recentes e paginar com tamanho limitado
- [x] Retornar detalhe com fotos, endereço e coordenadas, dados da natureza e corretor responsável
- [x] Usar DTOs públicos sem proprietário, documentos, código interno do imóvel, observações da empresa ou ID da empresa do prédio
- [x] Disponibilizar o catálogo de características sem token para preencher esse filtro
- [x] Permitir acesso sem token às cidades, busca e detalhe da vitrine
- [x] Documentar parâmetros, semântica dos filtros, paginação e resposta pública

### Verificação

- [x] `./mvnw -q -DskipTests compile`
- [x] `./mvnw -q -Dtest=ImovelVitrineServiceTest test` — 9 testes passaram
- [x] `./mvnw -q -Dtest='!MeuImovelApplicationTests' test` — 134 testes passaram
- [x] `git diff --check`
- [ ] `MeuImovelApplicationTests.contextLoads` e validação da query nativa — não inicializou porque PostgreSQL não está aceitando conexão em `localhost:5432` (`SQLState 08001`, connection refused)

## Instrução de continuidade

Antes de iniciar a próxima etapa, confira o estado real do código e atualize este documento caso encontre diferenças. Não marque itens como concluídos sem verificar sua implementação.

## [WEB] EXTRA — Gerenciamento de imagens

- [x] Armazenar imagens em uma pasta configurável, organizada por tipo e ID da entidade
- [x] Compartilhar armazenamento e validação de imagens entre os cadastros existentes e o controller de imagens
- [x] Disponibilizar leitura por URL, upload, substituição e remoção com autorização e isolamento por empresa
- [x] Validar conteúdo JPEG, PNG e WebP e limitar cada arquivo a 5 MB
- [x] Preservar as regras de foto obrigatória do corretor e do imóvel, promovendo nova capa quando a atual é excluída
- [x] Documentar endpoints e consumo na seção **Imagens** do `README.md`

### Verificação

- [x] Compilação Maven
- [x] `git diff --check`
- [x] Testes de `ImagemService`, `LocalFileStorageService` e `FotoValidator`: 8 testes passaram
- [x] Testes unitários do projeto, excluindo o teste de contexto: 77 testes passaram
- [ ] `MeuImovelApplicationTests.contextLoads`: não inicializou porque a autenticação do PostgreSQL local falhou (`SQLSTATE 28P01`)
