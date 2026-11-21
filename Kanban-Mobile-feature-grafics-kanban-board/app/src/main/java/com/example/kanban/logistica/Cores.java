package com.example.kanban.logistica; // ajuste ao package do app

import android.content.Context;
import android.graphics.Color;
import com.example.kanban.core.Constantes;

public final class Cores {
    private Cores() {}

    public static final int VERDE = Color.parseColor("#2E9E4F");
    public static final int AMARELO = Color.parseColor("#F5B335");
    public static final int VERMELHO = Color.parseColor("#D64545");

    /** Converte "verde" | "amarelo" | "vermelho" (Constantes.corPorEspera) em cor. */
    public static int porNome(String cor) {
        if (Constantes.COR_VERDE.equals(cor)) return VERDE;
        if (Constantes.COR_AMARELO.equals(cor)) return AMARELO;
        return VERMELHO;
    }

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}