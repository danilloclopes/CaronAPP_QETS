package me.br.caronapp.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.usuario.Usuario;

class ConsoleIntegrationTest {

    private ArrayList<Usuario> usuariosOriginais;
    private ArrayList<Carona> caronasOriginais;
    private int serialOriginal;

    @BeforeEach
    void prepararDominio() {
        usuariosOriginais = Auxiliar.usuarios;
        caronasOriginais = Auxiliar.caronasAtivas;
        serialOriginal = Carona.getSerial();
        Auxiliar.usuarios = new ArrayList<>();
        Auxiliar.caronasAtivas = new ArrayList<>();
        Carona.setSerial(0);
    }

    @AfterEach
    void restaurarDominio() {
        Auxiliar.usuarios = usuariosOriginais;
        Auxiliar.caronasAtivas = caronasOriginais;
        Carona.setSerial(serialOriginal);
    }

    private Console consoleComEntrada(String entrada) {
        return new Console(new Scanner(entrada));
    }

    private Usuario usuarioValido() {
        return new Usuario("ana", "Ana", "11111111111", "senha123");
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

        Usuario usuario = usuarioValido();
        console.setUser(usuario);
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.DESLOGAR, console.getStage());
        assertSame(usuario, console.getUser());
    }

    @Test
    void CT_U_C_20_draw_lobby_opcaoVerCorridas_deveIrParaVerCorridas() {
        Console console = consoleComEntrada("5\n");

        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        boolean resultado = console.draw();

        assertTrue(resultado);
        assertEquals(Console.Stage.VER_CORRIDAS, console.getStage());
    }

    @Test
    void opcaoSeisDoLobbyEncerraConsoleNaProximaIteracao() {
        Console console = consoleComEntrada("6");
        console.setUser(usuarioValido());
        console.setStage(Console.Stage.LOBBY);

        assertTrue(console.draw());
        assertEquals(Console.Stage.HALT, console.getStage());
        assertFalse(console.draw());
    }

    @Test
    void cadastroLogoutELoginRecuperamAMesmaConta() {
        Console console = consoleComEntrada("ana senha Ana 111 ana senha");
        console.setStage(Console.Stage.REGISTRO);
        assertTrue(console.draw());
        assertEquals(Console.Stage.LOBBY, console.getStage());
        assertEquals(1, Auxiliar.usuarios.size());
        Usuario cadastrada = Auxiliar.usuarios.get(0);
        assertSame(cadastrada, console.getUser());

        console.setStage(Console.Stage.DESLOGAR);
        assertTrue(console.draw());
        console.setStage(Console.Stage.LOGIN);
        assertTrue(console.draw());
        assertSame(cadastrada, console.getUser());
        assertEquals(Console.Stage.LOBBY, console.getStage());
    }

    @Test
    void ofertaListagemEntradaEConsultaUsamAMesmaCarona() {
        Usuario motorista = usuarioValido();
        Console oferta = consoleComEntrada("1 2030 10 20 14 30 1 0 10 20 Rua 50 12345");
        oferta.setUser(motorista);
        oferta.setStage(Console.Stage.HOST_CARONA);
        assertTrue(oferta.draw());
        assertEquals(Console.Stage.LOBBY, oferta.getStage());
        assertEquals(1, Auxiliar.caronasAtivas.size());
        Carona carona = Auxiliar.caronasAtivas.get(0);
        assertSame(motorista, carona.getHost());

        Usuario passageiro = new Usuario("bia", "Bia", "22222222222", "senha");
        int id = carona.getID();
        Console consulta = consoleComEntrada("1 1 " + id + " " + id + " 0");
        consulta.setUser(passageiro);
        consulta.setStage(Console.Stage.LISTAR_CARONAS);
        assertTrue(consulta.draw());
        assertEquals(Console.Stage.LOBBY, consulta.getStage());
        consulta.setStage(Console.Stage.ENTRAR_CARONA);
        assertTrue(consulta.draw());
        assertEquals(Console.Stage.LOBBY, consulta.getStage());
        assertSame(carona, passageiro.getPeloId(id));
        assertEquals(1, carona.getVagasPreenchidas());

        consulta.setStage(Console.Stage.VER_CORRIDAS);
        assertTrue(consulta.draw());
        assertEquals(Console.Stage.LOBBY, consulta.getStage());
        assertSame(carona, passageiro.getPeloId(id));
    }
}
