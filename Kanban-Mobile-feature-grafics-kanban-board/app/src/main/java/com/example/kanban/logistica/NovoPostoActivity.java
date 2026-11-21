package com.example.kanban.logistica;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kanban.R;
import com.example.kanban.core.CartoesService;

/** Tela de cadastro de posto de trabalho. */
public class NovoPostoActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novo_posto);

        View raiz = findViewById(R.id.raiz);
        int base = Cores.dp(this, 16);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(base + b.left, base + b.top, base + b.right, base + b.bottom);
            return insets;
        });

        EditText txtNome = findViewById(R.id.txtNome);
        Spinner spSetor = findViewById(R.id.spSetor);
        CheckBox chkAtivo = findViewById(R.id.chkAtivo);

        String[] setores = {"Escolha um setor", "Montagem", "Montagem final", "Soldagem", "Pintura", "Expedição", "Almoxarifado"};
        spSetor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, setores));

        findViewById(R.id.btnFechar).setOnClickListener(v -> finish());
        findViewById(R.id.btnSalvar).setOnClickListener(v -> {
            String nome = txtNome.getText().toString().trim();
            if (nome.isEmpty()) { txtNome.setError("Informe o nome do posto"); txtNome.requestFocus(); return; }
            String setor = spSetor.getSelectedItem().toString();
            if (spSetor.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Escolha um setor.", Toast.LENGTH_SHORT).show(); return;
            }
            CartoesService.get().criarPosto(nome, setor, chkAtivo.isChecked())
                    .addOnSuccessListener(r -> { Toast.makeText(this, "Posto salvo!", Toast.LENGTH_SHORT).show(); finish(); })
                    .addOnFailureListener(e -> Toast.makeText(this, "Não foi possível salvar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}
