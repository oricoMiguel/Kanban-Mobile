
package com.example.kanban.operador;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kanban.R;
import com.example.kanban.core.Atendimento;

import java.util.ArrayList;
import java.util.List;

public class AtendimentoAdapter
        extends RecyclerView.Adapter<AtendimentoAdapter.AtendimentoViewHolder> {

    private List<Atendimento> atendimentos = new ArrayList<>();

    public void atualizar(List<Atendimento> novosAtendimentos) {
        atendimentos = new ArrayList<>(novosAtendimentos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AtendimentoViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meu_cartao, parent, false);

        return new AtendimentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AtendimentoViewHolder holder, int position) {

        Atendimento a = atendimentos.get(position);

        Visual.aplicarBarraCor(
                holder.barra,
                Color.parseColor("#CFCFCB")
        );

        holder.txtInsumo.setText(
                a.insumoNome != null ? a.insumoNome : "Insumo"
        );

        Visual.chipStatus(
                holder.chipStatus,
                Visual.STATUS_CONCLUIDO
        );

        holder.txtSub.setText(
                "Atendido em " + a.minutosTotal + " min"
        );

        holder.btnConfirmar.setVisibility(View.GONE);
        holder.btnConfirmar.setOnClickListener(null);

        holder.itemView.setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
        return atendimentos.size();
    }

    static class AtendimentoViewHolder extends RecyclerView.ViewHolder {

        View barra;
        TextView txtInsumo, chipStatus, txtSub, btnConfirmar;

        AtendimentoViewHolder(@NonNull View itemView) {
            super(itemView);

            barra = itemView.findViewById(R.id.barra);
            txtInsumo = itemView.findViewById(R.id.txtInsumo);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            txtSub = itemView.findViewById(R.id.txtSub);
            btnConfirmar = itemView.findViewById(R.id.btnConfirmar);
        }
    }
}
