package me.br.caronapp.web;

import static org.junit.jupiter.api.Assertions.*;

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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.usuario.Usuario;

/*
 * Selenium #1 - Telas de Cadastro e Login da interface web (Danillo).
 *
 * A interface web (WebMain) tem uma pagina inicial com dois formularios: "Criar conta"
 * (envia para /cadastro) e "Entrar" (envia para /login). Cada teste sobe o servidor em
 * uma porta livre, com o arquivo de dados numa pasta temporaria, e usa o Chrome em modo
 * headless (sem abrir janela). O Selenium Manager baixa o ChromeDriver compativel.
 *
 * Textos com acento usam escapes unicode para nao depender do encoding do arquivo-fonte.
 */
class CadastroLoginSeleniumTest {

	private static final String CADASTRO_OK = "Cadastro realizado com sucesso.";
	private static final String LOGIN_OK = "Login realizado com sucesso.";
	private static final String USUARIO_DUPLICADO = "Usuário já cadastrado.";
	private static final String LOGIN_INVALIDO = "Usuário ou senha inválidos.";
	private static final String SAIU = "Você saiu da conta.";

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
		if (driver != null) driver.quit();
	}

	@BeforeEach
	void sobeServidor() throws Exception {
		usuariosOriginais = Auxiliar.usuarios;
		caronasOriginais = Auxiliar.caronasAtivas;
		serialOriginal = Carona.getSerial();
		Auxiliar.usuarios = new ArrayList<>();
		Auxiliar.caronasAtivas = new ArrayList<>();
		Carona.setSerial(0);

		iniciaServidor();
		driver.manage().deleteAllCookies();
	}

	@AfterEach
	void derrubaServidor() {
		if (app != null) app.close();
		Auxiliar.usuarios = usuariosOriginais;
		Auxiliar.caronasAtivas = caronasOriginais;
		Carona.setSerial(serialOriginal);
	}

	// ---------- helpers ----------

	private void iniciaServidor() throws Exception {
		app = new WebMain(0, pasta.resolve("web-data.bin"));
		app.iniciar();
	}

	private String url() {
		return "http://localhost:" + app.getPorta() + "/";
	}

	private void abreInicio() {
		driver.get(url());
		espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-form")));
	}

	private static void preenche(String id, String valor) {
		WebElement campo = driver.findElement(By.id(id));
		campo.clear();
		campo.sendKeys(valor);
	}

	private void cadastra(String username, String nome, String cpf, String senha) {
		abreInicio();
		preenche("cadastro-username", username);
		preenche("cadastro-nome", nome);
		preenche("cadastro-cpf", cpf);
		preenche("cadastro-senha", senha);
		driver.findElement(By.id("cadastro-submit")).click();
	}

	private void loga(String username, String senha) {
		abreInicio();
		preenche("login-username", username);
		preenche("login-senha", senha);
		driver.findElement(By.id("login-submit")).click();
	}

	private void sai() {
		driver.findElement(By.id("logout-submit")).click();
		assertEquals(SAIU, mensagem());
	}

	private static String mensagem() {
		return espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("mensagem"))).getText();
	}

	private static boolean existe(String id) {
		return !driver.findElements(By.id(id)).isEmpty();
	}

	// ---------- CT-S-SM-01 ----------
	@Test
	void ctSSm01_cadastroComSucessoEntraNoSistema() {
		cadastra("maria", "Maria Silva", "12345678900", "s3nha");

		assertEquals(CADASTRO_OK, mensagem());
		assertEquals("Maria Silva", driver.findElement(By.id("usuario-logado")).getText());
		assertTrue(existe("lista-caronas"), "depois do cadastro o usuario deveria ver a area logada (lobby)");
		assertFalse(existe("cadastro-form"), "o formulario de cadastro nao deveria aparecer para usuario logado");
		assertEquals(1, Auxiliar.usuarios.size());
	}

	// ---------- CT-S-SM-02 ----------
	@Test
	void ctSSm02_cadastroDeUsuarioDuplicadoERecusado() {
		cadastra("joao", "Joao", "111", "abc");
		assertEquals(CADASTRO_OK, mensagem());
		sai();

		cadastra("JOAO", "Outro Joao", "222", "xyz");

		assertEquals(USUARIO_DUPLICADO, mensagem());
		assertFalse(existe("usuario-logado"), "cadastro recusado nao pode autenticar ninguem");
		assertTrue(existe("cadastro-form"));
		assertEquals(1, Auxiliar.usuarios.size());
	}

	// ---------- CT-S-SM-03 ----------
	@Test
	void ctSSm03_loginComSenhaErradaMostraErro() {
		cadastra("ana", "Ana", "333", "certa");
		sai();

		loga("ana", "errada");

		assertEquals(LOGIN_INVALIDO, mensagem());
		assertFalse(existe("usuario-logado"));
		assertTrue(existe("login-form"), "deveria continuar na tela de login");
	}

	// ---------- CT-S-SM-04 ----------
	@Test
	void ctSSm04_loginComUsuarioInexistenteMostraErro() {
		loga("ninguem", "qualquer");

		assertEquals(LOGIN_INVALIDO, mensagem());
		assertFalse(existe("usuario-logado"));
	}

	// ---------- CT-S-SM-05 ----------
	@Test
	void ctSSm05_loginComSenhaCertaEntraNoLobby() {
		cadastra("bia", "Bia Costa", "444", "p@ss");
		sai();

		loga("bia", "p@ss");

		assertEquals(LOGIN_OK, mensagem());
		assertEquals("Bia Costa", driver.findElement(By.id("usuario-logado")).getText());
		assertTrue(existe("lista-caronas"));
		assertTrue(existe("carona-form"), "usuario logado deveria ver o formulario de oferecer carona");
	}

	// ---------- CT-S-SM-06 ----------
	@Test
	void ctSSm06_camposObrigatoriosNaoEnviamFormulario() {
		abreInicio();
		preenche("cadastro-username", "semsenha");
		preenche("cadastro-nome", "Sem Senha");
		preenche("cadastro-cpf", "555");
		driver.findElement(By.id("cadastro-submit")).click();

		// O atributo "required" impede o envio: nenhuma mensagem e nenhum usuario criado.
		assertFalse(existe("mensagem"));
		assertTrue(existe("cadastro-form"));
		assertEquals(0, Auxiliar.usuarios.size());
		String validacao = driver.findElement(By.id("cadastro-senha")).getAttribute("validationMessage");
		assertFalse(validacao == null || validacao.isEmpty(), "o navegador deveria avisar que a senha e obrigatoria");
	}

	// ---------- CT-S-SM-07 ----------
	@Test
	void ctSSm07_cadastroContinuaValidoAposReiniciarServidor() throws Exception {
		cadastra("leo", "Leo", "666", "segredo");
		assertEquals(CADASTRO_OK, mensagem());

		// Reinicia o servidor com o mesmo arquivo: a sessao some, o cadastro fica.
		app.close();
		Auxiliar.usuarios = new ArrayList<>();
		Auxiliar.caronasAtivas = new ArrayList<>();
		iniciaServidor();
		driver.manage().deleteAllCookies();

		loga("leo", "segredo");

		assertEquals(LOGIN_OK, mensagem());
		assertEquals("Leo", driver.findElement(By.id("usuario-logado")).getText());
		List<Usuario> recarregados = Auxiliar.usuarios;
		assertEquals(1, recarregados.size());
	}
}
