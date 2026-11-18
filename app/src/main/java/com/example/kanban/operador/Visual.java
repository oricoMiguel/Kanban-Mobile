package com.example.kanban.operador;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kanban.core.Constantes;
import com.example.kanban.logistica.Cores;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** Cores, rótulos e formatações compartilhadas pelas telas do operador. */
public final class Visual {
    private Visual() {}

    /** Status que só existe no histórico (coleção "atendimentos"). */
    public static final String STATUS_CONCLUIDO = "concluido";

    /* ---------- urgência (mesmas cores do CartaoQuadroAdapter) ---------- */

    public static int corBarra(String urgencia) {
        if (Constantes.URGENCIA_ALTA.equals(urgencia)) return Color.parseColor("#E53935");
        if (Constantes.URGENCIA_MEDIA.equals(urgencia)) return Color.parseColor("#FFB74D");
        return Color.parseColor("#43A047");
    }

    public static int fundoUrgencia(String urgencia) {
        if (Constantes.URGENCIA_ALTA.equals(urgencia)) return Color.parseColor("#FDE3E3");
        if (Constantes.URGENCIA_MEDIA.equals(urgencia)) return Color.parseColor("#FFE8C2");
        return Color.parseColor("#DDF3E2");
    }

    public static int textoUrgencia(String urgencia) {
        if (Constantes.URGENCIA_ALTA.equals(urgencia)) return Color.parseColor("#C62828");
        if (Constantes.URGENCIA_MEDIA.equals(urgencia)) return Color.parseColor("#B26A00");
        return Color.parseColor("#1B7A38");
    }

    public static String rotuloUrgencia(String urgencia) {
        if (Constantes.URGENCIA_ALTA.equals(urgencia)) return "alta";
        if (Constantes.URGENCIA_MEDIA.equals(urgencia)) return "média";
        return "baixa";
    }

    /* ---------- status ---------- */

    public static String rotuloStatus(String status) {
        if (Constantes.STATUS_EM_ROTA.equals(status)) return "Em rota";
        if (Constantes.STATUS_ENTREGUE.equals(status)) return "Entregue";
        if (STATUS_CONCLUIDO.equals(status)) return "Concluído";
        return "Aberto";
    }

    /** Pinta o selo (TextView com @drawable/bg_chip) conforme o status. */
    public static void chipStatus(TextView tv, String status) {
        int fundo, texto;
        if (Constantes.STATUS_EM_ROTA.equals(status)) {
            fundo = Color.parseColor("#CFE0FB"); texto = Color.parseColor("#1E4E9A");
        } else if (Constantes.STATUS_ENTREGUE.equals(status)) {
            fundo = Color.parseColor("#E3DCFB"); texto = Color.parseColor("#4A2BA8");
        } else if (STATUS_CONCLUIDO.equals(status)) {
            fundo = Color.parseColor("#CDEBD2"); texto = Color.parseColor("#1B7A38");
        } else {
            fundo = Color.parseColor("#F8DFA8"); texto = Color.parseColor("#7A4B00");
        }
        chip(tv, rotuloStatus(status), fundo, texto);
    }

    public static void chip(TextView tv, String texto, int fundo, int corTexto) {
        tv.setText(texto);
        tv.setTextColor(corTexto);
        tv.setBackgroundTintList(ColorStateList.valueOf(fundo));
    }

    /* ---------- barra lateral do cartão ---------- */

    public static void aplicarBarra(View barra, String urgencia) {
        aplicarBarraCor(barra, corBarra(urgencia));
    }

    public static void aplicarBarraCor(View barra, int cor) {
        float r = Cores.dp(barra.getContext(), 12);
        GradientDrawable g = new GradientDrawable();
        g.setColor(cor);
        g.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, r, r}); // arredondada só à esquerda
        barra.setBackground(g);
    }

    /* ---------- tempo ---------- */

    /** "HH:mm" ou "--:--" se ainda não houver horário. */
    public static String hora(Timestamp ts) {
        if (ts == null) return "--:--";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts.toDate());
    }

    /** 12 -> "12 min"; 75 -> "1 h 15 min". */
    public static String duracao(long minutos) {
        if (minutos < 60) return minutos + " min";
        return (minutos / 60) + " h " + String.format(Locale.getDefault(), "%02d", minutos % 60) + " min";
    }

    /** Para ordenar: timestamp ainda nulo (gravação pendente) vai para o fim. */
    public static long millis(Timestamp ts) {
        return ts == null ? Long.MAX_VALUE : ts.toDate().getTime();
    }

    /* ---------- util de tela ---------- */

    /**
     * Padrão do projeto para a barra de status/gestos. Aqui o topo e as laterais vão na raiz
     * e a margem de baixo vai na barra inferior (para ela ficar colada na borda).
     * Passe nav = null se a tela não tiver barra inferior.
     */
    public static void ajustarInsets(View raiz, View nav) {
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (nav != null) {
                v.setPadding(b.left, b.top, b.right, 0);
                nav.setPadding(0, 0, 0, b.bottom);
            } else {
                int base = Cores.dp(v.getContext(), 16);
                v.setPadding(base + b.left, base + b.top, base + b.right, base + b.bottom);
            }
            return insets;
        });
    }

    /** Loga (com o link de criar índice do Firestore, se for o caso) e avisa o usuário. */
    public static void erro(Context c, String onde, Exception e) {
        Log.e("Kanban", onde, e);
        Toast.makeText(c, onde + ": " + (e == null ? "erro" : e.getMessage()), Toast.LENGTH_LONG).show();
    }
}
