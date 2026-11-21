-- Preparação para persistência durável (a API atual mantém histórico em memória).
-- Executar somente após validar permissões e política de retenção.
CREATE TABLE IF NOT EXISTS arko.config.analise_historico (
  analise_id STRING,
  criado_em TIMESTAMP,
  origem_extracao STRING,
  dados_json STRING,
  alertas_json STRING,
  resultado_json STRING
) USING DELTA;
