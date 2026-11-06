package com.example.kanban.core;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

// Documento: atendimentos/{autoId}  (cartões já concluídos)
public class Atendimento {
    @DocumentId public String id;
    public String postoId, postoNome, insumoId, insumoNome, urgencia;
    public Timestamp abertoEm, emRotaEm, entregueEm, concluidoEm;
    public long minutosTotal;

    public Atendimento() {}
}