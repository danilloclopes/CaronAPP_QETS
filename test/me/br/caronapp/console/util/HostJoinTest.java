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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    private ArrayList<Carona> caronasOriginais;
    private int serialOriginal;

    @BeforeEach
    void preparar() {
        caronasOriginais = new ArrayList<>(Auxiliar.caronasAtivas);
        serialOriginal = Carona.getSerial();

        Auxiliar.caronasAtivas.clear();
        Carona.setSerial(0);
    }

    @AfterEach
    void limpar() {
        if (scanner != null) scanner.close();
        Auxiliar.caronasAtivas.clear();
        Auxiliar.caronasAtivas.addAll(caronasOriginais);
        Carona.setSerial(serialOriginal);
    }

    private Console consoleComEntrada(String entrada) {
        scanner = new Scanner(entrada).useLocale(Locale.US);
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

    @ParameterizedTest
    @CsvSource({
        "2028, 1, 1, 0, 0",
        "2028, 12, 31, 23, 59",
        "2028, 2, 28, 12, 30",
        "2028, 2, 29, 12, 30",
        "2029, 2, 28, 12, 30",
        "2028, 4, 30, 12, 30",
        "2028, 5, 31, 12, 30",
        "2400, 2, 29, 12, 30"
    })
    @DisplayName("host: preserva datas validas e limites de calendario e horario")
    void hostAceitaDataValida(int ano, int mes, int dia, int hora, int minuto) {
        String data = ano + " " + mes + " " + dia + " " + hora + " " + minuto;
        Carona carona = hospedar("2", data, "1", "0", ENDERECO_VALIDO);

        Calendar calendar = carona.getDataHora();
        assertEquals(ano, calendar.get(Calendar.YEAR));
        assertEquals(mes - 1, calendar.get(Calendar.MONTH));
        assertEquals(dia, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(hora, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(minuto, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2028 0 15 12 30", "2028 13 15 12 30",
        "2028 1 0 12 30", "2028 1 32 12 30",
        "2028 4 31 12 30", "2028 2 30 12 30",
        "2029 2 29 12 30", "2100 2 29 12 30",
        "2028 1 15 -1 30", "2028 1 15 24 30",
        "2028 1 15 12 -1", "2028 1 15 12 60"
    })
    @DisplayName("host: rejeita datas impossiveis e horarios fora dos limites")
    void hostRejeitaDataInvalida(String dataInvalida) {
        Carona carona = hospedar("2", dataInvalida + " " + DATA_VALIDA, "1", "0", ENDERECO_VALIDO);

        Calendar calendar = carona.getDataHora();
        assertEquals(2027, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, calendar.get(Calendar.MONTH));
        assertEquals(20, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, calendar.get(Calendar.MINUTE));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2028 abc", "2028 1 abc", "2028 1 15 abc", "2028 1 15 12 abc"
    })
    @DisplayName("host: pede novamente a data quando um campo recebe texto")
    void hostRejeitaCampoDeDataTextual(String dataInvalida) {
        Carona carona = hospedar("2", dataInvalida + " " + DATA_VALIDA, "1", "0", ENDERECO_VALIDO);
        assertEquals(2027, carona.getDataHora().get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, carona.getDataHora().get(Calendar.MONTH));
        assertEquals(20, carona.getDataHora().get(Calendar.DAY_OF_MONTH));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "3", "abc"})
    @DisplayName("host: rejeita sentido invalido e cria carona de volta")
    void hostRejeitaSentidoInvalido(String sentidoInvalido) {
        Carona carona = hospedar("2", DATA_VALIDA, sentidoInvalido + " 2", "0", ENDERECO_VALIDO);
        assertEquals(Sentido.VOLTA, carona.getRota().getSentido());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    @DisplayName("host: aceita quantidades validas incluindo os limites")
    void hostAceitaQuantidadeValida(int passageiros) {
        Carona carona = hospedar("" + passageiros, DATA_VALIDA, "1", "0", ENDERECO_VALIDO);
        assertEquals(passageiros, carona.getMaximoGuests());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-3", "9", "1.5"})
    @DisplayName("host: rejeita classes invalidas de quantidade e aceita a proxima entrada")
    void hostRejeitaClassesInvalidasDeQuantidade(String passageirosInvalidos) {
        Carona carona = hospedar(passageirosInvalidos + " 2", DATA_VALIDA, "1", "0", ENDERECO_VALIDO);
        assertEquals(2, carona.getMaximoGuests());
    }

    @ParameterizedTest
    @CsvSource({"1, IDA", "2, VOLTA"})
    @DisplayName("host: cria a rota correspondente a cada sentido valido")
    void hostAceitaSentidoValido(String sentido, Sentido sentidoEsperado) {
        Carona carona = hospedar("2", DATA_VALIDA, sentido, "0", ENDERECO_VALIDO);
        assertEquals(sentidoEsperado, carona.getRota().getSentido());
        if (sentidoEsperado == Sentido.IDA) {
            assertEquals("Rua", carona.getRota().getOrigem().getNomeRua());
            assertEquals(PontosDeEncontro.values()[0], carona.getRota().getDestino().getPontoDeEncontro());
        } else {
            assertEquals(PontosDeEncontro.values()[0], carona.getRota().getOrigem().getPontoDeEncontro());
            assertEquals("Rua", carona.getRota().getDestino().getNomeRua());
        }
    }

    @ParameterizedTest
    @EnumSource(PontosDeEncontro.class)
    @DisplayName("host: aceita cada campus incluindo o primeiro e o ultimo indice")
    void hostAceitaCampusValido(PontosDeEncontro campus) {
        Carona carona = hospedar("2", DATA_VALIDA, "1", "" + campus.ordinal(), ENDERECO_VALIDO);
        assertEquals(campus, carona.getRota().getDestino().getPontoDeEncontro());
    }

    @Test
    @DisplayName("host: rejeita campus textual e aceita o ultimo indice")
    void hostRejeitaCampusTextual() {
        int ultimoCampus = PontosDeEncontro.values().length - 1;
        Carona carona = hospedar("2", DATA_VALIDA, "1", "abc " + ultimoCampus, ENDERECO_VALIDO);
        assertEquals(PontosDeEncontro.values()[ultimoCampus], carona.getRota().getDestino().getPontoDeEncontro());
    }

    @Test
    @DisplayName("host: rejeita campus negativo e aceita o primeiro indice")
    void hostRejeitaCampusNegativo() {
        Carona carona = hospedar("2", DATA_VALIDA, "1", "-1 0", ENDERECO_VALIDO);
        assertEquals(PontosDeEncontro.values()[0], carona.getRota().getDestino().getPontoDeEncontro());
    }

    @Test
    @DisplayName("host: rejeita campus acima do ultimo indice e aceita o primeiro")
    void hostRejeitaCampusAcimaDoLimite() {
        int campusInvalido = PontosDeEncontro.values().length;
        Carona carona = hospedar("2", DATA_VALIDA, "1", campusInvalido + " 0", ENDERECO_VALIDO);
        assertEquals(PontosDeEncontro.values()[0], carona.getRota().getDestino().getPontoDeEncontro());
    }

    @Test
    @DisplayName("host: pede novamente a posicao quando latitude e texto")
    void hostRejeitaPosicaoTextual() {
        Carona carona = hospedar("2", DATA_VALIDA, "1", "0", "abc " + ENDERECO_VALIDO);
        assertEquals("Rua", carona.getRota().getOrigem().getNomeRua());
    }

    @ParameterizedTest
    @CsvSource({
        "-90, -180, RuaSul, 1, 00000000",
        "90, 180, RuaNorte, 100, 99999999",
        "0, 0, RuaCentro, 50, 12345",
        "-22.9, -43.1, RuaTeste, 20, 24000-000"
    })
    @DisplayName("host: preserva coordenadas e campos do endereco informado")
    void hostAceitaPosicaoValida(double latitude, double longitude, String rua, int numero, String cep) {
        String endereco = latitude + " " + longitude + " " + rua + " " + numero + " " + cep;
        Carona carona = hospedar("2", DATA_VALIDA, "1", "0", endereco);

        Posicao origem = carona.getRota().getOrigem();
        assertEquals(latitude, origem.getLatitude());
        assertEquals(longitude, origem.getLongitude());
        assertEquals(rua, origem.getNomeRua());
        assertEquals(numero, origem.getNumero());
        assertEquals(cep, origem.getCep());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10 abc", "10 20 Rua abc", "10 20 Rua 1.5"})
    @DisplayName("host: pede novamente o endereco quando um campo numerico e invalido")
    void hostRejeitaCampoDePosicaoInvalido(String enderecoInvalido) {
        Carona carona = hospedar("2", DATA_VALIDA, "1", "0", enderecoInvalido + " " + ENDERECO_VALIDO);

        Posicao origem = carona.getRota().getOrigem();
        assertEquals(10, origem.getLatitude());
        assertEquals(20, origem.getLongitude());
        assertEquals("Rua", origem.getNomeRua());
        assertEquals(50, origem.getNumero());
        assertEquals("12345", origem.getCep());
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

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "0", "-1"})
    @DisplayName("join: pede novamente o ID quando recebe entrada invalida")
    void entrarEmCaronaAposIdInvalido(String idInvalido) {
        Carona carona = caronaComUmaVaga();
        Usuario passageiro = new Usuario("passageiro", "Passageiro", "2", "senha");
        Console console = consoleComEntrada("1 " + idInvalido + " " + carona.getID());
        when(console.getUser()).thenReturn(passageiro);

        HostJoin.join(console);

        assertEquals(1, carona.getVagasPreenchidas());
        assertTrue(carona.estaNaCorrida(passageiro));
        assertTrue(passageiro.getCaronas().contains(carona));
        assertFalse(scanner.hasNext(), "Todas as entradas devem ter sido consumidas");
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("join: retorna ao Lobby quando nao existem caronas")
    void naoEntrarQuandoNaoExistemCaronas() {
        Console console = consoleComEntrada("1");

        HostJoin.join(console);

        assertTrue(Auxiliar.caronasAtivas.isEmpty());
        assertFalse(scanner.hasNext(), "Todas as entradas devem ter sido consumidas");
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("join: nao solicita ID quando nao ha caronas no sentido escolhido")
    void naoEntrarQuandoNaoExisteCaronaNoSentido() {
        Carona carona = caronaComUmaVaga();
        Console console = consoleComEntrada("2");

        HostJoin.join(console);

        assertEquals(0, carona.getVagasPreenchidas());
        assertFalse(scanner.hasNext(), "Todas as entradas devem ter sido consumidas");
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("join: nao adiciona o dono como passageiro")
    void naoEntrarNaPropriaCarona() {
        Carona carona = caronaComUmaVaga();
        Console console = consoleComEntrada("1 " + carona.getID());
        when(console.getUser()).thenReturn(carona.getHost());

        HostJoin.join(console);

        assertEquals(0, carona.getVagasPreenchidas());
        assertEquals(1, carona.getHost().getCaronas().size());
        assertSame(carona, carona.getHost().getCaronas().get(0));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("join: nao duplica a participacao de passageiro ja inscrito")
    void naoEntrarDuasVezesNaMesmaCarona() throws Exception {
        Carona carona = caronaComUmaVaga();
        carona.setMaximoGuests(2);
        Usuario passageiro = new Usuario("passageiro", "Passageiro", "2", "senha");
        carona.adicionaPassageiro(passageiro);
        Console console = consoleComEntrada("1 " + carona.getID());
        when(console.getUser()).thenReturn(passageiro);

        HostJoin.join(console);

        assertEquals(1, carona.getVagasPreenchidas());
        assertTrue(carona.estaNaCorrida(passageiro));
        assertEquals(1, passageiro.getCaronas().size());
        assertSame(carona, passageiro.getCaronas().get(0));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @ParameterizedTest
    @EnumSource(value = Estado.class, names = {"AGUARDANDO", "FECHADO", "FINALIZADO"})
    @DisplayName("join: nao adiciona passageiro em carona que nao esta aberta")
    void naoEntrarEmCaronaQueNaoEstaAberta(Estado estado) {
        Carona carona = caronaComUmaVaga();
        carona.setEstado(estado);
        caronaComUmaVaga();
        Usuario passageiro = new Usuario("passageiro", "Passageiro", "2", "senha");
        Console console = consoleComEntrada("1 " + carona.getID());
        when(console.getUser()).thenReturn(passageiro);

        HostJoin.join(console);

        assertEquals(0, carona.getVagasPreenchidas());
        assertFalse(carona.estaNaCorrida(passageiro));
        assertTrue(passageiro.getCaronas().isEmpty());
        assertFalse(scanner.hasNext(), "Todas as entradas devem ter sido consumidas");
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    @DisplayName("join: aceita passageiro na ultima vaga disponivel")
    void entrarNaUltimaVaga(int capacidade) throws Exception {
        Carona carona = caronaComUmaVaga();
        carona.setMaximoGuests(capacidade);
        for (int i = 0; i < capacidade - 1; i++) {
            carona.adicionaPassageiro(new Usuario("pass" + i, "Passageiro " + i, "3", "senha"));
        }
        Usuario passageiro = new Usuario("ultimo", "Ultimo", "2", "senha");
        Console console = consoleComEntrada("1 " + carona.getID());
        when(console.getUser()).thenReturn(passageiro);

        HostJoin.join(console);

        assertEquals(capacidade, carona.getVagasPreenchidas());
        assertTrue(carona.estaNaCorrida(passageiro));
        assertTrue(passageiro.getCaronas().contains(carona));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
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
    @DisplayName("join: pede novamente o ID quando a carona nao existe")
    void entrarEmCaronaAposIdInexistente() {
        Carona carona = caronaComUmaVaga();
        Usuario passageiro = new Usuario("passageiro", "Passageiro", "2", "senha");
        Console console = consoleComEntrada("1 " + (carona.getID() + 1) + " " + carona.getID());
        when(console.getUser()).thenReturn(passageiro);

        HostJoin.join(console);

        assertEquals(1, carona.getVagasPreenchidas());
        assertTrue(carona.estaNaCorrida(passageiro));
        assertTrue(passageiro.getCaronas().contains(carona));
        assertFalse(scanner.hasNext(), "Todas as entradas devem ter sido consumidas");
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    @DisplayName("Nao deve adicionar passageiro em carona lotada")
    void naoEntrarEmCaronaLotada(int capacidade) throws Exception {
        Carona carona = caronaComUmaVaga();
        carona.setMaximoGuests(capacidade);
        for (int i = 0; i < capacidade; i++) {
            carona.adicionaPassageiro(new Usuario("pass" + i, "Passageiro " + i, "2", "senha"));
        }
        Usuario segundo = new Usuario("segundo", "Segundo", "3", "senha");
        Console console = consoleComEntrada("1 " + carona.getID());
        when(console.getUser()).thenReturn(segundo);

        HostJoin.join(console);

        assertEquals(capacidade, carona.getVagasPreenchidas());
        assertFalse(carona.estaNaCorrida(segundo));
        assertFalse(segundo.getCaronas().contains(carona));
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
    }

}
