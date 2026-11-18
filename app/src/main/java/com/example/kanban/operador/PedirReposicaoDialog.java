package com.example.kanban.operador;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.kanban.R;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Constantes;
import com.example.kanban.core.Insumo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C2 + C3: escolher insumo e urgência e abrir o cartão.
 * Insumo que já tem cartão ativo no posto aparece desabilitado ("já pedido há X min").
 */
public final class PedirReposicaoDialog {
    private PedirReposicaoDialog() {}

    public static void mostrar(Activity a, String postoId, String postoNome,
                               List<Insumo> insumos, List<Cartao> ativos) {
        View v = a.getLayoutInflater().inflate(R.layout.dialog_pedir_reposicao, null);
        RadioGroup rgInsumos = v.findViewById(R.id.rgInsumos);
        RadioGroup rgUrgencia = v.findViewById(R.id.rgUrgencia);
        TextView txtErro = v.findViewById(R.id.txtErro);
        TextView txtSem = v.findViewById(R.id.txtSemInsumos);

        Map<Integer, Insumo> porId = new HashMap<>();
        for (Insumo i : insumos) {
            RadioButton rb = new RadioButton(a);
            rb.setId(View.generateViewId());
            Cartao existente = cartaoAtivoDe(ativos, i.id);
            if (existente != null) { // C3 CA1
                rb.setText(i.nome + " — já pedido há " + existente.minutosAberto() + " min");
                rb.setEnabled(false);
            } else {
                rb.setText(i.nome);
            }
            rgInsumos.addView(rb);
            porId.put(rb.getId(), i);
        }
        if (insumos.isEmpty()) txtSem.setVisibility(View.VISIBLE);

        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle("Pedir reposição")
                .setView(v)
                .setPositiveButton("Pedir", null) // clique definido abaixo para não fechar com erro
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            Insumo escolhido = porId.get(rgInsumos.getCheckedRadioButtonId());
            if (escolhido == null) { // C2 CA2
                txtErro.setVisibility(View.VISIBLE);
                return;
            }
            int sel = rgUrgencia.getCheckedRadioButtonId();
            String urgencia = sel == R.id.rbAlta ? Constantes.URGENCIA_ALTA
                    : sel == R.id.rbBaixa ? Constantes.URGENCIA_BAIXA
                    : Constantes.URGENCIA_MEDIA;
            dialog.dismiss();
            enviar(a.getApplicationContext(), postoId, postoNome, escolhido, urgencia);
        }));
        dialog.show();
    }

    private static void enviar(Context c, String postoId, String postoNome, Insumo i, String urgencia) {
        CartoesService.get().criarCartao(postoId, postoNome, i.id, i.nome, urgencia)
                .addOnSuccessListener(x ->
                        Toast.makeText(c, "Pedido de " + i.nome + " aberto", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(c,
                        CartoesService.ehDuplicado(e)
                                ? "Já existe um cartão aberto para " + i.nome + " neste posto."
                                : "Não foi possível pedir: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private static Cartao cartaoAtivoDe(List<Cartao> ativos, String insumoId) {
        for (Cartao c : ativos) if (insumoId != null && insumoId.equals(c.insumoId)) return c;
        return null;
    }
}
