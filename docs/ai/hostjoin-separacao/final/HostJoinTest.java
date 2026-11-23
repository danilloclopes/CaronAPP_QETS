package me.br.caronapp.console.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import me.br.caronapp.console.Console;
import me.br.caronapp.console.Console.Stage;

/** Validacoes locais de HostJoin com Console simulado e entrada em memoria. */
public class HostJoinTest {

    private Scanner scanner;
    private ByteArrayOutputStream saidaCapturada;
    private final Logger logger = Logger.getLogger(HostJoin.class.getName());
    private StreamHandler handler;
    private boolean usarHandlersOriginais;
    private Level nivelOriginal;

    @BeforeEach
    void preparar() throws UnsupportedEncodingException {
        saidaCapturada = new ByteArrayOutputStream();
        handler = new StreamHandler(saidaCapturada, new SimpleFormatter());
        handler.setEncoding(StandardCharsets.UTF_8.name());
        usarHandlersOriginais = logger.getUseParentHandlers();
        nivelOriginal = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);

    }

    @AfterEach
    void limpar() {
        logger.removeHandler(handler);
        handler.close();
        logger.setUseParentHandlers(usarHandlersOriginais);
        logger.setLevel(nivelOriginal);
        if (scanner != null) {
            scanner.close();
        }
    }

    private String saida() {
        handler.flush();
        return saidaCapturada.toString(StandardCharsets.UTF_8);
    }

    private Console consoleComEntrada(String entrada) {
        scanner = new Scanner(entrada).useLocale(Locale.US);
        Console console = mock(Console.class);
        when(console.getScanner()).thenReturn(scanner);
        return console;
    }

    @Test
    @DisplayName("Deve retornar -1 para quantidade textual")
    void definirPassageirosComTexto() {
        assertEquals(-1, HostJoin.definirPassageiros(consoleComEntrada("abc")));
        assertTrue(saida().contains("Quantidade inv"));
    }

    @Test
    @DisplayName("Deve retornar -1 para sentido textual")
    void selecionarSentidoComTexto() {
        assertEquals(-1, HostJoin.selecionarSentido(consoleComEntrada("abc")));
        assertTrue(saida().contains("Sentido inv"));
    }

    @ParameterizedTest
    @CsvSource({"0, true", "1, false", "4, false", "5, true"})
    @DisplayName("Deve informar quantidade invalida somente fora dos limites")
    void definirPassageirosInformaValidacao(int quantidade, boolean invalida) {
        assertEquals(quantidade, HostJoin.definirPassageiros(consoleComEntrada("" + quantidade)));
        assertEquals(invalida, saida().contains("Quantidade inv"));
    }

    @ParameterizedTest
    @CsvSource({"0, true", "1, false", "2, false", "3, true"})
    @DisplayName("Deve informar sentido invalido somente fora das opcoes")
    void selecionarSentidoInformaValidacao(int sentido, boolean invalido) {
        assertEquals(sentido, HostJoin.selecionarSentido(consoleComEntrada("" + sentido)));
        assertEquals(invalido, saida().contains("Sentido inv"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc"})
    @DisplayName("Campus invalido deve informar erro e retornar ao Lobby")
    void selecionarCampusInvalidoRetornaLobby(String campus) {
        Console console = consoleComEntrada(campus);

        assertNull(HostJoin.selecionarCampus(console));

        assertTrue(saida().contains("Id invalido..."));
        verify(console).setStage(Stage.LOBBY);
    }

}
