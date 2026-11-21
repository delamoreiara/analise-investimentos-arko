
package br.com.analise.investimentos.data;

import br.com.analise.investimentos.DatabricksClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ParametroRepository {

    private static final Logger log =
            LoggerFactory.getLogger(ParametroRepository.class);

    private static final long CACHE_TTL_MS = 60_000;

    private final DatabricksClient databricksClient;

    private volatile Map<String, String> cache;
    private volatile long cacheAtualizadoEm;

    public ParametroRepository(DatabricksClient databricksClient) {
        this.databricksClient = databricksClient;
    }

    public Map<String, String> buscarParametros() {

        long agora = System.currentTimeMillis();

        Map<String, String> atual = cache;

        if (atual != null && agora - cacheAtualizadoEm < CACHE_TTL_MS) {
            return atual;
        }

        synchronized (this) {

            agora = System.currentTimeMillis();
            atual = cache;

            if (atual != null && agora - cacheAtualizadoEm < CACHE_TTL_MS) {
                return atual;
            }

            Map<String, String> novosParametros = buscarNoDatabricks();

            cache = Collections.unmodifiableMap(
                    new HashMap<>(novosParametros)
            );

            cacheAtualizadoEm = System.currentTimeMillis();

            log.info("Parâmetros carregados do Databricks: {} registros",
                    cache.size());

            return cache;
        }
    }

    private Map<String, String> buscarNoDatabricks() {

        String sql = """
                SELECT chave, valor
                FROM arko.config.parametro
                """;

        List<Map<String, String>> linhas =
                databricksClient.executarConsultaGenerica(sql);

        Map<String, String> parametros = new HashMap<>();

        for (Map<String, String> linha : linhas) {
            parametros.put(
                    linha.get("chave"),
                    linha.get("valor")
            );
        }

        return parametros;
    }
}
