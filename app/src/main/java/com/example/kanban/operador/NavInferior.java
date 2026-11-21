package com.example.kanban.operador;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kanban.R;

/** Barra inferior do operador (view_bottom_nav): Início | Meus cartões | Ler QR. */
public final class NavInferior {
    private NavInferior() {}

    public static final int INICIO = 0, CARTOES = 1, QR = 2;

    private static final int AZUL = Color.parseColor("#2B5BA8");
    private static final int CINZA = Color.parseColor("#555555");

    public static void configurar(Activity a, int selecionado) {
        pintar(a.findViewById(R.id.iconInicio), a.findViewById(R.id.txtNavInicio), selecionado == INICIO);
        pintar(a.findViewById(R.id.iconCartoes), a.findViewById(R.id.txtNavCartoes), selecionado == CARTOES);
        pintar(a.findViewById(R.id.iconQr), a.findViewById(R.id.txtNavQr), selecionado == QR);

        // O início é a raiz do fluxo do operador: voltar para ele é só fechar as outras telas.
        a.findViewById(R.id.navInicio).setOnClickListener(v -> {
            if (selecionado != INICIO) a.startActivity(new Intent(a, OperadorHomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        });
        a.findViewById(R.id.navCartoes).setOnClickListener(v -> {
            if (selecionado != CARTOES) a.startActivity(new Intent(a, MeusCartoesActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        });
        a.findViewById(R.id.navQr).setOnClickListener(v -> {
            if (selecionado != QR) a.startActivity(new Intent(a, LerQrActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        });
    }

    private static void pintar(ImageView icone, TextView rotulo, boolean ativo) {
        int cor = ativo ? AZUL : CINZA;
        icone.setImageTintList(ColorStateList.valueOf(cor));
        rotulo.setTextColor(cor);
    }
}
