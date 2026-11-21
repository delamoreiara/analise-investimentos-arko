package br.com.analise.investimentos.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class HistoricoService {
    private final Map<String, AnaliseDetalhada> historico = new LinkedHashMap<>();
    private static final int LIMITE = 500;
    public synchronized void registrar(AnaliseDetalhada analise) {
        historico.put(analise.id(), analise);
        if (historico.size() > LIMITE) historico.remove(historico.keySet().iterator().next());
    }
    public synchronized Optional<AnaliseDetalhada> buscar(String id) {
        return Optional.ofNullable(historico.get(id));
    }
    public synchronized List<AnaliseDetalhada> listar() {
        return List.copyOf(historico.values()).reversed();
    }
}
