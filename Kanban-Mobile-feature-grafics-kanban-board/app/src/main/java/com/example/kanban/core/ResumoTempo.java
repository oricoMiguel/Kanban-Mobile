package com.example.kanban.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Resultado da tela 6 (tempo de atendimento)
public class ResumoTempo {
    public long mediaGeralMin;
    public int atendidos;
    public List<MediaPosto> postos = new ArrayList<>(); // mais lentos primeiro

    public static class MediaPosto {
        public String posto;
        public int cartoes;
        public long mediaMin;
    }

    /** Transforma a lista de atendimentos em média geral e médias por posto. */
    public static ResumoTempo calcular(List<Atendimento> lista) {
        Map<String, long[]> por = new LinkedHashMap<>(); // posto -> {soma, n}
        long soma = 0;
        for (Atendimento a : lista) {
            long[] acc = por.get(a.postoNome);
            if (acc == null) { acc = new long[2]; por.put(a.postoNome, acc); }
            acc[0] += a.minutosTotal;
            acc[1]++;
            soma += a.minutosTotal;
        }
        ResumoTempo r = new ResumoTempo();
        r.atendidos = lista.size();
        r.mediaGeralMin = lista.isEmpty() ? 0 : Math.round((double) soma / lista.size());
        for (Map.Entry<String, long[]> e : por.entrySet()) {
            MediaPosto m = new MediaPosto();
            m.posto = e.getKey();
            m.cartoes = (int) e.getValue()[1];
            m.mediaMin = Math.round((double) e.getValue()[0] / e.getValue()[1]);
            r.postos.add(m);
        }
        Collections.sort(r.postos, (a, b) -> Long.compare(b.mediaMin, a.mediaMin));
        return r;
    }
}