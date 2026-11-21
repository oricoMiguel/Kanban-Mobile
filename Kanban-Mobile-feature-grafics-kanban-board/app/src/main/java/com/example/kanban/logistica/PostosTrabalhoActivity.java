package com.example.kanban.logistica;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.kanban.core.Ouvinte;
import com.example.kanban.core.Posto;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Lista de postos de trabalho. */
public class PostosTrabalhoActivity extends AppCompatActivity {
    private final List<Posto> postos = new ArrayList<>();
    private PostoAdapter adapter;
    private ListenerRegistration reg;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postos_trabalho);
        View raiz = findViewById(R.id.raiz);
        int base = Cores.dp(this, 16);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(base + b.left, base + b.top, base + b.right, base + b.bottom);
            return insets;
        });

        adapter = new PostoAdapter(postos, p -> {
            Intent i = new Intent(this, PostoInsumosActivity.class);
            i.putExtra("postoId", p.id);
            i.putExtra("postoNome", p.nome);
            i.putExtra("postoSetor", p.setor);
            startActivity(i);
        });
        RecyclerView rv = findViewById(R.id.rvPostos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        findViewById(R.id.btnAdicionar).setOnClickListener(v -> startActivity(new Intent(this, NovoPostoActivity.class)));
        findViewById(R.id.btnNovoInsumo).setOnClickListener(v -> startActivity(new Intent(this, NovoInsumoActivity.class)));
    }

    @Override protected void onStart() {
        super.onStart();
        reg = CartoesService.get().escutarPostos(new Ouvinte<Posto>() {
            @Override public void aoReceber(List<Posto> itens) { postos.clear(); postos.addAll(itens); adapter.notifyDataSetChanged(); }
            @Override public void aoErrar(Exception e) { Toast.makeText(PostosTrabalhoActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        });
    }
    @Override protected void onStop() { super.onStop(); if (reg != null) { reg.remove(); reg = null; } }

    static class PostoAdapter extends RecyclerView.Adapter<PostoAdapter.VH> {
        interface Click { void open(Posto p); }
        final List<Posto> itens; final Click click;
        PostoAdapter(List<Posto> itens, Click click) { this.itens = itens; this.click = click; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_posto_trabalho, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Posto p = itens.get(pos);
            h.nome.setText(p.nome);
            h.setor.setText(p.setor == null || p.setor.isEmpty() ? "Sem setor" : p.setor);
            h.status.setText(p.ativo ? "Ativo" : "Inativo");
            h.itemView.setOnClickListener(v -> click.open(p));
        }
        @Override public int getItemCount() { return itens.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView nome, setor, status;
            VH(View v) { super(v); nome=v.findViewById(R.id.txtNome); setor=v.findViewById(R.id.txtSetor); status=v.findViewById(R.id.txtStatus); }
        }
    }
}
