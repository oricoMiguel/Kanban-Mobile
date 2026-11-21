package com.example.kanban.core;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreException.Code;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

/**
 * TODAS as telas usam esta classe. Ninguém grava em "cartoes" direto.
 * Uso: CartoesService.get().criarCartao(...)
 */
public class CartoesService {
    private static CartoesService instancia;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized CartoesService get() {
        if (instancia == null) instancia = new CartoesService();
        return instancia;
    }

    /** True se o erro foi a recusa de cartão duplicado (C3). */
    public static boolean ehDuplicado(Exception e) {
        return e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode() == Code.ALREADY_EXISTS;
    }

    /* ===================== OPERADOR ===================== */

    /** C2 + C3. Falha com ALREADY_EXISTS se já houver cartão ativo do insumo no posto. */
    public Task<Void> criarCartao(String postoId, String postoNome,
                                  String insumoId, String insumoNome, String urgencia) {
        DocumentReference ref = db.collection(Constantes.COL_CARTOES)
                .document(Constantes.idCartao(postoId, insumoId));

        return db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(ref);
            if (snap.exists()) {
                throw new FirebaseFirestoreException(
                        "Já existe um cartão aberto para " + insumoNome + " neste posto.",
                        Code.ALREADY_EXISTS);
            }
            Map<String, Object> d = new HashMap<>();
            d.put("postoId", postoId);
            d.put("postoNome", postoNome);
            d.put("insumoId", insumoId);
            d.put("insumoNome", insumoNome);
            d.put("urgencia", urgencia);
            d.put("status", Constantes.STATUS_ABERTO);
            d.put("abertoEm", FieldValue.serverTimestamp());
            d.put("emRotaEm", null);
            d.put("entregueEm", null);
            tx.set(ref, d);
            return null;
        });
    }

    /** C5. Só vale se estiver "entregue". Arquiva em "atendimentos" e remove o ativo. */
    public Task<Void> confirmarRecebimento(String postoId, String insumoId) {
        DocumentReference ref = db.collection(Constantes.COL_CARTOES)
                .document(Constantes.idCartao(postoId, insumoId));
        DocumentReference hist = db.collection(Constantes.COL_ATENDIMENTOS).document();

        return db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(ref);
            if (!snap.exists())
                throw new FirebaseFirestoreException("Cartão não encontrado.", Code.NOT_FOUND);
            if (!Constantes.STATUS_ENTREGUE.equals(snap.getString("status")))
                throw new FirebaseFirestoreException(
                        "Só dá para confirmar depois que a logística entregar.",
                        Code.FAILED_PRECONDITION);

            Timestamp agora = Timestamp.now();
            Timestamp abertoEm = snap.getTimestamp("abertoEm");
            long minutos = (agora.toDate().getTime() - abertoEm.toDate().getTime()) / 60000;

            Map<String, Object> d = new HashMap<>(snap.getData());
            d.put("concluidoEm", agora);
            d.put("minutosTotal", minutos);
            tx.set(hist, d);
            tx.delete(ref);
            return null;
        });
    }

    /* ===================== LOGÍSTICA ===================== */

    /** C4. aberto -> em_rota -> entregue (a ordem é validada). */
    public Task<Void> moverStatus(String postoId, String insumoId) {
        DocumentReference ref = db.collection(Constantes.COL_CARTOES)
                .document(Constantes.idCartao(postoId, insumoId));

        return db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(ref);
            if (!snap.exists())
                throw new FirebaseFirestoreException("Cartão não encontrado.", Code.NOT_FOUND);

            String atual = snap.getString("status");
            String proximo, campo;
            if (Constantes.STATUS_ABERTO.equals(atual)) {
                proximo = Constantes.STATUS_EM_ROTA;  campo = "emRotaEm";
            } else if (Constantes.STATUS_EM_ROTA.equals(atual)) {
                proximo = Constantes.STATUS_ENTREGUE; campo = "entregueEm";
            } else {
                throw new FirebaseFirestoreException(
                        "Este cartão já foi entregue.", Code.FAILED_PRECONDITION);
            }
            tx.update(ref, "status", proximo, campo, FieldValue.serverTimestamp());
            return null;
        });
    }

    /* ===================== TEMPO REAL ===================== */
    // Todos retornam ListenerRegistration: chame .remove() no onStop/onDestroy.

    /** Tela 12: todos os cartões ativos, mais antigos primeiro. */
    public ListenerRegistration escutarQuadro(Ouvinte<Cartao> o) {
        return db.collection(Constantes.COL_CARTOES)
                .orderBy("abertoEm", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { o.aoErrar(e); return; }
                    if (snap != null) o.aoReceber(snap.toObjects(Cartao.class));
                });
    }

    /** Telas 1, 2, 7: cartões ativos de um posto. */
    public ListenerRegistration escutarCartoesDoPosto(String postoId, Ouvinte<Cartao> o) {
        return db.collection(Constantes.COL_CARTOES)
                .whereEqualTo("postoId", postoId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { o.aoErrar(e); return; }
                    if (snap != null) o.aoReceber(snap.toObjects(Cartao.class));
                });
    }

    /** Telas 2 e 7: concluídos hoje de um posto. */
    public ListenerRegistration escutarConcluidosHoje(String postoId, Ouvinte<Atendimento> o) {
        return db.collection(Constantes.COL_ATENDIMENTOS)
                .whereEqualTo("postoId", postoId)
                .whereGreaterThanOrEqualTo("concluidoEm", new Timestamp(inicioDoDia(0)))
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { o.aoErrar(e); return; }
                    if (snap != null) o.aoReceber(snap.toObjects(Atendimento.class));
                });
    }

    /** Tela 4. */
    public ListenerRegistration escutarPostos(Ouvinte<Posto> o) {
        return db.collection(Constantes.COL_POSTOS).orderBy("nome")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { o.aoErrar(e); return; }
                    if (snap != null) o.aoReceber(snap.toObjects(Posto.class));
                });
    }

    /** Telas 3 e 10: insumos vinculados a um posto. */
    public ListenerRegistration escutarInsumosDoPosto(String postoId, Ouvinte<Insumo> o) {
        return db.collection(Constantes.COL_INSUMOS)
                .whereArrayContains("postoIds", postoId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { o.aoErrar(e); return; }
                    if (snap != null) o.aoReceber(snap.toObjects(Insumo.class));
                });
    }

    /* ===================== MÉTRICAS (tela 6) ===================== */

    /** dias = 0 (hoje), 7 ou 30. */
    public Task<ResumoTempo> tempoMedioPorPosto(int dias) {
        return db.collection(Constantes.COL_ATENDIMENTOS)
                .whereGreaterThanOrEqualTo("concluidoEm", new Timestamp(inicioDoDia(dias)))
                .get()
                .continueWith(task -> {
                    Map<String, long[]> por = new LinkedHashMap<>(); // posto -> {soma, n}
                    long somaTotal = 0;
                    int total = 0;
                    for (QueryDocumentSnapshot d : task.getResult()) {
                        Atendimento a = d.toObject(Atendimento.class);
                        long[] acc = por.get(a.postoNome);
                        if (acc == null) { acc = new long[2]; por.put(a.postoNome, acc); }
                        acc[0] += a.minutosTotal;
                        acc[1] += 1;
                        somaTotal += a.minutosTotal;
                        total++;
                    }
                    ResumoTempo r = new ResumoTempo();
                    r.atendidos = total;
                    r.mediaGeralMin = total == 0 ? 0 : Math.round((double) somaTotal / total);
                    for (Map.Entry<String, long[]> en : por.entrySet()) {
                        ResumoTempo.MediaPosto m = new ResumoTempo.MediaPosto();
                        m.posto = en.getKey();
                        m.cartoes = (int) en.getValue()[1];
                        m.mediaMin = Math.round((double) en.getValue()[0] / en.getValue()[1]);
                        r.postos.add(m);
                    }
                    Collections.sort(r.postos, new Comparator<ResumoTempo.MediaPosto>() {
                        @Override public int compare(ResumoTempo.MediaPosto a, ResumoTempo.MediaPosto b) {
                            return Long.compare(b.mediaMin, a.mediaMin);
                        }
                    });
                    return r;
                });
    }

    /** Tela 6 em tempo real: atendimentos concluídos hoje (0), 7 ou 30 dias. */
    public ListenerRegistration escutarAtendimentos(int dias, Ouvinte<Atendimento> o) {
        int diasAtras = dias == 0 ? 0 : dias - 1;

        Timestamp inicio = new Timestamp(inicioDoDia(diasAtras));
        Timestamp fim = Timestamp.now();

        return db.collection(Constantes.COL_ATENDIMENTOS)
                .whereGreaterThanOrEqualTo("concluidoEm", inicio)
                .whereLessThanOrEqualTo("concluidoEm", fim)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        o.aoErrar(e);
                        return;
                    }

                    if (snap != null) {
                        o.aoReceber(snap.toObjects(Atendimento.class));
                    }
                });
    }

    /* ===================== CADASTROS (C1) ===================== */

    /** Tela 9. */
    public Task<DocumentReference> criarPosto(String nome, String setor, boolean ativo) {
        Map<String, Object> d = new HashMap<>();
        d.put("nome", nome);
        d.put("setor", setor);
        d.put("ativo", ativo);
        return db.collection(Constantes.COL_POSTOS).add(d);
    }

    public Task<Void> atualizarPosto(String postoId, String nome, String setor, boolean ativo) {
        Map<String, Object> d = new HashMap<>();
        d.put("nome", nome);
        d.put("setor", setor);
        d.put("ativo", ativo);
        return db.collection(Constantes.COL_POSTOS).document(postoId).update(d);
    }

    /** Tela 8: novo insumo usado em vários postos. */
    public Task<DocumentReference> criarInsumo(String nome, String codigo, String unidade,
                                               java.util.List<String> postoIds) {
        Map<String, Object> d = new HashMap<>();
        d.put("nome", nome);
        d.put("codigo", codigo);
        d.put("unidade", unidade);
        d.put("postoIds", postoIds);
        return db.collection(Constantes.COL_INSUMOS).add(d);
    }

    /** Tela 3: adicionar insumo pelo nome direto no posto. */
    public Task<DocumentReference> criarInsumoNoPosto(String nome, String postoId) {
        return criarInsumo(nome, "", "", Collections.singletonList(postoId));
    }

    /** Tela 3: botão de lixeira (desvincula, não apaga o insumo). */
    public Task<Void> desvincularInsumoDoPosto(String insumoId, String postoId) {
        return db.collection(Constantes.COL_INSUMOS).document(insumoId)
                .update("postoIds", FieldValue.arrayRemove(postoId));
    }

    public Task<Void> vincularInsumoAoPosto(String insumoId, String postoId) {
        return db.collection(Constantes.COL_INSUMOS).document(insumoId)
                .update("postoIds", FieldValue.arrayUnion(postoId));
    }

    /* ===================== util ===================== */

    private static java.util.Date inicioDoDia(int diasAtras) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -diasAtras);
        return c.getTime();
    }
}