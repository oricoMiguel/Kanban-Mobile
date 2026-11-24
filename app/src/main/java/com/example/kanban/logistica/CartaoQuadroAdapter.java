
package com.example.kanban.logistica;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kanban.R;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.Constantes;

import java.util.ArrayList;
import java.util.List;

public class CartaoQuadroAdapter extends RecyclerView.Adapter<CartaoQuadroAdapter.VH> {

    public interface Acao {
        void aoMover(Cartao cartao);
    }

    private final List<Cartao> itens = new ArrayList<>();
    private final Acao acao;

    public CartaoQuadroAdapter(Acao acao) {
        this.acao = acao;
    }

    public void submit(List<Cartao> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    // Chamado a cada 30 segundos.
    // Atualiza os minutos e as cores dos cartões.
    public void atualizarTempos() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
        return new VH(
                LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_cartao_quadro, p, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {

        Cartao c = itens.get(pos);

        h.txtPosto.setText(c.postoNome);
        h.txtInsumo.setText(c.insumoNome);

        // Código do insumo
        if (c.insumoCodigo != null && !c.insumoCodigo.trim().isEmpty()) {
            h.txtCodigo.setText("Código: " + c.insumoCodigo);
            h.txtCodigo.setVisibility(View.VISIBLE);
        } else {
            h.txtCodigo.setVisibility(View.GONE);
        }

        // Unidade do insumo
        if (c.insumoUnidade != null && !c.insumoUnidade.trim().isEmpty()) {
            h.txtUnidade.setText("Unidade: " + c.insumoUnidade);
            h.txtUnidade.setVisibility(View.VISIBLE);
        } else {
            h.txtUnidade.setVisibility(View.GONE);
        }

        // Tempo de espera
        long minutos = c.minutosAberto();

        h.txtTempo.setText(
                "⏱ Aberto há " + minutos + " min"
        );

        aplicarCorTempo(h.txtTempo, minutos);

        // Barra lateral: continua indicando a urgência
        float r = Cores.dp(h.itemView.getContext(), 12);

        GradientDrawable barra = new GradientDrawable();

        if (Constantes.URGENCIA_ALTA.equals(c.urgencia)) {
            barra.setColor(Color.parseColor("#E53935"));
        } else if (Constantes.URGENCIA_MEDIA.equals(c.urgencia)) {
            barra.setColor(Color.parseColor("#FFB74D"));
        } else {
            barra.setColor(Color.parseColor("#43A047"));
        }

        barra.setCornerRadii(new float[]{
                r, r, 0, 0, 0, 0, r, r
        });

        h.barra.setBackground(barra);

        // Selo de urgência
        int fundo, texto;
        String rotulo;

        if (Constantes.URGENCIA_ALTA.equals(c.urgencia)) {
            fundo = Color.parseColor("#FDE3E3");
            texto = Color.parseColor("#C62828");
            rotulo = "Alta";
        } else if (Constantes.URGENCIA_MEDIA.equals(c.urgencia)) {
            fundo = Color.parseColor("#FFE8C2");
            texto = Color.parseColor("#B26A00");
            rotulo = "Média";
        } else {
            fundo = Color.parseColor("#DDF3E2");
            texto = Color.parseColor("#1B7A38");
            rotulo = "Baixa";
        }

        h.txtUrgencia.setText(rotulo);
        h.txtUrgencia.setTextColor(texto);
        h.txtUrgencia.setBackgroundTintList(
                ColorStateList.valueOf(fundo)
        );

        // Botão conforme a coluna
        if (Constantes.STATUS_ABERTO.equals(c.status)) {

            h.btnAcao.setVisibility(View.VISIBLE);
            h.txtAguardando.setVisibility(View.GONE);
            h.btnAcao.setText("Pegar e sair para entrega");

        } else if (Constantes.STATUS_EM_ROTA.equals(c.status)) {

            h.btnAcao.setVisibility(View.VISIBLE);
            h.txtAguardando.setVisibility(View.GONE);
            h.btnAcao.setText("Marcar como entregue");

        } else {

            // Entregue: só o operador pode concluir
            h.btnAcao.setVisibility(View.GONE);
            h.txtAguardando.setVisibility(View.VISIBLE);
        }

        h.btnAcao.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#F0F0F0"))
        );

        h.btnAcao.setOnClickListener(v -> {
            if (acao != null) {
                acao.aoMover(c);
            }
        });
    }

    // Muda a cor do indicador conforme o tempo de espera.
    private void aplicarCorTempo(TextView texto, long minutos) {

        int fundo;
        int corTexto;

        if (minutos <= 10) {

            // Verde: espera tranquila
            fundo = Color.parseColor("#DCFCE7");
            corTexto = Color.parseColor("#166534");

        } else if (minutos <= 20) {

            // Amarelo: atenção
            fundo = Color.parseColor("#FEF3C7");
            corTexto = Color.parseColor("#92400E");

        } else if (minutos <= 30) {

            // Laranja: espera elevada
            fundo = Color.parseColor("#FFEDD5");
            corTexto = Color.parseColor("#C2410C");

        } else {

            // Vermelho: espera crítica
            fundo = Color.parseColor("#FEE2E2");
            corTexto = Color.parseColor("#B91C1C");
        }

        GradientDrawable etiqueta = new GradientDrawable();
        etiqueta.setShape(GradientDrawable.RECTANGLE);
        etiqueta.setColor(fundo);
        etiqueta.setCornerRadius(
                Cores.dp(texto.getContext(), 20)
        );

        texto.setBackground(etiqueta);
        texto.setTextColor(corTexto);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        final View barra;

        final TextView txtPosto;
        final TextView txtUrgencia;
        final TextView txtInsumo;
        final TextView txtCodigo;
        final TextView txtUnidade;
        final TextView txtTempo;
        final TextView txtAguardando;

        final Button btnAcao;

        VH(@NonNull View v) {
            super(v);

            barra = v.findViewById(R.id.barra);

            txtPosto = v.findViewById(R.id.txtPosto);
            txtUrgencia = v.findViewById(R.id.txtUrgencia);
            txtInsumo = v.findViewById(R.id.txtInsumo);

            txtCodigo = v.findViewById(R.id.txtCodigo);
            txtUnidade = v.findViewById(R.id.txtUnidade);

            txtTempo = v.findViewById(R.id.txtTempo);
            txtAguardando = v.findViewById(R.id.txtAguardando);

            btnAcao = v.findViewById(R.id.btnAcao);
        }
    }
}
