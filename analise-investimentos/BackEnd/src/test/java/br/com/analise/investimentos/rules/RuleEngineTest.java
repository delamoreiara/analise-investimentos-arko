
package br.com.analise.investimentos.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    @Test
    void deveRetornarElegivelQuandoCondicaoForVerdadeira() {

        Regra regra = criarRegra(
                "R_4_2_RESERVA",
                "ELEGIVEL",
                "RESERVA",
                10,
                List.of(new Condicao(
                        "R_4_2_RESERVA", 1,
                        "emissor_porte", "=", "GRANDE"
                ))
        );

        Resultado resultado = ruleEngine.avaliar(
                List.of(regra),
                Map.of("emissor_porte", "GRANDE")
        );

        assertEquals("ELEGIVEL", resultado.decisao());
        assertEquals("RESERVA", resultado.objetivo());
        assertEquals(List.of("R_4_2_RESERVA"), resultado.regrasAplicadas());
    }

    @Test
    void deveRetornarReprovaQuandoRegraDeReprovacaoForAtendida() {

        Regra regra = criarRegra(
                "R_4_3_FGC",
                "REPROVA",
                null,
                10,
                List.of(new Condicao(
                        "R_4_3_FGC", 1,
                        "emissor_porte", "=", "PEQUENO"
                ))
        );

        Resultado resultado = ruleEngine.avaliar(
                List.of(regra),
                Map.of("emissor_porte", "PEQUENO")
        );

        assertEquals("REPROVA", resultado.decisao());
        assertEquals(List.of("R_4_3_FGC"), resultado.regrasAplicadas());
    }

    @Test
    void deveRetornarIndeterminadoQuandoCampoEstiverAusente() {

        Regra regra = criarRegra(
                "R_4_2_RESERVA",
                "ELEGIVEL",
                "RESERVA",
                10,
                List.of(new Condicao(
                        "R_4_2_RESERVA", 1,
                        "emissor_porte", "=", "GRANDE"
                ))
        );

        Resultado resultado = ruleEngine.avaliar(
                List.of(regra),
                Map.of()
        );

        assertEquals("INDETERMINADO", resultado.decisao());
        assertTrue(resultado.regrasAplicadas().isEmpty());
        assertEquals(
                List.of("R_4_2_RESERVA"),
                resultado.regrasNaoAvaliadas()
        );
    }

    @Test
    void naoDeveAplicarRegraQuandoCondicaoForFalsa() {

        Regra regra = criarRegra(
                "R_4_2_RESERVA",
                "ELEGIVEL",
                "RESERVA",
                10,
                List.of(new Condicao(
                        "R_4_2_RESERVA", 1,
                        "emissor_porte", "=", "GRANDE"
                ))
        );

        Resultado resultado = ruleEngine.avaliar(
                List.of(regra),
                Map.of("emissor_porte", "PEQUENO")
        );

        assertEquals("INDETERMINADO", resultado.decisao());
        assertTrue(resultado.regrasAplicadas().isEmpty());
        assertTrue(resultado.regrasNaoAvaliadas().isEmpty());
    }

    @Test
    void deveResolverParametroConfiguravel() {

        Regra regra = criarRegra(
                "R_4_2_RESERVA",
                "ELEGIVEL",
                "RESERVA",
                10,
                List.of(new Condicao(
                        "R_4_2_RESERVA", 1,
                        "liquidez_dias", "<=",
                        "$param:liquidez_maxima"
                ))
        );

        Resultado resultado = ruleEngine.avaliar(
                List.of(regra),
                Map.of("liquidez_dias", 0),
                Map.of("liquidez_maxima", "0")
        );

        assertEquals("ELEGIVEL", resultado.decisao());
        assertEquals(List.of("R_4_2_RESERVA"), resultado.regrasAplicadas());
    }

    @Test
    void devePriorizarReprovaSobreElegivel() {

        Regra elegivel = criarRegra(
                "R_ELEGIVEL",
                "ELEGIVEL",
                "RESERVA",
                10,
                List.of(new Condicao(
                        "R_ELEGIVEL", 1,
                        "ativo", "=", "SIM"
                ))
        );

        Regra reprova = criarRegra(
                "R_REPROVA",
                "REPROVA",
                null,
                20,
                List.of(new Condicao(
                        "R_REPROVA", 1,
                        "ativo", "=", "SIM"
                ))
        );

        Resultado resultado = ruleEngine.avaliar(
                List.of(elegivel, reprova),
                Map.of("ativo", "SIM")
        );

        assertEquals("REPROVA", resultado.decisao());
        assertEquals(
                List.of("R_ELEGIVEL", "R_REPROVA"),
                resultado.regrasAplicadas()
        );
        assertEquals("RESERVA", resultado.objetivo());
    }

    private Regra criarRegra(
            String id,
            String efeito,
            String objetivo,
            int prioridade,
            List<Condicao> condicoes) {

        return new Regra(
                id,
                "4.2",
                id,
                "RENDA_FIXA_BANCARIA",
                efeito,
                objetivo,
                prioridade,
                "Mensagem de teste",
                null,
                true,
                1,
                condicoes
        );
    }
}
