
package com.example.kanban.operador;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class MeusCartoesActivity extends AppCompatActivity {

    private String postoId;

    private List<Cartao> ativos = new ArrayList<>();
    private List<Atendimento> concluidos = new ArrayList<>();

    private TextView txtPosto, txtTituloAtivos;
    private TextView txtVazioAtivos, txtVazioConcluidos;

    private RecyclerView listaAtivos, listaConcluidos;

    private CartaoAtivoAdapter adapterAtivos;
    private AtendimentoAdapter adapterConcluidos;

    private ListenerRegistration regCartoes, regConcluidos;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
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

        Visual.ajustarInsets(
                findViewById(R.id.raiz),
                findViewById(R.id.navInferior)
        );

        NavInferior.configurar(this, NavInferior.CARTOES);

        txtPosto = findViewById(R.id.txtPosto);
        txtTituloAtivos = findViewById(R.id.txtTituloAtivos);

        txtVazioAtivos = findViewById(R.id.txtVazioAtivos);
        txtVazioConcluidos = findViewById(R.id.txtVazioConcluidos);

        listaAtivos = findViewById(R.id.listaAtivos);
        listaConcluidos = findViewById(R.id.listaConcluidos);

        listaAtivos.setLayoutManager(new LinearLayoutManager(this));
        listaConcluidos.setLayoutManager(new LinearLayoutManager(this));

        listaAtivos.setNestedScrollingEnabled(false);
        listaConcluidos.setNestedScrollingEnabled(false);

        adapterAtivos = new CartaoAtivoAdapter(
                new CartaoAtivoAdapter.Listener() {

                    @Override
                    public void onClick(Cartao c) {
                        CartaoDetalheActivity.abrir(
                                MeusCartoesActivity.this,
                                c.postoId,
                                c.insumoId
                        );
                    }

                    @Override
                    public void onConfirmar(Cartao c, TextView btn) {

                        btn.setEnabled(false);
                        btn.setAlpha(0.5f);

                        CartoesService.get()
                                .confirmarRecebimento(c.postoId, c.insumoId)
                                .addOnSuccessListener(x ->
                                        Toast.makeText(
                                                MeusCartoesActivity.this,
                                                "Recebimento de " + c.insumoNome
                                                        + " confirmado",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                )
                                .addOnFailureListener(e -> {
                                    btn.setEnabled(true);
                                    btn.setAlpha(1f);

                                    Visual.erro(
                                            MeusCartoesActivity.this,
                                            "Não foi possível confirmar",
                                            e
                                    );
                                });
                    }
                }
        );

        adapterConcluidos = new AtendimentoAdapter();

        listaAtivos.setAdapter(adapterAtivos);
        listaConcluidos.setAdapter(adapterConcluidos);

        CartoesService.get().buscarPosto(postoId)
                .addOnSuccessListener(p -> {
                    if (p != null) {
                        txtPosto.setText(p.nome + " · " + p.setor);
                    }
                });

        renderizar();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!Sessao.exigir(this, Constantes.PERFIL_OPERADOR)) return;

        CartoesService s = CartoesService.get();

        regCartoes = s.escutarCartoesDoPosto(
                postoId,
                new Ouvinte<Cartao>() {

                    @Override
                    public void aoReceber(List<Cartao> itens) {
                        ativos = new ArrayList<>(itens);

                        Collections.sort(ativos, (a, b) ->
                                Long.compare(
                                        Visual.millis(a.abertoEm),
                                        Visual.millis(b.abertoEm)
                                )
                        );

                        renderizar();
                    }

                    @Override
                    public void aoErrar(Exception e) {
                        Visual.erro(
                                MeusCartoesActivity.this,
                                "Erro nos cartões",
                                e
                        );
                    }
                }
        );

        regConcluidos = s.escutarConcluidosHoje(
                postoId,
                new Ouvinte<Atendimento>() {

                    @Override
                    public void aoReceber(List<Atendimento> itens) {
                        concluidos = new ArrayList<>(itens);

                        Collections.sort(concluidos, (a, b) ->
                                Long.compare(
                                        Visual.millis(b.concluidoEm),
                                        Visual.millis(a.concluidoEm)
                                )
                        );

                        renderizar();
                    }

                    @Override
                    public void aoErrar(Exception e) {
                        Visual.erro(
                                MeusCartoesActivity.this,
                                "Erro nos concluídos",
                                e
                        );
                    }
                }
        );

        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 30_000);
    }

    private void renderizar() {

        txtTituloAtivos.setText("Ativos (" + ativos.size() + ")");

        adapterAtivos.atualizar(ativos);
        adapterConcluidos.atualizar(concluidos);

        txtVazioAtivos.setVisibility(
                ativos.isEmpty() ? View.VISIBLE : View.GONE
        );

        txtVazioConcluidos.setVisibility(
                concluidos.isEmpty() ? View.VISIBLE : View.GONE
        );
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (regCartoes != null) {
            regCartoes.remove();
            regCartoes = null;
        }

        if (regConcluidos != null) {
            regConcluidos.remove();
            regConcluidos = null;
        }

        handler.removeCallbacks(tick);
    }
}
