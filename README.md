# Arko - Recomendador de Ativos

Plataforma web para analisar ativos informados em texto livre. O React envia a consulta ao backend Spring Boot, que identifica características, consulta o Databricks e aplica regras configuradas no Catálogo Unity.

## Arquitetura

```text
React + Vite
  -> POST /api/analyze/detalhado?texto=...
  -> Spring Boot
       -> parser local de características
       -> repositórios Databricks
       -> motor de regras por dados e parâmetros
  -> Databricks SQL Statement API
       -> Unity Catalog / tabelas Delta
```

O frontend usa componentes, hooks e services separados. O backend separa controlador, parser, repositórios, serviço e motor de regras.

## Executar

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

Opcional: `VITE_API_URL=http://localhost:8080`.

### Backend

```powershell
cd analise-investimentos\BackEnd
$env:DATABRICKS_HOST="https://<workspace>"
$env:DATABRICKS_TOKEN="<token>"
$env:DATABRICKS_WAREHOUSE_ID="<warehouse-id>"
.\mvnw.cmd spring-boot:run
```

Use somente os comandos dentro do bloco, sem copiar as linhas de delimitadores ```. Java 21 é necessário; o Maven Wrapper baixa a versão do Maven configurada no projeto.

### Docker

Na raiz do repositório, construa e execute a API:

```powershell
docker build -t arko-api -f analise-investimentos/BackEnd/Dockerfile analise-investimentos/BackEnd
docker run --rm -p 8080:8080 -e DATABRICKS_HOST="https://<workspace>" -e DATABRICKS_TOKEN="<token>" -e DATABRICKS_WAREHOUSE_ID="<warehouse-id>" -e APP_CORS_ALLOWED_ORIGINS="http://localhost:5173" arko-api
```

Em produção, defina `APP_CORS_ALLOWED_ORIGINS` como a origem pública do frontend. Configure as credenciais do Databricks como variáveis secretas no serviço de hospedagem, nunca no Dockerfile ou no repositório. A API escuta a porta informada pelo host em `PORT` (usa `8080` localmente).

## Deploy do backend no Render e conexão com a Vercel

O arquivo `render.yaml` configura o backend Docker, o health check em `/api/health`, a origem de produção da Vercel e as variáveis secretas do Databricks.

1. No Render, crie um **Blueprint** a partir deste repositório e forneça `DATABRICKS_HOST`, `DATABRICKS_TOKEN` e `DATABRICKS_WAREHOUSE_ID` quando solicitado. Não salve o token no repositório.
2. Aguarde o deploy e confirme que `https://<url-do-servico-render>/api/health` retorna `{"status":"UP"}`.
3. Na Vercel, configure `VITE_API_URL` como a URL HTTPS do serviço Render, sem barra final, por exemplo `https://analise-investimentos-arko-api.onrender.com`.
4. Faça um novo deploy do frontend na Vercel: `VITE_API_URL` é aplicado durante o build.
5. Se o domínio de produção da Vercel mudar, atualize `APP_CORS_ALLOWED_ORIGINS` no Render para a origem exata do frontend e faça redeploy do backend.

O plano gratuito do Render pode suspender o serviço após inatividade; a primeira requisição depois disso pode demorar enquanto a API inicia. Para desenvolvimento local, copie `frontend/.env.example` para `frontend/.env.local`.

## Databricks

O cliente usa a SQL Statement API e consultas parametrizadas. Os repositórios esperam:

- `arko.ref.emissor`
- `arko.config.regra`
- `arko.config.regra_condicao`
- `arko.config.parametro`

As regras não usam nome ou ticker em `if`. Cada regra tem classe, efeito, objetivo, prioridade e condições cadastradas em tabelas. Operadores suportados: `=`, `!=`, `>`, `>=`, `<`, `<=` e `contains`. Valores podem apontar para parâmetros com `$param:<chave>`.

## Fluxo de análise

1. Extrai classe, ticker, emissor, indexador, liquidez e vencimento.
2. Enriquece o emissor pela tabela de referência.
3. Avalia regras ativas da classe.
4. Precedência: `REPROVA` prevalece sobre `CONDICIONA`/`ELEGIVEL`.
5. Retorna veredito, objetivo, regras, fundamentos, histórico e estado de consulta.

Se o Databricks estiver indisponível ou o ativo não existir, a API retorna `INDETERMINADO` e uma explicação honesta. Não inventa fundamentos nem gráficos.

## Testes

```powershell
cd analise-investimentos\BackEnd
mvn test

cd ..\..\frontend
npm run build
```

## SQL e ETL

Scripts iniciais estão em `analise-investimentos/DataBricks/sql/`.

O projeto já contém a estrutura de regras e o cliente SQL. Antes da apresentação, a equipe deve criar as tabelas Delta em Unity Catalog, carregar dados públicos em camadas bronze/silver/ref/gold e reproduzir o ETL por notebook ou Job.

## Premissas e limitações

- CDB/LCI/LCA dependem de características no texto e da tabela de emissores.
- LCI/LCA isentas são comparáveis pelo equivalente bruto quando o campo histórico existir no Databricks.
- Histórico, dividend yield e fundamentos só aparecem quando estiverem disponíveis nas tabelas da base.
- LLM opcional interpreta texto; não participa do veredito.

## Cobertura dos checkpoints

- CDB, RDB, LCI e LCA têm subtipo identificado pelo parser.
- O fluxo detalhado consulta `gold.ativo`, `gold.fundamento`, `gold.rentabilidade`, `gold.serie_grafico` e `gold.dividend_yield_12m`, além de `ref.fundo`.
- A consulta ao Databricks não inventa valores: ativo desconhecido é informado como não encontrado; falha temporária gera veredito indeterminado.
- O frontend mostra “indisponível” para séries e dividend yield ausentes e mantém o valor zero distinto de ausência.
- Limitação conhecida: nomes das colunas do Databricks devem seguir os nomes utilizados nas consultas (`ticker`, `nome`, `identificador`, `classe`, `gestora`, `valor`, `rentabilidade`, `cota`, `dividend_yield_12m` ou `dividend_yield`). Regras específicas devem ser cadastradas em `arko.config`.
