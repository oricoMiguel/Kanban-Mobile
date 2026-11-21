package com.example.kanban.operador;

import android.app.Activity;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.Insumo;
import java.util.List;

/** Compatibilidade para chamadas existentes: a tela carrega os dados em tempo real. */
@Deprecated
public final class PedirReposicaoDialog {
    private PedirReposicaoDialog() {}
    public static void mostrar(Activity a, String postoId, String postoNome,
                               List<Insumo> insumos, List<Cartao> ativos) {
        PedirReposicaoActivity.abrir(a, postoId);
    }
}
