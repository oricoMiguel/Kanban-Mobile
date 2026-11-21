package com.example.kanban.operador;

import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kanban.R;
import com.example.kanban.core.*;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class LerQrActivity extends AppCompatActivity {
    private String codigo;
    private String reconhecido;
    private TextView resultado;
    private Button pedir;
    private final ActivityResultLauncher<ScanOptions> leitor = registerForActivityResult(new ScanContract(), r -> {
        if (r.getContents() == null) {
            codigo = null; reconhecido = null; pedir.setEnabled(false);
            resultado.setText("Leitura cancelada. Você pode tentar novamente ou digitar o código do posto.");
        } else consultar(r.getContents());
    });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Sessao.exigir(this, Constantes.PERFIL_OPERADOR)) return;
        setContentView(R.layout.activity_ler_qr);
        Visual.ajustarInsets(findViewById(R.id.raiz), null);
        NavInferior.configurar(this, NavInferior.QR);
        resultado = findViewById(R.id.resultadoQr); pedir = findViewById(R.id.pedirNestePosto);
        findViewById(R.id.voltar).setOnClickListener(v -> finish());
        findViewById(R.id.abrirCamera).setOnClickListener(v -> {
            codigo = null; reconhecido = null; pedir.setEnabled(false);
            ScanOptions options = new ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt("Aponte a câmera para o QR do posto").setBeepEnabled(false).setOrientationLocked(false);
            leitor.launch(options);
        });
        findViewById(R.id.buscarCodigo).setOnClickListener(v -> consultar(((EditText) findViewById(R.id.codigoPosto)).getText().toString()));
        pedir.setOnClickListener(v -> { if (reconhecido != null) PedirReposicaoActivity.abrir(this, reconhecido); });
        if (state != null && state.getString("codigo") != null) consultar(state.getString("codigo"));
    }
    private void consultar(String texto) {
        codigo = texto; reconhecido = null; pedir.setEnabled(false);
        String id = CodigoPosto.extrair(texto);
        if (id == null) { resultado.setText("QR inválido. Use o código do posto ou kanban://posto/codigo."); return; }
        controles(false); resultado.setText("Consultando posto…");
        CartoesService.get().buscarPosto(id).addOnSuccessListener(p -> {
            if (isDestroyed()) return;
            controles(true);
            if (p == null || !p.ativo) { resultado.setText("Posto não encontrado ou inativo."); return; }
            reconhecido = p.id; resultado.setText("✓ QR reconhecido\n" + p.nome + " · " + p.setor); pedir.setEnabled(true);
        }).addOnFailureListener(e -> {
            if (isDestroyed()) return;
            controles(true);
            resultado.setText("Não foi possível consultar o posto. Confira a conexão e tente novamente.");
        });
    }
    private void controles(boolean ativo) {
        findViewById(R.id.abrirCamera).setEnabled(ativo); findViewById(R.id.buscarCodigo).setEnabled(ativo);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("codigo", codigo); super.onSaveInstanceState(out);
    }
}
