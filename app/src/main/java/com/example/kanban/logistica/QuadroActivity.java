package com.example.kanban.logistica;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kanban.R; // ajuste ao seu package
import com.example.kanban.core.Cartao;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Constantes;
import com.example.kanban.core.Ouvinte;
import com.example.kanban.core.SeedDados;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Tela 12: quadro da logística (Aberto | Em rota | Entregue) em tempo real. */
public class QuadroActivity extends AppCompatActivity {

    private String abaAtual = Constantes.STATUS_ABERTO;
    private List<Cartao> todos = new ArrayList<>();

    private TextView tabAberto, tabEmRota, tabEntregue, txtVazio;
    private CartaoQuadroAdapter adapter;
    private ListenerRegistration reg;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            adapter.atualizarTempos();
            handler.postDelayed(this, 30_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quadro);

        // Desce o conteúdo para não ficar embaixo da barra de status / barra de gestos
        View raiz = findViewById(R.id.raiz);
        int base = Cores.dp(this, 16);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(base + b.left, base + b.top, base + b.right, base + b.bottom);
            return insets;
        });

        tabAberto = findViewById(R.id.tabAberto);
        tabEmRota = findViewById(R.id.tabEmRota);
        tabEntregue = findViewById(R.id.tabEntregue);
        txtVazio = findViewById(R.id.txtVazio);

        adapter = new CartaoQuadroAdapter(this::moverCartao);
        RecyclerView rv = findViewById(R.id.rvCartoes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        tabAberto.setOnClickListener(v -> trocarAba(Constantes.STATUS_ABERTO));
        tabEmRota.setOnClickListener(v -> trocarAba(Constantes.STATUS_EM_ROTA));
        tabEntregue.setOnClickListener(v -> trocarAba(Constantes.STATUS_ENTREGUE));
        findViewById(R.id.btnTempos).setOnClickListener(v ->
                startActivity(new Intent(this, TempoAtendimentoActivity.class)));

        atualizarTela();
    }

    @Override
    protected void onStart() {
        super.onStart();
        reg = CartoesService.get().escutarQuadro(new Ouvinte<Cartao>() {
            @Override public void aoReceber(List<Cartao> itens) {
                todos = itens;
                atualizarTela();
            }
            @Override public void aoErrar(Exception e) {
                Toast.makeText(QuadroActivity.this,
                        "Erro no quadro: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        handler.postDelayed(tick, 30_000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) reg.remove();
        handler.removeCallbacks(tick);
    }

    private void trocarAba(String status) {
        abaAtual = status;
        atualizarTela();
    }

    /** C4: aberto -> em rota -> entregue. O "concluído" é do operador. */
    private void moverCartao(Cartao c) {
        CartoesService.get().moverStatus(c.postoId, c.insumoId)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Não foi possível mover: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void atualizarTela() {
        int nAberto = 0, nRota = 0, nEntregue = 0;
        List<Cartao> filtrados = new ArrayList<>();
        for (Cartao c : todos) {
            if (Constantes.STATUS_ABERTO.equals(c.status)) nAberto++;
            else if (Constantes.STATUS_EM_ROTA.equals(c.status)) nRota++;
            else if (Constantes.STATUS_ENTREGUE.equals(c.status)) nEntregue++;
            if (abaAtual.equals(c.status)) filtrados.add(c);
        }
        tabAberto.setText("Aberto (" + nAberto + ")");
        tabEmRota.setText("Em rota (" + nRota + ")");
        tabEntregue.setText("Entregue (" + nEntregue + ")");

        marcar(tabAberto, Constantes.STATUS_ABERTO.equals(abaAtual));
        marcar(tabEmRota, Constantes.STATUS_EM_ROTA.equals(abaAtual));
        marcar(tabEntregue, Constantes.STATUS_ENTREGUE.equals(abaAtual));

        adapter.submit(filtrados);
        txtVazio.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void marcar(TextView tab, boolean selecionada) {
        tab.setSelected(selecionada);
        tab.setTextColor(selecionada ? Color.WHITE : Color.parseColor("#111111"));
    }
}