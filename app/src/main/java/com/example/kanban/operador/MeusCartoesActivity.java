package com.example.kanban.operador;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kanban.R;
import com.example.kanban.core.Atendimento;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Constantes;
import com.example.kanban.core.Ouvinte;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Meus cartões: ativos do posto + concluídos hoje, em tempo real. Confirma recebimento (C5). */
public class MeusCartoesActivity extends AppCompatActivity {

    private String postoId;

    private List<Cartao> ativos = new ArrayList<>();
    private List<Atendimento> concluidos = new ArrayList<>();

    private TextView txtPosto, txtTituloAtivos, txtVazioAtivos, txtVazioConcluidos;
    private LinearLayout listaAtivos, listaConcluidos;
    private ListenerRegistration regCartoes, regConcluidos;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            renderizar();
            handler.postDelayed(this, 30_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sessao.exigir(this, Constantes.PERFIL_OPERADOR)) return;
        postoId = Sessao.postoId(this);
        setContentView(R.layout.activity_meus_cartoes);

        Visual.ajustarInsets(findViewById(R.id.raiz), findViewById(R.id.navInferior));
        NavInferior.configurar(this, NavInferior.CARTOES);

        txtPosto = findViewById(R.id.txtPosto);
        txtTituloAtivos = findViewById(R.id.txtTituloAtivos);
        txtVazioAtivos = findViewById(R.id.txtVazioAtivos);
        txtVazioConcluidos = findViewById(R.id.txtVazioConcluidos);
        listaAtivos = findViewById(R.id.listaAtivos);
        listaConcluidos = findViewById(R.id.listaConcluidos);

        CartoesService.get().buscarPosto(postoId).addOnSuccessListener(p -> {
            if (p != null) txtPosto.setText(p.nome + " · " + p.setor);
        });
        renderizar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Sessao.exigir(this, Constantes.PERFIL_OPERADOR)) return;
        CartoesService s = CartoesService.get();

        regCartoes = s.escutarCartoesDoPosto(postoId, new Ouvinte<Cartao>() {
            @Override public void aoReceber(List<Cartao> itens) {
                ativos = new ArrayList<>(itens);
                Collections.sort(ativos, (a, b) -> Long.compare(Visual.millis(a.abertoEm), Visual.millis(b.abertoEm)));
                renderizar();
            }
            @Override public void aoErrar(Exception e) { Visual.erro(MeusCartoesActivity.this, "Erro nos cartões", e); }
        });
        regConcluidos = s.escutarConcluidosHoje(postoId, new Ouvinte<Atendimento>() {
            @Override public void aoReceber(List<Atendimento> itens) {
                concluidos = new ArrayList<>(itens);
                // mais recentes primeiro
                Collections.sort(concluidos, (a, b) -> Long.compare(Visual.millis(b.concluidoEm), Visual.millis(a.concluidoEm)));
                renderizar();
            }
            @Override public void aoErrar(Exception e) { Visual.erro(MeusCartoesActivity.this, "Erro nos concluídos", e); }
        });
        handler.postDelayed(tick, 30_000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (regCartoes != null) regCartoes.remove();
        if (regConcluidos != null) regConcluidos.remove();
        handler.removeCallbacks(tick);
    }

    private void renderizar() {
        txtTituloAtivos.setText("Ativos (" + ativos.size() + ")");

        listaAtivos.removeAllViews();
        for (Cartao c : ativos) listaAtivos.addView(itemAtivo(c));
        txtVazioAtivos.setVisibility(ativos.isEmpty() ? View.VISIBLE : View.GONE);

        listaConcluidos.removeAllViews();
        for (Atendimento a : concluidos) listaConcluidos.addView(itemConcluido(a));
        txtVazioConcluidos.setVisibility(concluidos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View itemAtivo(Cartao c) {
        View item = getLayoutInflater().inflate(R.layout.item_meu_cartao, listaAtivos, false);
        Visual.aplicarBarra(item.findViewById(R.id.barra), c.urgencia);
        ((TextView) item.findViewById(R.id.txtInsumo)).setText(c.insumoNome);
        Visual.chipStatus(item.findViewById(R.id.chipStatus), c.status);

        boolean entregue = Constantes.STATUS_ENTREGUE.equals(c.status);
        String sub = "Aberto há " + c.minutosAberto() + " min · "
                + (entregue ? "confirme o recebimento" : "Urgência " + Visual.rotuloUrgencia(c.urgencia));
        ((TextView) item.findViewById(R.id.txtSub)).setText(sub);

        TextView btn = item.findViewById(R.id.btnConfirmar);
        if (entregue) { // C5 CA1 / CA2: o botão só existe para cartão entregue
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(v -> {
                btn.setEnabled(false);
                btn.setAlpha(0.5f);
                CartoesService.get().confirmarRecebimento(c.postoId, c.insumoId)
                        .addOnSuccessListener(x -> Toast.makeText(this,
                                "Recebimento de " + c.insumoNome + " confirmado", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> {
                            btn.setEnabled(true);
                            btn.setAlpha(1f);
                            Visual.erro(this, "Não foi possível confirmar", e);
                        });
            });
        }
        item.setOnClickListener(v -> CartaoDetalheActivity.abrir(this, c.postoId, c.insumoId));
        return item;
    }

    private View itemConcluido(Atendimento a) {
        View item = getLayoutInflater().inflate(R.layout.item_meu_cartao, listaConcluidos, false);
        Visual.aplicarBarraCor(item.findViewById(R.id.barra), Color.parseColor("#CFCFCB"));
        ((TextView) item.findViewById(R.id.txtInsumo)).setText(a.insumoNome != null ? a.insumoNome : "Insumo");
        Visual.chipStatus(item.findViewById(R.id.chipStatus), Visual.STATUS_CONCLUIDO);
        ((TextView) item.findViewById(R.id.txtSub)).setText("Atendido em " + a.minutosTotal + " min");
        return item;
    }
}
