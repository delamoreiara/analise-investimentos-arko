
package br.com.analise.investimentos.rules;

import java.util.List;

public record Regra(
        String regraId,
        String secao,
        String nome,
        String classe,
        String efeito,
        String objetivo,
        int prioridade,
        String mensagem,
        String alternativa,
        boolean ativa,
        int versao,
        List<Condicao> condicoes
) {
}
