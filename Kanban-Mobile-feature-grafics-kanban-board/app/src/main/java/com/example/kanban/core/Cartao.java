package com.example.kanban.core;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

// Documento: cartoes/{postoId_insumoId}
public class Cartao {
    @DocumentId public String id;
    public String postoId, postoNome, insumoId, insumoNome;
    public String urgencia; // baixa | media | alta
    public String status;   // aberto | em_rota | entregue
    public Timestamp abertoEm, emRotaEm, entregueEm;

    public Cartao() {} // obrigatório para o Firestore

    public long minutosAberto() { return Constantes.minutosDesde(abertoEm); }
    public String cor() { return Constantes.corPorEspera(minutosAberto()); }
}