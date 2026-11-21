package com.example.kanban.operador;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kanban.R;
import com.example.kanban.core.*;

public class PedirReposicaoActivity extends AppCompatActivity {
    private PedidoViewModel model;
    private String postoId;
    private InsumosAdapter adapter;
    private boolean terminou;
    public static void abrir(Context c, String postoId) {
        c.startActivity(new Intent(c, PedirReposicaoActivity.class).putExtra("postoId", postoId));
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Sessao.exigir(this, Constantes.PERFIL_OPERADOR)) return;
        setContentView(R.layout.activity_pedir_reposicao);
        Visual.ajustarInsets(findViewById(R.id.raiz), null);
        postoId = getIntent().getStringExtra("postoId");
        if (postoId == null || postoId.isEmpty()) { finish(); return; }
        model = new ViewModelProvider(this).get(PedidoViewModel.class);
        if (state != null) {
            model.selecionado = state.getString("insumo", "");
            model.urgencia = state.getString("urgencia", Constantes.URGENCIA_MEDIA);
        }
        findViewById(R.id.voltar).setOnClickListener(v -> finish());
        findViewById(R.id.lerQr).setOnClickListener(v -> startActivity(new Intent(this, LerQrActivity.class)));
        findViewById(R.id.enviarPedido).setOnClickListener(v -> model.enviar());
        RecyclerView lista = findViewById(R.id.insumosPedido);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InsumosAdapter(); lista.setAdapter(adapter);
        RadioGroup urgencia = findViewById(R.id.urgenciaPedido);
        urgencia.check(Constantes.URGENCIA_ALTA.equals(model.urgencia) ? R.id.urgenciaAlta
                : Constantes.URGENCIA_BAIXA.equals(model.urgencia) ? R.id.urgenciaBaixa : R.id.urgenciaMedia);
        urgencia.setOnCheckedChangeListener((g, id) -> {
            model.urgencia = id == R.id.urgenciaAlta ? Constantes.URGENCIA_ALTA
                    : id == R.id.urgenciaBaixa ? Constantes.URGENCIA_BAIXA : Constantes.URGENCIA_MEDIA;
            explicarUrgencia();
        });
        explicarUrgencia();
        model.alteracao.observe(this, n -> renderizar());
    }
    @Override protected void onStart() {
        super.onStart();
        if (model != null && Sessao.exigir(this, Constantes.PERFIL_OPERADOR)) model.iniciar(postoId);
    }
    @Override protected void onStop() { super.onStop(); if (model != null) model.parar(); }
    private void explicarUrgencia() {
        ((TextView) findViewById(R.id.ajudaUrgencia)).setText(Constantes.URGENCIA_ALTA.equals(model.urgencia)
                ? "Acabou ou a produção está prestes a parar." : Constantes.URGENCIA_BAIXA.equals(model.urgencia)
                ? "Ainda há material para continuar o trabalho." : "Acaba nas próximas horas.");
    }
    private void renderizar() {
        if (model.enviado) {
            if (!terminou) { terminou = true; Toast.makeText(this, "Pedido de reposição aberto", Toast.LENGTH_LONG).show(); finish(); }
            return;
        }
        if (model.posto != null) ((TextView) findViewById(R.id.postoPedido)).setText(model.posto.nome + " · " + model.posto.setor);
        TextView status = findViewById(R.id.statusPedido);
        status.setText(!model.erro.isEmpty() ? model.erro : !model.pronto() ? "Carregando…"
                : model.insumos.isEmpty() ? "Nenhum insumo vinculado a este posto." : "");
        Button enviar = findViewById(R.id.enviarPedido);
        enviar.setEnabled(model.pronto() && !model.insumos.isEmpty() && !model.enviando);
        enviar.setText(model.enviando ? "Enviando…" : "Pedir reposição");
        findViewById(R.id.urgenciaBaixa).setEnabled(!model.enviando);
        findViewById(R.id.urgenciaMedia).setEnabled(!model.enviando);
        findViewById(R.id.urgenciaAlta).setEnabled(!model.enviando);
        adapter.notifyDataSetChanged();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        if (model != null) { out.putString("insumo", model.selecionado); out.putString("urgencia", model.urgencia); }
        super.onSaveInstanceState(out);
    }
    private class InsumosAdapter extends RecyclerView.Adapter<InsumosAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            final CheckedTextView texto;
            Holder(CheckedTextView v) { super(v); texto = v; }
        }
        @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
            return new Holder((CheckedTextView) getLayoutInflater().inflate(R.layout.item_insumo_pedido, parent, false));
        }
        @Override public void onBindViewHolder(Holder h, int position) {
            Insumo i = model.insumos.get(position);
            Cartao ativo = model.ativo(i.id);
            h.texto.setText(i.nome + "\n" + (i.unidade == null ? "" : i.unidade)
                    + (ativo == null ? "" : " · Já pedido há " + ativo.minutosAberto() + " min"));
            h.texto.setEnabled(ativo == null && !model.enviando);
            h.texto.setAlpha(ativo == null ? 1f : .5f);
            h.texto.setChecked(ativo == null && i.id.equals(model.selecionado));
            h.texto.setOnClickListener(v -> { model.selecionado = i.id; notifyDataSetChanged(); });
        }
        @Override public int getItemCount() { return model.insumos.size(); }
    }
}
