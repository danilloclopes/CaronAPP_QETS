package me.br.caronapp.console.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.br.caronapp.carona.Campus;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.Carona.Estado;
import me.br.caronapp.carona.PontosDeEncontro;
import me.br.caronapp.carona.Posicao;
import me.br.caronapp.carona.Rota;
import me.br.caronapp.carona.excecoes.VagasExcedidasException;
import me.br.caronapp.console.Console;
import me.br.caronapp.console.Console.Stage;
import me.br.caronapp.usuario.Usuario;

public class CaronaManagerTest {

    private Scanner scanner;
    private final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        Carona.setSerial(0);
        originalOut = System.out;
        System.setOut(new PrintStream(outputCapture));
    }

    @AfterEach
    void tearDown() {
        if (scanner != null) {
            scanner.close();
        }
        System.setOut(originalOut);
    }

    private Console mockConsole(String input, Usuario user) {
        scanner = new Scanner(input);
        Console console = mock(Console.class);
        when(console.getScanner()).thenReturn(scanner);
        when(console.getUser()).thenReturn(user);
        return console;
    }

    private Usuario criarUsuario(String username, String nome) {
        return new Usuario(username, nome, "12345678900", "senha123");
    }

    private Carona criarCarona(Usuario host, int maxGuests) {
        Posicao origem = new Posicao(10, 20, "Rua A", 100, "24000-000");
        Campus destino = new Campus(PontosDeEncontro.PRAIAVERMELHA);
        Rota rota = new Rota(origem, destino);
        return new Carona(host, maxGuests, Calendar.getInstance(), rota);
    }

    @Test
    @DisplayName("CT-U-CM-01: Usuario sem caronas deve informar que nao foi encontrada e retornar ao Lobby")
    void ct01_semCaronas_deveExibirCaronaNaoEncontradaERetornarLobby() {
        Usuario user = criarUsuario("user1", "Usuario 1");
        Console console = mockConsole("999", user);

        CaronaManager.verCorridas(console);

        assertTrue(outputCapture.toString().contains("Carona nao encontrada..."));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-02: Deve separar corretamente caronas como Dono e como Passageiro na listagem")
    void ct02_deveListarCaronasComoDonoEPassageiro() throws VagasExcedidasException {
        Usuario dono = criarUsuario("dono1", "Carlos Dono");
        Usuario passageiro = criarUsuario("pass1", "Lucas Passageiro");

        Carona caronaDono = criarCarona(dono, 3);
        Carona caronaOutro = criarCarona(criarUsuario("outroHost", "Outro Dono"), 3);
        caronaOutro.adicionaPassageiro(dono);

        // Seleciona a carona do dono e responde 0 (nao partir) se cair em menu
        Console console = mockConsole(caronaDono.getID() + " 0", dono);

        CaronaManager.verCorridas(console);

        String saida = outputCapture.toString();
        assertTrue(saida.contains("Caronas nas quais voce eh dono:"));
        assertTrue(saida.contains("Caronas nas quais voce eh passageiro:"));
        assertTrue(saida.contains(caronaDono.getID() + " - Carlos Dono"));
        assertTrue(saida.contains(caronaOutro.getID() + " - Outro Dono"));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-03: Dono com carona AGUARDANDO confirma partida (1) -> estado muda para FECHADO")
    void ct03_dono_estadoAguardando_confirmarPartida_mudaParaFechado() {
        Usuario host = criarUsuario("dono", "Dono");
        Carona carona = criarCarona(host, 3);
        carona.setEstado(Estado.AGUARDANDO);

        Console console = mockConsole(carona.getID() + " 1", host);

        CaronaManager.verCorridas(console);

        assertEquals(Estado.FECHADO, carona.getEstado());
        assertTrue(outputCapture.toString().contains("Vai partir agora? (1 - sim / 0 - nao)"));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-04: Dono com carona AGUARDANDO recusa partida (0) -> estado permanece AGUARDANDO")
    void ct04_dono_estadoAguardando_recusarPartida_permaneceAguardando() {
        Usuario host = criarUsuario("dono", "Dono");
        Carona carona = criarCarona(host, 3);
        carona.setEstado(Estado.AGUARDANDO);

        Console console = mockConsole(carona.getID() + " 0", host);

        CaronaManager.verCorridas(console);

        assertEquals(Estado.AGUARDANDO, carona.getEstado());
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-05: Dono com carona FECHADA confirma finalizacao (1) -> estado muda para FINALIZADO")
    void ct05_dono_estadoFechado_confirmarFinalizacao_mudaParaFinalizado() {
        Usuario host = criarUsuario("dono", "Dono");
        Carona carona = criarCarona(host, 3);
        carona.setEstado(Estado.FECHADO);

        Console console = mockConsole(carona.getID() + " 1", host);

        CaronaManager.verCorridas(console);

        assertEquals(Estado.FINALIZADO, carona.getEstado());
        assertTrue(outputCapture.toString().contains("Ja chegou no ponto? Deseja finalizar? (1 - sim / 0 - nao)"));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-06: Dono com carona FECHADA recusa finalizacao (0) -> estado permanece FECHADO")
    void ct06_dono_estadoFechado_recusarFinalizacao_permaneceFechado() {
        Usuario host = criarUsuario("dono", "Dono");
        Carona carona = criarCarona(host, 3);
        carona.setEstado(Estado.FECHADO);

        Console console = mockConsole(carona.getID() + " 0", host);

        CaronaManager.verCorridas(console);

        assertEquals(Estado.FECHADO, carona.getEstado());
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-07: Dono com carona em estado sem opcoes (ABERTO) -> exibe 'Sem opcoes disponiveis...'")
    void ct07_dono_estadoAberto_semOpcoesDisponiveis() {
        Usuario host = criarUsuario("dono", "Dono");
        Carona carona = criarCarona(host, 3);
        // Estado default da carona recem criada eh ABERTO
        assertEquals(Estado.ABERTO, carona.getEstado());

        Console console = mockConsole(String.valueOf(carona.getID()), host);

        CaronaManager.verCorridas(console);

        assertEquals(Estado.ABERTO, carona.getEstado());
        String saida = outputCapture.toString();
        assertTrue(saida.contains("Sem op") && saida.contains("dispon"));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-08: Passageiro com carona ABERTA confirma saida (1) -> passageiro eh removido")
    void ct08_passageiro_estadoAberto_confirmarSaida_removePassageiro() throws VagasExcedidasException {
        Usuario host = criarUsuario("hostUser", "Host User");
        Usuario passageiro = criarUsuario("passUser", "Passageiro User");

        Carona carona = criarCarona(host, 3);
        carona.adicionaPassageiro(passageiro);
        assertTrue(carona.estaNaCorrida(passageiro));

        Console console = mockConsole(carona.getID() + " 1", passageiro);

        CaronaManager.verCorridas(console);

        assertFalse(carona.estaNaCorrida(passageiro));
        assertFalse(passageiro.getCaronas().contains(carona));
        assertTrue(outputCapture.toString().contains("Deseja sair dessa corrida? (1 - sim / 0 - nao)"));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-09: Passageiro com carona ABERTA desiste de sair (0) -> continua na carona")
    void ct09_passageiro_estadoAberto_desistirSaida_continuaNaCarona() throws VagasExcedidasException {
        Usuario host = criarUsuario("hostUser", "Host User");
        Usuario passageiro = criarUsuario("passUser", "Passageiro User");

        Carona carona = criarCarona(host, 3);
        carona.adicionaPassageiro(passageiro);

        Console console = mockConsole(carona.getID() + " 0", passageiro);

        CaronaManager.verCorridas(console);

        assertTrue(carona.estaNaCorrida(passageiro));
        assertTrue(passageiro.getCaronas().contains(carona));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-10: Passageiro com carona AGUARDANDO -> exibe 'Sem opcoes disponiveis...'")
    void ct10_passageiro_estadoAguardando_semOpcoesDisponiveis() throws VagasExcedidasException {
        Usuario host = criarUsuario("hostUser", "Host User");
        Usuario passageiro = criarUsuario("passUser", "Passageiro User");

        Carona carona = criarCarona(host, 3);
        carona.adicionaPassageiro(passageiro);
        carona.setEstado(Estado.AGUARDANDO);

        Console console = mockConsole(String.valueOf(carona.getID()), passageiro);

        CaronaManager.verCorridas(console);

        assertTrue(carona.estaNaCorrida(passageiro));
        String saida = outputCapture.toString();
        assertTrue(saida.contains("Sem op") && saida.contains("dispon"));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-11: ID nao pertencente as caronas do usuario -> informa nao encontrada e vai ao Lobby")
    void ct11_idInexistenteNasCaronasDoUsuario_deveInformarNaoEncontrada() {
        Usuario host = criarUsuario("hostUser", "Host User");
        criarCarona(host, 3); // ID 1

        Console console = mockConsole("999", host); // ID 999 nao existe

        CaronaManager.verCorridas(console);

        assertTrue(outputCapture.toString().contains("Carona nao encontrada..."));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-12: Qualquer valor diferente de zero eh interpretado como confirmacao (Sim)")
    void ct12_qualquerValorDiferenteDeZeroInterpretaComoSim() {
        Usuario host = criarUsuario("hostUser", "Host User");
        Carona carona = criarCarona(host, 3);
        carona.setEstado(Estado.AGUARDANDO);

        // Digita 2 em vez de 1
        Console console = mockConsole(carona.getID() + " 2", host);

        CaronaManager.verCorridas(console);

        assertEquals(Estado.FECHADO, carona.getEstado());
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }
}
