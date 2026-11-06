package com.example.kanban.logistica;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kanban.R; // ajuste ao seu package
import com.example.kanban.core.Constantes;
import com.example.kanban.core.ResumoTempo;

import java.util.ArrayList;
import java.util.List;

public class PostoTempoAdapter extends RecyclerView.Adapter<PostoTempoAdapter.VH> {

    private final List<ResumoTempo.MediaPosto> itens = new ArrayList<>();
    private long max = 0;

    /** A lista já vem ordenada do mais lento para o mais rápido. */
    public void submit(List<ResumoTempo.MediaPosto> novos) {
        itens.clear();
        itens.addAll(novos);
        max = novos.isEmpty() ? 0 : novos.get(0).mediaMin;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_posto_tempo, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ResumoTempo.MediaPosto m = itens.get(pos);

        h.txtPosto.setText(m.posto);
        h.txtMin.setText(m.mediaMin + " min");
        h.txtCartoes.setText(m.cartoes + (m.cartoes == 1 ? " cartão" : " cartões"));

        GradientDrawable g = new GradientDrawable();
        g.setColor(Cores.porNome(Constantes.corPorEspera(m.mediaMin)));
        g.setCornerRadius(Cores.dp(h.itemView.getContext(), 4));
        h.fill.setBackground(g);

        // largura proporcional à maior média (a mais lenta fica 100%)
        float pct = max > 0 ? Math.max(3f, m.mediaMin * 100f / max) : 0f;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) h.fill.getLayoutParams();
        lp.weight = pct;
        h.fill.setLayoutParams(lp);
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txtPosto, txtMin, txtCartoes;
        final View fill;

        VH(@NonNull View v) {
            super(v);
            txtPosto = v.findViewById(R.id.txtPosto);
            txtMin = v.findViewById(R.id.txtMin);
            txtCartoes = v.findViewById(R.id.txtCartoes);
            fill = v.findViewById(R.id.fill);
        }
    }
}