package com.example.kanban.core;

/** Contrato do QR: id do documento (posto07) ou URI kanban://posto/posto07. */
public final class CodigoPosto {
    private CodigoPosto() {}
    public static String extrair(String conteudo) {
        if (conteudo == null) return null;
        String id = conteudo.trim();
        String prefixo = "kanban://posto/";
        if (id.startsWith(prefixo)) id = id.substring(prefixo.length());
        return id.matches("[A-Za-z0-9_-]{1,128}") ? id : null;
    }
}
