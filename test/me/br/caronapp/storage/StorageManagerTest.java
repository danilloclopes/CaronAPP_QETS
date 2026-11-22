package me.br.caronapp.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.io.TempDir;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Campus;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.PontosDeEncontro;
import me.br.caronapp.carona.Posicao;
import me.br.caronapp.carona.Rota;
import me.br.caronapp.usuario.Usuario;

/*
 * Casos de teste unitarios da classe StorageManager (docs/Casos-de-Teste-StorageManager.docx).
 *
 * Isolamento das dependencias:
 * - Disco: cada teste usa uma pasta temporaria (@TempDir) passada ao construtor
 *   StorageManager(arquivoUsuarios, arquivoCaronas); os arquivos reais usuarios.bin e
 *   caronas.bin do projeto nunca sao lidos nem alterados.
 * - Estado global: Carona.serial e as listas de Auxiliar sao salvos antes e restaurados depois.
 * - Log: as mensagens do Logger do StorageManager sao capturadas em memoria.
 *
 * Os casos que dependem de validacoes ausentes em Usuario, Carona e Auxiliar
 * (CT-U-SM-06, 07, 09, 10 e 11) ficam em StorageManagerDefeitosTest, para que esta suite
 * fique verde e possa ser usada pelo PITest.
 */
class StorageManagerTest {

	@TempDir
	Path pasta;

	private File arquivoUsuarios;
	private File arquivoCaronas;

	private int serialOriginal;
	private ArrayList<Usuario> auxUsuariosOriginal;
	private ArrayList<Carona> auxCaronasOriginal;

	private final Logger logger = Logger.getLogger(StorageManager.class.getName());
	private ByteArrayOutputStream logCapturado;
	private StreamHandler logHandler;
	private boolean usarHandlersOriginais;

	private StorageManager sm;
	private ArrayList<Usuario> usuarios;
	private ArrayList<Carona> caronas;

	@BeforeEach
	void setUp() throws IOException {
		arquivoUsuarios = pasta.resolve("usuarios.bin").toFile();
		arquivoCaronas = pasta.resolve("caronas.bin").toFile();

		serialOriginal = Carona.getSerial();
		Carona.setSerial(0);

		auxUsuariosOriginal = new ArrayList<Usuario>(Auxiliar.usuarios);
		auxCaronasOriginal = new ArrayList<Carona>(Auxiliar.caronasAtivas);
		Auxiliar.usuarios.clear();
		Auxiliar.caronasAtivas.clear();

		logCapturado = new ByteArrayOutputStream();
		logHandler = new StreamHandler(logCapturado, new SimpleFormatter());
		logHandler.setEncoding("UTF-8");
		usarHandlersOriginais = logger.getUseParentHandlers();
		logger.setUseParentHandlers(false);
		logger.addHandler(logHandler);

		sm = new StorageManager(arquivoUsuarios.getPath(), arquivoCaronas.getPath());
		usuarios = new ArrayList<Usuario>();
		caronas = new ArrayList<Carona>();
	}

	@AfterEach
	void tearDown() {
		logger.removeHandler(logHandler);
		logger.setUseParentHandlers(usarHandlersOriginais);
		Carona.setSerial(serialOriginal);
		Auxiliar.usuarios.clear();
		Auxiliar.usuarios.addAll(auxUsuariosOriginal);
		Auxiliar.caronasAtivas.clear();
		Auxiliar.caronasAtivas.addAll(auxCaronasOriginal);
	}

	// Imprime o resultado de cada teste no console.
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

	private static void info(String id, String oQue, String esperado) {
		System.out.println("[" + id + "] Testando: " + oQue);
		System.out.println("   Esperado: " + esperado);
	}

	// ---------- helpers ----------

	private static Calendar dataFutura() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 5);
		return c;
	}

	private static Rota rotaIda() {
		return new Rota(new Posicao(-22.9, -43.1), new Campus(PontosDeEncontro.IACS));
	}

	private static Carona novaCarona(Usuario host, int vagas) {
		return new Carona(host, vagas, dataFutura(), rotaIda());
	}

	private static String cp(int... codePoints) {
		return new String(codePoints, 0, codePoints.length);
	}

	// Mensagens que o StorageManager registrou no log durante o teste.
	private String log() {
		logHandler.flush();
		return new String(logCapturado.toByteArray(), StandardCharsets.UTF_8).trim();
	}

	private void salvaECarrega() {
		sm.salva(usuarios, caronas);
		usuarios = new ArrayList<Usuario>();
		caronas = new ArrayList<Carona>();
		sm.carrega(usuarios, caronas);
	}

	private void persisteERecarrega() {
		sm.salva(Auxiliar.usuarios, Auxiliar.caronasAtivas);
		usuarios = new ArrayList<Usuario>();
		caronas = new ArrayList<Carona>();
		sm.carrega(usuarios, caronas);
	}

	// Grava um arquivo no formato do StorageManager com os objetos informados.
	private static void gravaObjetos(File arquivo, Integer serial, Object... objetos) throws IOException {
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(arquivo))) {
			if (serial != null) out.writeInt(serial);
			for (Object o : objetos) out.writeObject(o);
		}
	}

	/*
	 * Classe so de teste usada para simular um arquivo com uma classe que nao existe
	 * mais no sistema: o nome dela e trocado nos bytes gravados por "ClasseFantasmaZ".
	 */
	static class ClasseFantasmaA implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	private static void gravaComClasseInexistente(File arquivo, Integer serial, Object antes) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			if (serial != null) out.writeInt(serial);
			if (antes != null) out.writeObject(antes);
			out.writeObject(new ClasseFantasmaA());
		}
		byte[] conteudo = bytes.toByteArray();
		byte[] de = "ClasseFantasmaA".getBytes(StandardCharsets.US_ASCII);
		byte[] para = "ClasseFantasmaZ".getBytes(StandardCharsets.US_ASCII);
		int pos = indice(conteudo, de);
		assertTrue(pos >= 0, "nome da classe deveria estar no arquivo serializado");
		System.arraycopy(para, 0, conteudo, pos, para.length);
		Files.write(arquivo.toPath(), conteudo);
	}

	private static int indice(byte[] dados, byte[] trecho) {
		for (int i = 0; i <= dados.length - trecho.length; i++) {
			if (Arrays.equals(Arrays.copyOfRange(dados, i, i + trecho.length), trecho)) return i;
		}
		return -1;
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

		assertTrue(arquivoUsuarios.isFile());
		assertTrue(arquivoCaronas.isFile());
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(arquivoUsuarios))) {
			Usuario lido = (Usuario) in.readObject();
			assertEquals("ana", lido.getUsername());
		}
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(arquivoCaronas))) {
			assertEquals(serialAntes, in.readInt());
			Carona lida = (Carona) in.readObject();
			assertEquals(c.getID(), lida.getID());
		}
		assertEquals("", log(), "caso feliz nao deveria registrar erro");
	}

	// ---------- CT-U-SM-02 ----------
	@Test
	void ctUSm02_salvaIOExceptionTratada() {
		info("CT-U-SM-02", "salva() - IOException tratada",
				"com 'usuarios.bin' bloqueado (e um diretorio), nenhuma excecao propaga e a mensagem do erro e registrada no log");
		// Um diretorio com o nome do arquivo faz FileOutputStream lancar FileNotFoundException.
		assertTrue(arquivoUsuarios.mkdir());
		usuarios.add(new Usuario("ana", "Ana", "111", "123"));

		assertDoesNotThrow(() -> sm.salva(usuarios, caronas));

		assertTrue(arquivoUsuarios.isDirectory());
		assertTrue(log().contains("Erro ao salvar os dados"), "e.getMessage() deveria ser registrado no log");
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
		assertEquals("", log(), "caso feliz nao deveria registrar erro");
	}

	// ---------- CT-U-SM-04 ----------
	@Test
	void ctUSm04_carregaArquivosInexistentes() {
		info("CT-U-SM-04", "carrega() - arquivos inexistentes",
				"FileNotFoundException tratada, mensagem registrada no log, listas continuam vazias");
		assertFalse(arquivoUsuarios.exists());
		assertFalse(arquivoCaronas.exists());

		assertDoesNotThrow(() -> sm.carrega(usuarios, caronas));

		assertEquals(0, usuarios.size());
		assertEquals(0, caronas.size());
		assertTrue(log().contains("Erro ao carregar os dados"), "e.getMessage() deveria ser registrado no log");
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

	// ---------- CT-U-SM-08 ----------
	@Test
	void ctUSm08_caracteresEspeciais() {
		info("CT-U-SM-08", "salva/carrega - caracteres especiais",
				"acentos, kanji, emoji e simbolos voltam identicos apos carrega()");
		// Codepoints via new String(int[]) para manter o arquivo-fonte ASCII.
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

	// ---------- CT-U-SM-14 ----------
	@Test
	void ctUSm14_construtorCaminhosInvalidos() {
		info("CT-U-SM-14", "construtor - caminhos invalidos",
				"caminho nulo, vazio, so com espacos ou igual para os dois arquivos lanca IllegalArgumentException");
		String valido = arquivoCaronas.getPath();

		assertThrows(IllegalArgumentException.class, () -> new StorageManager(null, valido));
		assertThrows(IllegalArgumentException.class, () -> new StorageManager("", valido));
		assertThrows(IllegalArgumentException.class, () -> new StorageManager("   ", valido));
		assertThrows(IllegalArgumentException.class, () -> new StorageManager(valido, null));
		assertThrows(IllegalArgumentException.class, () -> new StorageManager(valido, ""));
		assertThrows(IllegalArgumentException.class, () -> new StorageManager(valido, "   "));
		assertThrows(IllegalArgumentException.class, () -> new StorageManager(valido, valido));
	}

	// ---------- CT-U-SM-15 ----------
	@Test
	void ctUSm15_construtores() {
		info("CT-U-SM-15", "construtores - caminhos usados",
				"construtor padrao usa usuarios.bin/caronas.bin; o outro guarda os caminhos recebidos");
		StorageManager padrao = new StorageManager();
		assertEquals("usuarios.bin", padrao.getArquivoUsuarios());
		assertEquals("caronas.bin", padrao.getArquivoCaronas());

		assertEquals(arquivoUsuarios.getPath(), sm.getArquivoUsuarios());
		assertEquals(arquivoCaronas.getPath(), sm.getArquivoCaronas());

		// Caminho com espaco nas pontas (mas com conteudo) e aceito.
		StorageManager comEspaco = new StorageManager(" a.bin ", " b.bin ");
		assertEquals(" a.bin ", comEspaco.getArquivoUsuarios());
	}

	// ---------- CT-U-SM-16 ----------
	@Test
	void ctUSm16_salvaListasNulasPreservaArquivos() throws Exception {
		info("CT-U-SM-16", "salva() - listas nulas",
				"nao lanca NullPointerException, registra aviso e NAO apaga os dados ja gravados");
		Usuario ana = new Usuario("ana", "Ana", "111", "123");
		usuarios.add(ana);
		caronas.add(novaCarona(ana, 2));
		sm.salva(usuarios, caronas);
		long tamanhoUsuarios = arquivoUsuarios.length();
		long tamanhoCaronas = arquivoCaronas.length();

		assertDoesNotThrow(() -> sm.salva(null, caronas));
		assertDoesNotThrow(() -> sm.salva(usuarios, null));
		assertDoesNotThrow(() -> sm.salva(null, null));

		assertEquals(tamanhoUsuarios, arquivoUsuarios.length());
		assertEquals(tamanhoCaronas, arquivoCaronas.length());
		assertTrue(log().contains("Listas nulas: nada foi salvo"));

		ArrayList<Usuario> us = new ArrayList<Usuario>();
		ArrayList<Carona> cs = new ArrayList<Carona>();
		sm.carrega(us, cs);
		assertEquals(1, us.size());
		assertEquals(1, cs.size());
	}

	// ---------- CT-U-SM-17 ----------
	@Test
	void ctUSm17_variosRegistrosEmOrdem() {
		info("CT-U-SM-17", "salva/carrega - varios registros",
				"3 usuarios e 2 caronas voltam na mesma ordem; o serial salvo e o da ultima carona criada");
		Usuario a = new Usuario("a", "A", "1", "x");
		Usuario b = new Usuario("b", "B", "2", "y");
		Usuario c = new Usuario("c", "C", "3", "z");
		usuarios.addAll(Arrays.asList(a, b, c));
		Carona c1 = novaCarona(a, 1);
		Carona c2 = novaCarona(b, 4);
		caronas.addAll(Arrays.asList(c1, c2));

		salvaECarrega();

		assertEquals(3, usuarios.size());
		assertEquals("a", usuarios.get(0).getUsername());
		assertEquals("b", usuarios.get(1).getUsername());
		assertEquals("c", usuarios.get(2).getUsername());
		assertEquals(2, caronas.size());
		assertEquals(c1.getID(), caronas.get(0).getID());
		assertEquals(c2.getID(), caronas.get(1).getID());
		assertEquals(2, Carona.getSerial());
	}

	// ---------- CT-U-SM-18 ----------
	@Test
	void ctUSm18_salvaSobrescreve() {
		info("CT-U-SM-18", "salva() - sobrescreve",
				"um segundo salva() substitui o conteudo anterior (nao acrescenta no fim do arquivo)");
		usuarios.add(new Usuario("antigo", "Antigo", "1", "x"));
		sm.salva(usuarios, caronas);

		ArrayList<Usuario> novos = new ArrayList<Usuario>();
		novos.add(new Usuario("novo", "Novo", "2", "y"));
		sm.salva(novos, new ArrayList<Carona>());

		ArrayList<Usuario> us = new ArrayList<Usuario>();
		ArrayList<Carona> cs = new ArrayList<Carona>();
		sm.carrega(us, cs);
		assertEquals(1, us.size());
		assertEquals("novo", us.get(0).getUsername());
		assertEquals(0, cs.size());
	}

	// ---------- CT-U-SM-19 ----------
	@Test
	void ctUSm19_listasVazias() {
		info("CT-U-SM-19", "salva/carrega - listas vazias",
				"arquivos sao criados, carrega() nao adiciona nada e restaura o serial salvo");
		Carona.setSerial(7);

		salvaECarrega();

		assertTrue(arquivoUsuarios.isFile());
		assertTrue(arquivoCaronas.isFile());
		assertEquals(0, usuarios.size());
		assertEquals(0, caronas.size());
		assertEquals(7, Carona.getSerial());
		assertEquals("", log());
	}

	// ---------- CT-U-SM-20 ----------
	@Test
	void ctUSm20_carregaArquivoCorrompido() throws Exception {
		info("CT-U-SM-20", "carrega() - arquivo corrompido",
				"bytes invalidos em usuarios.bin geram IOException tratada; nada e carregado e o erro vai para o log");
		Files.write(arquivoUsuarios.toPath(), "isto nao e um arquivo serializado".getBytes(StandardCharsets.US_ASCII));
		gravaObjetos(arquivoCaronas, 5);
		Carona.setSerial(1);

		assertDoesNotThrow(() -> sm.carrega(usuarios, caronas));

		assertEquals(0, usuarios.size());
		assertEquals(0, caronas.size());
		assertEquals(1, Carona.getSerial());
		assertTrue(log().contains("Erro ao carregar os dados"));
	}

	// ---------- CT-U-SM-21 ----------
	@Test
	void ctUSm21_caronasSemSerial() throws Exception {
		info("CT-U-SM-21", "carrega() - caronas.bin sem serial",
				"fim de arquivo antes do serial e tratado como arquivo vazio: usuarios carregam e o serial nao muda");
		gravaObjetos(arquivoUsuarios, null, new Usuario("ana", "Ana", "1", "x"));
		gravaObjetos(arquivoCaronas, null);
		Carona.setSerial(3);

		assertDoesNotThrow(() -> sm.carrega(usuarios, caronas));

		assertEquals(1, usuarios.size());
		assertEquals(0, caronas.size());
		assertEquals(3, Carona.getSerial());
		assertEquals("", log());
	}

	// ---------- CT-U-SM-22 ----------
	@Test
	void ctUSm22_usuarioDeClasseInexistente() throws Exception {
		info("CT-U-SM-22", "carrega() - classe inexistente em usuarios.bin",
				"ClassNotFoundException tratada: usuarios anteriores ficam, caronas continuam sendo carregadas, erro no log");
		Usuario ana = new Usuario("ana", "Ana", "1", "x");
		gravaComClasseInexistente(arquivoUsuarios, null, ana);
		Carona c = novaCarona(ana, 2);
		gravaObjetos(arquivoCaronas, 9, c);

		assertDoesNotThrow(() -> sm.carrega(usuarios, caronas));

		assertEquals(1, usuarios.size());
		assertEquals("ana", usuarios.get(0).getUsername());
		assertEquals(1, caronas.size());
		assertEquals(9, Carona.getSerial());
		assertTrue(log().contains("Classe nao encontrada ao ler usuarios"));
	}

	// ---------- CT-U-SM-23 ----------
	@Test
	void ctUSm23_caronaDeClasseInexistente() throws Exception {
		info("CT-U-SM-23", "carrega() - classe inexistente em caronas.bin",
				"ClassNotFoundException tratada: usuarios e caronas anteriores ficam carregados, erro no log");
		Usuario ana = new Usuario("ana", "Ana", "1", "x");
		gravaObjetos(arquivoUsuarios, null, ana);
		Carona c = novaCarona(ana, 2);
		gravaComClasseInexistente(arquivoCaronas, 4, c);

		assertDoesNotThrow(() -> sm.carrega(usuarios, caronas));

		assertEquals(1, usuarios.size());
		assertEquals(1, caronas.size());
		assertEquals(c.getID(), caronas.get(0).getID());
		assertEquals(4, Carona.getSerial());
		assertTrue(log().contains("Classe nao encontrada ao ler caronas"));
	}

	// ---------- CT-U-SM-24 ----------
	@Test
	void ctUSm24_carregaAcrescentaNasListas() {
		info("CT-U-SM-24", "carrega() - listas ja preenchidas",
				"os registros do arquivo sao acrescentados depois dos que ja estavam na lista");
		usuarios.add(new Usuario("salvo", "Salvo", "1", "x"));
		sm.salva(usuarios, caronas);

		ArrayList<Usuario> us = new ArrayList<Usuario>();
		us.add(new Usuario("memoria", "Memoria", "2", "y"));
		sm.carrega(us, new ArrayList<Carona>());

		assertEquals(2, us.size());
		assertEquals("memoria", us.get(0).getUsername());
		assertEquals("salvo", us.get(1).getUsername());
	}

	// Sanidade do isolamento: os testes nunca apontam para os arquivos reais do projeto.
	@Test
	void isolamentoNaoUsaArquivosReais() {
		info("CT-U-SM-ISO", "isolamento",
				"os arquivos do teste ficam na pasta temporaria, fora do diretorio do projeto");
		assertTrue(arquivoUsuarios.toPath().startsWith(pasta));
		assertTrue(arquivoCaronas.toPath().startsWith(pasta));
		assertNotEquals(new File("usuarios.bin").getAbsolutePath(), arquivoUsuarios.getAbsolutePath());
	}
}
