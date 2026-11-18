package com.example.kanban.core;

import com.google.firebase.Timestamp;

public final class Constantes {
    private Constantes() {}

    // Coleções
    public static final String COL_POSTOS = "postos";
    public static final String COL_INSUMOS = "insumos";
    public static final String COL_CARTOES = "cartoes";
    public static final String COL_ATENDIMENTOS = "atendimentos";

    // Status do cartão
    public static final String STATUS_ABERTO = "aberto";
    public static final String STATUS_EM_ROTA = "em_rota";
    public static final String STATUS_ENTREGUE = "entregue";

    // Urgência
    public static final String URGENCIA_BAIXA = "baixa";
    public static final String URGENCIA_MEDIA = "media";
    public static final String URGENCIA_ALTA = "alta";

    // Perfil (tela 5)
    public static final String PERFIL_OPERADOR = "operador";
    public static final String PERFIL_LOGISTICA = "logistica";

    // Cores por tempo de espera
    public static final String COR_VERDE = "verde";
    public static final String COR_AMARELO = "amarelo";
    public static final String COR_VERMELHO = "vermelho";

    // C3: o id do cartão impede duplicado
    public static String idCartao(String postoId, String insumoId) {
        return postoId + "_" + insumoId;
    }

    // C6: minutos desde um Timestamp (retorna 0 se ainda for null)
    public static long minutosDesde(Timestamp ts) {
        if (ts == null) return 0;
        long diff = System.currentTimeMillis() - ts.toDate().getTime();
        return Math.max(0, diff / 60000);
    }

    // até 7 min verde, 7 a 15 amarelo, acima de 15 vermelho
    public static String corPorEspera(long minutos) {
        if (minutos <= 7) return COR_VERDE;
        if (minutos <= 15) return COR_AMARELO;
        return COR_VERMELHO;
    }
}