
package com.example.kanban.logistica;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kanban.R;
import com.example.kanban.core.CartoesService;

public class NovoPostoActivity extends AppCompatActivity {

    private final int PRETO = Color.parseColor("#222222");
    private final int CINZA = Color.parseColor("#777777");
    private final int BRANCO = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novo_posto);

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

        EditText txtNome = findViewById(R.id.txtNome);
        Spinner spSetor = findViewById(R.id.spSetor);
        CheckBox chkAtivo = findViewById(R.id.chkAtivo);

        // Aparência do campo de nome
        txtNome.setTextColor(PRETO);
        txtNome.setHintTextColor(CINZA);

        // Aparência do Spinner
        configurarSetores(spSetor);

        // Aparência do CheckBox
        chkAtivo.setTextColor(PRETO);
        chkAtivo.setButtonTintList(
                ColorStateList.valueOf(Color.parseColor("#555555"))
        );

        findViewById(R.id.btnFechar).setOnClickListener(v -> finish());

        findViewById(R.id.btnSalvar).setOnClickListener(v -> {

            String nome = txtNome.getText().toString().trim();

            if (nome.isEmpty()) {
                txtNome.setError("Informe o nome do posto");
                txtNome.requestFocus();
                return;
            }

            if (spSetor.getSelectedItemPosition() == 0) {
                Toast.makeText(
                        this,
                        "Escolha um setor.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String setor = spSetor.getSelectedItem().toString();

            CartoesService.get()
                    .criarPosto(nome, setor, chkAtivo.isChecked())
                    .addOnSuccessListener(r -> {
                        Toast.makeText(
                                this,
                                "Posto salvo!",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(
                                this,
                                "Não foi possível salvar: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
        });
    }

    private void configurarSetores(Spinner spinner) {

        String[] setores = {
                "Escolha um setor",
                "Montagem",
                "Montagem final",
                "Soldagem",
                "Pintura",
                "Expedição",
                "Almoxarifado"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                setores
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {
                TextView texto = (TextView) super.getView(
                        position, convertView, parent
                );

                texto.setTextColor(PRETO);
                texto.setTextSize(14);
                texto.setBackgroundColor(BRANCO);

                return texto;
            }

            @Override
            public View getDropDownView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {
                TextView texto = (TextView) super.getDropDownView(
                        position, convertView, parent
                );

                texto.setTextColor(PRETO);
                texto.setTextSize(14);
                texto.setBackgroundColor(BRANCO);

                texto.setPadding(
                        Cores.dp(NovoPostoActivity.this, 12),
                        Cores.dp(NovoPostoActivity.this, 12),
                        Cores.dp(NovoPostoActivity.this, 12),
                        Cores.dp(NovoPostoActivity.this, 12)
                );

                return texto;
            }
        };

        spinner.setAdapter(adapter);
    }
}
