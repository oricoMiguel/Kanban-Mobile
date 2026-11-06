package com.example.kanban.core;

import java.util.List;

// Callback dos listeners em tempo real
public interface Ouvinte<T> {
    void aoReceber(List<T> itens);
    default void aoErrar(Exception e) {}
}