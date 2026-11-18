package com.example.kanban.logistica;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
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
import com.example.kanban.core.Atendimento;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Ouvinte;
import com.example.kanban.core.ResumoTempo;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/** Tela 6: tempo de atendimento por posto (coleção "atendimentos"), em tempo real. */
public class TempoAtendimentoActivity extends AppCompatActivity {

    private int dias = 0; // 0 = hoje | 7 | 30
    private TextView tabHoje, tab7, tab30, txtMedia, txtAtendidos, txtVazio;
    private PostoTempoAdapter adapter;
    private ListenerRegistration reg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tempo_atendimento);

        // Desce o conteúdo para não ficar embaixo da barra de status / barra de gestos
        View raiz = findViewById(R.id.raiz);
        int base = Cores.dp(this, 16);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(base + b.left, base + b.top, base + b.right, base + b.bottom);
            return insets;
        });

        tabHoje = findViewById(R.id.tabHoje);
        tab7 = findViewById(R.id.tab7dias);
        tab30 = findViewById(R.id.tab30dias);
        txtMedia = findViewById(R.id.txtMedia);
        txtAtendidos = findViewById(R.id.txtAtendidos);
        txtVazio = findViewById(R.id.txtVazio);

        adapter = new PostoTempoAdapter();
        RecyclerView rv = findViewById(R.id.rvPostos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        tabHoje.setOnClickListener(v -> trocarPeriodo(0));
        tab7.setOnClickListener(v -> trocarPeriodo(7));
        tab30.setOnClickListener(v -> trocarPeriodo(30));

        montarLegenda();
        marcarAbas();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registrar();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void trocarPeriodo(int novoDias) {
        dias = novoDias;
        marcarAbas();
        registrar(); // remove o listener antigo e abre outro com o novo filtro
    }

    private void registrar() {
        if (reg != null) reg.remove();
        reg = CartoesService.get().escutarAtendimentos(dias, new Ouvinte<Atendimento>() {
            @Override public void aoReceber(List<Atendimento> lista) {
                ResumoTempo r = ResumoTempo.calcular(lista);
                txtMedia.setText(r.mediaGeralMin + " min");
                txtAtendidos.setText(String.valueOf(r.atendidos));
                adapter.submit(r.postos);
                txtVazio.setVisibility(r.postos.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void aoErrar(Exception e) {
                Toast.makeText(TempoAtendimentoActivity.this,
                        "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void marcarAbas() {
        marcar(tabHoje, dias == 0);
        marcar(tab7, dias == 7);
        marcar(tab30, dias == 30);
    }

    private void marcar(TextView tab, boolean selecionada) {
        tab.setSelected(selecionada);
        tab.setTextColor(selecionada ? Color.WHITE : Color.parseColor("#111111"));
    }

    private void montarLegenda() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        adicionar(sb, "● ", Cores.VERDE);
        adicionar(sb, "até 7 min    ", Color.parseColor("#777777"));
        adicionar(sb, "● ", Cores.AMARELO);
        adicionar(sb, "7 a 15    ", Color.parseColor("#777777"));
        adicionar(sb, "● ", Cores.VERMELHO);
        adicionar(sb, "acima de 15", Color.parseColor("#777777"));
        ((TextView) findViewById(R.id.txtLegenda)).setText(sb);
    }

    private void adicionar(SpannableStringBuilder sb, String texto, int cor) {
        int ini = sb.length();
        sb.append(texto);
        sb.setSpan(new ForegroundColorSpan(cor), ini, sb.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}