# Status do projeto

> Atualizado em 21/10/2026. Consulte os requisitos e a arquitetura para o detalhamento das regras de negócio e do banco.

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
- [x] Disponibilizar listagem e perfil público do corretor com dados públicos da empresa
- [x] Ocultar corretores inativos da listagem e do perfil público, incluindo seus contatos
- [x] Cobrir cadastro, autorização, isolamento por empresa, transições de status e perfil público com testes unitários

### Verificação

- [x] Testes unitários de CorretorService, EmpresaService, ProprietarioService e ImovelService: 49 testes passaram
- [x] `git diff --check`
- [ ] Teste de contexto do Spring não executado nesta validação

## W03 — Proprietários e características

- [x] Implementação-base existente: proprietários, busca por nome/documento, cidades e características
- [x] Validar documento e evitar duplicidade de proprietário dentro da empresa
- [ ] Integrar características ao cadastro e atualização do imóvel
- [ ] Revisar testes e critérios de aceitação pendentes

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
- [x] Criar testes unitários para preços, cadastro e fotos
- [x] Cobrir os critérios de cadastro da W04

### Verificação

- [x] Compilação Maven e testes unitários de `ImovelService` (3 testes)
- [ ] Teste de contexto (`MeuImovelApplicationTests.contextLoads`) — o PostgreSQL local rejeitou as credenciais configuradas (SQLSTATE 28P01)

### Fora do escopo atual

- Atualização e remoção de imóveis
- Publicação formal do imóvel; o cadastro exige endereço, cidade e coordenadas, mas o modelo atual não possui estado ou endpoint de publicação
- Naturezas específicas: casa, apartamento, terreno e rural
- Loteamentos, quadras e lotes
- Vitrine pública completa e filtros do aplicativo
- Favoritos e funcionalidades previstas para etapas posteriores

## Instrução de continuidade

Antes de iniciar a próxima etapa, confira o estado real do código e atualize este documento caso encontre diferenças. Não marque itens como concluídos sem verificar sua implementação.
