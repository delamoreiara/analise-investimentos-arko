package br.com.analise.investimentos.service;

import br.com.analise.investimentos.rules.Resultado;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnaliseDetalhada(String id, Instant criadoEm, String origemExtracao,
        Map<String, Object> dadosExtraidos, List<String> alertas, Resultado resultado,
        List<Map<String, Object>> fundamentos, List<Map<String, Object>> rentabilidade,
        Double dividendYield12m, boolean ativoEncontrado, String mensagem) {
}
