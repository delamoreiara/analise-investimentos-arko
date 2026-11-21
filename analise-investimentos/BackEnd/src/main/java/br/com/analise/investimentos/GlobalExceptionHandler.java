
package br.com.analise.investimentos;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DatabricksException.class)
    public ResponseEntity<Map<String, Object>> tratarDatabricks(
            DatabricksException e,
            HttpServletRequest request) {

        log.error("Falha na integração com Databricks", e);

        return resposta(
                HttpStatus.BAD_GATEWAY,
                "INTEGRATION_ERROR",
                "Não foi possível concluir a consulta ao serviço de dados.",
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarJsonInvalido(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {

        return resposta(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "O corpo da requisição está ausente ou possui formato inválido.",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> tratarArgumentoInvalido(
            IllegalArgumentException e,
            HttpServletRequest request) {

        return resposta(
                HttpStatus.BAD_REQUEST,
                "INVALID_ARGUMENT",
                "Os dados informados são inválidos.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroGeral(
            Exception e,
            HttpServletRequest request) {

        log.error("Erro interno na aplicação", e);

        return resposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Ocorreu um erro interno ao processar a solicitação.",
                request
        );
    }

    private ResponseEntity<Map<String, Object>> resposta(
            HttpStatus status,
            String codigo,
            String mensagem,
            HttpServletRequest request) {

        Map<String, Object> corpo = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "erro", codigo,
                "mensagem", mensagem,
                "path", request.getRequestURI()
        );

        return ResponseEntity.status(status).body(corpo);
    }
}
