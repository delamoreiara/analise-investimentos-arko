package br.com.analise.investimentos.rules;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RuleEngineClasseTest {
    @Test void naoAplicaRegraBancariaAoTesouro() {
        Regra regra = new Regra("BANCO", "4.2", "Reserva", "RENDA_FIXA_BANCARIA",
                "ELEGIVEL", "RESERVA", 1, "Banco grande", null, true, 1,
                List.of(new Condicao("BANCO", 1, "liquidez_dias", "<=", "0")));
        Resultado resultado = new RuleEngine().avaliar(List.of(regra),
                Map.of("classe", "TESOURO_DIRETO", "liquidez_dias", 0));
        assertEquals("INDETERMINADO", resultado.decisao());
        assertTrue(resultado.regrasAplicadas().isEmpty());
    }
}
