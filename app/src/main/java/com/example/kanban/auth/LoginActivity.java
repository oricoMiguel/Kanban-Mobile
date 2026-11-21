package com.example.kanban.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.kanban.R;
import com.example.kanban.core.Constantes;
import com.example.kanban.core.Posto;
import com.example.kanban.operador.Sessao;
import com.example.kanban.operador.Visual;
import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {
    private LoginViewModel model;
    private EditText email, senha;
    private Button entrar, cadastrar, continuar;
    private TextView mensagem;
    private Spinner posto;
    private String selecionado = "";
    private boolean navegou;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_login);
        Visual.ajustarInsets(findViewById(R.id.raiz), null);
        model = new ViewModelProvider(this).get(LoginViewModel.class);
        email = findViewById(R.id.email); senha = findViewById(R.id.senha);
        entrar = findViewById(R.id.entrar); cadastrar = findViewById(R.id.cadastrar);
        continuar = findViewById(R.id.continuar);
        mensagem = findViewById(R.id.mensagem); posto = findViewById(R.id.posto);
        selecionado = state == null ? Sessao.postoId(this) : state.getString("postoSelecionado", "");
        entrar.setOnClickListener(v -> autenticar(false));
        cadastrar.setOnClickListener(v -> autenticar(true));
        findViewById(R.id.sair).setOnClickListener(v -> Sessao.sair(this));
        continuar.setOnClickListener(v -> {
            int pos = posto.getSelectedItemPosition();
            if (pos >= 0 && pos < model.postos.size()) concluir(model.postos.get(pos).id);
        });
        model.alteracao.observe(this, n -> renderizar());
        model.restaurar();
    }
    private void autenticar(boolean cadastro) {
        String e = email.getText().toString().trim(), s = senha.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) { email.setError("Informe um e-mail válido."); return; }
        if (s.isEmpty() || (cadastro && s.length() < 6)) {
            senha.setError(cadastro ? "Use pelo menos 6 caracteres." : "Informe a senha."); return;
        }
        model.autenticar(e, s, cadastro);
    }
    private void renderizar() {
        boolean pronto = !model.perfil.isEmpty();
        findViewById(R.id.credenciais).setVisibility(pronto ? View.GONE : View.VISIBLE);
        findViewById(R.id.escolherPosto).setVisibility(pronto ? View.VISIBLE : View.GONE);
        entrar.setEnabled(!model.ocupado); cadastrar.setEnabled(!model.ocupado);
        findViewById(R.id.progresso).setVisibility(model.ocupado ? View.VISIBLE : View.GONE);
        mensagem.setText(model.erro); mensagem.setVisibility(model.erro.isEmpty() ? View.GONE : View.VISIBLE);
        if (!pronto) return;
        if (Constantes.PERFIL_LOGISTICA.equals(model.perfil)) { concluir(""); return; }
        ArrayList<String> nomes = new ArrayList<>();
        for (Posto p : model.postos) nomes.add(p.nome + " · " + p.setor);
        posto.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nomes));
        for (int i = 0; i < model.postos.size(); i++) if (model.postos.get(i).id.equals(selecionado)) posto.setSelection(i);
        continuar.setEnabled(!model.postos.isEmpty());
    }
    private void concluir(String id) {
        if (navegou) return;
        navegou = true;
        Sessao.salvar(this, model.perfil, id);
        Sessao.abrirInicio(this);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        int pos = posto.getSelectedItemPosition();
        if (pos >= 0 && pos < model.postos.size()) selecionado = model.postos.get(pos).id;
        out.putString("postoSelecionado", selecionado);
        super.onSaveInstanceState(out);
    }
}
