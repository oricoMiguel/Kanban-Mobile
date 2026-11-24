
package com.example.kanban.operador;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kanban.R;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.Constantes;

import java.util.ArrayList;
import java.util.List;

public class CartaoAndamentoAdapter
        extends RecyclerView.Adapter<CartaoAndamentoAdapter.CartaoViewHolder> {

    public interface OnCartaoClickListener {
        void onClick(Cartao cartao);
    }

    private List<Cartao> cartoes = new ArrayList<>();
    private final OnCartaoClickListener listener;

    public CartaoAndamentoAdapter(OnCartaoClickListener listener) {
        this.listener = listener;
    }

    public void atualizar(List<Cartao> novosCartoes) {
        cartoes = new ArrayList<>(novosCartoes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartaoViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cartao_andamento, parent, false);

        return new CartaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CartaoViewHolder holder, int position) {

        Cartao cartao = cartoes.get(position);

        Visual.aplicarBarra(holder.barra, cartao.urgencia);

        holder.txtInsumo.setText(cartao.insumoNome);

        holder.txtSub.setText(
                Visual.rotuloStatus(cartao.status)
                        + " · há "
                        + cartao.minutosAberto()
                        + " min"
        );

        if (Constantes.STATUS_ENTREGUE.equals(cartao.status)) {
            holder.chipConfirmar.setVisibility(View.VISIBLE);

            Visual.chip(
                    holder.chipConfirmar,
                    "Confirmar",
                    android.graphics.Color.parseColor("#CFE0FB"),
                    android.graphics.Color.parseColor("#1E4E9A")
            );
        } else {
            holder.chipConfirmar.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(cartao);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartoes.size();
    }

    static class CartaoViewHolder extends RecyclerView.ViewHolder {

        View barra;
        TextView txtInsumo, txtSub, chipConfirmar;

        CartaoViewHolder(@NonNull View itemView) {
            super(itemView);

            barra = itemView.findViewById(R.id.barra);
            txtInsumo = itemView.findViewById(R.id.txtInsumo);
            txtSub = itemView.findViewById(R.id.txtSub);
            chipConfirmar = itemView.findViewById(R.id.chipConfirmar);
        }
    }
}
