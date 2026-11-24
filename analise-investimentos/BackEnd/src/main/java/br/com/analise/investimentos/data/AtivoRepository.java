package br.com.analise.investimentos.data;

import br.com.analise.investimentos.DatabricksClient;
import br.com.analise.investimentos.DatabricksException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AtivoRepository {

    private final DatabricksClient databricksClient;

    public AtivoRepository(DatabricksClient databricksClient) {
        this.databricksClient = databricksClient;
    }

    public Optional<Map<String, Object>> buscarAtivo(Map<String, Object> dados) {
        String termo = primeiroTexto(dados, "ticker", "nome", "emissor_nome");
        if (termo == null) {
            return Optional.empty();
        }

        String sql = """
                SELECT *
                FROM arko.gold.ativo
                WHERE LOWER(COALESCE(CAST(ticker AS STRING), '')) = LOWER(TRIM(:termo))
                   OR LOWER(COALESCE(CAST(nome AS STRING), '')) LIKE CONCAT('%', LOWER(TRIM(:termo)), '%')
                   OR LOWER(COALESCE(CAST(identificador AS STRING), '')) = LOWER(TRIM(:termo))
                LIMIT 1
                """;
        List<Map<String, String>> resultado =
                databricksClient.executarConsultaGenerica(sql, Map.of("termo", termo));
        if (resultado.isEmpty()) return Optional.empty();
        return Optional.of(converter(resultado.get(0)));
    }

    public List<Map<String, Object>> buscarFundamentos(Map<String, Object> dados) {
        return consultarIdentificado(
                "SELECT * FROM arko.gold.fundamento WHERE LOWER(COALESCE(CAST(ticker AS STRING), '')) = LOWER(TRIM(:termo)) OR LOWER(COALESCE(CAST(nome AS STRING), '')) LIKE CONCAT('%', LOWER(TRIM(:termo)), '%') OR LOWER(COALESCE(CAST(identificador AS STRING), '')) = LOWER(TRIM(:termo)) LIMIT 100",
                dados);
    }

    public List<Map<String, Object>> buscarRentabilidade(Map<String, Object> dados) {
        return consultarIdentificado(
                "SELECT * FROM arko.gold.rentabilidade WHERE LOWER(COALESCE(CAST(ticker AS STRING), '')) = LOWER(TRIM(:termo)) OR LOWER(COALESCE(CAST(nome AS STRING), '')) LIKE CONCAT('%', LOWER(TRIM(:termo)), '%') OR LOWER(COALESCE(CAST(identificador AS STRING), '')) = LOWER(TRIM(:termo)) LIMIT 100",
                dados);
    }

    public List<Map<String, Object>> buscarSerieGrafico(Map<String, Object> dados) {
        return consultarIdentificado(
                "SELECT * FROM arko.gold.serie_grafico WHERE LOWER(COALESCE(CAST(ticker AS STRING), '')) = LOWER(TRIM(:termo)) OR LOWER(COALESCE(CAST(nome AS STRING), '')) LIKE CONCAT('%', LOWER(TRIM(:termo)), '%') OR LOWER(COALESCE(CAST(identificador AS STRING), '')) = LOWER(TRIM(:termo)) LIMIT 365",
                dados);
    }

    public Optional<Double> buscarDividendYield(Map<String, Object> dados) {
        List<Map<String, Object>> linhas = consultarIdentificado(
                "SELECT * FROM arko.gold.dividend_yield_12m WHERE LOWER(COALESCE(CAST(ticker AS STRING), '')) = LOWER(TRIM(:termo)) OR LOWER(COALESCE(CAST(nome AS STRING), '')) LIKE CONCAT('%', LOWER(TRIM(:termo)), '%') OR LOWER(COALESCE(CAST(identificador AS STRING), '')) = LOWER(TRIM(:termo)) LIMIT 1",
                dados);
        if (linhas.isEmpty()) return Optional.empty();
        Object valor = linhas.get(0).get("valor");
        if (valor == null) valor = linhas.get(0).get("dividend_yield_12m");
        if (valor == null) valor = linhas.get(0).get("dividend_yield");
        if (valor == null) return Optional.empty();
        try {
            return Optional.of(Double.valueOf(String.valueOf(valor).replace(",", ".")));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> buscarFundo(Map<String, Object> dados) {
        String termo = primeiroTexto(dados, "ticker", "nome", "emissor_nome");
        if (termo == null) return Optional.empty();
        String sql = """
                SELECT *
                FROM arko.ref.fundo
                WHERE LOWER(COALESCE(CAST(ticker AS STRING), '')) = LOWER(TRIM(:termo))
                   OR LOWER(COALESCE(CAST(nome AS STRING), '')) LIKE CONCAT('%', LOWER(TRIM(:termo)), '%')
                   OR LOWER(COALESCE(CAST(cnpj AS STRING), '')) = LOWER(TRIM(:termo))
                LIMIT 1
                """;
        List<Map<String, String>> resultado =
                databricksClient.executarConsultaGenerica(sql, Map.of("termo", termo));
        if (resultado.isEmpty()) return Optional.empty();
        return Optional.of(converter(resultado.get(0)));
    }

    private List<Map<String, Object>> consultarIdentificado(String sql, Map<String, Object> dados) {
        String termo = primeiroTexto(dados, "ticker", "nome", "emissor_nome");
        if (termo == null) return List.of();
        List<Map<String, String>> resultado =
                databricksClient.executarConsultaGenerica(sql, Map.of("termo", termo));
        List<Map<String, Object>> convertido = new ArrayList<>();
        for (Map<String, String> linha : resultado) convertido.add(converter(linha));
        return convertido;
    }

    private String primeiroTexto(Map<String, Object> dados, String... campos) {
        for (String campo : campos) {
            Object valor = dados.get(campo);
            if (valor != null && !String.valueOf(valor).isBlank()) return String.valueOf(valor).trim();
        }
        return null;
    }

    private Map<String, Object> converter(Map<String, String> linha) {
        Map<String, Object> convertido = new LinkedHashMap<>();
        linha.forEach((chave, valor) -> {
            if (valor == null) {
                convertido.put(chave, null);
                return;
            }
            try {
                convertido.put(chave, Double.valueOf(valor));
            } catch (NumberFormatException e) {
                convertido.put(chave, valor);
            }
        });
        return convertido;
    }
}
