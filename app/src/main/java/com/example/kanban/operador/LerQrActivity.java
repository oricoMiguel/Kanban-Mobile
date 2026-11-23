package com.example.kanban.operador;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.kanban.R;
import com.example.kanban.core.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.CameraPreview;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import java.util.Collections;
import java.util.List;

public class LerQrActivity extends AppCompatActivity {
    private String codigo;
    private String reconhecido;
    private TextView resultado;
    private Button pedir;
    private BarcodeView camera;
    private boolean lendo, consultando;
    private final ActivityResultLauncher<String> permissao = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), permitida -> {
        if (permitida) iniciarCamera();
        else {
            lendo = false;
            mensagem("Permita o acesso à câmera para ler o QR ou digite o código do posto.", false);
        }
    });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Sessao.exigir(this, Constantes.PERFIL_OPERADOR)) return;
        setContentView(R.layout.activity_ler_qr);
        Visual.ajustarInsets(findViewById(R.id.raiz), null);
        resultado = findViewById(R.id.resultadoQr); pedir = findViewById(R.id.pedirNestePosto);
        camera = findViewById(R.id.cameraQr);
        camera.setDecoderFactory(new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));
        camera.addStateListener(new CameraPreview.StateListener() {
            @Override public void previewSized() {}
            @Override public void previewStarted() {}
            @Override public void previewStopped() {}
            @Override public void cameraClosed() {}
            @Override public void cameraError(Exception e) {
                pararCamera();
                mensagem("Não foi possível abrir a câmera. Tente novamente ou digite o código do posto.", false);
            }
        });
        findViewById(R.id.voltar).setOnClickListener(v -> finish());
        findViewById(R.id.abrirCamera).setOnClickListener(v -> {
            codigo = null; reconhecido = null; pedir.setEnabled(false);
            mensagem("Aponte a câmera para o QR do posto.", false);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                iniciarCamera();
            else permissao.launch(Manifest.permission.CAMERA);
        });
        findViewById(R.id.alternarCodigo).setOnClickListener(v -> {
            View entrada = findViewById(R.id.entradaManual);
            boolean mostrar = entrada.getVisibility() != View.VISIBLE;
            entrada.setVisibility(mostrar ? View.VISIBLE : View.GONE);
            if (mostrar) { pararCamera(); findViewById(R.id.codigoPosto).requestFocus(); }
        });
        findViewById(R.id.buscarCodigo).setOnClickListener(v -> consultar(((EditText) findViewById(R.id.codigoPosto)).getText().toString()));
        pedir.setOnClickListener(v -> { if (reconhecido != null) PedirReposicaoActivity.abrir(this, reconhecido); });
        if (state != null) {
            findViewById(R.id.entradaManual).setVisibility(state.getBoolean("manual") ? View.VISIBLE : View.GONE);
            if (state.getString("codigo") != null) consultar(state.getString("codigo"));
            else lendo = state.getBoolean("lendo");
        }
    }
    private void iniciarCamera() {
        lendo = true;
        camera.setVisibility(View.VISIBLE);
        findViewById(R.id.qrIdle).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.dicaCamera)).setText("Aponte para o QR do posto");
        camera.resume();
        camera.decodeSingle(new BarcodeCallback() {
            @Override public void barcodeResult(BarcodeResult r) { consultar(r.getText()); }
            @Override public void possibleResultPoints(List<ResultPoint> pontos) {}
        });
    }
    private void pararCamera() {
        lendo = false;
        if (camera != null) {
            camera.pause();
            camera.setVisibility(View.GONE);
            findViewById(R.id.qrIdle).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.dicaCamera)).setText("Toque para abrir a câmera");
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if (camera != null && lendo && !consultando) iniciarCamera();
    }
    @Override protected void onPause() {
        if (camera != null) camera.pause();
        super.onPause();
    }
    private void consultar(String texto) {
        codigo = texto; reconhecido = null; pedir.setEnabled(false);
        pararCamera();
        String id = CodigoPosto.extrair(texto);
        if (id == null) { mensagem("QR inválido. Use o código do posto ou kanban://posto/codigo.", false); return; }
        consultando = true; controles(false); mensagem("Consultando posto…", false);
        CartoesService.get().buscarPosto(id).addOnSuccessListener(p -> {
            if (isDestroyed()) return;
            consultando = false; controles(true);
            if (p == null || !p.ativo) { mensagem("Posto não encontrado ou inativo.", false); return; }
            reconhecido = p.id; mensagem("✓ QR reconhecido\n" + p.nome + " · " + p.setor, true); pedir.setEnabled(true);
            android.util.Log.d("TESTE_QR", "ID reconhecido: " + reconhecido);
        }).addOnFailureListener(e -> {
            if (isDestroyed()) return;
            consultando = false; controles(true);
            mensagem("Não foi possível consultar o posto. Confira a conexão e tente novamente.", false);
        });
    }
    private void mensagem(String texto, boolean sucesso) {
        resultado.setText(texto);
        resultado.setTextColor(Color.parseColor(sucesso ? "#075D12" : "#444444"));
        findViewById(R.id.faixaResultado).setBackgroundResource(sucesso ? R.drawable.bg_qr_success : R.drawable.bg_qr_idle);
    }
    private void controles(boolean ativo) {
        findViewById(R.id.abrirCamera).setEnabled(ativo); findViewById(R.id.buscarCodigo).setEnabled(ativo);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("codigo", codigo);
        out.putBoolean("lendo", lendo);
        out.putBoolean("manual", findViewById(R.id.entradaManual).getVisibility() == View.VISIBLE);
        super.onSaveInstanceState(out);
    }
}
