package com.example.kanban.core;

import com.google.firebase.firestore.DocumentId;

// Documento: postos/{postoId}
public class Posto {
    @DocumentId public String id;
    public String nome;   // "Posto 07"
    public String setor;  // "Montagem final"
    public boolean ativo = true;

    public Posto() {}
}