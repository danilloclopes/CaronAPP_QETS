package me.br.caronapp.console.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Carona.Estado;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.Rota.Sentido;
import me.br.caronapp.carona.Rota;
import me.br.caronapp.carona.excecoes.VagasExcedidasException;
import me.br.caronapp.console.Console.Stage;
import me.br.caronapp.console.Console;
import me.br.caronapp.usuario.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Etapa 1: coordenacao com dominio simulado")
public class HostJoinDominioSimuladoTest {
    private ArrayList<Carona> caronasOriginais;
    private Scanner scanner;
    private final Logger logger = Logger.getLogger(HostJoin.class.getName());
    private ByteArrayOutputStream saida;
    private StreamHandler handler;

    @BeforeEach
    void preparar() {
        saida = new ByteArrayOutputStream();
        handler = new StreamHandler(saida, new SimpleFormatter());
        logger.addHandler(handler);
        caronasOriginais = new ArrayList<>(Auxiliar.caronasAtivas);
        Auxiliar.caronasAtivas.clear();
    }

    @AfterEach
    void restaurar() {
        logger.removeHandler(handler);
        handler.close();
        if (scanner != null) {
            scanner.close();
        }
        Auxiliar.caronasAtivas.clear();
        Auxiliar.caronasAtivas.addAll(caronasOriginais);
    }

    @ParameterizedTest
    @CsvSource({
        "1, IDA, sucesso",
        "2, VOLTA, sucesso",
        "1, IDA, recusado",
        "1, IDA, lotado"
    })
    void coordenarParticipacaoComDominioSimulado(int opcao, Sentido sentido, String resultado)
            throws VagasExcedidasException {
        Console console = mock(Console.class);
        Usuario passageiro = mock(Usuario.class);
        Usuario motorista = mock(Usuario.class);
        Rota rota = mock(Rota.class);
        Carona carona = mock(Carona.class);
        scanner = new Scanner(opcao + " 999 42");

        when(console.getScanner()).thenReturn(scanner);
        when(console.getUser()).thenReturn(passageiro);
        when(carona.getID()).thenReturn(42);
        when(carona.getEstado()).thenReturn(Estado.ABERTO);
        when(carona.getRota()).thenReturn(rota);
        when(rota.getSentido()).thenReturn(sentido);
        when(carona.getHost()).thenReturn(motorista);
        when(motorista.getName()).thenReturn("Motorista stub");
        if (resultado.equals("lotado")) {
            when(carona.adicionaPassageiro(passageiro)).thenThrow(new VagasExcedidasException());
        } else {
            when(carona.adicionaPassageiro(passageiro)).thenReturn(resultado.equals("sucesso"));
        }
        Auxiliar.caronasAtivas.add(carona);

        HostJoin.join(console);

        handler.flush();
        String mensagem = resultado.equals("sucesso") ? "efetuado com sucesso"
                : resultado.equals("recusado") ? "Falha." : "vagas dispon";
        assertTrue(saida.toString().contains(mensagem));
        verify(carona).adicionaPassageiro(passageiro);
        verify(console, atLeastOnce()).setStage(Stage.LOBBY);
        assertFalse(scanner.hasNext(), "Deve repetir o ID invalido e consumir o ID valido");
    }
}
