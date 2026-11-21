
package br.com.analise.investimentos;

import br.com.analise.investimentos.data.EmissorRepository;
import br.com.analise.investimentos.data.RegraRepository;
import br.com.analise.investimentos.parser.TextParser;
import br.com.analise.investimentos.rules.Regra;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DatabricksController {

    private final RegraRepository regraRepository;
    private final DatabricksClient databricksClient;
    private final TextParser textParser;
    private final EmissorRepository emissorRepository;

    public DatabricksController(
            DatabricksClient databricksClient,
            RegraRepository regraRepository,
            TextParser textParser,
            EmissorRepository emissorRepository) {

        this.databricksClient = databricksClient;
        this.regraRepository = regraRepository;
        this.textParser = textParser;
        this.emissorRepository = emissorRepository;
    }

    @GetMapping("/api/databricks/test")
    public List<EmissorDTO> testarConexao() {

        String sql = """
                SELECT nome, porte
                FROM arko.ref.emissor
                LIMIT 3
                """;

        return databricksClient.executarConsulta(sql);
    }

    @GetMapping("/api/regras/test")
    public List<Regra> testarRegras() {
        return regraRepository.buscarRegrasAtivas();
    }

    @GetMapping("/api/databricks/test-parametro")
    public List<EmissorDTO> testarParametro() {

        String sql = """
                SELECT nome, porte
                FROM arko.ref.emissor
                WHERE nome = :nome
                """;

        Map<String, String> parametros = Map.of(
                "nome", "Bradesco"
        );

        return databricksClient.executarConsulta(sql, parametros);
    }

    @GetMapping("/api/emissor/test")
    public Map<String, Object> testarEmissor(
            @RequestParam String nome) {

        return emissorRepository.buscarPorNome(nome)
                .map(emissor -> {
                    Map<String, Object> resposta = new HashMap<>();
                    resposta.put("nome", emissor.nome());
                    resposta.put("porte", emissor.porte());
                    return resposta;
                })
                .orElseGet(() -> Map.of(
                        "mensagem", "Emissor não encontrado"
                ));
    }

    @PostMapping("/parse")
    public Map<String, Object> parse(
            @RequestBody Map<String, String> body) {

        return textParser.parse(body.get("texto"));
    }
}
