package br.com.analise.investimentos.service;

import br.com.analise.investimentos.parser.TextParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;

@Service
public class ExtracaoService {
    private static final Logger log = LoggerFactory.getLogger(ExtracaoService.class);
    private static final Set<String> CAMPOS = Set.of("classe", "tipo_ativo", "ticker", "emissor_nome", "emissor_porte", "percentual_cdi", "indexador", "liquidez_dias", "vencimento_ano");
    private final TextParser parser;
    private final ObjectMapper mapper;
    private final boolean llmAtiva;
    private final String endpoint;
    private final String apiKey;
    private final String modelo;
    private final RestClient http = RestClient.create();

    public ExtracaoService(TextParser parser, ObjectMapper mapper,
            @Value("${analise.llm.enabled:false}") boolean llmAtiva,
            @Value("${analise.llm.endpoint:}") String endpoint,
            @Value("${analise.llm.api-key:}") String apiKey,
            @Value("${analise.llm.model:}") String modelo) {
        this.parser = parser;
        this.mapper = mapper;
        this.llmAtiva = llmAtiva;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    public Extracao extrair(String texto) {
        Map<String,Object> regex = new LinkedHashMap<>(parser.parse(texto));
        List<String> alertas = new ArrayList<>();
        String origem = "REGEX";
        if (llmAtiva) {
            if (endpoint.isBlank() || apiKey.isBlank() || modelo.isBlank()) {
                alertas.add("LLM habilitada sem configuração completa; utilizado parser local.");
            } else {
                try {
                    Map<String,Object> resposta = consultarLlm(texto);
                    for (var e : resposta.entrySet()) {
                        if (CAMPOS.contains(e.getKey()) && e.getValue() != null && !regex.containsKey(e.getKey())) {
                            regex.put(e.getKey(), e.getValue());
                        }
                    }
                    origem = "LLM_COM_FALLBACK_REGEX";
                } catch (Exception ex) {
                    log.warn("Falha na extração LLM; usando parser local: {}", ex.getClass().getSimpleName());
                    alertas.add("LLM indisponível; utilizado parser local.");
                }
            }
        }
        Object classe = regex.get("classe");
        if (classe == null || "INDETERMINADA".equals(classe)) alertas.add("Classe não identificada com segurança.");
        if ("RENDA_FIXA_BANCARIA".equals(classe) && !regex.containsKey("emissor_nome")) alertas.add("Emissor não identificado.");
        if ("TESOURO_DIRETO".equals(classe) && !regex.containsKey("tipo_ativo")) alertas.add("Subtipo do Tesouro não identificado.");
        if (regex.containsKey("liquidez_dias") && !(regex.get("liquidez_dias") instanceof Number)) {
            regex.remove("liquidez_dias"); alertas.add("Liquidez inválida descartada.");
        }
        return new Extracao(Map.copyOf(regex), List.copyOf(alertas), origem);
    }

    private Map<String,Object> consultarLlm(String texto) throws Exception {
        String prompt = "Extraia somente JSON plano com campos: classe (RENDA_FIXA_BANCARIA, TESOURO_DIRETO, FUNDO_INVESTIMENTO, ACAO, FII), tipo_ativo, ticker, emissor_nome, percentual_cdi, indexador, liquidez_dias, vencimento_ano. Não invente valores. Texto: " + texto;
        Map<String,Object> body = Map.of("model", modelo, "temperature", 0,
                "messages", List.of(Map.of("role", "user", "content", prompt)));
        String raw = http.post().uri(endpoint).header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
        var root = mapper.readTree(raw);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        content = content.replaceAll("(?s)^\\s*```(?:json)?\\s*|\\s*```\\s*$", "").trim();
        return mapper.readValue(content, new TypeReference<Map<String,Object>>(){});
    }
    public record Extracao(Map<String,Object> dados, List<String> alertas, String origem) { }
}
