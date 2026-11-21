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
mvn spring-boot:run
```

Java 21 e Maven são necessários.

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
