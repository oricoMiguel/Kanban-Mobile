package com.example.kanban.logistica;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kanban.R; // ajuste ao seu package
import com.example.kanban.core.Cartao;
import com.example.kanban.core.Constantes;

import java.util.ArrayList;
import java.util.List;

public class CartaoQuadroAdapter extends RecyclerView.Adapter<CartaoQuadroAdapter.VH> {

    public interface Acao { void aoMover(Cartao cartao); }

    private final List<Cartao> itens = new ArrayList<>();
    private final Acao acao;

    public CartaoQuadroAdapter(Acao acao) { this.acao = acao; }

    public void submit(List<Cartao> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    /** Chamado a cada 30s: os minutos e as cores mudam com o tempo. */
    public void atualizarTempos() { notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_cartao_quadro, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Cartao c = itens.get(pos);

        h.txtPosto.setText(c.postoNome);
        h.txtInsumo.setText(c.insumoNome);
        h.txtTempo.setText("⏱ Aberto há " + c.minutosAberto() + " min");

        // barra lateral: cor por tempo de espera (cantos arredondados só à esquerda)
        float r = Cores.dp(h.itemView.getContext(), 12);
        GradientDrawable barra = new GradientDrawable();
        if (Constantes.URGENCIA_ALTA.equals(c.urgencia)) {
            barra.setColor(Color.parseColor("#E53935"));
        } else if (Constantes.URGENCIA_MEDIA.equals(c.urgencia)) {
            barra.setColor(Color.parseColor("#FFB74D"));
        } else {
            barra.setColor(Color.parseColor("#43A047"));
        }
        barra.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, r, r});
        h.barra.setBackground(barra);

        // selo de urgência
        int fundo, texto;
        String rotulo;
        if (Constantes.URGENCIA_ALTA.equals(c.urgencia)) {
            fundo = Color.parseColor("#FDE3E3"); texto = Color.parseColor("#C62828"); rotulo = "Alta";
        } else if (Constantes.URGENCIA_MEDIA.equals(c.urgencia)) {
            fundo = Color.parseColor("#FFE8C2"); texto = Color.parseColor("#B26A00"); rotulo = "Média";
        } else {
            fundo = Color.parseColor("#DDF3E2"); texto = Color.parseColor("#1B7A38"); rotulo = "Baixa";
        }
        h.txtUrgencia.setText(rotulo);
        h.txtUrgencia.setTextColor(texto);
        h.txtUrgencia.setBackgroundTintList(ColorStateList.valueOf(fundo));

        // botão conforme a coluna
        if (Constantes.STATUS_ABERTO.equals(c.status)) {
            h.btnAcao.setVisibility(View.VISIBLE);
            h.txtAguardando.setVisibility(View.GONE);
            h.btnAcao.setText("Pegar e sair para entrega");
        } else if (Constantes.STATUS_EM_ROTA.equals(c.status)) {
            h.btnAcao.setVisibility(View.VISIBLE);
            h.txtAguardando.setVisibility(View.GONE);
            h.btnAcao.setText("Marcar como entregue");
        } else { // entregue: só o operador pode concluir
            h.btnAcao.setVisibility(View.GONE);
            h.txtAguardando.setVisibility(View.VISIBLE);
        }
        h.btnAcao.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#F0F0F0"))
        );
        h.btnAcao.setOnClickListener(v -> acao.aoMover(c));
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final View barra;
        final TextView txtPosto, txtUrgencia, txtInsumo, txtTempo, txtAguardando;
        final Button btnAcao;

        VH(@NonNull View v) {
            super(v);
            barra = v.findViewById(R.id.barra);
            txtPosto = v.findViewById(R.id.txtPosto);
            txtUrgencia = v.findViewById(R.id.txtUrgencia);
            txtInsumo = v.findViewById(R.id.txtInsumo);
            txtTempo = v.findViewById(R.id.txtTempo);
            txtAguardando = v.findViewById(R.id.txtAguardando);
            btnAcao = v.findViewById(R.id.btnAcao);
        }
    }
}