package me.br.caronapp.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.usuario.Usuario;

/** Testes de sistema da oferta de caronas pela interface web. */
@EnabledIfSystemProperty(named = "selenium", matches = "true")
public class HostJoinSeleniumTest {

    @TempDir
    Path pasta;

    private WebDriver driver;
    private WebDriverWait wait;
    private WebMain app;
    private ArrayList<Usuario> usuariosOriginais;
    private ArrayList<Carona> caronasOriginais;
    private int serialOriginal;
    private String data;

    @BeforeEach
    public void abrirNavegador() throws Exception {
        usuariosOriginais = Auxiliar.usuarios;
        caronasOriginais = Auxiliar.caronasAtivas;
        serialOriginal = Carona.getSerial();
        Auxiliar.usuarios = new ArrayList<>();
        Auxiliar.caronasAtivas = new ArrayList<>();
        Carona.setSerial(0);
        app = new WebMain(0, pasta.resolve("dados.bin"));
        app.iniciar();

        ChromeOptions options = new ChromeOptions();
        if (Boolean.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get("http://127.0.0.1:" + app.getPorta());
        assertEquals("CaronAPP", driver.getTitle());

        driver.findElement(By.id("cadastro-username")).sendKeys("ricardo");
        driver.findElement(By.id("cadastro-nome")).sendKeys("Ricardo Selenium");
        driver.findElement(By.id("cadastro-cpf")).sendKeys("12345678900");
        driver.findElement(By.id("cadastro-senha")).sendKeys("SenhaTeste123");
        driver.findElement(By.id("cadastro-submit")).click();
        assertEquals("Ricardo Selenium", wait.until(d -> d.findElement(By.id("usuario-logado"))).getText());
        data = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    @AfterEach
    public void fecharNavegador() {
        try {
            if (driver != null) {
                driver.quit();
            }
        } finally {
            if (app != null) {
                app.close();
            }
            Auxiliar.usuarios = usuariosOriginais;
            Auxiliar.caronasAtivas = caronasOriginais;
            Carona.setSerial(serialOriginal);
        }
    }

    @Test
    public void criarCaronaDeIda() {
        criarEConferirCarona("IDA", "2", "Indo para a UFF");
    }

    @Test
    public void criarCaronaDeVolta() {
        criarEConferirCarona("VOLTA", "2", "Saindo da UFF");
    }

    // Valores limite: imediatamente dentro do intervalo permitido.
    @ParameterizedTest
    @ValueSource(strings = {"1", "4"})
    public void criarCaronaComLimiteValidoDePassageiros(String vagas) {
        criarEConferirCarona("IDA", vagas, "Indo para a UFF");
    }

    // Valores limite: imediatamente fora do intervalo permitido.
    @ParameterizedTest
    @ValueSource(strings = {"0", "5"})
    public void rejeitarLimiteInvalidoDePassageiros(String vagas) {
        preencherCarona("IDA", vagas);
        verificarCampoInvalido("carona-vagas");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "carona-vagas", "carona-data", "carona-rua", "carona-numero",
        "carona-cep", "carona-latitude", "carona-longitude"
    })
    public void rejeitarCampoObrigatorioVazio(String campo) {
        preencherCarona("IDA", "2");
        driver.findElement(By.id(campo)).clear();
        verificarCampoInvalido(campo);
    }

    @ParameterizedTest
    @CsvSource({
        "carona-vagas, 1.5",
        "carona-numero, -1",
        "carona-latitude, -91",
        "carona-latitude, 91",
        "carona-longitude, -181",
        "carona-longitude, 181"
    })
    public void rejeitarCampoNumericoInvalido(String campo, String valor) {
        preencherCarona("IDA", "2");
        driver.findElement(By.id(campo)).clear();
        driver.findElement(By.id(campo)).sendKeys(valor);
        verificarCampoInvalido(campo);
    }

    @Test
    public void rejeitarRuaSomenteComEspacosNoServidor() {
        preencherCarona("IDA", "2");
        driver.findElement(By.id("carona-rua")).clear();
        driver.findElement(By.id("carona-rua")).sendKeys("   ");
        driver.findElement(By.id("carona-submit")).click();
        assertEquals("Preencha o campo rua.", wait.until(d -> d.findElement(By.id("mensagem"))).getText());
        assertTrue(driver.findElements(By.cssSelector("#minhas article")).isEmpty());
        driver.navigate().refresh();
        assertTrue(driver.findElements(By.cssSelector("#lista-caronas article")).isEmpty());
    }

    private void preencherCarona(String sentido, String vagas) {
        driver.findElement(By.id("carona-vagas")).sendKeys(vagas);
        // datetime-local varia com o idioma do navegador; definimos apenas o valor,
        // preservando a validacao HTML e o envio pelo clique real no formulario.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];",
                driver.findElement(By.id("carona-data")), data);
        new Select(driver.findElement(By.id("carona-sentido"))).selectByValue(sentido);
        new Select(driver.findElement(By.id("carona-campus"))).selectByValue("IACS");
        driver.findElement(By.id("carona-rua")).sendKeys("Rua das Flores");
        driver.findElement(By.id("carona-numero")).sendKeys("10");
        driver.findElement(By.id("carona-cep")).sendKeys("24000-000");
        driver.findElement(By.id("carona-latitude")).sendKeys("-22.9");
        driver.findElement(By.id("carona-longitude")).sendKeys("-43.1");
    }

    private void criarEConferirCarona(String sentido, String vagas, String descricao) {
        preencherCarona(sentido, vagas);
        driver.findElement(By.id("carona-submit")).click();
        assertEquals("Carona criada com sucesso.", wait.until(d -> d.findElement(By.id("mensagem"))).getText());
        assertEquals(1, driver.findElements(By.cssSelector("#lista-caronas article")).size());
        assertEquals(1, driver.findElements(By.cssSelector("#minhas article")).size());
        WebElement carona = driver.findElement(By.cssSelector("#minhas article"));
        assertTrue(carona.getText().contains(descricao));
        assertTrue(carona.getText().contains("Ricardo Selenium"));
        assertTrue(carona.getText().contains("Rua das Flores, 10"));
        assertTrue(carona.getText().contains("24000-000"));
        assertTrue(carona.getText().contains(LocalDateTime.parse(data)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        assertEquals(vagas, carona.findElement(By.className("vagas")).getText());
        assertEquals("ABERTO", carona.findElement(By.className("estado")).getText());
        driver.navigate().refresh();
        assertEquals(1, driver.findElements(By.cssSelector("#minhas article")).size());
    }

    private void verificarCampoInvalido(String campo) {
        String url = driver.getCurrentUrl();
        driver.findElement(By.id("carona-submit")).click();
        assertEquals(url, driver.getCurrentUrl());
        assertFalse(driver.findElement(By.id(campo)).getDomProperty("validationMessage").isEmpty());
        assertTrue(driver.findElements(By.cssSelector("#minhas article")).isEmpty());
        driver.navigate().refresh();
        assertTrue(driver.findElements(By.cssSelector("#lista-caronas article")).isEmpty());
    }
}
