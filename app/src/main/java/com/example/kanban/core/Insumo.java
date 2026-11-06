package com.example.kanban.core;

import com.google.firebase.firestore.DocumentId;
import java.util.ArrayList;
import java.util.List;

// Documento: insumos/{insumoId}
public class Insumo {
    @DocumentId public String id;
    public String nome;     // "Parafuso M6"
    public String codigo;   // "PAR-006"
    public String unidade;  // "Caixa"
    public List<String> postoIds = new ArrayList<>();

    public Insumo() {}
}