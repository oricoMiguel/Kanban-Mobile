package com.example.kanban.operador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kanban.R;
import com.example.kanban.core.Atendimento;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Constantes;
import com.example.kanban.core.Insumo;
import com.example.kanban.core.Ouvinte;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Início do operador: resumo do posto, pedir reposição e cartões em andamento (tempo real). */
public class OperadorHomeActivity extends AppCompatActivity {

    private static final int MAX_EM_ANDAMENTO = 3;

    private final String postoId = Sessao.POSTO_ID;
    private String postoNome = "Posto";

    private List<Cartao> ativos = new ArrayList<>();
    private List<Atendimento> concluidos = new ArrayList<>();
    private List<Insumo> insumos = new ArrayList<>();

    private TextView txtPostoNome, txtPostoSetor, txtAtivos, txtEntregues, txtVazio;
    private LinearLayout lista;
    private ListenerRegistration regCartoes, regConcluidos, regInsumos;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            renderizar(); // os minutos mudam com o tempo
            handler.postDelayed(this, 30_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operador_home);

        Visual.ajustarInsets(findViewById(R.id.raiz), findViewById(R.id.navInferior));
        NavInferior.configurar(this, NavInferior.INICIO);

        txtPostoNome = findViewById(R.id.txtPostoNome);
        txtPostoSetor = findViewById(R.id.txtPostoSetor);
        txtAtivos = findViewById(R.id.txtAtivos);
        txtEntregues = findViewById(R.id.txtEntregues);
        txtVazio = findViewById(R.id.txtVazio);
        lista = findViewById(R.id.listaAndamento);

        findViewById(R.id.btnPedir).setOnClickListener(v ->
                PedirReposicaoDialog.mostrar(this, postoId, postoNome, insumos, ativos));
        findViewById(R.id.btnVerTodos).setOnClickListener(v ->
                startActivity(new Intent(this, MeusCartoesActivity.class)));

        CartoesService.get().buscarPosto(postoId).addOnSuccessListener(p -> {
            if (p == null) return;
            postoNome = p.nome;
            txtPostoNome.setText(p.nome);
            txtPostoSetor.setText(p.setor);
        });
        renderizar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        CartoesService s = CartoesService.get();

        regCartoes = s.escutarCartoesDoPosto(postoId, new Ouvinte<Cartao>() {
            @Override public void aoReceber(List<Cartao> itens) {
                ativos = new ArrayList<>(itens);
                Collections.sort(ativos, (a, b) -> Long.compare(Visual.millis(a.abertoEm), Visual.millis(b.abertoEm)));
                renderizar();
            }
            @Override public void aoErrar(Exception e) { Visual.erro(OperadorHomeActivity.this, "Erro nos cartões", e); }
        });
        regConcluidos = s.escutarConcluidosHoje(postoId, new Ouvinte<Atendimento>() {
            @Override public void aoReceber(List<Atendimento> itens) {
                concluidos = itens;
                renderizar();
            }
            // Esta consulta (postoId + concluidoEm) pede um índice composto no Firestore:
            // o erro no Logcat traz o link para criá-lo com um clique.
            @Override public void aoErrar(Exception e) { Visual.erro(OperadorHomeActivity.this, "Erro nos concluídos", e); }
        });
        regInsumos = s.escutarInsumosDoPosto(postoId, new Ouvinte<Insumo>() {
            @Override public void aoReceber(List<Insumo> itens) { insumos = itens; }
            @Override public void aoErrar(Exception e) { Visual.erro(OperadorHomeActivity.this, "Erro nos insumos", e); }
        });
        handler.postDelayed(tick, 30_000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (regCartoes != null) regCartoes.remove();
        if (regConcluidos != null) regConcluidos.remove();
        if (regInsumos != null) regInsumos.remove();
        handler.removeCallbacks(tick);
    }

    private void renderizar() {
        txtAtivos.setText(String.valueOf(ativos.size()));
        txtEntregues.setText(String.valueOf(concluidos.size()));

        lista.removeAllViews();
        int n = Math.min(MAX_EM_ANDAMENTO, ativos.size());
        for (int i = 0; i < n; i++) {
            Cartao c = ativos.get(i);
            View item = getLayoutInflater().inflate(R.layout.item_cartao_andamento, lista, false);

            Visual.aplicarBarra(item.findViewById(R.id.barra), c.urgencia);
            ((TextView) item.findViewById(R.id.txtInsumo)).setText(c.insumoNome);
            ((TextView) item.findViewById(R.id.txtSub)).setText(
                    Visual.rotuloStatus(c.status) + " · há " + c.minutosAberto() + " min");

            // C5: só o cartão entregue pede confirmação
            TextView chip = item.findViewById(R.id.chipConfirmar);
            if (Constantes.STATUS_ENTREGUE.equals(c.status)) {
                chip.setVisibility(View.VISIBLE);
                Visual.chip(chip, "Confirmar",
                        android.graphics.Color.parseColor("#CFE0FB"),
                        android.graphics.Color.parseColor("#1E4E9A"));
            }
            item.setOnClickListener(v -> CartaoDetalheActivity.abrir(this, c.postoId, c.insumoId));
            lista.addView(item);
        }
        txtVazio.setVisibility(ativos.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
