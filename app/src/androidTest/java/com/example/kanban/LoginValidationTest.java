package com.example.kanban;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.kanban.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Valida o formulário sem criar contas nem gravar no Firebase remoto. */
@RunWith(AndroidJUnit4.class)
public class LoginValidationTest {
    @Before public void semSessao() { FirebaseAuth.getInstance().signOut(); }
    @Test public void emailInvalidoNaoEnvia() {
        try (ActivityScenario<LoginActivity> tela = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.email)).perform(replaceText("email-invalido"), closeSoftKeyboard());
            onView(withId(R.id.entrar)).perform(scrollTo(), click());
            onView(withId(R.id.email)).check(matches(hasErrorText("Informe um e-mail válido.")));
        }
    }
    @Test public void cadastroExigeSenhaMinima() {
        try (ActivityScenario<LoginActivity> tela = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.email)).perform(replaceText("operador@example.com"));
            onView(withId(R.id.senha)).perform(replaceText("123"), closeSoftKeyboard());
            onView(withId(R.id.cadastrar)).perform(scrollTo(), click());
            onView(withId(R.id.senha)).check(matches(hasErrorText("Use pelo menos 6 caracteres.")));
        }
    }
    @Test public void recriarTelaPreservaDigitacao() {
        try (ActivityScenario<LoginActivity> tela = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.email)).perform(replaceText("operador@example.com"));
            onView(withId(R.id.senha)).perform(replaceText("senha-teste"), closeSoftKeyboard());
            tela.recreate();
            onView(withId(R.id.email)).check(matches(withText("operador@example.com")));
            onView(withId(R.id.senha)).check(matches(withText("senha-teste")));
        }
    }
}
