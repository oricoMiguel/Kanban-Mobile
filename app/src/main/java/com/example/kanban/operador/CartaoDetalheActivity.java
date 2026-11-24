package com.example.kanban.operador;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kanban.R;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Constantes;
import com.example.kanban.core.Ouvinte;
import com.example.kanban.logistica.Cores;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Detalhe do cartão com a linha do tempo "Andamento" (Aberto → Em rota → Entregue → Concluído).
 * As ações disponíveis seguem o perfil autenticado (C4 / C5).
 */
public class CartaoDetalheActivity extends AppCompatActivity {

    private static final String EXTRA_POSTO_ID = "postoId";
    private static final String EXTRA_INSUMO_ID = "insumoId";

    public static void abrir(Context c, String postoId, String insumoId) {
        Intent i = new Intent(c, CartaoDetalheActivity.class);
        i.putExtra(EXTRA_POSTO_ID, postoId);
        i.putExtra(EXTRA_INSUMO_ID, insumoId);
        c.startActivity(i);
    }

    private String postoId, insumoId, setor = "";
    private String perfil = Constantes.PERFIL_OPERADOR;
    private Cartao cartao;
    private boolean jaCarregou = false;

    private TextView tabOperador, tabLogistica, txtInsumo, txtPosto, chipUrgencia,
            txtAbertoHa, txtDuracao, txtMensagem, btnAcao;
    private LinearLayout bannerTempo, listaPassos;
    private ListenerRegistration reg;

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
        if (!Sessao.exigir(this, null)) return;
        perfil = Sessao.perfil(this);
        setContentView(R.layout.activity_cartao_detalhe);

        postoId = getIntent().getStringExtra(EXTRA_POSTO_ID);
        insumoId = getIntent().getStringExtra(EXTRA_INSUMO_ID);
        if (postoId == null || insumoId == null) { finish(); return; }

        Visual.ajustarInsets(findViewById(R.id.raiz), null);

        tabOperador = findViewById(R.id.tabOperador);
        tabLogistica = findViewById(R.id.tabLogistica);
        txtInsumo = findViewById(R.id.txtInsumo);
        txtPosto = findViewById(R.id.txtPosto);
        chipUrgencia = findViewById(R.id.chipUrgencia);
        bannerTempo = findViewById(R.id.bannerTempo);
        txtAbertoHa = findViewById(R.id.txtAbertoHa);
        txtDuracao = findViewById(R.id.txtDuracao);
        listaPassos = findViewById(R.id.listaPassos);
        txtMensagem = findViewById(R.id.txtMensagem);
        btnAcao = findViewById(R.id.btnAcao);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        tabOperador.setVisibility(View.GONE);
        tabLogistica.setVisibility(View.GONE);
        ((View) tabOperador.getParent()).setVisibility(View.GONE);

        CartoesService.get().buscarPosto(postoId).addOnSuccessListener(p -> {
            if (p == null) return;
            setor = p.setor;
            renderizar();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Sessao.exigir(this, null)) return;
        if (postoId == null) return;
        reg = CartoesService.get().escutarCartao(postoId, insumoId, new Ouvinte<Cartao>() {
            @Override public void aoReceber(List<Cartao> itens) {
                if (itens.isEmpty()) {
                    // o cartão some de "cartoes" quando é concluído (vira "atendimentos")
                    Toast.makeText(CartaoDetalheActivity.this,
                            jaCarregou ? "Cartão concluído" : "Cartão não encontrado",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                cartao = itens.get(0);
                jaCarregou = true;
                renderizar();
            }
            @Override public void aoErrar(Exception e) { Visual.erro(CartaoDetalheActivity.this, "Erro no cartão", e); }
        });
        handler.postDelayed(tick, 30_000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) reg.remove();
        handler.removeCallbacks(tick);
    }

    private void marcar(TextView tab, boolean selecionada) {
        tab.setSelected(selecionada);
        tab.setTextColor(selecionada ? Color.WHITE : Color.parseColor("#111111"));
    }

    private void renderizar() {
        marcar(tabOperador, Constantes.PERFIL_OPERADOR.equals(perfil));
        marcar(tabLogistica, Constantes.PERFIL_LOGISTICA.equals(perfil));
        if (cartao == null) return;
        Cartao c = cartao;

        txtInsumo.setText(c.insumoNome);
        txtPosto.setText(setor.isEmpty() ? c.postoNome : c.postoNome + " · " + setor);

        // selo + faixa de tempo, na cor da urgência
        int fundo = Visual.fundoUrgencia(c.urgencia), texto = Visual.textoUrgencia(c.urgencia);
        Visual.chip(chipUrgencia, "Urgência " + Visual.rotuloUrgencia(c.urgencia), fundo, texto);

        GradientDrawable faixa = new GradientDrawable();
        faixa.setColor(fundo);
        faixa.setCornerRadius(Cores.dp(this, 16));
        bannerTempo.setBackground(faixa);
        txtAbertoHa.setTextColor(texto);
        txtDuracao.setTextColor(texto);
        txtDuracao.setText(Visual.duracao(c.minutosAberto()));

        desenharPassos(c);
        desenharAcao(c);
    }

    /** C6 CA2: horário de cada mudança de status. */
    private void desenharPassos(Cartao c) {
        // índice do passo atual: aberto=1 (próximo é "Em rota"), em_rota=2, entregue=3
        int atual = Constantes.STATUS_ENTREGUE.equals(c.status) ? 3
                : Constantes.STATUS_EM_ROTA.equals(c.status) ? 2 : 1;

        String[] titulos = {"Aberto", "Em rota", "Entregue", "Concluído"};
        String[] feitos = {
                Visual.hora(c.abertoEm) + " · pedido do operador",
                Visual.hora(c.emRotaEm) + " · logística saiu para entrega",
                Visual.hora(c.entregueEm) + " · entregue no posto",
                ""
        };

        listaPassos.removeAllViews();
        for (int i = 0; i < titulos.length; i++) {
            View passo = getLayoutInflater().inflate(R.layout.item_passo, listaPassos, false);
            ImageView icone = passo.findViewById(R.id.icone);
            ((TextView) passo.findViewById(R.id.txtTitulo)).setText(titulos[i]);
            TextView sub = passo.findViewById(R.id.txtSub);

            if (i < atual) {
                icone.setBackgroundResource(R.drawable.bg_passo_ok);
                icone.setImageResource(R.drawable.ic_check);
                icone.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                sub.setText(feitos[i]);
            } else if (i == atual) {
                icone.setBackgroundResource(R.drawable.bg_passo_atual);
                icone.setImageResource(R.drawable.ic_schedule);
                icone.setImageTintList(ColorStateList.valueOf(Color.parseColor("#2B5BA8")));
                sub.setText("Pendente");
            } else {
                icone.setBackgroundResource(R.drawable.bg_passo_pendente);
                icone.setImageDrawable(null);
                sub.setText("Pendente");
            }
            if (i == titulos.length - 1) passo.findViewById(R.id.linha).setVisibility(View.GONE);
            listaPassos.addView(passo);
        }
    }

    /** Operador só confirma (C5); logística só move (C4). O resto vira mensagem. */
    private void desenharAcao(Cartao c) {
        boolean operador = Constantes.PERFIL_OPERADOR.equals(perfil);
        String s = c.status;
        String mensagem = null, botao = null;

        if (operador) {
            if (Constantes.STATUS_ABERTO.equals(s)) mensagem = "Aguardando a logística pegar o pedido.";
            else if (Constantes.STATUS_EM_ROTA.equals(s)) mensagem = "A logística está a caminho com o pedido.";
            else botao = "Confirmar recebimento";
        } else {
            if (Constantes.STATUS_ABERTO.equals(s)) botao = "Pegar e sair para entrega";
            else if (Constantes.STATUS_EM_ROTA.equals(s)) botao = "Marcar como entregue";
            else mensagem = "Aguardando confirmação do operador.";
        }

        txtMensagem.setVisibility(mensagem != null ? View.VISIBLE : View.GONE);
        if (mensagem != null) txtMensagem.setText(mensagem);

        btnAcao.setVisibility(botao != null ? View.VISIBLE : View.GONE);
        if (botao != null) {
            btnAcao.setText(botao);
            btnAcao.setEnabled(true);
            btnAcao.setAlpha(1f);
            btnAcao.setOnClickListener(v -> executar(c, operador));
        }
    }

    private void executar(Cartao c, boolean operador) {
        btnAcao.setEnabled(false); // evita toque duplo enquanto a transação roda
        btnAcao.setAlpha(0.5f);
        CartoesService s = CartoesService.get();
        Task<Void> t = operador
                ? s.confirmarRecebimento(c.postoId, c.insumoId)
                : s.moverStatus(c.postoId, c.insumoId);
        t.addOnFailureListener(e -> {
            btnAcao.setEnabled(true);
            btnAcao.setAlpha(1f);
            Visual.erro(this, "Não foi possível concluir", e);
        });
        // sucesso: o listener em tempo real redesenha a tela (ou fecha, se o cartão foi concluído)
    }
}
