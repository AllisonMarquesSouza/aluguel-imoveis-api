# Status do projeto

> Atualizado em 19/10/2026. Consulte os requisitos e a arquitetura para o detalhamento das regras de negócio e do banco.

## W01 — Empresa e usuários

- [x] Implementação-base existente: cadastro, login/JWT, perfis e empresas
- [ ] Completar e verificar o isolamento multitenant nas operações do sistema
- [ ] Revisar autenticação/autorização para usuários inativos ou bloqueados
- [ ] Revisar testes e critérios de aceitação pendentes

## W02 — Cadastro de corretores

- [x] Implementação-base existente: cadastro, vínculo com empresa e validação de foto
- [ ] Expor ativação e desativação de corretores pela API
- [ ] Disponibilizar perfil público do corretor e dados da empresa
- [ ] Garantir o efeito do status inativo sobre vitrine e contato
- [ ] Revisar testes e critérios de aceitação pendentes

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
