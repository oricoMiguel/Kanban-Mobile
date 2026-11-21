package com.example.kanban.core;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Rode UMA vez (só um do grupo): SeedDados.executar(); depois apague a chamada. */
public final class SeedDados {
    private SeedDados() {}

    public static Task<Void> executar() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch b = db.batch();

        String[][] postos = {
                {"posto01", "Posto 01", "Pré-montagem"},
                {"posto03", "Posto 03", "Montagem inicial"},
                {"posto05", "Posto 05", "Embalagem"},
                {"posto07", "Posto 07", "Montagem final"},
                {"posto09", "Posto 09", "Testes"},
                {"posto12", "Posto 12", "Soldagem"},
        };
        for (String[] p : postos) {
            Map<String, Object> d = new HashMap<>();
            d.put("nome", p[1]);
            d.put("setor", p[2]);
            d.put("ativo", true);
            b.set(db.collection(Constantes.COL_POSTOS).document(p[0]), d);
        }

        insumo(b, db, "parafuso-m6", "Parafuso M6", "PAR-006", "Caixa",
                Arrays.asList("posto01", "posto03", "posto07", "posto12"));
        insumo(b, db, "arruela-lisa", "Arruela lisa", "ARR-001", "Caixa",
                Arrays.asList("posto03", "posto07", "posto12"));
        insumo(b, db, "conector-12v", "Conector 12V", "CON-012", "Saco",
                Arrays.asList("posto07", "posto09"));
        insumo(b, db, "graxa", "Graxa", "GRA-001", "Tubo",
                Arrays.asList("posto05", "posto07"));
        insumo(b, db, "etiqueta-lote", "Etiqueta de lote", "ETQ-001", "Rolo",
                Arrays.asList("posto05", "posto07"));

        return b.commit();
    }

    /** Dados de teste para a tela 6 (tempo de atendimento). Apague antes da demo. */
    public static Task<Void> executarAtendimentos() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch b = db.batch();

        // {postoId, postoNome, minutosTotal}
        Object[][] dados = {
                {"posto03", "Posto 03", 22L}, {"posto03", "Posto 03", 16L},
                {"posto12", "Posto 12", 11L}, {"posto12", "Posto 12", 9L},
                {"posto07", "Posto 07", 8L},  {"posto07", "Posto 07", 7L},
                {"posto01", "Posto 01", 6L},  {"posto05", "Posto 05", 4L},
        };
        for (Object[] d : dados) {
            Map<String, Object> m = new HashMap<>();
            m.put("postoId", d[0]);
            m.put("postoNome", d[1]);
            m.put("minutosTotal", d[2]);
            m.put("concluidoEm", Timestamp.now());
            b.set(db.collection(Constantes.COL_ATENDIMENTOS).document(), m);
        }
        return b.commit();
    }

    private static void insumo(WriteBatch b, FirebaseFirestore db, String id, String nome,
                               String codigo, String unidade, List<String> postoIds) {
        Map<String, Object> d = new HashMap<>();
        d.put("nome", nome);
        d.put("codigo", codigo);
        d.put("unidade", unidade);
        d.put("postoIds", postoIds);
        b.set(db.collection(Constantes.COL_INSUMOS).document(id), d);
    }


    /** Cria cartões de demonstração para o quadro de reposição. */
    public static Task<Void> executarCartoes() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch b = db.batch();

        adicionarCartao(b, db, "posto01", "Posto 01",
                "parafuso-m6", "Parafuso M6", "alta", "aberto", 25);

        adicionarCartao(b, db, "posto03", "Posto 03",
                "arruela-lisa", "Arruela lisa", "media", "aberto", 12);

        adicionarCartao(b, db, "posto05", "Posto 05",
                "graxa", "Graxa", "baixa", "aberto", 5);

        adicionarCartao(b, db, "posto07", "Posto 07",
                "conector-12v", "Conector 12V", "alta", "em_rota", 20);

        adicionarCartao(b, db, "posto09", "Posto 09",
                "conector-12v", "Conector 12V", "media", "entregue", 30);

        return b.commit();
    }

    private static void adicionarCartao(
            WriteBatch b,
            FirebaseFirestore db,
            String postoId,
            String postoNome,
            String insumoId,
            String insumoNome,
            String urgencia,
            String status,
            int minutos) {

        Timestamp aberto = new Timestamp(
                new java.util.Date(System.currentTimeMillis() - minutos * 60000L));

        Map<String, Object> d = new HashMap<>();
        d.put("postoId", postoId);
        d.put("postoNome", postoNome);
        d.put("insumoId", insumoId);
        d.put("insumoNome", insumoNome);
        d.put("urgencia", urgencia);
        d.put("status", status);
        d.put("abertoEm", aberto);

        d.put("emRotaEm",
                "aberto".equals(status) ? null : aberto);

        d.put("entregueEm",
                "entregue".equals(status) ? Timestamp.now() : null);

        b.set(
                db.collection(Constantes.COL_CARTOES)
                        .document(Constantes.idCartao(postoId, insumoId)),
                d
        );
    }

}