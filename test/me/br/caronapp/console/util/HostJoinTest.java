package me.br.caronapp.console.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Campus;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.Carona.Estado;
import me.br.caronapp.carona.PontosDeEncontro;
import me.br.caronapp.carona.Posicao;
import me.br.caronapp.carona.Rota;
import me.br.caronapp.carona.Rota.Sentido;
import me.br.caronapp.console.Console;
import me.br.caronapp.console.Console.Stage;
import me.br.caronapp.usuario.Usuario;

public class HostJoinTest {

    private static final String DATA_VALIDA = "2027 10 20 14 30";
    private static final String ENDERECO_VALIDO = "10 20 Rua 50 12345";

    private Scanner scanner;

    @AfterEach
    void limpar() {
        if (scanner != null) scanner.close();
        Auxiliar.caronasAtivas.clear();
    }

    private Console consoleComEntrada(String entrada) {
        scanner = new Scanner(entrada);
        Console console = mock(Console.class);
        when(console.getScanner()).thenReturn(scanner);
        return console;
    }

    private Usuario donoFalso() {
        return new Usuario("dono", "Dono", "1", "senha");
    }

    private Posicao enderecoFalso() {
        return new Posicao(10, 20, "Rua", 50, "12345");
    }

    private Carona hospedar(String passageiros, String data, String sentido, String campus, String endereco) {
        String entrada = String.join(" ", passageiros, data, sentido, campus, endereco);
        Console console = consoleComEntrada(entrada);
        when(console.getUser()).thenReturn(donoFalso());

        HostJoin.host(console);

        assertEquals(1, Auxiliar.caronasAtivas.size());
        assertFalse(scanner.hasNext(), "Todas as entradas devem ter sido consumidas");
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
        return Auxiliar.caronasAtivas.get(0);
    }
  
    private Carona caronaComUmaVaga() {
        Usuario dono = donoFalso();
        Posicao endereco = enderecoFalso();
        Campus campus = new Campus(PontosDeEncontro.PRAIAVERMELHA);
        Rota rota = new Rota(endereco, campus);
        Carona carona = new Carona(dono, 1, Calendar.getInstance(), rota);
        Auxiliar.caronasAtivas.add(carona);
        return carona;
    }

    @Test
    @DisplayName("Deve retornar -1 para quantidade textual")
    void definirPassageirosComTexto() {
        assertEquals(-1, HostJoin.definirPassageiros(consoleComEntrada("abc")));
    }

    @Test
    @DisplayName("Deve retornar -1 para sentido textual")
    void selecionarSentidoComTexto() {
        assertEquals(-1, HostJoin.selecionarSentido(consoleComEntrada("abc")));
    }

    @Test
    @DisplayName("Deve criar carona de ida com dados validos")
    void criarCaronaDeIda() {
        Usuario dono = donoFalso();
        Console console = consoleComEntrada("2 2027 10 20 14 30 1 0 10 20 Rua 50 12345");
        when(console.getUser()).thenReturn(dono);

        HostJoin.host(console);

        assertEquals(1, Auxiliar.caronasAtivas.size());
        Carona carona = Auxiliar.caronasAtivas.get(0);
        assertSame(dono, carona.getHost());
        assertEquals(2, carona.getMaximoGuests());
        assertEquals(Estado.ABERTO, carona.getEstado());
        assertEquals(Sentido.IDA, carona.getRota().getSentido());
        assertTrue(dono.getCaronas().contains(carona));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("host: rejeita 0 vagas e aceita o limite minimo de 1")
    void hostRejeitaZeroVagas() {
        Carona carona = hospedar("0 1", DATA_VALIDA, "1", "0", ENDERECO_VALIDO);
        assertEquals(1, carona.getMaximoGuests());
    }

    @Test
    @DisplayName("host: rejeita 5 vagas e aceita o limite maximo de 4")
    void hostRejeitaCincoVagas() {
        Carona carona = hospedar("5 4", DATA_VALIDA, "1", "0", ENDERECO_VALIDO);
        assertEquals(4, carona.getMaximoGuests());
    }

    @Test
    @DisplayName("host: pede novamente a quantidade quando recebe texto")
    void hostRejeitaPassageirosTextuais() {
        Carona carona = hospedar("abc 2", DATA_VALIDA, "1", "0", ENDERECO_VALIDO);
        assertEquals(2, carona.getMaximoGuests());
    }

    @Test
    @DisplayName("host: pede novamente a data quando recebe texto")
    void hostRejeitaDataTextual() {
        Carona carona = hospedar("2", "abc " + DATA_VALIDA, "1", "0", ENDERECO_VALIDO);
        assertEquals(2027, carona.getDataHora().get(Calendar.YEAR));
    }

    @Test
    @DisplayName("host: rejeita sentidos 0 e 3 e cria carona de volta")
    void hostRejeitaSentidoInvalido() {
        Carona carona = hospedar("2", DATA_VALIDA, "0 3 2", "0", ENDERECO_VALIDO);
        assertEquals(Sentido.VOLTA, carona.getRota().getSentido());
    }

    @Test
    @DisplayName("host: rejeita campus textual e aceita o ultimo indice")
    void hostRejeitaCampusTextual() {
        int ultimoCampus = PontosDeEncontro.values().length - 1;
        Carona carona = hospedar("2", DATA_VALIDA, "1", "abc " + ultimoCampus, ENDERECO_VALIDO);
        assertEquals(PontosDeEncontro.values()[ultimoCampus], carona.getRota().getDestino().getPontoDeEncontro());
    }

    @Test
    @DisplayName("host: pede novamente a posicao quando latitude e texto")
    void hostRejeitaPosicaoTextual() {
        Carona carona = hospedar("2", DATA_VALIDA, "1", "0", "abc " + ENDERECO_VALIDO);
        assertEquals("Rua", carona.getRota().getOrigem().getNomeRua());
    }

    @Test
    @DisplayName("Deve encontrar carona pelo ID")
    void selecionarCaronaExistente() {
        Carona carona = caronaComUmaVaga();

        assertSame(carona, HostJoin.selecionarCarona(consoleComEntrada("" + carona.getID())));
    }

    @Test
    @DisplayName("Deve retornar null para ID inexistente")
    void selecionarCaronaInexistente() {
        Carona carona = caronaComUmaVaga();

        assertNull(HostJoin.selecionarCarona(consoleComEntrada("" + (carona.getID() + 1))));
    }

    @Test
    @DisplayName("Deve entrar em carona aberta com vaga")
    void entrarEmCaronaComVaga() {
        Carona carona = caronaComUmaVaga();
        Usuario passageiro = new Usuario("passageiro", "Passageiro", "2", "senha");
        Console console = consoleComEntrada("1 " + carona.getID());
        when(console.getUser()).thenReturn(passageiro);

        HostJoin.join(console);

        assertEquals(1, carona.getVagasPreenchidas());
        assertTrue(passageiro.getCaronas().contains(carona));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("Nao deve adicionar passageiro em carona lotada")
    void naoEntrarEmCaronaLotada() throws Exception {
        Carona carona = caronaComUmaVaga();
        carona.adicionaPassageiro(new Usuario("primeiro", "Primeiro", "2", "senha"));
        Usuario segundo = new Usuario("segundo", "Segundo", "3", "senha");
        Console console = consoleComEntrada("1 " + carona.getID());
        when(console.getUser()).thenReturn(segundo);

        HostJoin.join(console);

        assertEquals(1, carona.getVagasPreenchidas());
        assertFalse(segundo.getCaronas().contains(carona));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

}
