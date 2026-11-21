
package br.com.analise.investimentos.service;

import br.com.analise.investimentos.EmissorDTO;
import br.com.analise.investimentos.data.EmissorRepository;
import br.com.analise.investimentos.data.ParametroRepository;
import br.com.analise.investimentos.data.RegraRepository;
import br.com.analise.investimentos.parser.TextParser;
import br.com.analise.investimentos.rules.Regra;
import br.com.analise.investimentos.rules.Resultado;
import br.com.analise.investimentos.rules.RuleEngine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzeServiceTest {

    @Mock
    private RegraRepository regraRepository;

    @Mock
    private EmissorRepository emissorRepository;

    @Mock
    private ParametroRepository parametroRepository;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private TextParser textParser;

    @InjectMocks
    private AnalyzeService analyzeService;

    private List<Regra> regras;
    private Map<String, String> parametros;
    private Resultado resultadoEsperado;

    @BeforeEach
    void setUp() {
        regras = new ArrayList<>();
        parametros = Map.of("liquidez_maxima", "0");

        resultadoEsperado = new Resultado(
                "ELEGIVEL",
                "RESERVA",
                List.of("R_4_2_RESERVA"),
                List.of("Liquidez diária e instituição grande."),
                List.of()
        );

        when(regraRepository.buscarRegrasAtivas())
                .thenReturn(regras);

        when(parametroRepository.buscarParametros())
                .thenReturn(parametros);

        when(ruleEngine.avaliar(anyList(), anyMap(), anyMap()))
                .thenReturn(resultadoEsperado);
    }

    @Test
    void deveAnalisarAtivoComEmissorConhecido() {

        Map<String, Object> dados = new HashMap<>();
        dados.put("emissor_nome", "Bradesco");
        dados.put("liquidez_dias", 0);

        when(textParser.parse(anyString()))
                .thenReturn(dados);

        when(emissorRepository.buscarPorNome("Bradesco"))
                .thenReturn(Optional.of(
                        new EmissorDTO("Bradesco", "GRANDE")
                ));

        Resultado resultado = analyzeService.analisar(
                "CDB Bradesco 100% CDI liquidez diaria"
        );

        assertEquals("ELEGIVEL", resultado.decisao());

        assertEquals("Bradesco", dados.get("emissor_nome"));
        assertEquals("GRANDE", dados.get("emissor_porte"));

        verify(emissorRepository).buscarPorNome("Bradesco");
        verify(ruleEngine).avaliar(
                eq(regras),
                anyMap(),
                eq(parametros)
        );
    }

    @Test
    void deveManterEmissorDesconhecidoSemPorte() {

        Map<String, Object> dados = new HashMap<>();
        dados.put("emissor_nome", "Banco Desconhecido");

        when(textParser.parse(anyString()))
                .thenReturn(dados);

        when(emissorRepository.buscarPorNome("Banco Desconhecido"))
                .thenReturn(Optional.empty());

        Resultado resultado = analyzeService.analisar(
                "CDB Banco Desconhecido"
        );

        assertEquals("ELEGIVEL", resultado.decisao());
        assertFalse(dados.containsKey("emissor_porte"));

        verify(emissorRepository)
                .buscarPorNome("Banco Desconhecido");

        verify(ruleEngine).avaliar(
                eq(regras),
                anyMap(),
                eq(parametros)
        );
    }

    @Test
    void naoDeveConsultarEmissorQuandoNomeNaoForIdentificado() {

        Map<String, Object> dados = new HashMap<>();
        dados.put("classe", "RENDA_FIXA_BANCARIA");

        when(textParser.parse(anyString()))
                .thenReturn(dados);

        Resultado resultado = analyzeService.analisar(
                "Investimento sem emissor identificado"
        );

        assertEquals("ELEGIVEL", resultado.decisao());

        verifyNoInteractions(emissorRepository);

        verify(ruleEngine).avaliar(
                eq(regras),
                anyMap(),
                eq(parametros)
        );
    }
}
