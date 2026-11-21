
package br.com.analise.investimentos.service;

import br.com.analise.investimentos.data.EmissorRepository;
import br.com.analise.investimentos.data.ParametroRepository;
import br.com.analise.investimentos.data.RegraRepository;
import br.com.analise.investimentos.parser.TextParser;
import br.com.analise.investimentos.rules.Resultado;
import br.com.analise.investimentos.rules.RuleEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnalyzeService {

    private final RegraRepository regraRepository;
    private final EmissorRepository emissorRepository;
    private final ParametroRepository parametroRepository;
    private final RuleEngine ruleEngine;
    private final TextParser textParser;
    private ExtracaoService extracaoService;
    private HistoricoService historicoService;

    public AnalyzeService(
            RegraRepository regraRepository,
            EmissorRepository emissorRepository,
            ParametroRepository parametroRepository,
            RuleEngine ruleEngine,
            TextParser textParser) {

        this.regraRepository = regraRepository;
        this.emissorRepository = emissorRepository;
        this.parametroRepository = parametroRepository;
        this.ruleEngine = ruleEngine;
        this.textParser = textParser;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarDetalhamento(ExtracaoService extracaoService, HistoricoService historicoService) {
        this.extracaoService = extracaoService;
        this.historicoService = historicoService;
    }

    public Resultado analisar(String texto) {
        return executar(textParser.parse(texto));
    }

    public AnaliseDetalhada analisarDetalhado(String texto) {
        var extracao = extracaoService.extrair(texto);
        Map<String, Object> dados = new java.util.LinkedHashMap<>(extracao.dados());
        Resultado resultado;
        boolean ativoEncontrado = false;
        List<Map<String, Object>> fundamentos = new ArrayList<>();
        List<Map<String, Object>> rentabilidade = new ArrayList<>();
        Double dividendYield12m = null;
        String mensagem;
        try {
            ativoEncontrado = false;
            resultado = executar(dados);
            mensagem = ativoEncontrado
                    ? "Análise concluída com dados consultados no Databricks."
                    : "Não encontrei dados cadastrados para este ativo; a análise usa somente as características identificadas no texto.";
        } catch (br.com.analise.investimentos.DatabricksException e) {
            resultado = new Resultado(
                    "INDETERMINADO",
                    null,
                    List.of(),
                    List.of("Não foi possível consultar os dados no Databricks neste momento."),
                    List.of()
            );
            mensagem = "Não foi possível consultar o Databricks. Nenhum veredito foi inferido.";
        }
        AnaliseDetalhada analise = new AnaliseDetalhada(
                java.util.UUID.randomUUID().toString(),
                java.time.Instant.now(),
                extracao.origem(),
                dados,
                extracao.alertas(),
                resultado,
                fundamentos,
                rentabilidade,
                dividendYield12m,
                ativoEncontrado,
                mensagem
        );
        historicoService.registrar(analise);
        return analise;
    }

    private Resultado executar(Map<String, Object> dados) {

        Object nome = dados.get("emissor_nome");

        if (nome instanceof String nomeEmissor
                && !nomeEmissor.isBlank()) {

            emissorRepository.buscarPorNome(nomeEmissor)
                    .ifPresent(emissor -> {
                        dados.put("emissor_nome", emissor.nome());
                        dados.put("emissor_porte", emissor.porte());
                    });
        }

        return ruleEngine.avaliar(
                regraRepository.buscarRegrasAtivas(),
                dados,
                parametroRepository.buscarParametros()
        );
    }
}
