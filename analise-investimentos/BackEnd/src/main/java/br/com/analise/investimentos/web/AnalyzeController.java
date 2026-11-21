package br.com.analise.investimentos.web;

import br.com.analise.investimentos.rules.Resultado;
import br.com.analise.investimentos.service.AnalyzeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

@RestController
@RequestMapping("/api/analyze")
@CrossOrigin(origins = "*")
public class AnalyzeController {

    private final AnalyzeService analyzeService;

    public AnalyzeController(AnalyzeService analyzeService) {
        this.analyzeService = analyzeService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private br.com.analise.investimentos.service.HistoricoService historicoService;

    @PostMapping("/detalhado")
    public br.com.analise.investimentos.service.AnaliseDetalhada analisarDetalhado(
            @RequestParam("texto") String texto) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("O campo texto é obrigatório.");
        return analyzeService.analisarDetalhado(texto);
    }

    @GetMapping("/historico")
    public java.util.List<br.com.analise.investimentos.service.AnaliseDetalhada> historico() {
        return historicoService.listar();
    }

    @GetMapping("/historico/{id}")
    public org.springframework.http.ResponseEntity<br.com.analise.investimentos.service.AnaliseDetalhada> buscar(@PathVariable String id) {
        return historicoService.buscar(id).map(org.springframework.http.ResponseEntity::ok)
                .orElseGet(() -> org.springframework.http.ResponseEntity.notFound().build());
    }

    @PostMapping
    public Resultado analisar(@RequestParam("texto") String texto) {

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O campo texto é obrigatório.");
        }

        return analyzeService.analisar(texto);
    }
}