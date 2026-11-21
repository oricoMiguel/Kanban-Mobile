package com.example.kanban;

import com.example.kanban.core.CodigoPosto;
import org.junit.Test;
import static org.junit.Assert.*;

public class CodigoPostoTest {
    @Test public void aceitaIdEUriDocumentados() {
        assertEquals("posto07", CodigoPosto.extrair(" posto07 \n"));
        assertEquals("posto-07_A", CodigoPosto.extrair("kanban://posto/posto-07_A"));
    }
    @Test public void rejeitaConteudoQueNaoIdentificaDocumento() {
        String[] invalidos = {null, "", " ", "postos/posto07", "../posto07", "https://site.com/posto07",
                "kanban://posto/", "kanban://posto/a/b", "kanban://posto/a?perfil=logistica", "posto 07", "a".repeat(129)};
        for (String valor : invalidos) assertNull("Deveria rejeitar: " + valor, CodigoPosto.extrair(valor));
    }
}
