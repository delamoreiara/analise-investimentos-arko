
package br.com.analise.investimentos.rules;

public record Condicao(
        String regraId,
        int grupo,
        String campo,
        String operador,
        String valor
) {
}
