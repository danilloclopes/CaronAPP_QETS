package me.br.caronapp.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.usuario.Usuario;

/**
 * Testes de sistema (Selenium) para o fluxo de participacao em caronas no WebMain:
 * entrada em carona, impedimento de participacao duplicada ou sem vagas e saida com
 * liberacao da vaga.
 */
class CaronaParticipacaoSeleniumTest {

    private static final String CADASTRO_OK = "Cadastro realizado com sucesso.";
    private static final String LOGIN_OK = "Login realizado com sucesso.";
    private static final String SAIU_CONTA = "Voc\u00ea saiu da conta.";
    private static final String CARONA_CRIADA = "Carona criada com sucesso.";
    private static final String PARTICIPACAO_OK = "Participa\u00e7\u00e3o confirmada.";
    private static final String SAIU_CARONA = "Voc\u00ea saiu da carona.";
    private static final String ENTRADA_RECUSADA =
            "N\u00e3o foi poss\u00edvel entrar: voc\u00ea j\u00e1 participa ou a carona n\u00e3o est\u00e1 aberta.";
    private static final String SEM_VAGAS = "N\u00e3o h\u00e1 vagas dispon\u00edveis nessa carona.";

    private static WebDriver driver;
    private static WebDriverWait espera;

    @TempDir
    Path pasta;

    private WebMain app;
    private ArrayList<Usuario> usuariosOriginais;
    private ArrayList<Carona> caronasOriginais;
    private int serialOriginal;

    @BeforeAll
    static void abreNavegador() {
        ChromeOptions opcoes = new ChromeOptions();
        opcoes.addArguments("--headless=new", "--window-size=1280,900", "--disable-gpu", "--no-sandbox");
        driver = new ChromeDriver(opcoes);
        espera = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @AfterAll
    static void fechaNavegador() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void sobeServidor() throws Exception {
        usuariosOriginais = Auxiliar.usuarios;
        caronasOriginais = Auxiliar.caronasAtivas;
        serialOriginal = Carona.getSerial();
        Auxiliar.usuarios = new ArrayList<>();
        Auxiliar.caronasAtivas = new ArrayList<>();
        Carona.setSerial(0);

        app = new WebMain(0, pasta.resolve("web-data.bin"));
        app.iniciar();
        driver.manage().deleteAllCookies();
    }

    @AfterEach
    void derrubaServidor() {
        if (app != null) {
            app.close();
        }
        Auxiliar.usuarios = usuariosOriginais;
        Auxiliar.caronasAtivas = caronasOriginais;
        Carona.setSerial(serialOriginal);
    }

    private String url() {
        return "http://localhost:" + app.getPorta() + "/";
    }

    private static void preenche(String id, String valor) {
        WebElement campo = driver.findElement(By.id(id));
        campo.clear();
        campo.sendKeys(valor);
    }

    private static String executarEAguardarMensagem(Runnable acao) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.__caronappToken = 'antigo';");
        acao.run();
        return new WebDriverWait(driver, Duration.ofSeconds(5))
                .ignoring(WebDriverException.class)
                .until(d -> {
                    Boolean pronto = (Boolean) ((JavascriptExecutor) d).executeScript(
                            "return window.__caronappToken === undefined && document.readyState === 'complete';");
                    if (!Boolean.TRUE.equals(pronto)) {
                        return null;
                    }
                    WebElement el = d.findElement(By.id("mensagem"));
                    return el.isDisplayed() ? el.getText() : null;
                });
    }

    private void cadastra(String username, String nome) {
        driver.get(url());
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("cadastro-form")));
        preenche("cadastro-username", username);
        preenche("cadastro-nome", nome);
        preenche("cadastro-cpf", "12345678900");
        preenche("cadastro-senha", "senha123");
        assertEquals(CADASTRO_OK, executarEAguardarMensagem(() -> driver.findElement(By.id("cadastro-submit")).click()));
    }

    private void loga(String username) {
        driver.get(url());
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-form")));
        preenche("login-username", username);
        preenche("login-senha", "senha123");
        assertEquals(LOGIN_OK, executarEAguardarMensagem(() -> driver.findElement(By.id("login-submit")).click()));
    }

    private void saiDaConta() {
        assertEquals(SAIU_CONTA, executarEAguardarMensagem(() -> driver.findElement(By.id("logout-submit")).click()));
    }

    private void ofereceCarona(int vagas) {
        preenche("carona-vagas", String.valueOf(vagas));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('carona-data').value = arguments[0];",
                "2027-10-20T14:30");
        new Select(driver.findElement(By.id("carona-sentido"))).selectByValue("IDA");
        new Select(driver.findElement(By.id("carona-campus"))).selectByValue("PRAIAVERMELHA");
        preenche("carona-rua", "Rua Passo da Patria");
        preenche("carona-numero", "156");
        preenche("carona-cep", "24210-240");
        preenche("carona-latitude", "-22.9064");
        preenche("carona-longitude", "-43.1332");
        assertEquals(CARONA_CRIADA, executarEAguardarMensagem(() -> driver.findElement(By.id("carona-submit")).click()));
    }

    private void preparaCaronaComoMotorista(String usernameMotorista, String nomeMotorista, int vagas) {
        cadastra(usernameMotorista, nomeMotorista);
        ofereceCarona(vagas);
        saiDaConta();
    }

    private String enviaPostDiretoParaEntrar(int caronaId) {
        return executarEAguardarMensagem(() -> ((JavascriptExecutor) driver).executeScript(
                "const f = document.createElement('form');"
                        + "f.method = 'POST';"
                        + "f.action = '/caronas/entrar';"
                        + "const i = document.createElement('input');"
                        + "i.type = 'hidden'; i.name = 'id'; i.value = arguments[0];"
                        + "f.appendChild(i); document.body.appendChild(f); f.submit();",
                String.valueOf(caronaId)));
    }

    private String vagasDisponiveisNaSecao(String secaoId, int caronaId) {
        return driver.findElement(By.cssSelector(
                "#" + secaoId + " article[data-carona-id='" + caronaId + "'] .vagas")).getText();
    }

    private List<WebElement> cardsNaSecao(String secaoId, int caronaId) {
        return driver.findElements(By.cssSelector(
                "#" + secaoId + " article[data-carona-id='" + caronaId + "']"));
    }

    private boolean botaoExiste(String buttonId) {
        return !driver.findElements(By.id(buttonId)).isEmpty();
    }

    @Test
    void ctSCm01_passageiroEntraNaCaronaEReduzVagasDisponiveis() {
        preparaCaronaComoMotorista("motorista1", "Carlos Motorista", 2);

        cadastra("pass1", "Lucas Passageiro");
        assertEquals("2", vagasDisponiveisNaSecao("disponiveis", 1));
        assertTrue(botaoExiste("entrar-1"));

        String msg = executarEAguardarMensagem(() -> driver.findElement(By.id("entrar-1")).click());

        assertEquals(PARTICIPACAO_OK, msg);
        assertEquals("1", vagasDisponiveisNaSecao("disponiveis", 1));
        assertFalse(botaoExiste("entrar-1"), "Botao de entrar deve sumir apos confirmar participacao");
        assertEquals(1, cardsNaSecao("minhas", 1).size());
        assertTrue(botaoExiste("sair-1"), "Passageiro deve visualizar botao para sair da carona aberta");
    }

    @Test
    void ctSCm02_impedeParticipacaoDuplicadaDoMesmoPassageiro() {
        preparaCaronaComoMotorista("motorista2", "Carlos Motorista", 2);

        cadastra("pass2", "Marina Passageira");
        assertEquals(PARTICIPACAO_OK, executarEAguardarMensagem(() -> driver.findElement(By.id("entrar-1")).click()));
        assertEquals("1", vagasDisponiveisNaSecao("disponiveis", 1));
        assertFalse(botaoExiste("entrar-1"));

        String msg = enviaPostDiretoParaEntrar(1);

        assertEquals(ENTRADA_RECUSADA, msg);
        assertEquals("1", vagasDisponiveisNaSecao("disponiveis", 1),
                "Tentativa duplicada nao pode consumir outra vaga");
        assertEquals(1, cardsNaSecao("minhas", 1).size());
    }

    @Test
    void ctSCm03_impedeParticipacaoDoProprioMotoristaNaCarona() {
        cadastra("motorista3", "Ana Motorista");
        ofereceCarona(2);

        assertFalse(botaoExiste("entrar-1"), "Motorista nao deve ver botao para entrar na propria carona");
        assertEquals("2", vagasDisponiveisNaSecao("disponiveis", 1));

        String msg = enviaPostDiretoParaEntrar(1);

        assertEquals(ENTRADA_RECUSADA, msg);
        assertEquals("2", vagasDisponiveisNaSecao("disponiveis", 1));
        assertFalse(botaoExiste("sair-1"), "Motorista nao deve ter botao de sair da carona");
    }

    @Test
    void ctSCm04_impedeParticipacaoQuandoNaoHaVagasDisponiveis() {
        preparaCaronaComoMotorista("motorista4", "Pedro Motorista", 1);

        cadastra("passA", "Primeiro Passageiro");
        assertEquals(PARTICIPACAO_OK, executarEAguardarMensagem(() -> driver.findElement(By.id("entrar-1")).click()));
        assertEquals("0", vagasDisponiveisNaSecao("disponiveis", 1));
        saiDaConta();

        cadastra("passB", "Segundo Passageiro");
        assertEquals("0", vagasDisponiveisNaSecao("disponiveis", 1));
        assertFalse(botaoExiste("entrar-1"), "Sem vagas, botao entrar nao deve ser exibido");

        String msg = enviaPostDiretoParaEntrar(1);

        assertEquals(SEM_VAGAS, msg);
        assertEquals("0", vagasDisponiveisNaSecao("disponiveis", 1));
        assertTrue(cardsNaSecao("minhas", 1).isEmpty(),
                "Passageiro barrado por falta de vagas nao deve aparecer em Minhas corridas");
    }

    @Test
    void ctSCm05_passageiroSaiDaCaronaELiberaVagaParaOutroUsuario() {
        preparaCaronaComoMotorista("motorista5", "Joao Motorista", 1);

        cadastra("pass1", "Primeiro Passageiro");
        assertEquals(PARTICIPACAO_OK, executarEAguardarMensagem(() -> driver.findElement(By.id("entrar-1")).click()));
        assertEquals("0", vagasDisponiveisNaSecao("disponiveis", 1));
        saiDaConta();

        // Segundo passageiro comprova que nao havia vaga antes da saida do primeiro.
        cadastra("pass2", "Segundo Passageiro");
        assertFalse(botaoExiste("entrar-1"));
        saiDaConta();

        // Primeiro passageiro sai da carona e libera a vaga.
        loga("pass1");
        String msgSaida = executarEAguardarMensagem(() -> driver.findElement(By.id("sair-1")).click());

        assertEquals(SAIU_CARONA, msgSaida);
        assertTrue(cardsNaSecao("minhas", 1).isEmpty(), "Carona deve sair de Minhas corridas");
        assertEquals("1", vagasDisponiveisNaSecao("disponiveis", 1), "Vaga deve voltar para 1 apos saida");
        assertTrue(botaoExiste("entrar-1"), "Botao de entrar deve reaparecer apos liberar vaga");
        saiDaConta();

        // Segundo passageiro agora consegue ocupar a vaga liberada.
        loga("pass2");
        assertEquals("1", vagasDisponiveisNaSecao("disponiveis", 1));
        assertEquals(PARTICIPACAO_OK, executarEAguardarMensagem(() -> driver.findElement(By.id("entrar-1")).click()));
        assertEquals("0", vagasDisponiveisNaSecao("disponiveis", 1));
        assertEquals(1, cardsNaSecao("minhas", 1).size());
    }
}
