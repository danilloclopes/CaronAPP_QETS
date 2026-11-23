package me.br.caronapp.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.Test;

import me.br.caronapp.usuario.Usuario;

public class ConsoleTest {

    @Test
    void CT_U_C_01_construtor_deveInicializarConsole() {
        Console console = new Console();

        assertNull(console.getUser());
        assertNotNull(console.getScanner());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_02_logout_deveRemoverUsuarioEVoltarParaLoginRegistro() {
        Console console = new Console(new Scanner(""));

        console.setUser(mock(Usuario.class));
        console.setStage(Console.Stage.LOBBY);

        console.logout();

        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_03_logout_semUsuario_naoDeveLancarExcecao() {
        Console console = new Console(new Scanner(""));

        console.logout();

        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_04_setUserEGetUser_deveAssociarUsuario() {
        Console console = new Console(new Scanner(""));
        Usuario usuario = mock(Usuario.class);

        console.setUser(usuario);

        assertEquals(usuario, console.getUser());
    }

    @Test
    void CT_U_C_05_setUserNull_deveRemoverUsuario() {
        Console console = new Console(new Scanner(""));

        console.setUser(mock(Usuario.class));
        console.setUser(null);

        assertNull(console.getUser());
    }

    @Test
    void CT_U_C_06_setStage_deveAlterarEstagio() {
        Console console = new Console(new Scanner(""));

        console.setStage(Console.Stage.LOBBY);

        assertEquals(Console.Stage.LOBBY, console.getStage());
    }

    @Test
    void CT_U_C_07_getScanner_deveRetornarScannerValido() {
        Console console = new Console(new Scanner(""));

        assertNotNull(console.getScanner());
    }

    @Test
    void CT_U_C_08_isAdm_usuarioAdministrador_deveRetornarTrue() {
        Console console = new Console(new Scanner(""));
        Usuario usuario = mock(Usuario.class);
        when(usuario.isAdm()).thenReturn(true);

        console.setUser(usuario);

        assertTrue(console.isAdm());
    }

    @Test
    void CT_U_C_09_isAdm_usuarioComum_deveRetornarFalse() {
        Console console = new Console(new Scanner(""));

        console.setUser(mock(Usuario.class));

        assertFalse(console.isAdm());
    }

    @Test
    void CT_U_C_10_isAdm_semUsuario_deveRetornarFalse() {
        Console console = new Console(new Scanner(""));

        assertFalse(console.isAdm());
    }

    @Test
    void CT_U_C_11_draw_loginRegistro_opcaoLogin_deveIrParaLogin() {
        Console console = new Console(new Scanner("1\n"));

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.LOGIN, console.getStage());
    }

    @Test
    void CT_U_C_12_draw_loginRegistro_opcaoRegistro_deveIrParaRegistro() {
        Console console = new Console(new Scanner("2\n"));

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_13_draw_lobby_opcaoListarCaronas_deveIrParaListarCaronas() {
        Console console = new Console(new Scanner("1\n"));

        console.setUser(mock(Usuario.class));
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.LISTAR_CARONAS, console.getStage());
    }

    @Test
    void CT_U_C_14_draw_lobby_opcaoHostCarona_deveIrParaHostCarona() {
        Console console = new Console(new Scanner("2\n"));

        console.setUser(mock(Usuario.class));
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.HOST_CARONA, console.getStage());
    }

    @Test
    void CT_U_C_15_draw_lobby_opcaoEntrarCarona_deveIrParaEntrarCarona() {
        Console console = new Console(new Scanner("3\n"));

        console.setUser(mock(Usuario.class));
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.ENTRAR_CARONA, console.getStage());
    }

    @Test
    void CT_U_C_16_draw_lobby_opcaoDeslogar_deveIrParaDeslogar() {
        Console console = new Console(new Scanner("4\n"));
        Usuario usuario = mock(Usuario.class);

        console.setUser(usuario);
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.DESLOGAR, console.getStage());
        assertEquals(usuario, console.getUser());
    }

    @Test
    void CT_U_C_17_draw_deslogar_deveExecutarLogout() {
        Console console = new Console(new Scanner(""));

        console.setUser(mock(Usuario.class));
        console.setStage(Console.Stage.DESLOGAR);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_18_draw_halt_deveRetornarFalse() {
        Console console = new Console(new Scanner(""));

        console.setStage(Console.Stage.HALT);

        boolean resultado = console.draw();

        assertFalse(resultado);
        assertThrows(IllegalStateException.class, () -> console.getScanner().hasNext());
    }

    @Test
    void CT_U_C_19_draw_usuarioLogado_deveExibirNomeDoUsuario() throws Exception {
        Console console = new Console(new Scanner(""));
        Usuario usuario = mock(Usuario.class);
        when(usuario.getName()).thenReturn("Ana");
        console.setUser(usuario);
        console.setStage(Console.Stage.HALT);

        Logger logger = Logger.getLogger(Console.class.getName());
        ByteArrayOutputStream mensagens = new ByteArrayOutputStream();
        StreamHandler handler = new StreamHandler(mensagens, new SimpleFormatter());
        handler.setEncoding(StandardCharsets.UTF_8.name());
        logger.addHandler(handler);
        try {
            assertFalse(console.draw());
            handler.flush();
            String texto = mensagens.toString(StandardCharsets.UTF_8);
            assertTrue(texto.contains("Usuario: Ana"));
            assertTrue(texto.contains("Finalizando console..."));
        } finally {
            logger.removeHandler(handler);
            handler.close();
        }
    }
}
