
package br.com.analise.investimentos.rules;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuleEngine {

    public Resultado avaliar(
            List<Regra> regras,
            Map<String, ?> dados) {

        return avaliar(regras, dados, Map.of());
    }

    public Resultado avaliar(
            List<Regra> regras,
            Map<String, ?> dados,
            Map<String, String> parametros) {

        List<String> aplicadas = new ArrayList<>();
        List<String> justificativas = new ArrayList<>();
        List<String> naoAvaliadas = new ArrayList<>();

        String decisao = "INDETERMINADO";
        String objetivo = null;

        List<Regra> ordenadas = regras.stream()
                .sorted(Comparator.comparingInt(Regra::prioridade))
                .toList();

        for (Regra regra : ordenadas) {

            // Uma regra só se aplica à classe para a qual foi cadastrada.
            // Evita aplicar regras bancárias a Tesouro, fundos, ações e FIIs.
            Object classeAtivo = dados.get("classe");
            if (regra.classe() != null && !regra.classe().isBlank()
                    && (classeAtivo != null && !regra.classe().equalsIgnoreCase(classeAtivo.toString()))) {
                continue;
            }


            Boolean resultado = avaliarRegra(
                    regra, dados, parametros
            );

            if (resultado == null) {
                naoAvaliadas.add(regra.regraId());
                continue;
            }

            if (!resultado) {
                continue;
            }

            aplicadas.add(regra.regraId());
            justificativas.add(regra.mensagem());

            if (objetivo == null && regra.objetivo() != null) {
                objetivo = regra.objetivo();
            }

            decisao = definirPrecedencia(
                    decisao, regra.efeito()
            );
        }

        return new Resultado(
                decisao,
                objetivo,
                aplicadas,
                justificativas,
                naoAvaliadas
        );
    }

    private Boolean avaliarRegra(
            Regra regra,
            Map<String, ?> dados,
            Map<String, String> parametros) {

        if (regra.condicoes() == null
                || regra.condicoes().isEmpty()) {
            return true;
        }

        Map<Integer, List<Condicao>> grupos = new HashMap<>();

        for (Condicao condicao : regra.condicoes()) {
            grupos.computeIfAbsent(
                    condicao.grupo(),
                    grupo -> new ArrayList<>()
            ).add(condicao);
        }

        boolean existeGrupoIndeterminado = false;

        for (List<Condicao> grupo : grupos.values()) {

            boolean grupoFalso = false;
            boolean grupoIndeterminado = false;

            for (Condicao condicao : grupo) {

                Boolean resultado = avaliarCondicao(
                        condicao, dados, parametros
                );

                if (resultado == null) {
                    grupoIndeterminado = true;
                } else if (!resultado) {
                    grupoFalso = true;
                    break;
                }
            }

            if (!grupoFalso && !grupoIndeterminado) {
                return true;
            }

            if (!grupoFalso) {
                existeGrupoIndeterminado = true;
            }
        }

        return existeGrupoIndeterminado ? null : false;
    }

    private Boolean avaliarCondicao(
            Condicao condicao,
            Map<String, ?> dados,
            Map<String, String> parametros) {

        Object valorAtual = dados.get(condicao.campo());

        if (valorAtual == null) {
            return null;
        }

        String esperado = condicao.valor();

        if (esperado != null && esperado.startsWith("$param:")) {
            String chave = esperado.substring("$param:".length());
            esperado = parametros.get(chave);

            if (esperado == null) {
                return null;
            }
        }

        String atual = valorAtual.toString();

        String operador = condicao.operador()
                .trim()
                .toLowerCase();

        return switch (operador) {
            case "=", "==" ->
                    comparar(atual, esperado) == 0;
            case "!=" ->
                    comparar(atual, esperado) != 0;
            case ">" ->
                    compararNumerico(atual, esperado, ">");
            case ">=" ->
                    compararNumerico(atual, esperado, ">=");
            case "<" ->
                    compararNumerico(atual, esperado, "<");
            case "<=" ->
                    compararNumerico(atual, esperado, "<=");
            case "contains" ->
                    atual.toLowerCase()
                            .contains(esperado.toLowerCase());
            default -> null;
        };
    }

    private int comparar(String atual, String esperado) {
        try {
            return new BigDecimal(atual)
                    .compareTo(new BigDecimal(esperado));
        } catch (NumberFormatException e) {
            return atual.compareToIgnoreCase(esperado);
        }
    }

    private Boolean compararNumerico(
            String atual,
            String esperado,
            String operador) {

        try {
            int comparacao = new BigDecimal(atual)
                    .compareTo(new BigDecimal(esperado));

            return switch (operador) {
                case ">" -> comparacao > 0;
                case ">=" -> comparacao >= 0;
                case "<" -> comparacao < 0;
                case "<=" -> comparacao <= 0;
                default -> null;
            };

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String definirPrecedencia(
            String atual,
            String novo) {

        Map<String, Integer> prioridade = Map.of(
                "INDETERMINADO", 0,
                "ELEGIVEL", 1,
                "APROVA", 1,
                "CONDICIONA", 2,
                "REPROVA", 3
        );

        return prioridade.getOrDefault(novo, 0)
                > prioridade.getOrDefault(atual, 0)
                ? novo
                : atual;
    }
}
