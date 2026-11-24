package com.example.kanban.operador;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.example.kanban.auth.LoginActivity;
import com.example.kanban.core.Constantes;
import com.example.kanban.logistica.QuadroActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Contexto local da conta. A autorização de dados é feita pelas regras do Firestore. */
public final class Sessao {
    private Sessao() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("sessao", Context.MODE_PRIVATE);
    }
    public static String postoId(Context c) { return prefs(c).getString("postoId", ""); }
    public static String perfil(Context c) { return prefs(c).getString("perfil", ""); }
    public static void salvar(Context c, String perfil, String postoId) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        prefs(c).edit().putString("uid", u.getUid()).putString("perfil", perfil)
                .putString("postoId", postoId).apply();
    }
    public static boolean exigir(Activity a, String permitido) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        String perfil = perfil(a);
        boolean valida = u != null && u.getUid().equals(prefs(a).getString("uid", ""))
                && (Constantes.PERFIL_OPERADOR.equals(perfil) || Constantes.PERFIL_LOGISTICA.equals(perfil))
                && (!Constantes.PERFIL_OPERADOR.equals(perfil) || !postoId(a).isEmpty());
        if (!valida) {
            a.startActivity(new Intent(a, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            a.finish(); return false;
        }
        if (permitido != null && !permitido.equals(perfil)) { abrirInicio(a); return false; }
        return true;
    }
    public static void abrirInicio(Activity a) {
        Class<?> destino = Constantes.PERFIL_LOGISTICA.equals(perfil(a))
                ? QuadroActivity.class : OperadorHomeActivity.class;
        a.startActivity(new Intent(a, destino)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        a.finish();
    }
    public static void sair(Activity a) {
        FirebaseAuth.getInstance().signOut();
        prefs(a).edit().clear().apply();
        a.startActivity(new Intent(a, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        a.finish();
    }
}
