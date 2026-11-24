
package com.example.kanban.operador;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.kanban.core.*;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class PedidoViewModel extends ViewModel {

    public final MutableLiveData<Integer> alteracao =
            new MutableLiveData<>(0);

    public List<Insumo> insumos = new ArrayList<>();
    public List<Cartao> ativos = new ArrayList<>();

    public Posto posto;

    public String selecionado = "";
    public String urgencia = Constantes.URGENCIA_MEDIA;
    public String erro = "";

    public boolean enviando;
    public boolean enviado;
    public boolean carregouInsumos;
    public boolean carregouCartoes;

    private ListenerRegistration regInsumos;
    private ListenerRegistration regCartoes;

    private String postoId;

    private void avisar() {
        Integer valor = alteracao.getValue();
        alteracao.setValue(valor == null ? 1 : valor + 1);
    }

    public void iniciar(String id) {

        if (regInsumos != null) return;

        postoId = id;

        erro = "";
        carregouInsumos = false;
        carregouCartoes = false;
        posto = null;

        CartoesService s = CartoesService.get();

        s.buscarPosto(id)
                .addOnSuccessListener(p -> {

                    posto = p;

                    if (p == null || !p.ativo) {
                        erro = "Posto não encontrado ou inativo.";
                    }

                    avisar();
                })
                .addOnFailureListener(e ->
                        falha("Não foi possível carregar o posto. Confira a conexão e tente novamente.")
                );

        regInsumos = s.escutarInsumosDoPosto(
                id,
                new Ouvinte<Insumo>() {

                    @Override
                    public void aoReceber(List<Insumo> itens) {
                        insumos = itens;
                        carregouInsumos = true;
                        avisar();
                    }

                    @Override
                    public void aoErrar(Exception e) {
                        carregouInsumos = false;

                        android.util.Log.e(
                                "PedidoReposicao",
                                "Erro ao carregar insumos",
                                e
                        );

                        falha("Não foi possível carregar os insumos. Volte e tente novamente.");
                    }
                }
        );

        regCartoes = s.escutarCartoesDoPosto(
                id,
                new Ouvinte<Cartao>() {

                    @Override
                    public void aoReceber(List<Cartao> itens) {
                        ativos = itens;
                        carregouCartoes = true;
                        avisar();
                    }

                    @Override
                    public void aoErrar(Exception e) {
                        carregouCartoes = false;

                        android.util.Log.e(
                                "PedidoReposicao",
                                "Erro ao verificar pedidos",
                                e
                        );

                        falha("Não foi possível verificar os pedidos. Volte e tente novamente.");
                    }
                }
        );
    }

    public Cartao ativo(String insumoId) {

        for (Cartao c : ativos) {
            if (insumoId.equals(c.insumoId)) {
                return c;
            }
        }

        return null;
    }

    public boolean pronto() {
        return posto != null
                && posto.ativo
                && carregouInsumos
                && carregouCartoes;
    }

    public void enviar() {

        if (enviando || enviado) return;

        if (!pronto()) {
            falha("Aguarde o carregamento do posto e dos insumos.");
            return;
        }

        Insumo escolhido = null;

        for (Insumo i : insumos) {
            if (selecionado.equals(i.id)) {
                escolhido = i;
                break;
            }
        }

        if (escolhido == null) {
            falha("Escolha um insumo para pedir reposição.");
            return;
        }

        if (ativo(escolhido.id) != null) {
            falha("Já existe um pedido deste insumo neste posto.");
            return;
        }

        enviando = true;
        erro = "";
        avisar();

        CartoesService.get()
                .criarCartao(
                        postoId,
                        posto.nome,
                        escolhido.id,
                        escolhido.nome,
                        escolhido.codigo,
                        escolhido.unidade,
                        urgencia
                )
                .addOnSuccessListener(v -> {

                    enviando = false;
                    enviado = true;

                    avisar();
                })
                .addOnFailureListener(e -> {

                    enviando = false;

                    android.util.Log.e(
                            "PedidoReposicao",
                            "Erro ao criar cartão no Firestore",
                            e
                    );

                    falha(
                            CartoesService.ehDuplicado(e)
                                    ? "Já existe um pedido deste insumo neste posto."
                                    : "Não foi possível enviar. Confira o Logcat."
                    );
                });
    }

    private void falha(String texto) {
        erro = texto;
        avisar();
    }

    public void parar() {

        if (regInsumos != null) {
            regInsumos.remove();
            regInsumos = null;
        }

        if (regCartoes != null) {
            regCartoes.remove();
            regCartoes = null;
        }
    }

    @Override
    protected void onCleared() {
        parar();
    }
}
