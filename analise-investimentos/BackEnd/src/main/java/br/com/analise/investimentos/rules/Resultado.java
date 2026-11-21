
package br.com.analise.investimentos.rules;

import java.util.List;

public record Resultado(
        String decisao,
        String objetivo,
        List<String> regrasAplicadas,
        List<String> justificativas,
        List<String> regrasNaoAvaliadas
) {
}
