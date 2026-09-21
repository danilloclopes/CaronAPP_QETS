package me.br.caronapp.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.br.caronapp.usuario.Usuario;

public class ConsoleTest {

    private InputStream entradaOriginal;
    private PrintStream saidaOriginal;

    @BeforeEach
    void setUp() {
        entradaOriginal = System.in;
        saidaOriginal = System.out;
    }

    @AfterEach
    void tearDown() {
        System.setIn(entradaOriginal);
        System.setOut(saidaOriginal);
    }

    private Console consoleComEntrada(String entrada) {
        System.setIn(new ByteArrayInputStream(
                entrada.getBytes(StandardCharsets.UTF_8)
        ));

        return new Console();
    }

    private Usuario usuarioValido() {
        return new Usuario(
                "ana",
                "Ana",
                "11111111111",
                "senha123"
        );
    }

    private Usuario usuarioAdministrador() throws Exception {
        Usuario usuario = usuarioValido();

        /*
         * A classe Usuario não possui setter público para o atributo adm.
         * Por isso, o teste utiliza reflexão somente para preparar o cenário.
         */
        Field campoAdm = Usuario.class.getDeclaredField("adm");
        campoAdm.setAccessible(true);
        campoAdm.setBoolean(usuario, true);

        return usuario;
    }

    @Test
    void CT_U_C_01_construtor_deveInicializarConsole() {
        Console console = consoleComEntrada("");

        assertNull(console.getUser());
        assertNotNull(console.getScanner());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_02_logout_deveRemoverUsuarioEVoltarParaLoginRegistro() {
        Console console = consoleComEntrada("");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        console.logout();

        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_03_logout_semUsuario_naoDeveLancarExcecao() {
        Console console = consoleComEntrada("");

        console.logout();

        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_04_setUserEGetUser_deveAssociarUsuario() {
        Console console = consoleComEntrada("");
        Usuario usuario = usuarioValido();

        console.setUser(usuario);

        assertEquals(usuario, console.getUser());
    }

    @Test
    void CT_U_C_05_setUserNull_deveRemoverUsuario() {
        Console console = consoleComEntrada("");

        console.setUser(usuarioValido());
        console.setUser(null);

        assertNull(console.getUser());
    }

    @Test
    void CT_U_C_06_setStage_deveAlterarEstagio() {
        Console console = consoleComEntrada("");

        console.setStage(Console.Stage.LOBBY);

        assertEquals(Console.Stage.LOBBY, console.getStage());
    }

    @Test
    void CT_U_C_07_getScanner_deveRetornarScannerValido() {
        Console console = consoleComEntrada("");

        assertNotNull(console.getScanner());
    }

    @Test
    void CT_U_C_08_isAdm_usuarioAdministrador_deveRetornarTrue()
            throws Exception {
        Console console = consoleComEntrada("");
        Usuario usuario = usuarioAdministrador();

        console.setUser(usuario);

        assertTrue(console.isAdm());
    }

    @Test
    void CT_U_C_09_isAdm_usuarioComum_deveRetornarFalse() {
        Console console = consoleComEntrada("");

        console.setUser(usuarioValido());

        assertFalse(console.isAdm());
    }

    @Test
    void CT_U_C_10_isAdm_semUsuario_deveRetornarFalse() {
        Console console = consoleComEntrada("");

        /*
         * Este teste falhará com a implementação atual:
         *
         * return usuario.isAdm();
         *
         * O comportamento esperado é retornar false quando não há usuário.
         */
        assertFalse(console.isAdm());
    }

    @Test
    void CT_U_C_11_draw_loginRegistro_opcaoLogin_deveIrParaLogin() {
        Console console = consoleComEntrada("1\n");

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.LOGIN, console.getStage());
    }

    @Test
    void CT_U_C_12_draw_loginRegistro_opcaoRegistro_deveIrParaRegistro() {
        Console console = consoleComEntrada("2\n");

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_13_draw_lobby_opcaoListarCaronas_deveIrParaListarCaronas() {
        Console console = consoleComEntrada("1\n");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.LISTAR_CARONAS, console.getStage());
    }

    @Test
    void CT_U_C_14_draw_lobby_opcaoHostCarona_deveIrParaHostCarona() {
        Console console = consoleComEntrada("2\n");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.HOST_CARONA, console.getStage());
    }

    @Test
    void CT_U_C_15_draw_lobby_opcaoEntrarCarona_deveIrParaEntrarCarona() {
        Console console = consoleComEntrada("3\n");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.ENTRAR_CARONA, console.getStage());
    }

    @Test
    void CT_U_C_16_draw_lobby_opcaoDeslogar_deveIrParaDeslogar() {
        Console console = consoleComEntrada("4\n");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.DESLOGAR, console.getStage());
        assertEquals(usuarioValido().getUsername(),
                console.getUser().getUsername());
    }

    @Test
    void CT_U_C_17_draw_deslogar_deveExecutarLogout() {
        Console console = consoleComEntrada("");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.DESLOGAR);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_18_draw_halt_deveRetornarFalse() {
        Console console = consoleComEntrada("");

        console.setStage(Console.Stage.HALT);

        boolean resultado = console.draw();

        assertFalse(resultado);
    }

    @Test
    void CT_U_C_19_draw_usuarioLogado_deveExibirNomeDoUsuario() {
        Console console = consoleComEntrada("6\n");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        ByteArrayOutputStream saidaCapturada =
                new ByteArrayOutputStream();

        System.setOut(new PrintStream(saidaCapturada));

        boolean resultado = console.draw();

        String saida = saidaCapturada.toString(
                StandardCharsets.UTF_8
        );

        assertTrue(resultado);
        assertTrue(saida.contains("Usuario: Ana"));
        assertEquals(Console.Stage.HALT, console.getStage());
    }

    @Test
    void CT_U_C_20_draw_lobby_opcaoVerCorridas_deveIrParaVerCorridas() {
        Console console = consoleComEntrada("5\n");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        /*
         * A execução de CaronaManager.verCorridas() pode solicitar
         * um ID de carona. O valor 999 representa um ID inexistente.
         */
        console = consoleComEntrada("5\n999\n");
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.LOBBY, console.getStage());
    }
}
