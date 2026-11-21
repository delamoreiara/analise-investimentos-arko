
package br.com.analise.investimentos.data;

import br.com.analise.investimentos.DatabricksClient;
import br.com.analise.investimentos.EmissorDTO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class EmissorRepository {

    private final DatabricksClient databricksClient;

    public EmissorRepository(DatabricksClient databricksClient) {
        this.databricksClient = databricksClient;
    }

    public Optional<EmissorDTO> buscarPorNome(String nome) {

        String sql = """
                SELECT nome, porte
                FROM arko.ref.emissor
                WHERE LOWER(TRIM(nome)) = LOWER(TRIM(:nome))
                LIMIT 1
                """;

        List<Map<String, String>> resultado =
                databricksClient.executarConsultaGenerica(
                        sql,
                        Map.of("nome", nome)
                );

        if (resultado.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> emissor = resultado.get(0);

        return Optional.of(new EmissorDTO(
                emissor.get("nome"),
                emissor.get("porte")
        ));
    }
}
