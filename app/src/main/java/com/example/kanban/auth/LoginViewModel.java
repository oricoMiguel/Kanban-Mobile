package com.example.kanban.auth;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.kanban.core.Constantes;
import com.example.kanban.core.Posto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import java.util.ArrayList;
import java.util.List;

/** Mantém autenticação e carregamento em curso durante a rotação. */
public class LoginViewModel extends ViewModel {
    public final MutableLiveData<Integer> alteracao = new MutableLiveData<>(0);
    public boolean ocupado;
    public String erro = "", perfil = "";
    public final List<Posto> postos = new ArrayList<>();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private void avisar() { alteracao.setValue(alteracao.getValue() + 1); }
    public void restaurar() {
        if (auth.getCurrentUser() != null && !ocupado && perfil.isEmpty()) carregarPerfil();
    }
    public void autenticar(String email, String senha, boolean cadastro) {
        if (ocupado) return;
        ocupado = true; erro = ""; avisar();
        if (cadastro) {
            auth.createUserWithEmailAndPassword(email, senha).addOnSuccessListener(r -> {
                auth.signOut(); ocupado = false;
                erro = "Conta criada. Peça ao grupo para definir seu perfil em usuarios/" + r.getUser().getUid()
                        + " no Firebase. Depois, entre com seu e-mail e senha.";
                avisar();
            }).addOnFailureListener(this::falhou);
        } else {
            auth.signInWithEmailAndPassword(email, senha).addOnSuccessListener(r -> carregarPerfil())
                    .addOnFailureListener(this::falhou);
        }
    }
    private void carregarPerfil() {
        if (auth.getCurrentUser() == null) return;
        ocupado = true; erro = ""; avisar();
        db.collection("usuarios").document(auth.getCurrentUser().getUid()).get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    Object valorPerfil = doc.get("perfil");
                    String p = valorPerfil instanceof String ? (String) valorPerfil : "";
                    if (!Constantes.PERFIL_OPERADOR.equals(p) && !Constantes.PERFIL_LOGISTICA.equals(p)) {
                        auth.signOut(); ocupado = false;
                        erro = "Seu perfil ainda não foi liberado. Peça ao grupo para configurar usuarios/{uid}.";
                        avisar(); return;
                    }
                    if (Constantes.PERFIL_LOGISTICA.equals(p)) {
                        perfil = p; ocupado = false; avisar(); return;
                    }
                    db.collection(Constantes.COL_POSTOS).whereEqualTo("ativo", true).get(Source.SERVER)
                            .addOnSuccessListener(snap -> {
                                postos.clear(); postos.addAll(snap.toObjects(Posto.class));
                                postos.sort((a, b) -> String.valueOf(a.nome).compareToIgnoreCase(String.valueOf(b.nome)));
                                perfil = p; ocupado = false;
                                if (postos.isEmpty()) erro = "Nenhum posto ativo. Peça à logística para cadastrar um posto.";
                                avisar();
                            }).addOnFailureListener(this::falhou);
                }).addOnFailureListener(this::falhou);
    }
    private void falhou(Exception e) {
        ocupado = false;
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if (code.equals("ERROR_EMAIL_ALREADY_IN_USE")) erro = "Este e-mail já está cadastrado. Use Entrar.";
            else if (code.equals("ERROR_WEAK_PASSWORD")) erro = "Use uma senha com pelo menos 6 caracteres.";
            else if (code.equals("ERROR_OPERATION_NOT_ALLOWED")) erro = "Habilite E-mail/senha no Firebase Authentication.";
            else erro = "Não foi possível entrar. Confira o e-mail e a senha e tente novamente.";
        } else erro = "Não foi possível carregar o acesso. Confira a conexão e a configuração do Firebase e tente novamente.";
        avisar();
    }
}
