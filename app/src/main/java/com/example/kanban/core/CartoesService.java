
package com.example.kanban.core;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreException.Code;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Normalizer;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class CartoesService {

    private static CartoesService instancia;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized CartoesService get() {
        if (instancia == null) {
            instancia = new CartoesService();
        }
        return instancia;
    }

    /**
     * True se o erro foi a recusa de cartão duplicado.
     */
    public static boolean ehDuplicado(Exception e) {
        return e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode()
                == Code.ALREADY_EXISTS;
    }

    /* ===================== OPERADOR ===================== */

    /**
     * Cria um cartão de reposição.
     */
    public Task<Void> criarCartao(
            String postoId,
            String postoNome,
            String insumoId,
            String insumoNome,
            String insumoCodigo,
            String insumoUnidade,
            String urgencia
    ) {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return Tasks.forException(
                    new Exception("Usuário não autenticado.")
            );
        }

        String operadorUid = usuario.getUid();

        DocumentReference ref = db.collection(Constantes.COL_CARTOES)
                .document(Constantes.idCartao(postoId, insumoId));

        return db.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(ref);

            if (snap.exists()) {
                throw new FirebaseFirestoreException(
                        "Já existe um pedido deste insumo neste posto.",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS
                );
            }

            Map<String, Object> d = new HashMap<>();

            d.put("postoId", postoId);
            d.put("postoNome", postoNome);

            d.put("insumoId", insumoId);
            d.put("insumoNome", insumoNome);
            d.put("insumoCodigo", insumoCodigo);
            d.put("insumoUnidade", insumoUnidade);

            d.put("urgencia", urgencia);
            d.put("status", Constantes.STATUS_ABERTO);

            d.put("operadorUid", operadorUid);

            d.put("abertoEm", FieldValue.serverTimestamp());
            d.put("emRotaEm", null);
            d.put("entregueEm", null);

            tx.set(ref, d);

            return null;
        });
    }

    /**
     * Confirma o recebimento e arquiva o cartão.
     */
    public Task<Void> confirmarRecebimento(
            String postoId,
            String insumoId
    ) {

        DocumentReference ref = db.collection(Constantes.COL_CARTOES)
                .document(Constantes.idCartao(postoId, insumoId));

        DocumentReference hist = db.collection(Constantes.COL_ATENDIMENTOS)
                .document();

        return db.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(ref);

            if (!snap.exists()) {
                throw new FirebaseFirestoreException(
                        "Cartão não encontrado.",
                        Code.NOT_FOUND
                );
            }

            if (!Constantes.STATUS_ENTREGUE.equals(
                    snap.getString("status"))) {

                throw new FirebaseFirestoreException(
                        "Só dá para confirmar depois que a logística entregar.",
                        Code.FAILED_PRECONDITION
                );
            }

            Timestamp agora = Timestamp.now();
            Timestamp abertoEm = snap.getTimestamp("abertoEm");

            long minutos = (agora.toDate().getTime()
                    - abertoEm.toDate().getTime()) / 60000;

            Map<String, Object> d = new HashMap<>(snap.getData());

            d.put("concluidoEm", agora);
            d.put("minutosTotal", minutos);

            tx.set(hist, d);
            tx.delete(ref);

            return null;
        });
    }

    /* ===================== LOGÍSTICA ===================== */

    /**
     * Altera o status do cartão na ordem correta.
     */
    public Task<Void> moverStatus(String postoId, String insumoId) {

        DocumentReference ref = db.collection(Constantes.COL_CARTOES)
                .document(Constantes.idCartao(postoId, insumoId));

        return db.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(ref);

            if (!snap.exists()) {
                throw new FirebaseFirestoreException(
                        "Cartão não encontrado.",
                        Code.NOT_FOUND
                );
            }

            String atual = snap.getString("status");
            String proximo;
            String campo;

            if (Constantes.STATUS_ABERTO.equals(atual)) {
                proximo = Constantes.STATUS_EM_ROTA;
                campo = "emRotaEm";

            } else if (Constantes.STATUS_EM_ROTA.equals(atual)) {
                proximo = Constantes.STATUS_ENTREGUE;
                campo = "entregueEm";

            } else {
                throw new FirebaseFirestoreException(
                        "Este cartão já foi entregue.",
                        Code.FAILED_PRECONDITION
                );
            }

            tx.update(
                    ref,
                    "status", proximo,
                    campo, FieldValue.serverTimestamp()
            );

            return null;
        });
    }

    /* ===================== TEMPO REAL ===================== */

    /**
     * Todos os cartões ativos, mais antigos primeiro.
     */
    public ListenerRegistration escutarQuadro(Ouvinte<Cartao> o) {

        return db.collection(Constantes.COL_CARTOES)
                .orderBy("abertoEm", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        o.aoErrar(e);
                        return;
                    }

                    if (snap != null) {
                        o.aoReceber(snap.toObjects(Cartao.class));
                    }
                });
    }

    /**
     * Cartões ativos de um posto.
     */
    public ListenerRegistration escutarCartoesDoPosto(
            String postoId,
            Ouvinte<Cartao> o
    ) {

        return db.collection(Constantes.COL_CARTOES)
                .whereEqualTo("postoId", postoId)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        o.aoErrar(e);
                        return;
                    }

                    if (snap != null) {
                        o.aoReceber(snap.toObjects(Cartao.class));
                    }
                });
    }

    /**
     * Cartões concluídos hoje de um posto.
     */
    public ListenerRegistration escutarConcluidosHoje(
            String postoId,
            Ouvinte<Atendimento> o
    ) {

        return db.collection(Constantes.COL_ATENDIMENTOS)
                .whereEqualTo("postoId", postoId)
                .whereGreaterThanOrEqualTo(
                        "concluidoEm",
                        new Timestamp(inicioDoDia(0))
                )
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

    /**
     * Um cartão ativo pelo identificador.
     */
    public ListenerRegistration escutarCartao(
            String postoId,
            String insumoId,
            Ouvinte<Cartao> o
    ) {

        return db.collection(Constantes.COL_CARTOES)
                .document(Constantes.idCartao(postoId, insumoId))
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        o.aoErrar(e);
                        return;
                    }

                    java.util.List<Cartao> lista = new java.util.ArrayList<>();

                    if (snap != null && snap.exists()) {
                        lista.add(snap.toObject(Cartao.class));
                    }

                    o.aoReceber(lista);
                });
    }

    /**
     * Busca um posto.
     */
    public Task<Posto> buscarPosto(String postoId) {

        return db.collection(Constantes.COL_POSTOS)
                .document(postoId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .continueWith(task ->
                        task.getResult().toObject(Posto.class)
                );
    }

    /**
     * Escuta todos os postos.
     */
    public ListenerRegistration escutarPostos(Ouvinte<Posto> o) {

        return db.collection(Constantes.COL_POSTOS)
                .orderBy("nome")
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        o.aoErrar(e);
                        return;
                    }

                    if (snap != null) {
                        o.aoReceber(snap.toObjects(Posto.class));
                    }
                });
    }

    /**
     * Insumos vinculados a um posto.
     */
    public ListenerRegistration escutarInsumosDoPosto(
            String postoId,
            Ouvinte<Insumo> o
    ) {

        return db.collection(Constantes.COL_INSUMOS)
                .whereArrayContains("postoIds", postoId)
                .addSnapshotListener((snap, e) -> {

                    if (e != null) {
                        o.aoErrar(e);
                        return;
                    }

                    if (snap != null) {
                        o.aoReceber(snap.toObjects(Insumo.class));
                    }
                });
    }

    /* ===================== MÉTRICAS ===================== */

    /**
     * dias = 0 (hoje), 7 ou 30.
     */
    public Task<ResumoTempo> tempoMedioPorPosto(int dias) {

        return db.collection(Constantes.COL_ATENDIMENTOS)
                .whereGreaterThanOrEqualTo(
                        "concluidoEm",
                        new Timestamp(inicioDoDia(dias))
                )
                .get()
                .continueWith(task -> {

                    Map<String, long[]> por = new LinkedHashMap<>();

                    long somaTotal = 0;
                    int total = 0;

                    for (QueryDocumentSnapshot d : task.getResult()) {

                        Atendimento a = d.toObject(Atendimento.class);

                        long[] acc = por.get(a.postoNome);

                        if (acc == null) {
                            acc = new long[2];
                            por.put(a.postoNome, acc);
                        }

                        acc[0] += a.minutosTotal;
                        acc[1] += 1;

                        somaTotal += a.minutosTotal;
                        total++;
                    }

                    ResumoTempo r = new ResumoTempo();

                    r.atendidos = total;
                    r.mediaGeralMin = total == 0
                            ? 0
                            : Math.round((double) somaTotal / total);

                    for (Map.Entry<String, long[]> en : por.entrySet()) {

                        ResumoTempo.MediaPosto m =
                                new ResumoTempo.MediaPosto();

                        m.posto = en.getKey();
                        m.cartoes = (int) en.getValue()[1];

                        m.mediaMin = Math.round(
                                (double) en.getValue()[0] / en.getValue()[1]
                        );

                        r.postos.add(m);
                    }

                    Collections.sort(
                            r.postos,
                            new Comparator<ResumoTempo.MediaPosto>() {
                                @Override
                                public int compare(
                                        ResumoTempo.MediaPosto a,
                                        ResumoTempo.MediaPosto b
                                ) {
                                    return Long.compare(
                                            b.mediaMin,
                                            a.mediaMin
                                    );
                                }
                            }
                    );

                    return r;
                });
    }

    /**
     * Atendimentos concluídos hoje, nos últimos 7 ou 30 dias.
     */
    public ListenerRegistration escutarAtendimentos(
            int dias,
            Ouvinte<Atendimento> o
    ) {

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

    /* ===================== CADASTROS ===================== */

    /**
     * Cria um posto com ID sequencial: posto01, posto02, posto03...
     */
    public Task<DocumentReference> criarPosto(
            String nome,
            String setor,
            boolean ativo
    ) {

        return db.collection(Constantes.COL_POSTOS)
                .get()
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    int maiorNumero = 0;

                    for (DocumentSnapshot doc :
                            task.getResult().getDocuments()) {

                        String id = doc.getId();

                        if (id.startsWith("posto")) {
                            try {
                                int numero = Integer.parseInt(
                                        id.substring(5)
                                );

                                maiorNumero = Math.max(
                                        maiorNumero,
                                        numero
                                );

                            } catch (NumberFormatException ignored) {
                                // Ignora IDs fora do padrão postoXX.
                            }
                        }
                    }

                    return criarPostoComId(
                            nome,
                            setor,
                            ativo,
                            maiorNumero + 1
                    );
                });
    }

    /**
     * Cria o posto sem sobrescrever um documento existente.
     * Se o ID já estiver ocupado, tenta o próximo número.
     */
    private Task<DocumentReference> criarPostoComId(
            String nome,
            String setor,
            boolean ativo,
            int numero
    ) {

        String postoId = String.format(
                Locale.ROOT,
                "posto%02d",
                numero
        );

        DocumentReference ref = db.collection(Constantes.COL_POSTOS)
                .document(postoId);

        return db.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(ref);

            if (snap.exists()) {
                return false;
            }

            Map<String, Object> d = new HashMap<>();

            d.put("nome", nome);
            d.put("setor", setor);
            d.put("ativo", ativo);

            tx.set(ref, d);

            return true;

        }).continueWithTask(task -> {

            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }

            if (Boolean.TRUE.equals(task.getResult())) {
                return Tasks.forResult(ref);
            }

            return criarPostoComId(
                    nome,
                    setor,
                    ativo,
                    numero + 1
            );
        });
    }

    /**
     * Atualiza um posto.
     */
    public Task<Void> atualizarPosto(
            String postoId,
            String nome,
            String setor,
            boolean ativo
    ) {

        Map<String, Object> d = new HashMap<>();

        d.put("nome", nome);
        d.put("setor", setor);
        d.put("ativo", ativo);

        return db.collection(Constantes.COL_POSTOS)
                .document(postoId)
                .update(d);
    }

    /**
     * Cria um insumo com ID baseado no nome.
     * Exemplo: Parafuso Teste -> parafuso-teste.
     */
    public Task<DocumentReference> criarInsumo(
            String nome,
            String codigo,
            String unidade,
            java.util.List<String> postoIds
    ) {

        String insumoId = gerarIdInsumo(nome);

        if (insumoId.isEmpty()) {
            return Tasks.forException(
                    new Exception("Informe um nome válido para o insumo.")
            );
        }

        DocumentReference ref = db.collection(Constantes.COL_INSUMOS)
                .document(insumoId);

        Map<String, Object> d = new HashMap<>();

        d.put("nome", nome.trim());
        d.put("codigo", codigo);
        d.put("unidade", unidade);
        d.put("postoIds", postoIds);

        return db.runTransaction(tx -> {

            DocumentSnapshot snap = tx.get(ref);

            if (snap.exists()) {
                throw new FirebaseFirestoreException(
                        "Já existe um insumo com esse nome. Verifique o cadastro.",
                        Code.ALREADY_EXISTS
                );
            }

            tx.set(ref, d);

            return true;

        }).continueWithTask(task -> {

            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }

            return Tasks.forResult(ref);
        });
    }

    /**
     * Transforma o nome em um ID amigável para o Firestore.
     */
    private String gerarIdInsumo(String nome) {

        if (nome == null || nome.trim().isEmpty()) {
            return "";
        }

        String id = Normalizer.normalize(
                nome.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        id = id.replaceAll("\\p{M}", "");
        id = id.replaceAll("[^a-z0-9]+", "-");
        id = id.replaceAll("(^-+|-+$)", "");

        return id;
    }

    /**
     * Adiciona insumo diretamente ao posto.
     */
    public Task<DocumentReference> criarInsumoNoPosto(
            String nome,
            String postoId
    ) {

        return criarInsumo(
                nome,
                "",
                "",
                Collections.singletonList(postoId)
        );
    }

    /**
     * Desvincula um insumo do posto.
     */
    public Task<Void> desvincularInsumoDoPosto(
            String insumoId,
            String postoId
    ) {

        return db.collection(Constantes.COL_INSUMOS)
                .document(insumoId)
                .update("postoIds", FieldValue.arrayRemove(postoId));
    }

    /**
     * Vincula um insumo ao posto.
     */
    public Task<Void> vincularInsumoAoPosto(
            String insumoId,
            String postoId
    ) {

        return db.collection(Constantes.COL_INSUMOS)
                .document(insumoId)
                .update("postoIds", FieldValue.arrayUnion(postoId));
    }

    /* ===================== UTIL ===================== */

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
