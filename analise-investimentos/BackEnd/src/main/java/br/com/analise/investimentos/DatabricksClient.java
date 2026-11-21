
package br.com.analise.investimentos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabricksClient {

    private final RestClient restClient;
    private final String warehouseId;
    private final ObjectMapper objectMapper;

    private static final int MAX_TENTATIVAS = 30;
    private static final long INTERVALO_MS = 1000;

    public DatabricksClient(
            @Value("${databricks.host}") String host,
            @Value("${databricks.token}") String token,
            @Value("${databricks.warehouse-id}") String warehouseId,
            ObjectMapper objectMapper) {

        this.warehouseId = warehouseId;
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl(host)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    public List<EmissorDTO> executarConsulta(String sql) {
        return executarConsulta(sql, Map.of());
    }

    public List<EmissorDTO> executarConsulta(
            String sql,
            Map<String, String> parametros) {

        List<Map<String, String>> linhas =
                executarConsultaGenerica(sql, parametros);

        List<EmissorDTO> emissores = new ArrayList<>();

        for (Map<String, String> linha : linhas) {
            emissores.add(new EmissorDTO(
                    linha.get("nome"),
                    linha.get("porte")
            ));
        }

        return emissores;
    }

    public List<Map<String, String>> executarConsultaGenerica(String sql) {
        return executarConsultaGenerica(sql, Map.of());
    }

    public List<Map<String, String>> executarConsultaGenerica(
            String sql,
            Map<String, String> parametros) {

        try {
            Map<String, Object> body = new HashMap<>();

            body.put("warehouse_id", warehouseId);
            body.put("statement", sql);
            body.put("wait_timeout", "30s");
            body.put("on_wait_timeout", "CONTINUE");

            List<Map<String, String>> parametrosSql = new ArrayList<>();

            parametros.forEach((nome, valor) -> {
                parametrosSql.add(Map.of(
                        "name", nome,
                        "value", valor,
                        "type", "STRING"
                ));
            });

            if (!parametrosSql.isEmpty()) {
                body.put("parameters", parametrosSql);
            }

            String resposta = restClient.post()
                    .uri("/api/2.0/sql/statements")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode json = lerJson(resposta);

            json = aguardarConclusao(json);

            JsonNode colunas = json.path("manifest")
                    .path("schema")
                    .path("columns");

            JsonNode linhas = json.path("result")
                    .path("data_array");

            if (!colunas.isArray() || !linhas.isArray()) {
                throw new DatabricksException(
                        "Resposta do Databricks sem estrutura de resultado válida",
                        null
                );
            }

            List<Map<String, String>> resultado = new ArrayList<>();

            for (JsonNode linha : linhas) {

                if (!linha.isArray() || linha.size() != colunas.size()) {
                    throw new DatabricksException(
                            "Linha retornada pelo Databricks possui estrutura inválida",
                            null
                    );
                }

                Map<String, String> registro = new LinkedHashMap<>();

                for (int i = 0; i < colunas.size(); i++) {

                    String nomeColuna = colunas.get(i)
                            .path("name")
                            .asText();

                    JsonNode valor = linha.get(i);

                    registro.put(
                            nomeColuna,
                            valor == null || valor.isNull()
                                    ? null
                                    : valor.asText()
                    );
                }

                resultado.add(registro);
            }

            return resultado;

        } catch (DatabricksException e) {
            throw e;

        } catch (RestClientResponseException e) {

            throw new DatabricksException(
                    "Databricks retornou erro HTTP: "
                            + e.getStatusCode().value(),
                    e.getStatusCode().value(),
                    e
            );

        } catch (ResourceAccessException e) {

            throw new DatabricksException(
                    "Não foi possível estabelecer comunicação com o Databricks",
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new DatabricksException(
                    "Consulta ao Databricks foi interrompida",
                    e
            );

        } catch (Exception e) {

            throw new DatabricksException(
                    "Erro inesperado ao executar consulta no Databricks",
                    e
            );
        }
    }

    private JsonNode lerJson(String resposta) {

        if (resposta == null || resposta.isBlank()) {
            throw new DatabricksException(
                    "Databricks retornou uma resposta vazia",
                    null
            );
        }

        try {
            JsonNode json = objectMapper.readTree(resposta);

            if (json == null || json.isNull()) {
                throw new DatabricksException(
                        "Databricks retornou JSON inválido",
                        null
                );
            }

            return json;

        } catch (DatabricksException e) {
            throw e;

        } catch (Exception e) {
            throw new DatabricksException(
                    "Não foi possível interpretar a resposta do Databricks",
                    e
            );
        }
    }

    private JsonNode aguardarConclusao(JsonNode json)
            throws InterruptedException {

        String estado = json.path("status")
                .path("state")
                .asText();

        String statementId = json.path("statement_id").asText();

        int tentativas = 0;

        while ("PENDING".equals(estado) || "RUNNING".equals(estado)) {

            if (tentativas >= MAX_TENTATIVAS) {
                throw new DatabricksException(
                        "Tempo limite aguardando execução no Databricks",
                        null
                );
            }

            if (statementId.isBlank()) {
                throw new DatabricksException(
                        "Databricks não retornou o identificador da consulta",
                        null
                );
            }

            Thread.sleep(INTERVALO_MS);
            tentativas++;

            String resposta = restClient.get()
                    .uri(
                            "/api/2.0/sql/statements/{statement_id}",
                            statementId
                    )
                    .retrieve()
                    .body(String.class);

            json = lerJson(resposta);

            estado = json.path("status")
                    .path("state")
                    .asText();
        }

        if ("FAILED".equals(estado)
                || "CANCELED".equals(estado)
                || "CLOSED".equals(estado)) {

            String codigo = json.path("status")
                    .path("error")
                    .path("error_code")
                    .asText();

            String mensagem = json.path("status")
                    .path("error")
                    .path("message")
                    .asText("Motivo não informado pelo Databricks");

            throw new DatabricksException(
                    "Execução no Databricks terminou com estado "
                            + estado
                            + (codigo.isBlank() ? "" : " [" + codigo + "]")
                            + ": " + mensagem,
                    null
            );
        }

        if (!"SUCCEEDED".equals(estado)) {
            throw new DatabricksException(
                    "Estado inesperado do Databricks: " + estado,
                    null
            );
        }

        return json;
    }
}
