package me.br.caronapp.web;

import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.usuario.Usuario;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WebMainTest {
    @TempDir Path pasta;
    private WebMain app;
    private ArrayList<Usuario> usuarios;
    private ArrayList<Carona> caronas;
    private int serial;

    @BeforeEach void iniciar() throws Exception {
        usuarios = Auxiliar.usuarios;
        caronas = Auxiliar.caronasAtivas;
        serial = Carona.getSerial();
        Auxiliar.usuarios = new ArrayList<>();
        Auxiliar.caronasAtivas = new ArrayList<>();
        Carona.setSerial(0);
        app = new WebMain(0, pasta.resolve("dados.bin"));
        app.iniciar();
    }

    @AfterEach void encerrar() {
        if (app != null) app.close();
        Auxiliar.usuarios = usuarios;
        Auxiliar.caronasAtivas = caronas;
        Carona.setSerial(serial);
    }

    private HttpClient cliente() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    private HttpResponse<String> get(HttpClient c) throws Exception {
        return c.send(HttpRequest.newBuilder(URI.create("http://localhost:" + app.getPorta() + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String post(HttpClient c, String rota, String... campos) throws Exception {
        StringJoiner corpo = new StringJoiner("&");
        for (int i = 0; i < campos.length; i += 2)
            corpo.add(URLEncoder.encode(campos[i], "UTF-8") + "=" + URLEncoder.encode(campos[i + 1], "UTF-8"));
        HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create("http://localhost:" + app.getPorta() + rota))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(corpo.toString())).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r.statusCode(), r.body());
        return r.body();
    }

    @Test void fluxoComVagasSessoesEReinicio() throws Exception {
        HttpClient motorista = cliente(), passageiro = cliente(), terceiro = cliente();
        assertTrue(post(terceiro, "/caronas/entrar", "id", "1").contains("Faça login"));
        assertTrue(post(motorista, "/cadastro", "username", "ana", "nome", "Ana <script>", "cpf", "111", "senha", "123")
                .contains("Ana &lt;script&gt;"));
        assertTrue(post(terceiro, "/cadastro", "username", "ANA", "nome", "Outra", "cpf", "222", "senha", "123")
                .contains("Usuário já cadastrado"));
        post(passageiro, "/cadastro", "username", "bia", "nome", "Bia", "cpf", "222", "senha", "123");
        post(terceiro, "/cadastro", "username", "leo", "nome", "Leo", "cpf", "333", "senha", "123");
        assertTrue(post(motorista, "/caronas/criar", "vagas", "0").contains("de 1 a 4"));
        assertEquals(0, Auxiliar.caronasAtivas.size());
        post(motorista, "/caronas/criar", "vagas", "1", "data", LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString(),
                "sentido", "IDA", "campus", "IACS", "latitude", "-22.9", "longitude", "-43.1", "rua", "Rua A", "numero", "10", "cep", "24000000");
        assertTrue(post(passageiro, "/caronas/entrar", "id", "1").contains("Participação confirmada"));
        assertTrue(post(passageiro, "/caronas/entrar", "id", "1").contains("você já participa"));
        assertTrue(post(terceiro, "/caronas/entrar", "id", "1").contains("Não há vagas"));
        assertTrue(post(passageiro, "/caronas/finalizar", "id", "1").contains("Somente o motorista"));
        assertTrue(post(passageiro, "/caronas/entrar", "id", "999").contains("Carona não encontrada"));

        app.close();
        app = new WebMain(0, pasta.resolve("dados.bin"));
        app.iniciar();
        assertFalse(get(passageiro).body().contains("id='usuario-logado'"));
        assertTrue(post(passageiro, "/login", "username", "bia", "senha", "errada").contains("Usuário ou senha inválidos"));
        post(passageiro, "/login", "username", "bia", "senha", "123");
        assertTrue(post(passageiro, "/caronas/sair", "id", "1").contains("Você saiu da carona"));
        assertEquals(0, Auxiliar.caronasAtivas.get(0).getVagasPreenchidas());
        assertTrue(Auxiliar.logar("bia", "123").getCaronas().isEmpty());
        assertSame(Auxiliar.logar("ana", "123"), Auxiliar.caronasAtivas.get(0).getHost());
    }

    @Test void partidaEFinalizacaoMantemEstadoEHistorico() throws Exception {
        HttpClient motorista = cliente();
        post(motorista, "/cadastro", "username", "ana", "nome", "Ana", "cpf", "111", "senha", "123");
        post(motorista, "/caronas/criar", "vagas", "4", "data", LocalDateTime.now().plusMinutes(20).withSecond(0).withNano(0).toString(),
                "sentido", "VOLTA", "campus", "GRAGOATA", "latitude", "-22.9", "longitude", "-43.1", "rua", "Rua B", "numero", "20", "cep", "24000000");
        assertEquals(Carona.Estado.AGUARDANDO, Auxiliar.caronasAtivas.get(0).getEstado());
        assertTrue(post(motorista, "/caronas/partir", "id", "1").contains("Partida confirmada"));
        get(motorista);
        assertEquals(Carona.Estado.FECHADO, Auxiliar.caronasAtivas.get(0).getEstado());
        assertTrue(post(motorista, "/caronas/finalizar", "id", "1").contains("Carona finalizada"));
        assertEquals(0, Auxiliar.usuarios.get(0).getCaronas().size());
        assertEquals(1, Auxiliar.usuarios.get(0).getHistorico().size());
    }
}
