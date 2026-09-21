package me.br.caronapp.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConsoleTest {

    private java.io.InputStream entradaOriginal;

    @BeforeEach
    void setUp() {
        entradaOriginal = System.in;
    }

    @AfterEach
    void tearDown() {
        System.setIn(entradaOriginal);
    }

    @Test
    void CT_U_C_11_draw_loginRegistro_opcaoLogin() {
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));

        Console console = new Console();

        boolean resultado = console.draw();

        assertTrue(resultado);

        // Caso seja adicionado getStage():
        // assertEquals(Console.Stage.LOGIN, console.getStage());
    }

    @Test
    void CT_U_C_12_draw_loginRegistro_opcaoRegistro() {
        System.setIn(new ByteArrayInputStream("2\n".getBytes()));

        Console console = new Console();

        boolean resultado = console.draw();

        assertTrue(resultado);

        // Caso seja adicionado getStage():
        // assertEquals(Console.Stage.REGISTRO, console.getStage());
    }
}
