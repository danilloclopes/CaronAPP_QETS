package me.br.caronapp.console.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.Carona.Estado;
import me.br.caronapp.carona.excecoes.UsuarioNaoEncontradoException;
import me.br.caronapp.console.Console;
import me.br.caronapp.console.Console.Stage;
import me.br.caronapp.usuario.Usuario;

/**
 * Testes unitarios isolados da classe {@link CaronaManager}.
 * Todas as dependencias externas ({@link Console}, {@link Usuario} e {@link Carona})
 * sao substituidas por mocks, aplicando particionamento de equivalencia e analise
 * de valores limite.
 */
public class CaronaManagerTest {

    private Scanner scanner;
    private ByteArrayOutputStream outputCapture;
    private final Logger logger = Logger.getLogger(CaronaManager.class.getName());
    private StreamHandler handler;
    private boolean usarHandlersOriginais;
    private Level nivelOriginal;

    @BeforeEach
    void setUp() throws UnsupportedEncodingException {
        outputCapture = new ByteArrayOutputStream();
        handler = new StreamHandler(outputCapture, new SimpleFormatter());
        handler.setEncoding(StandardCharsets.UTF_8.name());
        usarHandlersOriginais = logger.getUseParentHandlers();
        nivelOriginal = logger.getLevel();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);
    }

    @AfterEach
    void tearDown() {
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
        return outputCapture.toString(StandardCharsets.UTF_8);
    }

    private Console mockConsole(String input, Usuario user) {
        scanner = new Scanner(input);
        Console console = mock(Console.class);
        when(console.getScanner()).thenReturn(scanner);
        when(console.getUser()).thenReturn(user);
        return console;
    }

    private Usuario mockUsuario(String nome) {
        Usuario user = mock(Usuario.class);
        when(user.getName()).thenReturn(nome);
        // Usuario define equals(Usuario) como sobrecarga; o stub explicito garante
        // a comparacao correta entre mocks nas chamadas de CaronaManager.
        when(user.equals(user)).thenReturn(true);
        return user;
    }

    private Carona mockCarona(int id, Usuario host, Estado estado, String destino) {
        Carona carona = mock(Carona.class);
        when(carona.getID()).thenReturn(id);
        when(carona.getHost()).thenReturn(host);
        when(carona.getEstado()).thenReturn(estado);
        when(carona.toString()).thenReturn(destino);
        return carona;
    }

    private void configurarListaCaronas(Usuario user, Carona... caronas) {
        when(user.getCaronas()).thenReturn(new ArrayList<>(Arrays.asList(caronas)));
    }

    @Test
    @DisplayName("CT-U-CM-01: Lista vazia (limite 0) deve exibir cabecalhos, informar nao encontrada e ir ao Lobby")
    void ct01_listaVazia_deveInformarNaoEncontradaERetornarLobby() {
        Usuario user = mockUsuario("Usuario 1");
        configurarListaCaronas(user);
        when(user.getPeloId(1)).thenReturn(null);
        Console console = mockConsole("1", user);

        CaronaManager.verCorridas(console);

        String saida = saida();
        assertTrue(saida.contains("Caronas nas quais voce eh dono:"));
        assertTrue(saida.contains("Caronas nas quais voce eh passageiro:"));
        assertTrue(saida.contains("Digite um ID de carona:"));
        assertTrue(saida.contains("Carona nao encontrada..."));
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-02: Deve listar apenas caronas como dono quando usuario nao eh passageiro")
    void ct02_apenasCaronasComoDono_deveListarNaSecaoDeDono() {
        Usuario dono = mockUsuario("Carlos Dono");
        Carona carona1 = mockCarona(1, dono, Estado.ABERTO, "Praia Vermelha");
        Carona carona2 = mockCarona(2, dono, Estado.ABERTO, "Gragoata");
        configurarListaCaronas(dono, carona1, carona2);
        when(dono.getPeloId(1)).thenReturn(carona1);
        Console console = mockConsole("1", dono);

        CaronaManager.verCorridas(console);

        String saida = saida();
        int idxDono = saida.indexOf("Caronas nas quais voce eh dono:");
        int idxPassageiro = saida.indexOf("Caronas nas quais voce eh passageiro:");
        int idxC1 = saida.indexOf("1 - Carlos Dono - Praia Vermelha");
        int idxC2 = saida.indexOf("2 - Carlos Dono - Gragoata");

        assertTrue(idxDono >= 0 && idxPassageiro > idxDono);
        assertTrue(idxC1 > idxDono && idxC1 < idxPassageiro);
        assertTrue(idxC2 > idxC1 && idxC2 < idxPassageiro);
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-03: Deve listar apenas caronas como passageiro quando usuario nao eh dono")
    void ct03_apenasCaronasComoPassageiro_deveListarNaSecaoDePassageiro() {
        Usuario passageiro = mockUsuario("Lucas Passageiro");
        Usuario host1 = mockUsuario("Host Um");
        Usuario host2 = mockUsuario("Host Dois");
        Carona carona1 = mockCarona(10, host1, Estado.FECHADO, "Valonguinho");
        Carona carona2 = mockCarona(20, host2, Estado.FINALIZADO, "Praia Vermelha");
        configurarListaCaronas(passageiro, carona1, carona2);
        when(passageiro.getPeloId(10)).thenReturn(carona1);
        Console console = mockConsole("10", passageiro);

        CaronaManager.verCorridas(console);

        String saida = saida();
        int idxPassageiro = saida.indexOf("Caronas nas quais voce eh passageiro:");
        int idxC1 = saida.indexOf("10 - Host Um - Valonguinho");
        int idxC2 = saida.indexOf("20 - Host Dois - Praia Vermelha");

        assertTrue(idxPassageiro >= 0);
        assertTrue(idxC1 > idxPassageiro);
        assertTrue(idxC2 > idxC1);
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-04: Lista mista (dono e passageiro intercalados) deve separar corretamente as secoes")
    void ct04_listaMista_deveSepararSecoesDeDonoEPassageiro() {
        Usuario usuario = mockUsuario("Carlos");
        Usuario outroHost1 = mockUsuario("Ana Host");
        Usuario outroHost2 = mockUsuario("Beto Host");
        Carona cDono1 = mockCarona(1, usuario, Estado.ABERTO, "Praia Vermelha");
        Carona cPass1 = mockCarona(2, outroHost1, Estado.AGUARDANDO, "Gragoata");
        Carona cDono2 = mockCarona(3, usuario, Estado.ABERTO, "Valonguinho");
        Carona cPass2 = mockCarona(4, outroHost2, Estado.FECHADO, "Biomedico");

        configurarListaCaronas(usuario, cDono1, cPass1, cDono2, cPass2);
        when(usuario.getPeloId(1)).thenReturn(cDono1);
        Console console = mockConsole("1", usuario);

        CaronaManager.verCorridas(console);

        String saida = saida();
        int idxDono = saida.indexOf("Caronas nas quais voce eh dono:");
        int idxPass = saida.indexOf("Caronas nas quais voce eh passageiro:");
        int idxDigite = saida.indexOf("Digite um ID de carona:");

        assertTrue(saida.indexOf("1 - Carlos - Praia Vermelha") > idxDono);
        assertTrue(saida.indexOf("3 - Carlos - Valonguinho") < idxPass);
        assertTrue(saida.indexOf("2 - Ana Host - Gragoata") > idxPass);
        assertTrue(saida.indexOf("4 - Beto Host - Biomedico") > idxPass);
        assertTrue(saida.indexOf("4 - Beto Host - Biomedico") < idxDigite);
        verify(console).setStage(Stage.LOBBY);
    }

    @ParameterizedTest(name = "CT-U-CM-05: ID nao encontrado ({0}) retorna mensagem de erro e volta ao Lobby")
    @ValueSource(ints = {-1, 0, 999})
    @DisplayName("CT-U-CM-05: Valores limite e equivalencia de ID inexistente")
    void ct05_idInexistente_deveExibirNaoEncontradaERetornarLobby(int idInformado) {
        Usuario user = mockUsuario("Dono");
        Carona carona = mockCarona(1, user, Estado.ABERTO, "Praia Vermelha");
        configurarListaCaronas(user, carona);
        when(user.getPeloId(idInformado)).thenReturn(null);
        Console console = mockConsole(String.valueOf(idInformado), user);

        CaronaManager.verCorridas(console);

        assertTrue(saida().contains("Carona nao encontrada..."));
        verify(carona, never()).setEstado(any());
        verify(console).setStage(Stage.LOBBY);
    }

    @ParameterizedTest(name = "CT-U-CM-06: Dono AGUARDANDO com entrada {0} != 0 confirma partida -> FECHADO")
    @ValueSource(ints = {-1, 1, 2})
    @DisplayName("CT-U-CM-06: Dono confirma partida com valores diferentes de 0")
    void ct06_dono_aguardando_confirmarPartida_mudaParaFechado(int opcaoConfirmacao) {
        Usuario host = mockUsuario("Dono");
        Carona carona = mockCarona(1, host, Estado.AGUARDANDO, "Praia Vermelha");
        configurarListaCaronas(host, carona);
        when(host.getPeloId(1)).thenReturn(carona);
        Console console = mockConsole("1 " + opcaoConfirmacao, host);

        CaronaManager.verCorridas(console);

        assertTrue(saida().contains("Vai partir agora? (1 - sim / 0 - nao)"));
        verify(carona).setEstado(Estado.FECHADO);
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-07: Dono com carona AGUARDANDO recusa partida (0) -> nao altera estado")
    void ct07_dono_aguardando_recusarPartida_naoAlteraEstado() {
        Usuario host = mockUsuario("Dono");
        Carona carona = mockCarona(1, host, Estado.AGUARDANDO, "Praia Vermelha");
        configurarListaCaronas(host, carona);
        when(host.getPeloId(1)).thenReturn(carona);
        Console console = mockConsole("1 0", host);

        CaronaManager.verCorridas(console);

        assertTrue(saida().contains("Vai partir agora? (1 - sim / 0 - nao)"));
        verify(carona, never()).setEstado(any());
        verify(console).setStage(Stage.LOBBY);
    }

    @ParameterizedTest(name = "CT-U-CM-08: Dono FECHADO com entrada {0} != 0 finaliza carona -> FINALIZADO")
    @ValueSource(ints = {-1, 1, 2})
    @DisplayName("CT-U-CM-08: Dono confirma finalizacao com valores diferentes de 0")
    void ct08_dono_fechado_confirmarFinalizacao_mudaParaFinalizado(int opcaoConfirmacao) {
        Usuario host = mockUsuario("Dono");
        Carona carona = mockCarona(1, host, Estado.FECHADO, "Praia Vermelha");
        configurarListaCaronas(host, carona);
        when(host.getPeloId(1)).thenReturn(carona);
        Console console = mockConsole("1 " + opcaoConfirmacao, host);

        CaronaManager.verCorridas(console);

        assertTrue(saida().contains("Ja chegou no ponto? Deseja finalizar? (1 - sim / 0 - nao)"));
        verify(carona).setEstado(Estado.FINALIZADO);
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-09: Dono com carona FECHADO recusa finalizacao (0) -> nao altera estado")
    void ct09_dono_fechado_recusarFinalizacao_naoAlteraEstado() {
        Usuario host = mockUsuario("Dono");
        Carona carona = mockCarona(1, host, Estado.FECHADO, "Praia Vermelha");
        configurarListaCaronas(host, carona);
        when(host.getPeloId(1)).thenReturn(carona);
        Console console = mockConsole("1 0", host);

        CaronaManager.verCorridas(console);

        assertTrue(saida().contains("Ja chegou no ponto? Deseja finalizar? (1 - sim / 0 - nao)"));
        verify(carona, never()).setEstado(any());
        verify(console).setStage(Stage.LOBBY);
    }

    @ParameterizedTest(name = "CT-U-CM-10: Dono em estado {0} nao possui opcoes disponiveis")
    @EnumSource(value = Estado.class, names = {"ABERTO", "FINALIZADO"})
    @DisplayName("CT-U-CM-10: Dono em estados sem acao disponivel exibe mensagem padrao")
    void ct10_dono_estadosSemOpcoes_exibeSemOpcoes(Estado estado) {
        Usuario host = mockUsuario("Dono");
        Carona carona = mockCarona(1, host, estado, "Praia Vermelha");
        configurarListaCaronas(host, carona);
        when(host.getPeloId(1)).thenReturn(carona);
        Console console = mockConsole("1", host);

        CaronaManager.verCorridas(console);

        String saida = saida();
        assertTrue(saida.contains("Sem op") && saida.contains("dispon"));
        verify(carona, never()).setEstado(any());
        verify(console).setStage(Stage.LOBBY);
    }

    @ParameterizedTest(name = "CT-U-CM-11: Passageiro ABERTO com entrada {0} != 0 confirma saida -> removePassageiro")
    @ValueSource(ints = {-1, 1, 2})
    @DisplayName("CT-U-CM-11: Passageiro confirma saida com valores diferentes de 0")
    void ct11_passageiro_aberto_confirmarSaida_removePassageiro(int opcaoConfirmacao)
            throws UsuarioNaoEncontradoException {
        Usuario host = mockUsuario("Host");
        Usuario passageiro = mockUsuario("Passageiro");
        Carona carona = mockCarona(5, host, Estado.ABERTO, "Gragoata");
        configurarListaCaronas(passageiro, carona);
        when(passageiro.getPeloId(5)).thenReturn(carona);
        Console console = mockConsole("5 " + opcaoConfirmacao, passageiro);

        CaronaManager.verCorridas(console);

        assertTrue(saida().contains("Deseja sair dessa corrida? (1 - sim / 0 - nao)"));
        verify(carona).removePassageiro(passageiro);
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-12: Passageiro ABERTO desiste de sair (0) -> nao remove passageiro")
    void ct12_passageiro_aberto_recusarSaida_naoRemovePassageiro() throws UsuarioNaoEncontradoException {
        Usuario host = mockUsuario("Host");
        Usuario passageiro = mockUsuario("Passageiro");
        Carona carona = mockCarona(5, host, Estado.ABERTO, "Gragoata");
        configurarListaCaronas(passageiro, carona);
        when(passageiro.getPeloId(5)).thenReturn(carona);
        Console console = mockConsole("5 0", passageiro);

        CaronaManager.verCorridas(console);

        assertTrue(saida().contains("Deseja sair dessa corrida? (1 - sim / 0 - nao)"));
        verify(carona, never()).removePassageiro(any(Usuario.class));
        verify(console).setStage(Stage.LOBBY);
    }

    @Test
    @DisplayName("CT-U-CM-13: Passageiro ABERTO quando removePassageiro lanca UsuarioNaoEncontradoException -> trata sem interromper fluxo")
    void ct13_passageiro_aberto_excecaoAoRemover_deveTratarERetornarLobby()
            throws UsuarioNaoEncontradoException {
        Usuario host = mockUsuario("Host");
        Usuario passageiro = mockUsuario("Passageiro");
        Carona carona = mockCarona(5, host, Estado.ABERTO, "Gragoata");
        doThrow(new UsuarioNaoEncontradoException()).when(carona).removePassageiro(passageiro);

        configurarListaCaronas(passageiro, carona);
        when(passageiro.getPeloId(5)).thenReturn(carona);
        Console console = mockConsole("5 1", passageiro);

        CaronaManager.verCorridas(console);

        String saida = saida();
        assertTrue(saida.contains("Nao foi possivel remover o passageiro da carona."));
        assertTrue(saida.contains("UsuarioNaoEncontradoException"));
        verify(carona).removePassageiro(passageiro);
        verify(console).setStage(Stage.LOBBY);
    }

    @ParameterizedTest(name = "CT-U-CM-14: Passageiro em estado {0} nao possui opcoes disponiveis")
    @EnumSource(value = Estado.class, names = {"AGUARDANDO", "FECHADO", "FINALIZADO"})
    @DisplayName("CT-U-CM-14: Passageiro em estados sem acao disponivel exibe mensagem padrao")
    void ct14_passageiro_estadosSemOpcoes_exibeSemOpcoes(Estado estado) throws UsuarioNaoEncontradoException {
        Usuario host = mockUsuario("Host");
        Usuario passageiro = mockUsuario("Passageiro");
        Carona carona = mockCarona(5, host, estado, "Gragoata");
        configurarListaCaronas(passageiro, carona);
        when(passageiro.getPeloId(5)).thenReturn(carona);
        Console console = mockConsole("5", passageiro);

        CaronaManager.verCorridas(console);

        String saida = saida();
        assertTrue(saida.contains("Sem op") && saida.contains("dispon"));
        verify(carona, never()).removePassageiro(any(Usuario.class));
        verify(console).setStage(Stage.LOBBY);
    }
}
