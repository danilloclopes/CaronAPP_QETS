package me.br.caronapp.console.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

/**
 * Testes de integracao entre {@link CaronaManager} e as entidades reais do dominio
 * ({@link Usuario}, {@link Carona}, {@link Rota}, {@link Campus} e {@link Posicao}).
 * Apenas {@link Console} eh simulado para fornecer entrada de {@link Scanner} e sessao.
 */
public class CaronaManagerIntegrationTest {

    private Scanner scanner;
    private final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private int serialOriginal;

    @BeforeEach
    void setUp() {
        serialOriginal = Carona.getSerial();
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
        Carona.setSerial(serialOriginal);
    }

    private Console consoleComEntrada(String input, Usuario user) {
        if (scanner != null) {
            scanner.close();
        }
        scanner = new Scanner(input);
        Console console = mock(Console.class);
        when(console.getScanner()).thenReturn(scanner);
        when(console.getUser()).thenReturn(user);
        return console;
    }

    private Usuario criarUsuario(String username, String nome) {
        return new Usuario(username, nome, "12345678900", "senha123");
    }

    private Carona criarCarona(Usuario host, int maxGuests, PontosDeEncontro ponto) {
        Posicao origem = new Posicao(-22.9064, -43.1332, "Rua Passo da Patria", 156, "24210-240");
        Campus destino = new Campus(ponto);
        Rota rota = new Rota(origem, destino);
        return new Carona(host, maxGuests, Calendar.getInstance(), rota);
    }

    @Test
    @DisplayName("CT-I-CM-01: Integracao de listagem separa caronas reais de dono e passageiro")
    void ct01_listagemRealSeparaDonoEPassageiro() throws VagasExcedidasException {
        Usuario usuario = criarUsuario("rafael", "Rafael Silva");
        Usuario outroHost = criarUsuario("ana", "Ana Paula");

        Carona caronaDono = criarCarona(usuario, 3, PontosDeEncontro.PRAIAVERMELHA);
        Carona caronaOutro = criarCarona(outroHost, 2, PontosDeEncontro.GRAGOATA);
        assertTrue(caronaOutro.adicionaPassageiro(usuario));

        CaronaManager.verCorridas(consoleComEntrada(String.valueOf(caronaDono.getID()), usuario));

        String saida = outputCapture.toString();
        int idxDono = saida.indexOf("Caronas nas quais voce eh dono:");
        int idxPass = saida.indexOf("Caronas nas quais voce eh passageiro:");
        String linhaDono = caronaDono.getID() + " - Rafael Silva - " + caronaDono;
        String linhaPass = caronaOutro.getID() + " - Ana Paula - " + caronaOutro;

        assertTrue(saida.indexOf(linhaDono) > idxDono && saida.indexOf(linhaDono) < idxPass);
        assertTrue(saida.indexOf(linhaPass) > idxPass);
    }

    @Test
    @DisplayName("CT-I-CM-02: Ciclo completo do motorista (AGUARDANDO -> FECHADO -> FINALIZADO) atualiza historico real")
    void ct02_cicloCompletoMotoristaAtualizaEstadoEHistorico() throws VagasExcedidasException {
        Usuario motorista = criarUsuario("host", "Carlos Motorista");
        Usuario passageiro = criarUsuario("pass", "Lucas Passageiro");
        Carona carona = criarCarona(motorista, 2, PontosDeEncontro.PRAIAVERMELHA);
        assertTrue(carona.adicionaPassageiro(passageiro));

        carona.setEstado(Estado.AGUARDANDO);
        Console cPartida = consoleComEntrada(carona.getID() + " 1", motorista);
        CaronaManager.verCorridas(cPartida);

        assertEquals(Estado.FECHADO, carona.getEstado());
        assertTrue(motorista.getCaronas().contains(carona));
        verify(cPartida).setStage(Stage.LOBBY);

        Console cFinalizar = consoleComEntrada(carona.getID() + " 1", motorista);
        CaronaManager.verCorridas(cFinalizar);

        assertEquals(Estado.FINALIZADO, carona.getEstado());
        assertFalse(motorista.getCaronas().contains(carona));
        assertTrue(motorista.getHistorico().contains(carona));
        assertFalse(passageiro.getCaronas().contains(carona));
        assertTrue(passageiro.getHistorico().contains(carona));
        verify(cFinalizar).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-I-CM-03: Saida de passageiro via CaronaManager libera vaga real para novo passageiro")
    void ct03_saidaPassageiroLiberaVagaReal() throws VagasExcedidasException {
        Usuario motorista = criarUsuario("host", "Motorista");
        Usuario primeiro = criarUsuario("p1", "Primeiro");
        Usuario segundo = criarUsuario("p2", "Segundo");

        Carona carona = criarCarona(motorista, 1, PontosDeEncontro.VALONGUINHO);
        assertTrue(carona.adicionaPassageiro(primeiro));
        assertEquals(1, carona.getVagasPreenchidas());
        assertTrue(carona.estaNaCorrida(primeiro));

        Console console = consoleComEntrada(carona.getID() + " 1", primeiro);
        CaronaManager.verCorridas(console);

        assertEquals(0, carona.getVagasPreenchidas());
        assertFalse(carona.estaNaCorrida(primeiro));
        assertFalse(primeiro.getCaronas().contains(carona));
        assertTrue(carona.adicionaPassageiro(segundo));
        assertEquals(1, carona.getVagasPreenchidas());
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-I-CM-04: Passageiro que desiste de sair (0) mantem participacao e vaga ocupada")
    void ct04_recusaSaidaMantemVagaOcupada() throws VagasExcedidasException {
        Usuario motorista = criarUsuario("host", "Motorista");
        Usuario passageiro = criarUsuario("p1", "Passageiro");

        Carona carona = criarCarona(motorista, 1, PontosDeEncontro.GRAGOATA);
        assertTrue(carona.adicionaPassageiro(passageiro));

        Console console = consoleComEntrada(carona.getID() + " 0", passageiro);
        CaronaManager.verCorridas(console);

        assertEquals(1, carona.getVagasPreenchidas());
        assertTrue(carona.estaNaCorrida(passageiro));
        assertTrue(passageiro.getCaronas().contains(carona));
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-I-CM-05: Informar ID de carona de outro usuario nao altera estado nem listas")
    void ct05_idDeOutroUsuarioNaoAlteraCarona() {
        Usuario donoA = criarUsuario("donoA", "Dono A");
        Usuario donoB = criarUsuario("donoB", "Dono B");
        Carona caronaA = criarCarona(donoA, 2, PontosDeEncontro.PRAIAVERMELHA);
        Carona caronaB = criarCarona(donoB, 2, PontosDeEncontro.GRAGOATA);

        Console console = consoleComEntrada(String.valueOf(caronaA.getID()), donoB);
        CaronaManager.verCorridas(console);

        assertTrue(outputCapture.toString().contains("Carona nao encontrada..."));
        assertEquals(Estado.ABERTO, caronaA.getEstado());
        assertEquals(1, donoB.getCaronas().size());
        assertTrue(donoB.getCaronas().contains(caronaB));
        verify(console).setStage(Stage.LOBBY);
    }
}
