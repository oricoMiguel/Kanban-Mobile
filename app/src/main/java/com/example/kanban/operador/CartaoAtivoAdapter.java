
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

public class CartaoAtivoAdapter
        extends RecyclerView.Adapter<CartaoAtivoAdapter.CartaoViewHolder> {

    public interface Listener {
        void onClick(Cartao cartao);
        void onConfirmar(Cartao cartao, TextView botao);
    }

    private List<Cartao> cartoes = new ArrayList<>();
    private final Listener listener;

    public CartaoAtivoAdapter(Listener listener) {
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
                .inflate(R.layout.item_meu_cartao, parent, false);

        return new CartaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CartaoViewHolder holder, int position) {

        Cartao c = cartoes.get(position);

        Visual.aplicarBarra(holder.barra, c.urgencia);

        holder.txtInsumo.setText(c.insumoNome);

        Visual.chipStatus(holder.chipStatus, c.status);

        boolean entregue =
                Constantes.STATUS_ENTREGUE.equals(c.status);

        String sub = "Aberto há " + c.minutosAberto() + " min · "
                + (entregue
                ? "confirme o recebimento"
                : "Urgência " + Visual.rotuloUrgencia(c.urgencia));

        holder.txtSub.setText(sub);

        if (entregue) {
            holder.btnConfirmar.setVisibility(View.VISIBLE);
            holder.btnConfirmar.setEnabled(true);
            holder.btnConfirmar.setAlpha(1f);

            holder.btnConfirmar.setOnClickListener(v ->
                    listener.onConfirmar(c, holder.btnConfirmar)
            );
        } else {
            holder.btnConfirmar.setVisibility(View.GONE);
            holder.btnConfirmar.setOnClickListener(null);
        }

        holder.itemView.setOnClickListener(v ->
                listener.onClick(c)
        );
    }

    @Override
    public int getItemCount() {
        return cartoes.size();
    }

    static class CartaoViewHolder extends RecyclerView.ViewHolder {

        View barra;
        TextView txtInsumo, chipStatus, txtSub, btnConfirmar;

        CartaoViewHolder(@NonNull View itemView) {
            super(itemView);

            barra = itemView.findViewById(R.id.barra);
            txtInsumo = itemView.findViewById(R.id.txtInsumo);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            txtSub = itemView.findViewById(R.id.txtSub);
            btnConfirmar = itemView.findViewById(R.id.btnConfirmar);
        }
    }
}
