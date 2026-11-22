package me.br.caronapp.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;

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
 * Testes de DEFEITO da Entrega 1 (CT-U-SM-06, 07, 09, 10 e 11).
 *
 * Estes casos FALHAM DE PROPOSITO: eles documentam validacoes de negocio que ainda nao
 * existem em Usuario, Carona e Auxiliar (issues #1, #2, #3, #5 e #8 do repositorio).
 * Ficam separados de StorageManagerTest para que a suite da StorageManager fique verde
 * e o PITest (que exige todos os testes passando) possa rodar. Quando as validacoes
 * forem implementadas, estes testes passam a ficar verdes sem nenhuma alteracao.
 *
 * Os arquivos sao gravados em uma pasta temporaria (@TempDir), nunca nos .bin reais.
 */
class StorageManagerDefeitosTest {

	// Regra de negocio: de 1 a 4 passageiros (mesma faixa validada em HostJoin).
	private static final int MIN_GUESTS = 1;
	private static final int MAX_GUESTS = 4;

	@TempDir
	Path pasta;

	private int serialOriginal;
	private ArrayList<Usuario> auxUsuariosOriginal;
	private ArrayList<Carona> auxCaronasOriginal;

	private StorageManager sm;
	private ArrayList<Usuario> usuarios;
	private ArrayList<Carona> caronas;

	@BeforeEach
	void setUp() {
		serialOriginal = Carona.getSerial();
		Carona.setSerial(0);
		auxUsuariosOriginal = new ArrayList<Usuario>(Auxiliar.usuarios);
		auxCaronasOriginal = new ArrayList<Carona>(Auxiliar.caronasAtivas);
		Auxiliar.usuarios.clear();
		Auxiliar.caronasAtivas.clear();

		sm = new StorageManager(pasta.resolve("usuarios.bin").toString(), pasta.resolve("caronas.bin").toString());
		usuarios = new ArrayList<Usuario>();
		caronas = new ArrayList<Carona>();
	}

	@AfterEach
	void tearDown() {
		Carona.setSerial(serialOriginal);
		Auxiliar.usuarios.clear();
		Auxiliar.usuarios.addAll(auxUsuariosOriginal);
		Auxiliar.caronasAtivas.clear();
		Auxiliar.caronasAtivas.addAll(auxCaronasOriginal);
	}

	@RegisterExtension
	static final TestWatcher RESULTADO = new TestWatcher() {
		@Override
		public void testSuccessful(ExtensionContext ctx) {
			System.out.println("   Resultado: PASSOU\n");
		}

		@Override
		public void testFailed(ExtensionContext ctx, Throwable causa) {
			System.out.println("   Resultado: FALHOU (defeito conhecido) - " + causa.getMessage() + "\n");
		}
	};

	private static void info(String id, String oQue, String esperado) {
		System.out.println("[" + id + "] Testando: " + oQue);
		System.out.println("   Esperado: " + esperado);
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

	// Persiste o estado real do sistema (Auxiliar) e recarrega em listas novas.
	private void persisteERecarrega() {
		sm.salva(Auxiliar.usuarios, Auxiliar.caronasAtivas);
		usuarios = new ArrayList<Usuario>();
		caronas = new ArrayList<Carona>();
		sm.carrega(usuarios, caronas);
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

}
