
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

    private EditText nome, matricula, email, senha;
    private Button entrar, cadastrar, continuar;
    private TextView mensagem;
    private Spinner posto;

    private String selecionado = "";
    private boolean navegou;
    private boolean modoCadastro;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_login);

        Visual.ajustarInsets(findViewById(R.id.raiz), null);

        model = new ViewModelProvider(this).get(LoginViewModel.class);

        nome = findViewById(R.id.nome);
        matricula = findViewById(R.id.matricula);
        email = findViewById(R.id.email);
        senha = findViewById(R.id.senha);

        entrar = findViewById(R.id.entrar);
        cadastrar = findViewById(R.id.cadastrar);
        continuar = findViewById(R.id.continuar);

        mensagem = findViewById(R.id.mensagem);
        posto = findViewById(R.id.posto);

        selecionado = state == null
                ? Sessao.postoId(this)
                : state.getString("postoSelecionado", "");
        if (state != null && state.getBoolean("modoCadastro", false)) {
            alternarModoCadastro(true);
        }

        entrar.setOnClickListener(v -> {
            if (modoCadastro) {
                alternarModoCadastro(false);
            } else {
                autenticar(false);
            }
        });

        cadastrar.setOnClickListener(v -> {
            if (!modoCadastro) {
                alternarModoCadastro(true);
            } else {
                autenticar(true);
            }
        });

        findViewById(R.id.sair).setOnClickListener(
                v -> Sessao.sair(this)
        );

        continuar.setOnClickListener(v -> {
            int pos = posto.getSelectedItemPosition();

            if (pos >= 0 && pos < model.postos.size()) {
                concluir(model.postos.get(pos).id);
            }
        });

        model.alteracao.observe(this, n -> renderizar());

        model.restaurar();
        renderizar();
    }

    private void alternarModoCadastro(boolean ativar) {
        modoCadastro = ativar;

        findViewById(R.id.dadosCadastro).setVisibility(
                ativar ? View.VISIBLE : View.GONE
        );

        entrar.setText(ativar ? "Voltar" : "Entrar");

        cadastrar.setText(
                ativar ? "Finalizar cadastro" : "Criar conta"
        );

        mensagem.setVisibility(View.GONE);
        mensagem.setText("");

        if (ativar) {
            nome.requestFocus();
        }
    }

    private void autenticar(boolean cadastro) {
        String e = email.getText().toString().trim();
        String s = senha.getText().toString();

        if (cadastro) {
            String n = nome.getText().toString().trim();
            String m = matricula.getText().toString().trim();

            if (n.isEmpty()) {
                nome.setError("Informe seu nome completo.");
                nome.requestFocus();
                return;
            }

            if (m.isEmpty()) {
                matricula.setError("Informe sua matrícula.");
                matricula.requestFocus();
                return;
            }

            model.validarCadastro(n, m, e, s);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            email.setError("Informe um e-mail válido.");
            return;
        }

        if (s.isEmpty()) {
            senha.setError("Informe a senha.");
            return;
        }

        model.autenticar(e, s, false);
    }

    private void renderizar() {
        boolean pronto = !model.perfil.isEmpty();

        findViewById(R.id.perfilOperador).setSelected(
                Constantes.PERFIL_OPERADOR.equals(model.perfil)
        );

        findViewById(R.id.perfilLogistica).setSelected(
                Constantes.PERFIL_LOGISTICA.equals(model.perfil)
        );

        findViewById(R.id.credenciais).setVisibility(
                pronto ? View.GONE : View.VISIBLE
        );

        entrar.setVisibility(pronto ? View.GONE : View.VISIBLE);
        cadastrar.setVisibility(pronto ? View.GONE : View.VISIBLE);

        continuar.setVisibility(pronto ? View.VISIBLE : View.GONE);

        findViewById(R.id.sair).setVisibility(
                pronto ? View.VISIBLE : View.GONE
        );

        posto.setEnabled(pronto);

        entrar.setEnabled(!model.ocupado);
        cadastrar.setEnabled(!model.ocupado);

        findViewById(R.id.progresso).setVisibility(
                model.ocupado ? View.VISIBLE : View.GONE
        );

        mensagem.setText(model.erro);

        mensagem.setVisibility(
                model.erro.isEmpty() ? View.GONE : View.VISIBLE
        );

        if (!pronto) {
            posto.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"Entre para escolher um posto"}
            ));
            return;
        }

        if (Constantes.PERFIL_LOGISTICA.equals(model.perfil)) {
            concluir("");
            return;
        }

        ArrayList<String> nomes = new ArrayList<>();

        for (Posto p : model.postos) {
            nomes.add(p.nome + " · " + p.setor);
        }

        posto.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                nomes
        ));

        for (int i = 0; i < model.postos.size(); i++) {
            if (model.postos.get(i).id.equals(selecionado)) {
                posto.setSelection(i);
            }
        }

        continuar.setEnabled(!model.postos.isEmpty());
    }

    private void concluir(String id) {
        if (navegou) return;

        navegou = true;

        Sessao.salvar(this, model.perfil, id);
        Sessao.abrirInicio(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        int pos = posto.getSelectedItemPosition();

        if (pos >= 0 && pos < model.postos.size()) {
            selecionado = model.postos.get(pos).id;
        }

        out.putString("postoSelecionado", selecionado);
        out.putBoolean("modoCadastro", modoCadastro);

        super.onSaveInstanceState(out);
    }
}
