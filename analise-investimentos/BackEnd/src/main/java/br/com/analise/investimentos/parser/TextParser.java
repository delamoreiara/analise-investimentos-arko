
package br.com.analise.investimentos.parser;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextParser {

    public Map<String, Object> parse(String texto) {

        Map<String, Object> dados = new LinkedHashMap<>();

        String normalizado = java.text.Normalizer.normalize(texto.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");

        // Classe do investimento
        if (normalizado.contains("cdb")
                || normalizado.contains("rdb")
                || normalizado.contains("lci")
                || normalizado.contains("lca")) {
            dados.put("classe", "RENDA_FIXA_BANCARIA");
        }
        if (normalizado.contains("lci")) dados.put("tipo_ativo", "LCI");
        else if (normalizado.contains("lca")) dados.put("tipo_ativo", "LCA");
        else if (normalizado.contains("cdb")) dados.put("tipo_ativo", "CDB");
        else if (normalizado.contains("rdb")) dados.put("tipo_ativo", "RDB");

        // Prioridade: FII antes de fundo, Tesouro antes de ação.
        if (normalizado.matches("(?s).*(?:\\bfii\\b|\\bfiis\\b|fundo imobiliario|fundos imobiliarios).*")) {
            dados.put("classe", "FII");
        } else if (normalizado.matches("(?s).*(?:\\btesouro\\b|\\bntn-b\\b|\\blft\\b|\\bltn\\b).*")) {
            dados.put("classe", "TESOURO_DIRETO");
        } else if (normalizado.matches("(?s).*(?:\\bfundo\\b|\\bfundos\\b|\\bfia\\b|\\bfic\\b|\\bficfim\\b|multimercado).*")) {
            dados.put("classe", "FUNDO_INVESTIMENTO");
        } else if (normalizado.matches("(?s).*(?:\\bacao\\b|\\bacoes\\b|\\betf\\b).*")) {
            dados.put("classe", "ACAO");
        } else if (Pattern.compile("\\b[A-Z]{4}\\d{1,2}\\b").matcher(texto).find()) {
            dados.put("classe", "ACAO");
        }

        Matcher ticker = Pattern.compile("\\b([A-Z]{4}\\d{1,2})\\b").matcher(texto);
        if (ticker.find()) {
            dados.put("ticker", ticker.group(1));
            if (ticker.group(1).endsWith("11") && !dados.containsKey("classe")) {
                // Código 11 é a forma mais comum de FII na B3.
                // O Databricks pode confirmar ou corrigir a classe pela tabela gold.
                dados.put("classe", "FII");
            }
        }

        // Identificação de subtipo do Tesouro
        if ("TESOURO_DIRETO".equals(dados.get("classe"))) {
            if (normalizado.contains("selic") || normalizado.contains("lft")) dados.put("tipo_ativo", "TESOURO_SELIC");
            else if (normalizado.contains("ipca") || normalizado.contains("ntn-b")) dados.put("tipo_ativo", "TESOURO_IPCA");
            else if (normalizado.contains("prefixado") || normalizado.contains("ltn")) dados.put("tipo_ativo", "TESOURO_PREFIXADO");
        }

        // Percentual do CDI
        Matcher taxa = Pattern.compile(
                "(\\d+(?:[.,]\\d+)?)\\s*%\\s*(?:do\\s*)?cdi",
                Pattern.CASE_INSENSITIVE
        ).matcher(texto);

        if (taxa.find()) {
            dados.put(
                    "percentual_cdi",
                    Double.parseDouble(
                            taxa.group(1).replace(",", ".")
                    )
            );
            dados.put("indexador", "CDI");
        }

        // Indexadores
        if (normalizado.contains("ipca+")
                || normalizado.contains("ipca +")) {
            dados.put("indexador", "IPCA+");
        } else if (normalizado.contains("prefixado")
                || normalizado.contains("pre-fixado")) {
            dados.put("indexador", "PREFIXADO");
        } else if (normalizado.contains("cdi")) {
            dados.putIfAbsent("indexador", "CDI");
        }

        // Liquidez diária
        if (normalizado.contains("liquidez diaria")
                || normalizado.contains("liquidez diaria")) {
            dados.put("liquidez_dias", 0);
        } else {
            Matcher liquidez = Pattern.compile(
                    "d\\s*\\+\\s*(\\d+)",
                    Pattern.CASE_INSENSITIVE
            ).matcher(texto);

            if (liquidez.find()) {
                dados.put(
                        "liquidez_dias",
                        Integer.parseInt(liquidez.group(1))
                );
            }
        }

        Matcher gestora = Pattern.compile(
                "(?:gestora|gestado por)\\s*[:=-]?\\s*([A-Za-zÀ-ÿ0-9][A-Za-zÀ-ÿ0-9 .&-]+)",
                Pattern.CASE_INSENSITIVE
        ).matcher(texto);
        if (gestora.find()) dados.put("gestora", gestora.group(1).trim());

        Matcher classeFundo = Pattern.compile(
                "(multimercado|renda fixa|acoes|ações|imobiliario|imobiliário|mix)",
                Pattern.CASE_INSENSITIVE
        ).matcher(normalizado);
        if (classeFundo.find()) dados.put("classe_fundo", classeFundo.group(1).trim());

        // Vencimento
        Matcher vencimento = Pattern.compile(
                "\\b(20\\d{2})\\b"
        ).matcher(texto);

        if (vencimento.find()) {
            dados.put(
                    "vencimento_ano",
                    Integer.parseInt(vencimento.group(1))
            );
        }

        // Identificação inicial do emissor
        Matcher emissor = Pattern.compile(
                "(?:CDB|RDB|LCI|LCA)\\s+(.+?)(?=\\s+\\d+(?:[.,]\\d+)?\\s*%|\\s+liquidez|$)",
                Pattern.CASE_INSENSITIVE
        ).matcher(texto);

        if (emissor.find()) {
            dados.put("emissor_nome", emissor.group(1).trim());
        }

        return dados;
    }
}
