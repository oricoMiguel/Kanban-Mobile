package com.example.kanban.logistica;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Tela de cadastro de insumo, baseada no fluxo C1 do projeto. */
public class NovoInsumoActivity extends AppCompatActivity {
    private EditText txtNome, txtCodigo;
    private Spinner spUnidade;
    private LinearLayout listaPostos;
    private final List<Posto> postos = new ArrayList<>();
    private final List<String> postosSelecionados = new ArrayList<>();
    private ListenerRegistration reg;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novo_insumo);

        View raiz = findViewById(R.id.raiz);
        int base = Cores.dp(this, 16);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(base + b.left, base + b.top, base + b.right, base + b.bottom);
            return insets;
        });

        txtNome = findViewById(R.id.txtNome);
        txtCodigo = findViewById(R.id.txtCodigo);
        spUnidade = findViewById(R.id.spUnidade);
        listaPostos = findViewById(R.id.listaPostos);

        String[] unidades = {"Caixa", "Unidade", "Pacote", "Kg", "Litro", "Metro"};
        spUnidade.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, unidades));

        findViewById(R.id.btnFechar).setOnClickListener(v -> finish());
        findViewById(R.id.btnSalvar).setOnClickListener(v -> salvar());
    }

    @Override protected void onStart() {
        super.onStart();
        reg = CartoesService.get().escutarPostos(new Ouvinte<Posto>() {
            @Override public void aoReceber(List<Posto> itens) {
                postos.clear();
                for (Posto p : itens) if (p.ativo) postos.add(p);
                montarPostos();
            }
            @Override public void aoErrar(Exception e) {
                Toast.makeText(NovoInsumoActivity.this, "Erro ao carregar postos: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override protected void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void montarPostos() {
        listaPostos.removeAllViews();
        for (Posto p : postos) {
            MaterialButton b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setText(p.nome);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setCheckable(true);
            b.setChecked(postosSelecionados.contains(p.id));
            b.setOnClickListener(v -> {
                if (postosSelecionados.contains(p.id)) postosSelecionados.remove(p.id);
                else postosSelecionados.add(p.id);
                b.setChecked(postosSelecionados.contains(p.id));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, Cores.dp(this, 40));
            lp.setMargins(0, 0, Cores.dp(this, 8), 0);
            listaPostos.addView(b, lp);
        }
    }

    private void salvar() {
        String nome = txtNome.getText().toString().trim();
        String codigo = txtCodigo.getText().toString().trim();
        if (nome.isEmpty()) { txtNome.setError("Informe o nome do insumo"); txtNome.requestFocus(); return; }
        if (postosSelecionados.isEmpty()) {
            Toast.makeText(this, "Selecione pelo menos um posto.", Toast.LENGTH_SHORT).show();
            return;
        }
        String unidade = spUnidade.getSelectedItem().toString();
        CartoesService.get().criarInsumo(nome, codigo, unidade, new ArrayList<>(postosSelecionados))
                .addOnSuccessListener(r -> { Toast.makeText(this, "Insumo salvo!", Toast.LENGTH_SHORT).show(); finish(); })
                .addOnFailureListener(e -> Toast.makeText(this, "Não foi possível salvar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
