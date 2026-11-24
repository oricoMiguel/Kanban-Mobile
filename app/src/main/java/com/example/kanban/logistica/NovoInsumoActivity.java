
package com.example.kanban.logistica;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kanban.R;
import com.example.kanban.core.CartoesService;
import com.example.kanban.core.Ouvinte;
import com.example.kanban.core.Posto;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NovoInsumoActivity extends AppCompatActivity {

    private EditText txtNome, txtCodigo;
    private Spinner spUnidade;
    private LinearLayout listaPostos;
    private MaterialButton btnSalvar;

    private final List<Posto> postos = new ArrayList<>();
    private final List<String> postosSelecionados = new ArrayList<>();

    private ListenerRegistration reg;
    private boolean salvando = false;

    private final int PRETO = Color.parseColor("#111111");
    private final int CINZA = Color.parseColor("#777777");
    private final int BRANCO = Color.WHITE;
    private final int FUNDO_SELECIONADO = Color.parseColor("#E8E0F8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novo_insumo);

        View raiz = findViewById(R.id.raiz);

        int base = Cores.dp(this, 16);

        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            v.setPadding(
                    base + b.left,
                    base + b.top,
                    base + b.right,
                    base + b.bottom
            );

            return insets;
        });

        txtNome = findViewById(R.id.txtNome);
        txtCodigo = findViewById(R.id.txtCodigo);
        spUnidade = findViewById(R.id.spUnidade);
        listaPostos = findViewById(R.id.listaPostos);
        btnSalvar = findViewById(R.id.btnSalvar);

        // Cores dos campos de texto
        txtNome.setTextColor(PRETO);
        txtNome.setHintTextColor(CINZA);

        txtCodigo.setTextColor(PRETO);
        txtCodigo.setHintTextColor(CINZA);
        txtCodigo.setBackgroundTintList(
                ColorStateList.valueOf(CINZA)
        );

        configurarUnidades();

        findViewById(R.id.btnFechar).setOnClickListener(v -> finish());

        btnSalvar.setTextColor(PRETO);
        btnSalvar.setOnClickListener(v -> salvar());
    }

    private void configurarUnidades() {

        String[] unidades = {
                "Caixa",
                "Unidade",
                "Pacote",
                "Kg",
                "Litro",
                "Metro"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                unidades
        ) {

            @Override
            public View getView(int position, View convertView,
                                android.view.ViewGroup parent) {

                TextView texto = (TextView) super.getView(
                        position, convertView, parent
                );

                texto.setTextColor(PRETO);
                texto.setTextSize(14);
                texto.setBackgroundColor(BRANCO);

                return texto;
            }

            @Override
            public View getDropDownView(int position, View convertView,
                                        android.view.ViewGroup parent) {

                TextView texto = (TextView) super.getDropDownView(
                        position, convertView, parent
                );

                texto.setTextColor(PRETO);
                texto.setTextSize(14);
                texto.setBackgroundColor(BRANCO);
                texto.setPadding(
                        Cores.dp(NovoInsumoActivity.this, 12),
                        Cores.dp(NovoInsumoActivity.this, 12),
                        Cores.dp(NovoInsumoActivity.this, 12),
                        Cores.dp(NovoInsumoActivity.this, 12)
                );

                return texto;
            }
        };

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spUnidade.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        reg = CartoesService.get().escutarPostos(new Ouvinte<Posto>() {

            @Override
            public void aoReceber(List<Posto> itens) {

                postos.clear();

                for (Posto p : itens) {
                    if (p.ativo) {
                        postos.add(p);
                    }
                }

                montarPostos();
            }

            @Override
            public void aoErrar(Exception e) {

                Toast.makeText(
                        NovoInsumoActivity.this,
                        "Não foi possível carregar os postos.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }

    private void montarPostos() {

        listaPostos.removeAllViews();

        // Remove seleções de postos que não estão mais disponíveis.
        List<String> idsAtivos = new ArrayList<>();

        for (Posto p : postos) {
            idsAtivos.add(p.id);
        }

        postosSelecionados.retainAll(idsAtivos);

        for (Posto p : postos) {

            MaterialButton b = new MaterialButton(
                    this,
                    null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle
            );

            b.setText(p.nome);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setCheckable(true);
            b.setChecked(postosSelecionados.contains(p.id));

            aplicarAparenciaPosto(b);

            b.setOnClickListener(v -> {

                if (postosSelecionados.contains(p.id)) {
                    postosSelecionados.remove(p.id);
                } else {
                    postosSelecionados.add(p.id);
                }

                b.setChecked(postosSelecionados.contains(p.id));

                aplicarAparenciaPosto(b);
            });

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            -2,
                            Cores.dp(this, 40)
                    );

            lp.setMargins(
                    0,
                    0,
                    Cores.dp(this, 8),
                    0
            );

            listaPostos.addView(b, lp);
        }
    }

    private void aplicarAparenciaPosto(MaterialButton b) {

        b.setTextColor(PRETO);

        b.setStrokeColor(
                ColorStateList.valueOf(CINZA)
        );

        b.setBackgroundTintList(
                ColorStateList.valueOf(
                        b.isChecked() ? FUNDO_SELECIONADO : BRANCO
                )
        );
    }

    private boolean validarFormulario() {

        String nome = txtNome.getText().toString().trim();

        if (nome.isEmpty()) {
            txtNome.setError("Informe o nome do insumo.");
            txtNome.requestFocus();
            return false;
        }

        if (spUnidade.getSelectedItem() == null) {
            Toast.makeText(
                    this,
                    "Selecione uma unidade.",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (postosSelecionados.isEmpty()) {
            Toast.makeText(
                    this,
                    "Selecione pelo menos um posto.",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    private void salvar() {

        if (salvando) {
            return;
        }

        if (!validarFormulario()) {
            return;
        }

        String nome = txtNome.getText().toString().trim();
        String codigo = txtCodigo.getText().toString().trim();
        String unidade = spUnidade.getSelectedItem().toString();

        List<String> idsPostos =
                new ArrayList<>(postosSelecionados);

        salvando = true;
        btnSalvar.setEnabled(false);
        btnSalvar.setText("Salvando...");

        CartoesService.get()
                .criarInsumo(nome, codigo, unidade, idsPostos)
                .addOnSuccessListener(r -> {

                    Toast.makeText(
                            this,
                            "Insumo salvo com sucesso!",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    salvando = false;

                    btnSalvar.setEnabled(true);
                    btnSalvar.setText("Salvar insumo");

                    Toast.makeText(
                            this,
                            "Não foi possível salvar o insumo. Tente novamente.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
