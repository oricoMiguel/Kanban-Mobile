package com.example.kanban.operador;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kanban.R;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.Insumo;
import java.util.Collections;
import java.util.List;

/** Cards selecionáveis do formulário, com a mesma vinculação usada nas prévias de UI. */
public class InsumosPedidoAdapter extends RecyclerView.Adapter<InsumosPedidoAdapter.Holder> {
    public interface Selecao { void escolher(String id); }
    private final Selecao selecao;
    private List<Insumo> insumos = Collections.emptyList();
    private List<Cartao> ativos = Collections.emptyList();
    private String selecionado = "";
    private boolean enviando;
    public InsumosPedidoAdapter(Selecao selecao) { this.selecao = selecao; }
    public void atualizar(List<Insumo> insumos, List<Cartao> ativos, String selecionado, boolean enviando) {
        this.insumos = insumos; this.ativos = ativos; this.selecionado = selecionado; this.enviando = enviando;
        notifyDataSetChanged();
    }
    static class Holder extends RecyclerView.ViewHolder {
        final TextView nome, unidade, pedido;
        Holder(View v) {
            super(v); nome = v.findViewById(R.id.nomeInsumo);
            unidade = v.findViewById(R.id.unidadeInsumo); pedido = v.findViewById(R.id.pedidoExistente);
        }
    }
    @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_insumo_pedido, parent, false));
    }
    @Override public void onBindViewHolder(Holder h, int position) {
        Insumo i = insumos.get(position);
        Cartao ativo = null;
        for (Cartao c : ativos) if (i.id.equals(c.insumoId)) { ativo = c; break; }
        h.nome.setText(i.nome); h.unidade.setText(i.unidade == null ? "" : i.unidade);
        h.pedido.setText(ativo == null ? "" : "◷ Já pedido há " + ativo.minutosAberto() + " min");
        h.pedido.setVisibility(ativo == null ? View.GONE : View.VISIBLE);
        h.itemView.setEnabled(ativo == null && !enviando);
        h.nome.setTextColor(Color.parseColor(ativo == null ? "#111111" : "#888888"));
        h.itemView.setSelected(ativo == null && i.id.equals(selecionado));
        h.itemView.setOnClickListener(v -> selecao.escolher(i.id));
    }
    @Override public int getItemCount() { return insumos.size(); }
}
