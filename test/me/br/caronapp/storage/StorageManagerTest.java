package me.br.caronapp.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Campus;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.PontosDeEncontro;
import me.br.caronapp.carona.Posicao;
import me.br.caronapp.carona.Rota;
import me.br.caronapp.usuario.Usuario;

/*
 * Casos de teste CT-U-SM-01 a CT-U-SM-13 (docs/Casos-de-Teste-StorageManager.docx).
 *
 * StorageManager usa caminhos relativos fixos ("usuarios.bin" e "caronas.bin") no diretorio
 * de trabalho corrente. Para isolar os testes, os arquivos existentes sao movidos para um
 * backup antes de cada teste e restaurados depois.
 */
class StorageManagerTest {

	private static final File USUARIOS = new File("usuarios.bin");
	private static final File CARONAS = new File("caronas.bin");

	// Regra de negocio: de 1 a 4 passageiros (mesma faixa validada em HostJoin).
	private static final int MIN_GUESTS = 1;
	private static final int MAX_GUESTS = 4;

	private File backupDir;
	private int serialOriginal;
	private PrintStream stdoutOriginal;
	private ByteArrayOutputStream stdoutCapturado;

	private StorageManager sm;
	private ArrayList<Usuario> usuarios;
	private ArrayList<Carona> caronas;

	private ArrayList<Usuario> auxUsuariosOriginal;
	private ArrayList<Carona> auxCaronasOriginal;

	@BeforeEach
	void setUp() throws IOException {
		backupDir = Files.createTempDirectory("sm-backup").toFile();
		for (File f : new File[] { USUARIOS, CARONAS }) {
			if (f.exists()) Files.move(f.toPath(), new File(backupDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		serialOriginal = Carona.getSerial();
		Carona.setSerial(0);

		stdoutOriginal = System.out;
		stdoutCapturado = new ByteArrayOutputStream();
		System.setOut(new PrintStream(stdoutCapturado, true, "UTF-8"));

		// Auxiliar guarda o estado global do sistema (usuarios e caronas); isola e restaura.
		auxUsuariosOriginal = new ArrayList<Usuario>(Auxiliar.usuarios);
		auxCaronasOriginal = new ArrayList<Carona>(Auxiliar.caronasAtivas);
		Auxiliar.usuarios.clear();
		Auxiliar.caronasAtivas.clear();

		sm = new StorageManager();
		usuarios = new ArrayList<Usuario>();
		caronas = new ArrayList<Carona>();
	}

	@AfterEach
	void tearDown() throws IOException {
		System.setOut(stdoutOriginal);
		Carona.setSerial(serialOriginal);
		Auxiliar.usuarios.clear();
		Auxiliar.usuarios.addAll(auxUsuariosOriginal);
		Auxiliar.caronasAtivas.clear();
		Auxiliar.caronasAtivas.addAll(auxCaronasOriginal);
		System.gc();
		apaga(USUARIOS);
		apaga(CARONAS);
		for (File f : backupDir.listFiles()) {
			Files.move(f.toPath(), new File(f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		backupDir.delete();
	}

	// Imprime o resultado de cada teste (roda depois do tearDown, quando System.out ja foi restaurado).
	@RegisterExtension
	static final TestWatcher RESULTADO = new TestWatcher() {
		@Override
		public void testSuccessful(ExtensionContext ctx) {
			System.out.println("   Resultado: PASSOU\n");
		}

		@Override
		public void testFailed(ExtensionContext ctx, Throwable causa) {
			System.out.println("   Resultado: FALHOU - " + causa.getMessage() + "\n");
		}
	};

	// Imprime no console real (System.out fica capturado durante o teste).
	private void info(String id, String o_que, String esperado) {
		stdoutOriginal.println("[" + id + "] Testando: " + o_que);
		stdoutOriginal.println("   Esperado: " + esperado);
	}

	// ---------- helpers ----------

	private static void apaga(File f) throws IOException {
		if (f.isDirectory()) f.delete();
		else Files.deleteIfExists(f.toPath());
	}

	private static Calendar dataFutura() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 5);
		return c;
	}

	private static Rota rotaIda() {
		return new Rota(new Posicao(-22.9, -43.1), new Campus(PontosDeEncontro.IACS));
	}

	private Carona novaCarona(Usuario host, int vagas) {
		return new Carona(host, vagas, dataFutura(), rotaIda());
	}

	private static String cp(int... codePoints) {
		return new String(codePoints, 0, codePoints.length);
	}

	private String saida() throws Exception {
		return stdoutCapturado.toString("UTF-8").trim();
	}

	// Persiste o estado real do sistema (Auxiliar) e recarrega em listas novas.
	private void persisteERecarrega() {
		sm.salva(Auxiliar.usuarios, Auxiliar.caronasAtivas);
		usuarios = new ArrayList<Usuario>();
		caronas = new ArrayList<Carona>();
		sm.carrega(usuarios, caronas);
	}

	private void salvaECarrega() {
		sm.salva(usuarios, caronas);
		usuarios = new ArrayList<Usuario>();
		caronas = new ArrayList<Carona>();
		sm.carrega(usuarios, caronas);
	}

	// ---------- CT-U-SM-01 ----------
	@Test
	void ctUSm01_salvaCasoFeliz() throws Exception {
		info("CT-U-SM-01", "salva() - caso feliz",
				"grava usuarios.bin e caronas.bin; ao reabrir, 1o Usuario 'ana' e 1a Carona com o mesmo ID; caronas.bin inicia com o serial");
		Carona.setSerial(1);
		Usuario ana = new Usuario("ana", "Ana", "111", "123");
		usuarios.add(ana);
		Carona c = novaCarona(ana, 3);
		caronas.add(c);
		int serialAntes = Carona.getSerial();

		assertDoesNotThrow(() -> sm.salva(usuarios, caronas));

		assertTrue(USUARIOS.isFile());
		assertTrue(CARONAS.isFile());
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(USUARIOS))) {
			Usuario lido = (Usuario) in.readObject();
			assertEquals("ana", lido.getUsername());
		}
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(CARONAS))) {
			assertEquals(serialAntes, in.readInt());
			Carona lida = (Carona) in.readObject();
			assertEquals(c.getID(), lida.getID());
		}
	}

	// ---------- CT-U-SM-02 ----------
	@Test
	void ctUSm02_salvaIOExceptionTratada() throws Exception {
		info("CT-U-SM-02", "salva() - IOException tratada",
				"com 'usuarios.bin' bloqueado (e um diretorio), nenhuma excecao propaga e a mensagem do erro e impressa");
		// Um diretorio com o nome do arquivo faz FileOutputStream lancar FileNotFoundException.
		assertTrue(USUARIOS.mkdir());
		usuarios.add(new Usuario("ana", "Ana", "111", "123"));

		assertDoesNotThrow(() -> sm.salva(usuarios, caronas));

		assertTrue(USUARIOS.isDirectory());
		assertFalse(saida().isEmpty(), "e.getMessage() deveria ser impresso");
	}

	// ---------- CT-U-SM-03 ----------
	@Test
	void ctUSm03_carregaCasoFeliz() {
		info("CT-U-SM-03", "carrega() - caso feliz",
				"listas restauradas (1 usuario 'ana', 1 carona) e Carona.serial atualizado com o valor do arquivo");
		Usuario ana = new Usuario("ana", "Ana", "111", "123");
		usuarios.add(ana);
		Carona c = novaCarona(ana, 3);
		caronas.add(c);
		int idEsperado = c.getID();
		sm.salva(usuarios, caronas);

		Carona.setSerial(99);
		ArrayList<Usuario> us = new ArrayList<Usuario>();
		ArrayList<Carona> cs = new ArrayList<Carona>();
		assertDoesNotThrow(() -> sm.carrega(us, cs));

		assertEquals(1, us.size());
		assertEquals("ana", us.get(0).getUsername());
		assertEquals(1, cs.size());
		assertEquals(idEsperado, cs.get(0).getID());
		assertEquals(1, Carona.getSerial());
	}

	// ---------- CT-U-SM-04 ----------
	@Test
	void ctUSm04_carregaArquivosInexistentes() throws Exception {
		info("CT-U-SM-04", "carrega() - arquivos inexistentes",
				"FileNotFoundException tratada, mensagem impressa, listas continuam vazias");
		assertFalse(USUARIOS.exists());
		assertFalse(CARONAS.exists());

		assertDoesNotThrow(() -> sm.carrega(usuarios, caronas));

		assertEquals(0, usuarios.size());
		assertEquals(0, caronas.size());
		assertFalse(saida().isEmpty(), "e.getMessage() deveria ser impresso");
	}

	// ---------- CT-U-SM-05 ----------
	@Test
	void ctUSm05_carregaParametrosNulos() {
		info("CT-U-SM-05", "carrega() - parametros nulos",
				"guard clause retorna sem abrir arquivos: nada lido, serial intacto, sem NullPointerException");
		Usuario ana = new Usuario("ana", "Ana", "111", "123");
		usuarios.add(ana);
		caronas.add(novaCarona(ana, 2));
		sm.salva(usuarios, caronas);
		Carona.setSerial(42);

		ArrayList<Usuario> vazioU = new ArrayList<Usuario>();
		ArrayList<Carona> vazioC = new ArrayList<Carona>();

		assertDoesNotThrow(() -> sm.carrega(null, vazioC));
		assertDoesNotThrow(() -> sm.carrega(vazioU, null));
		assertDoesNotThrow(() -> sm.carrega(null, null));

		// Nenhum arquivo foi lido: listas e serial permanecem intactos.
		assertEquals(0, vazioU.size());
		assertEquals(0, vazioC.size());
		assertEquals(42, Carona.getSerial());
	}

	// ---------- CT-U-SM-06 ----------
	@Test
	void ctUSm06_stringsVazias() {
		info("CT-U-SM-06", "regra de negocio - strings vazias",
				"Usuario com username/name/cpf/senha vazios e rejeitado (IllegalArgumentException); register() retorna null; nada e persistido");

		assertThrows(IllegalArgumentException.class, () -> new Usuario("", "", "", ""));
		assertThrows(IllegalArgumentException.class, () -> new Usuario("   ", "Ana", "111", "123"));
		assertThrows(IllegalArgumentException.class, () -> new Usuario("ana", "", "111", "123"));
		assertThrows(IllegalArgumentException.class, () -> new Usuario("ana", "Ana", "", "123"));
		assertThrows(IllegalArgumentException.class, () -> new Usuario("ana", "Ana", "111", ""));

		assertNull(Auxiliar.register("", "", "", ""));
		assertEquals(0, Auxiliar.usuarios.size());

		persisteERecarrega();
		assertEquals(0, usuarios.size());
	}

	// ---------- CT-U-SM-07 ----------
	@Test
	void ctUSm07_camposNulos() {
		info("CT-U-SM-07", "regra de negocio - campos nulos",
				"Usuario com name/cpf/senha (ou username) nulos e rejeitado (IllegalArgumentException); register() retorna null; nada e persistido");

		assertThrows(IllegalArgumentException.class, () -> new Usuario("joao", null, null, null));
		assertThrows(IllegalArgumentException.class, () -> new Usuario(null, "Joao", "111", "123"));
		assertThrows(IllegalArgumentException.class, () -> new Usuario("joao", null, "111", "123"));
		assertThrows(IllegalArgumentException.class, () -> new Usuario("joao", "Joao", null, "123"));
		assertThrows(IllegalArgumentException.class, () -> new Usuario("joao", "Joao", "111", null));

		assertNull(Auxiliar.register("joao", null, null, null));
		assertNull(Auxiliar.register(null, "Joao", "111", "123"));
		assertEquals(0, Auxiliar.usuarios.size());

		persisteERecarrega();
		assertEquals(0, usuarios.size());
	}

	// ---------- CT-U-SM-08 ----------
	@Test
	void ctUSm08_caracteresEspeciais() {
		info("CT-U-SM-08", "salva/carrega - caracteres especiais",
				"acentos, kanji, emoji e simbolos voltam identicos apos carrega()");
		// Codepoints via new String(int[]) para manter o arquivo-fonte ASCII (projeto usa Cp1252).
		String username = cp('j', 'o', 0xE3, 'o', '_', '#', '1', '!');
		String nome = cp('M', 'a', 'r', 0xED, 'a', ' ', 'J', 'o', 's', 0xE9, ' ', 'D', '\'', 0xC1, 'v', 'i', 'l', 'a',
				' ', 0x65E5, 0x672C, 0x8A9E, ' ', 0x1F600);
		String senha = cp('s', '3', 'n', 'h', '@', '!', '#', '$', '%', 0xA8, '&', '*', '(', ')', '_', '+', '{', '}', '[', ']');
		usuarios.add(new Usuario(username, nome, "123", senha));

		assertDoesNotThrow(this::salvaECarrega);

		Usuario u = usuarios.get(0);
		assertEquals(username, u.getUsername());
		assertEquals(nome, u.getName());
		assertEquals(senha, u.getSenha());
	}

	// ---------- CT-U-SM-09 ----------
	@Test
	void ctUSm09_numerosForaDaRealidade() {
		info("CT-U-SM-09", "regra de negocio - numeros fora da realidade",
				"maximoGuests fora de [1,4], data no passado (1900) e stars fora do intervalo sao rejeitados/ignorados pelo dominio; nada invalido e persistido");

		Usuario host = Auxiliar.register("host", "Host", "111", "123");
		assertNotNull(host);

		// maximoGuests fora do intervalo permitido (1 a 4) e rejeitado no construtor.
		for (int invalido : new int[] { -5, 0, MAX_GUESTS + 1, Integer.MAX_VALUE }) {
			assertThrows(IllegalArgumentException.class, () -> novaCarona(host, invalido));
		}
		assertEquals(0, Carona.getSerial(), "carona invalida nao pode consumir o serial");
		assertEquals(0, host.getCaronas().size());

		// Limites validos sao aceitos.
		assertDoesNotThrow(() -> novaCarona(host, MIN_GUESTS));
		Carona c = novaCarona(host, MAX_GUESTS);

		// Setters tambem validam e preservam o valor anterior.
		assertThrows(IllegalArgumentException.class, () -> c.setMaximoGuests(-5));
		assertThrows(IllegalArgumentException.class, () -> c.setMaximoGuests(Integer.MAX_VALUE));
		assertEquals(MAX_GUESTS, c.getMaximoGuests());

		// Data no passado (ano 1900) e rejeitada no construtor e no setter.
		Calendar antiga = Calendar.getInstance();
		antiga.set(1900, Calendar.JANUARY, 1, 0, 0, 0);
		assertThrows(IllegalArgumentException.class, () -> new Carona(host, 3, antiga, rotaIda()));
		long dataOriginal = c.getDataHora().getTimeInMillis();
		assertThrows(IllegalArgumentException.class, () -> c.setDataHora(antiga));
		assertThrows(IllegalArgumentException.class, () -> c.setDataHora(null));
		assertEquals(dataOriginal, c.getDataHora().getTimeInMillis());

		// stars: avaliacoes fora de [0,5] sao ignoradas, a nota continua em [0,10].
		host.giveStars(-100);
		host.giveStars(999999);
		assertEquals(10, host.getStars());
		assertTrue(host.getStars() >= 0 && host.getStars() <= 10);
	}

	// ---------- CT-U-SM-10 ----------
	@Test
	void ctUSm10_hostForaDeListaUsuarios() {
		info("CT-U-SM-10", "regra de negocio - host fora de listaUsuarios",
				"Auxiliar.criarCarona() recusa carona cujo host nao esta cadastrado; nada e persistido em caronas.bin");

		assertNotNull(Auxiliar.register("outro", "Outro", "222", "123"));
		Usuario orfao = new Usuario("host_orfao", "Orfao", "1", "s");
		Carona daOrfao = novaCarona(orfao, 3);

		Auxiliar.criarCarona(daOrfao);
		assertEquals(0, Auxiliar.caronasAtivas.size());

		persisteERecarrega();
		assertEquals(0, caronas.size());

		// Controle: host cadastrado pode criar a carona.
		Usuario cadastrado = Auxiliar.register("cadastrado", "Cadastrado", "333", "123");
		Auxiliar.criarCarona(novaCarona(cadastrado, 3));
		assertEquals(1, Auxiliar.caronasAtivas.size());
	}

	// ---------- CT-U-SM-11 ----------
	@Test
	void ctUSm11_hostNulo() {
		info("CT-U-SM-11", "regra de negocio - host nulo",
				"Carona sem host e rejeitada (IllegalArgumentException); nao consome serial; criarCarona(null) retorna false");

		assertThrows(IllegalArgumentException.class, () -> new Carona(null, 3, dataFutura(), rotaIda()));
		assertEquals(0, Carona.getSerial());
		Auxiliar.criarCarona(null);
		assertEquals(0, Auxiliar.caronasAtivas.size());

		persisteERecarrega();
		assertEquals(0, caronas.size());
	}

	// ---------- CT-U-SM-13 ----------
	@Test
	void ctUSm13_usernamesDuplicados() {
		info("CT-U-SM-13", "regra de negocio - usernames duplicados",
				"register() recusa segundo usuario com o mesmo username (sem diferenciar maiusculas); so o primeiro e persistido");

		assertNotNull(Auxiliar.register("ana", "Ana Um", "111", "a"));
		assertNull(Auxiliar.register("ana", "Ana Dois", "222", "b"));
		assertNull(Auxiliar.register("ANA", "Ana Tres", "333", "c"));
		assertEquals(1, Auxiliar.usuarios.size());

		persisteERecarrega();
		assertEquals(1, usuarios.size());
		assertEquals("ana", usuarios.get(0).getUsername());
		assertEquals("111", usuarios.get(0).getCpf());
	}
}
