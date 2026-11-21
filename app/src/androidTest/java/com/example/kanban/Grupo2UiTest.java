package com.example.kanban;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.kanban.auth.LoginActivity;
import com.example.kanban.core.Cartao;
import com.example.kanban.core.Insumo;
import com.example.kanban.operador.InsumosPedidoAdapter;
import com.example.kanban.operador.Visual;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Confere seleção/indisponibilidade nos cards reais e gera prévias com dados locais de teste. */
public class Grupo2UiTest {
    @Test public void cardsRespeitamPedidoAtivoESelecao() throws Exception {
        FirebaseAuth.getInstance().signOut();
        AtomicReference<String> escolhido = new AtomicReference<>("");
        try (ActivityScenario<LoginActivity> tela = ActivityScenario.launch(LoginActivity.class)) {
            tela.onActivity(a -> {
                a.setContentView(R.layout.activity_pedir_reposicao);
                Visual.ajustarInsets(a.findViewById(R.id.raiz), null);
                ViewCompat.requestApplyInsets(a.findViewById(R.id.raiz));
                ((TextView) a.findViewById(R.id.postoPedido)).setText("Posto 07 · Montagem final");
                a.findViewById(R.id.statusPedido).setVisibility(View.GONE);
                ((RadioGroup) a.findViewById(R.id.urgenciaPedido)).check(R.id.urgenciaMedia);
                ((TextView) a.findViewById(R.id.ajudaUrgencia)).setText("Acaba nas próximas horas.");
                RecyclerView lista = a.findViewById(R.id.insumosPedido);
                lista.setLayoutManager(new GridLayoutManager(a, 2));
                InsumosPedidoAdapter adapter = new InsumosPedidoAdapter(escolhido::set);
                lista.setAdapter(adapter);
                Cartao graxa = new Cartao(); graxa.insumoId = "graxa";
                graxa.abertoEm = new Timestamp(new Date(System.currentTimeMillis() - 8 * 60000L));
                adapter.atualizar(Arrays.asList(insumo("parafuso", "Parafuso M6", "Caixa 500 un"),
                    insumo("arruela", "Arruela lisa", "Caixa 1000 un"),
                    insumo("conector", "Conector 12V", "Saco 100 un"), insumo("graxa", "Graxa", "Tubo")),
                    Collections.singletonList(graxa), "", false);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            tela.onActivity(a -> {
                RecyclerView lista = a.findViewById(R.id.insumosPedido);
                View primeiro = lista.findViewHolderForAdapterPosition(0).itemView;
                View duplicado = lista.findViewHolderForAdapterPosition(3).itemView;
                assertFalse(duplicado.isEnabled());
                assertTrue(primeiro.performClick());
                assertEquals("parafuso", escolhido.get());
            });
            captura("pedido-fixture.png");
            tela.onActivity(a -> {
                a.setContentView(R.layout.activity_ler_qr);
                Visual.ajustarInsets(a.findViewById(R.id.raiz), null);
                ViewCompat.requestApplyInsets(a.findViewById(R.id.raiz));
                a.findViewById(R.id.faixaResultado).setBackgroundResource(R.drawable.bg_qr_success);
                TextView resultado = a.findViewById(R.id.resultadoQr);
                resultado.setText("✓ QR reconhecido\nPosto 07 · Montagem final");
                resultado.setTextColor(Color.parseColor("#075D12"));
                a.findViewById(R.id.pedirNestePosto).setEnabled(true);
                ((TextView) a.findViewById(R.id.dicaCamera)).setText("Imagem da câmera");
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            captura("qr-fixture.png");
        }
    }
    private static Insumo insumo(String id, String nome, String unidade) {
        Insumo i = new Insumo(); i.id = id; i.nome = nome; i.unidade = unidade; return i;
    }
    private static void captura(String nome) throws Exception {
        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
        Bitmap imagem = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try (FileOutputStream out = new FileOutputStream(new File(dir, nome))) {
            imagem.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally { imagem.recycle(); }
    }
}
