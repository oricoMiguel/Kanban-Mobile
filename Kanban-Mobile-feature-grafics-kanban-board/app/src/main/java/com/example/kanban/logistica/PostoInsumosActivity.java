package com.example.kanban.logistica;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kanban.R;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Insumo;
import com.example.kanban.core.Ouvinte;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Detalhe de um posto: lista de insumos vinculados e inclusão rápida. */
public class PostoInsumosActivity extends AppCompatActivity {
    private String postoId, postoNome;
    private final List<Insumo> insumos = new ArrayList<>();
    private InsumoAdapter adapter;
    private ListenerRegistration reg;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posto_insumos);
        postoId = getIntent().getStringExtra("postoId");
        postoNome = getIntent().getStringExtra("postoNome");

        View raiz = findViewById(R.id.raiz);
        int base = Cores.dp(this, 16);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(base + b.left, base + b.top, base + b.right, base + b.bottom);
            return insets;
        });
        ((TextView)findViewById(R.id.txtPosto)).setText(postoNome == null ? "Posto" : postoNome);
        ((TextView)findViewById(R.id.txtSubtitulo)).setText(getIntent().getStringExtra("postoSetor"));

        EditText entrada = findViewById(R.id.txtNovoInsumo);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdicionar).setOnClickListener(v -> {
            String nome = entrada.getText().toString().trim();
            if (nome.isEmpty()) { entrada.setError("Informe o nome do insumo"); return; }
            CartoesService.get().criarInsumoNoPosto(nome, postoId)
                    .addOnSuccessListener(r -> { entrada.setText(""); Toast.makeText(this, "Insumo adicionado.", Toast.LENGTH_SHORT).show(); })
                    .addOnFailureListener(e -> Toast.makeText(this, "Não foi possível adicionar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        adapter = new InsumoAdapter(insumos, item -> CartoesService.get().desvincularInsumoDoPosto(item.id, postoId)
                .addOnFailureListener(e -> Toast.makeText(this, "Não foi possível remover: " + e.getMessage(), Toast.LENGTH_LONG).show()));
        RecyclerView rv = findViewById(R.id.rvInsumos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    @Override protected void onStart() {
        super.onStart();
        reg = CartoesService.get().escutarInsumosDoPosto(postoId, new Ouvinte<Insumo>() {
            @Override public void aoReceber(List<Insumo> itens) { insumos.clear(); insumos.addAll(itens); adapter.notifyDataSetChanged(); }
            @Override public void aoErrar(Exception e) { Toast.makeText(PostoInsumosActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }
    @Override protected void onStop() { super.onStop(); if (reg != null) { reg.remove(); reg = null; } }

    static class InsumoAdapter extends RecyclerView.Adapter<InsumoAdapter.VH> {
        interface Delete { void remove(Insumo i); }
        final List<Insumo> itens; final Delete delete;
        InsumoAdapter(List<Insumo> itens, Delete delete) { this.itens=itens; this.delete=delete; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_insumo_posto, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Insumo i=itens.get(pos);
            h.nome.setText(i.nome);
            String detalhe = "";
            if (i.codigo != null && !i.codigo.isEmpty()) detalhe += i.codigo;
            if (i.unidade != null && !i.unidade.isEmpty()) detalhe += (detalhe.isEmpty()?"":" • ") + i.unidade;
            h.detalhe.setText(detalhe.isEmpty()?"Insumo cadastrado":detalhe);
            h.btn.setOnClickListener(v -> delete.remove(i));
        }
        @Override public int getItemCount(){return itens.size();}
        static class VH extends RecyclerView.ViewHolder { TextView nome, detalhe; View btn; VH(View v){super(v);nome=v.findViewById(R.id.txtNome);detalhe=v.findViewById(R.id.txtDetalhe);btn=v.findViewById(R.id.btnRemover);} }
    }
}
