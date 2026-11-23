
package com.example.kanban.operador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kanban.R;
import com.example.kanban.auth.LoginActivity;
import com.example.kanban.core.Atendimento;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Constantes;
import com.example.kanban.core.Ouvinte;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OperadorHomeActivity extends AppCompatActivity {

    private static final int MAX_EM_ANDAMENTO = 5;

    private String postoId;
    private String postoNome = "Posto";

    private List<Cartao> ativos = new ArrayList<>();
    private List<Atendimento> concluidos = new ArrayList<>();

    private TextView txtPostoNome, txtPostoSetor, txtAtivos,
            txtEntregues, txtVazio;

    private RecyclerView lista;
    private CartaoAndamentoAdapter adapter;

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

        setContentView(R.layout.activity_operador_home);

        Visual.ajustarInsets(
                findViewById(R.id.raiz),
                findViewById(R.id.navInferior)
        );

        NavInferior.configurar(this, NavInferior.INICIO);

        txtPostoNome = findViewById(R.id.txtPostoNome);
        txtPostoSetor = findViewById(R.id.txtPostoSetor);
        txtAtivos = findViewById(R.id.txtAtivos);
        txtEntregues = findViewById(R.id.txtEntregues);
        txtVazio = findViewById(R.id.txtVazio);

        lista = findViewById(R.id.listaAndamento);

        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setNestedScrollingEnabled(false);

        adapter = new CartaoAndamentoAdapter(cartao ->
                CartaoDetalheActivity.abrir(
                        this,
                        cartao.postoId,
                        cartao.insumoId
                )
        );

        lista.setAdapter(adapter);

        findViewById(R.id.btnPedir).setOnClickListener(v ->
                PedirReposicaoActivity.abrir(this, postoId)
        );

        findViewById(R.id.contaOperador).setOnClickListener(v -> {

            PopupMenu menu = new PopupMenu(this, v);

            menu.getMenu().add("Trocar de posto")
                    .setOnMenuItemClickListener(item -> {

                        pararEscutas();

                        Intent intent = new Intent(
                                OperadorHomeActivity.this,
                                LoginActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();

                        return true;
                    });

            menu.getMenu().add("Sair da conta")
                    .setOnMenuItemClickListener(item -> {

                        pararEscutas();
                        Sessao.sair(this);

                        return true;
                    });

            menu.show();
        });

        findViewById(R.id.btnVerTodos).setOnClickListener(v ->
                startActivity(new Intent(this, MeusCartoesActivity.class))
        );

        CartoesService.get().buscarPosto(postoId)
                .addOnSuccessListener(p -> {
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

        if (!Sessao.exigir(this, Constantes.PERFIL_OPERADOR)) return;

        CartoesService s = CartoesService.get();

        regCartoes = s.escutarCartoesDoPosto(postoId,
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
                                OperadorHomeActivity.this,
                                "Erro nos cartões",
                                e
                        );
                    }
                });

        regConcluidos = s.escutarConcluidosHoje(postoId,
                new Ouvinte<Atendimento>() {

                    @Override
                    public void aoReceber(List<Atendimento> itens) {
                        concluidos = new ArrayList<>(itens);
                        renderizar();
                    }

                    @Override
                    public void aoErrar(Exception e) {
                        Visual.erro(
                                OperadorHomeActivity.this,
                                "Erro nos concluídos",
                                e
                        );
                    }
                });

        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 30_000);
    }

    private void pararEscutas() {

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

    @Override
    protected void onStop() {
        super.onStop();
        pararEscutas();
    }

    private void renderizar() {

        txtAtivos.setText(String.valueOf(ativos.size()));
        txtEntregues.setText(String.valueOf(concluidos.size()));

        int n = Math.min(MAX_EM_ANDAMENTO, ativos.size());

        List<Cartao> exibidos = new ArrayList<>(
                ativos.subList(0, n)
        );

        adapter.atualizar(exibidos);

        txtVazio.setVisibility(
                ativos.isEmpty() ? View.VISIBLE : View.GONE
        );
    }
}
