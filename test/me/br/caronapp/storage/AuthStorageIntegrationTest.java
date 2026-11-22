package me.br.caronapp.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Campus;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.PontosDeEncontro;
import me.br.caronapp.carona.Posicao;
import me.br.caronapp.carona.Rota;
import me.br.caronapp.console.Console;
import me.br.caronapp.console.Console.Stage;
import me.br.caronapp.console.util.LoginRegistro;
import me.br.caronapp.usuario.Usuario;

/*
 * Teste de Integracao #1 - LoginRegistro + Auxiliar + StorageManager (sem mocks).
 *
 * Fluxo testado, igual ao do sistema real (Main):
 *   1. o usuario se cadastra pela tela de registro (LoginRegistro.registro -> Auxiliar.register);
 *   2. o sistema salva o estado em disco (StorageManager.salva);
 *   3. o programa "reinicia": a memoria (Auxiliar) e limpa e os dados sao recarregados
 *      (StorageManager.carrega);
 *   4. o usuario faz login com o cadastro recarregado (LoginRegistro.login -> Auxiliar.logar).
 *
 * Unico isolamento: os arquivos ficam numa pasta temporaria e a entrada do teclado e
 * simulada com System.setIn (o Console le de System.in).
 */
class AuthStorageIntegrationTest {

	@TempDir
	Path pasta;

	private StorageManager storage;

	private InputStream entradaOriginal;
	private PrintStream saidaOriginal;
	private ByteArrayOutputStream saida;

	private int serialOriginal;
	private ArrayList<Usuario> auxUsuariosOriginal;
	private ArrayList<Carona> auxCaronasOriginal;

	@BeforeEach
	void setUp() throws Exception {
		storage = new StorageManager(pasta.resolve("usuarios.bin").toString(), pasta.resolve("caronas.bin").toString());

		entradaOriginal = System.in;
		saidaOriginal = System.out;
		saida = new ByteArrayOutputStream();
		System.setOut(new PrintStream(saida, true, "UTF-8"));

		serialOriginal = Carona.getSerial();
		Carona.setSerial(0);
		auxUsuariosOriginal = new ArrayList<Usuario>(Auxiliar.usuarios);
		auxCaronasOriginal = new ArrayList<Carona>(Auxiliar.caronasAtivas);
		Auxiliar.usuarios.clear();
		Auxiliar.caronasAtivas.clear();
	}

	@AfterEach
	void tearDown() {
		System.setIn(entradaOriginal);
		System.setOut(saidaOriginal);
		Carona.setSerial(serialOriginal);
		Auxiliar.usuarios.clear();
		Auxiliar.usuarios.addAll(auxUsuariosOriginal);
		Auxiliar.caronasAtivas.clear();
		Auxiliar.caronasAtivas.addAll(auxCaronasOriginal);
	}

	// ---------- helpers ----------

	// Cria um Console real cujo teclado digita o texto informado.
	private static Console consoleDigitando(String... linhas) {
		String texto = String.join("\n", linhas) + "\n";
		System.setIn(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)));
		return new Console();
	}

	// Ordem lida por LoginRegistro.usernamePassword(console, true): usuario, senha, nome, CPF.
	private static Console registra(String username, String senha, String nome, String cpf) {
		Console console = consoleDigitando(username, senha, nome, cpf);
		console.setStage(Stage.REGISTRO);
		LoginRegistro.registro(console);
		return console;
	}

	private static Console loga(String username, String senha) {
		Console console = consoleDigitando(username, senha);
		console.setStage(Stage.LOGIN);
		LoginRegistro.login(console);
		return console;
	}

	// Simula o fechamento e a reabertura do programa: salva, limpa a memoria e recarrega.
	private void reiniciaSistema() {
		storage.salva(Auxiliar.usuarios, Auxiliar.caronasAtivas);
		Auxiliar.usuarios.clear();
		Auxiliar.caronasAtivas.clear();
		storage.carrega(Auxiliar.usuarios, Auxiliar.caronasAtivas);
	}

	private static Carona novaCarona(Usuario host) {
		Calendar data = Calendar.getInstance();
		data.add(Calendar.DAY_OF_MONTH, 3);
		return new Carona(host, 3, data, new Rota(new Posicao(-22.9, -43.1), new Campus(PontosDeEncontro.IACS)));
	}

	// ---------- CT-I-SM-01 ----------
	@Test
	void ctISm01_cadastraSalvaRecarregaELoga() {
		Console cadastro = registra("maria", "s3nha", "Maria", "12345678900");
		assertEquals(Stage.LOBBY, cadastro.getStage(), "registro valido deveria levar ao lobby");
		assertEquals(1, Auxiliar.usuarios.size());

		reiniciaSistema();

		assertEquals(1, Auxiliar.usuarios.size(), "o usuario cadastrado deveria voltar do arquivo");
		Usuario recarregado = Auxiliar.usuarios.get(0);
		assertEquals("maria", recarregado.getUsername());
		assertEquals("Maria", recarregado.getName());
		assertEquals("12345678900", recarregado.getCpf());

		Console login = loga("maria", "s3nha");
		assertEquals(Stage.LOBBY, login.getStage(), "login com o usuario recarregado deveria entrar no lobby");
		assertSame(recarregado, login.getUser(), "o usuario logado deve ser o objeto recarregado do arquivo");
	}

	// ---------- CT-I-SM-02 ----------
	@Test
	void ctISm02_senhaErradaAposRecarregar() {
		registra("joao", "certa", "Joao", "111");
		reiniciaSistema();

		Console login = loga("joao", "errada");

		assertEquals(Stage.LOGIN_REGISTRO, login.getStage(), "senha errada deveria voltar para a tela inicial");
		assertNull(login.getUser());
		assertTrue(saida.toString().contains("Falha ao logar"));
	}

	// ---------- CT-I-SM-03 ----------
	@Test
	void ctISm03_loginIgnoraMaiusculasNoUsuario() {
		registra("Carla", "abc", "Carla", "222");
		reiniciaSistema();

		Console login = loga("CARLA", "abc");

		assertEquals(Stage.LOBBY, login.getStage());
		assertEquals("Carla", login.getUser().getUsername());
	}

	// ---------- CT-I-SM-04 ----------
	@Test
	void ctISm04_usernameDuplicadoAposRecarregar() {
		registra("pedro", "p1", "Pedro", "333");
		reiniciaSistema();

		Console segundo = registra("PEDRO", "p2", "Outro", "444");

		assertEquals(Stage.LOGIN_REGISTRO, segundo.getStage(), "username ja persistido nao pode ser cadastrado de novo");
		assertTrue(saida.toString().contains("Falha ao registrar"));
		assertEquals(1, Auxiliar.usuarios.size());

		// O cadastro original continua valido depois de mais um ciclo de gravacao.
		reiniciaSistema();
		assertEquals(Stage.LOBBY, loga("pedro", "p1").getStage());
		assertEquals(Stage.LOGIN_REGISTRO, loga("pedro", "p2").getStage());
	}

	// ---------- CT-I-SM-05 ----------
	@Test
	void ctISm05_variosUsuariosPersistidos() {
		registra("u1", "a", "Um", "1");
		registra("u2", "b", "Dois", "2");
		registra("u3", "c", "Tres", "3");

		reiniciaSistema();

		assertEquals(3, Auxiliar.usuarios.size());
		assertEquals(Stage.LOBBY, loga("u1", "a").getStage());
		assertEquals(Stage.LOBBY, loga("u2", "b").getStage());
		assertEquals(Stage.LOBBY, loga("u3", "c").getStage());
	}

	// ---------- CT-I-SM-06 ----------
	@Test
	void ctISm06_caronaDoUsuarioPersistida() {
		Usuario host = registra("ana", "x", "Ana", "555").getUser();
		Carona carona = novaCarona(host);
		Auxiliar.criarCarona(carona);
		int serialAntes = Carona.getSerial();

		reiniciaSistema();

		assertEquals(1, Auxiliar.caronasAtivas.size());
		Carona recarregada = Auxiliar.caronasAtivas.get(0);
		assertEquals(carona.getID(), recarregada.getID());
		assertEquals("ana", recarregada.getHost().getUsername());
		assertEquals(serialAntes, Carona.getSerial(), "o serial deve continuar de onde parou");

		Usuario logado = loga("ana", "x").getUser();
		assertNotNull(logado);
		assertEquals(1, logado.getCaronas().size(), "o usuario recarregado deve manter a carona que criou");
	}

	// ---------- CT-I-SM-07 ----------
	/*
	 * DEFEITO CONHECIDO (issue #12 - [INTEGRACAO]): usuarios.bin e caronas.bin sao
	 * gravados em streams separados, entao cada arquivo guarda a sua propria copia dos
	 * objetos. Depois de recarregar, a carona em Auxiliar.caronasAtivas e a carona na lista
	 * do motorista (Usuario.getCaronas(), usada em "Ver corridas") sao objetos diferentes.
	 */
	@Test
	void ctISm07_motoristaVePassageiroQueEntrouAposRecarregar() throws Exception {
		Usuario host = registra("bia", "x", "Bia", "666").getUser();
		Auxiliar.criarCarona(novaCarona(host));
		registra("leo", "y", "Leo", "777");

		reiniciaSistema();

		// Passageiro entra na carona ativa (mesmo caminho do HostJoin.join).
		Usuario passageiro = loga("leo", "y").getUser();
		Carona ativa = Auxiliar.caronasAtivas.get(0);
		assertTrue(ativa.adicionaPassageiro(passageiro));
		assertEquals(1, ativa.getVagasPreenchidas());

		// Motorista abre "Ver corridas": a lista vem de Usuario.getCaronas().
		Usuario motorista = loga("bia", "x").getUser();
		Carona naListaDoMotorista = motorista.getCaronas().get(0);
		assertEquals(ativa.getID(), naListaDoMotorista.getID());
		assertEquals(1, naListaDoMotorista.getVagasPreenchidas(),
				"o motorista deveria ver o passageiro que entrou na sua carona depois de reiniciar o sistema");
		assertSame(motorista, ativa.getHost(),
				"depois de recarregar, o host da carona deveria ser o mesmo objeto do usuario cadastrado");
	}
}
