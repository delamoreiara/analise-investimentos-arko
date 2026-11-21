-- Exemplos CONSERVADORES: classificação/alertas, não recomendação financeira.
-- Revise critérios com responsável de negócio antes de executar.
-- Não executar automaticamente em produção.
INSERT INTO arko.config.regra (regra_id, secao, nome, classe, efeito, objetivo, prioridade, mensagem, alternativa, ativa, versao) VALUES
('TD_SELIC_INFO','TD','Tesouro Selic identificado','TESOURO_DIRETO','CONDICIONA',NULL,50,'Tesouro Selic identificado; verificar prazo, preço de venda e adequação ao objetivo.',NULL,true,1),
('TD_IPCA_INFO','TD','Tesouro IPCA identificado','TESOURO_DIRETO','CONDICIONA',NULL,51,'Tesouro IPCA+ identificado; avaliar marcação a mercado e horizonte.',NULL,true,1),
('TD_PRE_INFO','TD','Tesouro Prefixado identificado','TESOURO_DIRETO','CONDICIONA',NULL,52,'Tesouro Prefixado identificado; avaliar risco de taxa e prazo.',NULL,true,1),
('FUNDO_INFO','FUN','Fundo identificado','FUNDO_INVESTIMENTO','CONDICIONA',NULL,60,'Fundo identificado; verificar regulamento, taxas, riscos e resgate.',NULL,true,1),
('ACAO_INFO','ACO','Ação identificada','ACAO','CONDICIONA',NULL,70,'Ação identificada; avaliar volatilidade, fundamentos e concentração.',NULL,true,1),
('FII_INFO','FII','FII identificado','FII','CONDICIONA',NULL,80,'FII identificado; avaliar liquidez, imóveis, vacância e riscos.',NULL,true,1);
INSERT INTO arko.config.regra_condicao (regra_id, grupo, campo, operador, valor) VALUES
('TD_SELIC_INFO',1,'tipo_ativo','=','TESOURO_SELIC'),
('TD_IPCA_INFO',1,'tipo_ativo','=','TESOURO_IPCA'),
('TD_PRE_INFO',1,'tipo_ativo','=','TESOURO_PREFIXADO');
