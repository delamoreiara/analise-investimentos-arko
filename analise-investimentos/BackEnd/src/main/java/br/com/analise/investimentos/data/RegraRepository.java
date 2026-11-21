
package br.com.analise.investimentos.data;

import br.com.analise.investimentos.DatabricksClient;
import br.com.analise.investimentos.rules.Condicao;
import br.com.analise.investimentos.rules.Regra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RegraRepository {

    private static final Logger log =
            LoggerFactory.getLogger(RegraRepository.class);

    private static final long CACHE_TTL_MS = 60_000;

    private final DatabricksClient databricksClient;

    private volatile List<Regra> cache;
    private volatile long cacheAtualizadoEm;

    public RegraRepository(DatabricksClient databricksClient) {
        this.databricksClient = databricksClient;
    }

    public List<Regra> buscarRegrasAtivas() {

        long agora = System.currentTimeMillis();

        List<Regra> atual = cache;

        if (atual != null && agora - cacheAtualizadoEm < CACHE_TTL_MS) {
            return atual;
        }

        synchronized (this) {

            agora = System.currentTimeMillis();
            atual = cache;

            if (atual != null && agora - cacheAtualizadoEm < CACHE_TTL_MS) {
                return atual;
            }

            List<Regra> novasRegras = buscarNoDatabricks();

            cache = List.copyOf(novasRegras);
            cacheAtualizadoEm = System.currentTimeMillis();

            log.info(
                    "Regras carregadas do Databricks: {} registros",
                    cache.size()
            );

            return cache;
        }
    }

    private List<Regra> buscarNoDatabricks() {

        String sqlRegras = """
                SELECT
                    regra_id,
                    secao,
                    nome,
                    classe,
                    efeito,
                    objetivo,
                    prioridade,
                    mensagem,
                    alternativa,
                    ativa,
                    versao
                FROM arko.config.regra
                WHERE ativa = true
                ORDER BY prioridade
                """;

        String sqlCondicoes = """
                SELECT
                    c.regra_id,
                    c.grupo,
                    c.campo,
                    c.operador,
                    c.valor
                FROM arko.config.regra_condicao c
                INNER JOIN arko.config.regra r
                    ON r.regra_id = c.regra_id
                WHERE r.ativa = true
                """;

        List<Map<String, String>> linhasRegras =
                databricksClient.executarConsultaGenerica(sqlRegras);

        List<Map<String, String>> linhasCondicoes =
                databricksClient.executarConsultaGenerica(sqlCondicoes);

        Map<String, List<Condicao>> condicoesPorRegra =
                new HashMap<>();

        for (Map<String, String> linha : linhasCondicoes) {

            String regraId = linha.get("regra_id");

            Condicao condicao = new Condicao(
                    regraId,
                    Integer.parseInt(linha.get("grupo")),
                    linha.get("campo"),
                    linha.get("operador"),
                    linha.get("valor")
            );

            condicoesPorRegra
                    .computeIfAbsent(regraId, id -> new ArrayList<>())
                    .add(condicao);
        }

        List<Regra> regras = new ArrayList<>();

        for (Map<String, String> linha : linhasRegras) {

            String regraId = linha.get("regra_id");

            List<Condicao> condicoes = List.copyOf(
                    condicoesPorRegra.getOrDefault(regraId, List.of())
            );

            Regra regra = new Regra(
                    regraId,
                    linha.get("secao"),
                    linha.get("nome"),
                    linha.get("classe"),
                    linha.get("efeito"),
                    linha.get("objetivo"),
                    Integer.parseInt(linha.get("prioridade")),
                    linha.get("mensagem"),
                    linha.get("alternativa"),
                    Boolean.parseBoolean(linha.get("ativa")),
                    Integer.parseInt(linha.get("versao")),
                    condicoes
            );

            regras.add(regra);
        }

        return regras;
    }
}
