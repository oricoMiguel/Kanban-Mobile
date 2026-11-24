
package com.example.kanban.core;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Cartao {

    @DocumentId public String id;

    public String postoId, postoNome;
    public String insumoId, insumoNome;
    public String insumoCodigo, insumoUnidade;

    public String urgencia;
    public String status;

    public Timestamp abertoEm, emRotaEm, entregueEm;

    public Cartao() {}

    public long minutosAberto() {
        return Constantes.minutosDesde(abertoEm);
    }

    public String cor() {
        return Constantes.corPorEspera(minutosAberto());
    }
}
