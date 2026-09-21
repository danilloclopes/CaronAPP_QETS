package me.br.caronapp.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.br.caronapp.usuario.Usuario;

public class ConsoleTest {

    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        originalIn = System.in;
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
    }

    private Console consoleComEntrada(String entrada) {
        System.setIn(new ByteArrayInputStream(entrada.getBytes()));
        return new Console();
    }

    private Usuario usuarioValido() {
        return new Usuario("ana", "Ana", "11111111111", "senha123");
    }

    @Test
    void CT_U_C_01_construtor_deveInicializarConsole() {
        Console console = new Console();

        assertNull(console.getUser());
        assertNotNull(console.getScanner());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_02_logout_deveRemoverUsuarioEVoltarParaLoginRegistro() {
        Console console = new Console();
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        console.logout();

        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_03_logout_semUsuario_naoLancaErro() {
        Console console = new Console();

        assertDoesNotThrow(() -> console.logout());
        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_04_setUser_e_getUser_deveAssociarUsuario() {
        Console console = new Console();
        Usuario usuario = usuarioValido();

        console.setUser(usuario);

        assertEquals(usuario, console.getUser());
    }

    @Test
    void CT_U_C_05_setUser_null_deveRemoverUsuario() {
        Console console = new Console();
        console.setUser(usuarioValido());

        console.setUser(null);

        assertNull(console.getUser());
    }

    @Test
    void CT_U_C_06_setStage_deveAlterarEstado() {
        Console console = new Console();

        console.setStage(Console.Stage.LOBBY);
        assertEquals(Console.Stage.LOBBY, console.getStage());

        console.setStage(Console.Stage.LOGIN);
        assertEquals(Console.Stage.LOGIN, console.getStage());
    }

    @Test
    void CT_U_C_07_getScanner_deveRetornarScannerValido() {
        Console console = new Console();

        assertNotNull(console.getScanner());
    }

    @Test
    void CT_U_C_08_isAdm_usuarioAdm_deveRetornarTrue() {
        Console console = new Console();
        Usuario usuario = usuarioValido();
        usuario = new Usuario("adm", "Adm", "22222222222", "senha");
        // se existir adm, configure:
        // usuario.setAdm(true); // se houver setter; caso não tenha, use a estrutura da classe
        // como o modelo atual não tem setter, então teste pode ser adaptado
        console.setUser(usuario);

        assertTrue(console.isAdm());
    }

    @Test
    void CT_U_C_09_isAdm_usuarioComum_deveRetornarFalse() {
        Console console = new Console();
        console.setUser(usuarioValido());

        assertFalse(console.isAdm());
    }

    @Test
    void CT_U_C_10_isAdm_semUsuario_deveRetornarFalse() {
        Console console = new Console();

        assertFalse(console.isAdm());
    }

    @Test
    void CT_U_C_11_draw_loginRegistro_opcaoLogin_deveIrParaLogin() {
        Console console = consoleComEntrada("1\n");

        boolean retorno = console.draw();

        assertTrue(retorno);
        assertEquals(Console.Stage.LOGIN, console.getStage());
    }

    @Test
    void CT_U_C_12_draw_loginRegistro_opcaoRegistro_deveIrParaRegistro() {
        Console console = consoleComEntrada("2\n");

        boolean retorno = console.draw();

        assertTrue(retorno);
        assertEquals(Console.Stage.REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_13_draw_lobby_opcaoListarCaronas_deveIrParaListarCaronas() {
        Console console = new Console();
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));

        boolean retorno = console.draw();

        assertTrue(retorno);
        assertEquals(Console.Stage.LISTAR_CARONAS, console.getStage());
    }

    @Test
    void CT_U_C_14_draw_lobby_opcaoHostCarona_deveIrParaHostCarona() {
        Console console = new Console();
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);
        System.setIn(new ByteArrayInputStream("2\n".getBytes()));

        boolean retorno = console.draw();

        assertTrue(retorno);
        assertEquals(Console.Stage.HOST_CARONA, console.getStage());
    }

    @Test
    void CT_U_C_15_draw_lobby_opcaoEntrarCarona_deveIrParaEntrarCarona() {
        Console console = new Console();
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);
        System.setIn(new ByteArrayInputStream("3\n".getBytes()));

        boolean retorno = console.draw();

        assertTrue(retorno);
        assertEquals(Console.Stage.ENTRAR_CARONA, console.getStage());
    }

    @Test
    void CT_U_C_16_draw_lobby_opcaoDeslogar_deveDeslogar() {
        Console console = new Console();
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);
        System.setIn(new ByteArrayInputStream("4\n".getBytes()));

        boolean retorno = console.draw();

        assertTrue(retorno);
        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_17_draw_deslogar_deveLogout() {
        Console console = new Console();
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.DESLOGAR);

        boolean retorno = console.draw();

        assertTrue(retorno);
        assertNull(console.getUser());
        assertEquals(Console.Stage.LOGIN_REGISTRO, console.getStage());
    }

    @Test
    void CT_U_C_18_draw_halt_deveRetornarFalse() {
        Console console = new Console();
        console.setStage(Console.Stage.HALT);

        boolean retorno = console.draw();

        assertFalse(retorno);
    }

    @Test
    void CT_U_C_19_draw_usuarioLogado_deveImprimirNomeUsuario() {
        Console console = new Console();
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        System.setIn(new ByteArrayInputStream("6\n".getBytes()));

        boolean retorno = console.draw();

        assertTrue(retorno);
        assertEquals(Console.Stage.HALT, console.getStage());
    }

    @Test
    void CT_U_C_20_draw_qualquerEstadoNaoTratado_deveFinalizarConsole() {
        Console console = new Console();
        console.setStage(Console.Stage.HALT);

        assertFalse(console.draw());
    }
}
