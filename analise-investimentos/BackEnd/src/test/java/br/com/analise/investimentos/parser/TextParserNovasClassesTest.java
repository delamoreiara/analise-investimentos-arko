package br.com.analise.investimentos.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextParserNovasClassesTest {
    private final TextParser parser = new TextParser();

    @Test void reconheceTesouroSelic() {
        var dados = parser.parse("Tesouro Selic 2029");
        assertEquals("TESOURO_DIRETO", dados.get("classe"));
        assertEquals("TESOURO_SELIC", dados.get("tipo_ativo"));
    }
    @Test void reconheceTesouroIpca() {
        assertEquals("TESOURO_IPCA", parser.parse("Tesouro IPCA+ 2035").get("tipo_ativo"));
    }
    @Test void reconheceFundo() {
        assertEquals("FUNDO_INVESTIMENTO", parser.parse("Fundo multimercado XYZ").get("classe"));
    }
    @Test void reconheceFii() {
        assertEquals("FII", parser.parse("FII HGLG11").get("classe"));
    }
    @Test void reconheceAcao() {
        assertEquals("ACAO", parser.parse("Ação PETR4").get("classe"));
    }
    @Test void preservaCdb() {
        assertEquals("RENDA_FIXA_BANCARIA", parser.parse("CDB Bradesco 100% CDI liquidez diaria").get("classe"));
    }
}
