package me.br.caronapp.console.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.Carona.Estado;
import me.br.caronapp.carona.excecoes.UsuarioNaoEncontradoException;
import me.br.caronapp.console.Console;
import me.br.caronapp.console.Console.Stage;
import me.br.caronapp.usuario.Usuario;

/*
 * Implementado por Eperson Cardoso Mayrink Xavier Filho
 * Modificado por: Sergio Ricardo
 */

public class CaronaManager {

	private static final Logger LOGGER = Logger.getLogger(CaronaManager.class.getName());
	private static final String SEM_OPCOES = "Sem op\u00e7\u00f5es dispon\u00edveis...";

	private CaronaManager() {
	}

	public static void verCorridas(Console console) {
		Usuario usuario = console.getUser();
		List<Carona> caronas = usuario.getCaronas();
		List<Carona> comoPassageiro = new ArrayList<>();

		LOGGER.info("Caronas nas quais voce eh dono:");
		for (Carona carona : caronas) {
			if (!carona.getHost().equals(usuario)) {
				comoPassageiro.add(carona);
				continue;
			}
			registrarCarona(carona);
		}

		LOGGER.info("Caronas nas quais voce eh passageiro:");
		for (Carona carona : comoPassageiro) {
			registrarCarona(carona);
		}

		LOGGER.info("Digite um ID de carona: ");
		Carona toCheck = usuario.getPeloId(console.getScanner().nextInt());
		if (toCheck != null) {
			verOpcoesCorrida(toCheck, console);
		} else {
			LOGGER.info("Carona nao encontrada...");
		}

		console.setStage(Stage.LOBBY);
	}

	private static void registrarCarona(Carona carona) {
		LOGGER.log(Level.INFO, "{0} - {1} - {2}",
				new Object[] { carona.getID(), carona.getHost().getName(), carona });
	}

	private static void verOpcoesCorrida(Carona carona, Console console) {
		if (console.getUser().equals(carona.getHost())) {
			verOpcoesDono(carona, console);
		} else {
			verOpcoesPassageiro(carona, console);
		}
	}

	private static void verOpcoesDono(Carona carona, Console console) {
		switch (carona.getEstado()) {
		case AGUARDANDO:
			LOGGER.info("Vai partir agora? (1 - sim / 0 - nao)");
			if (console.getScanner().nextInt() != 0) {
				carona.setEstado(Estado.FECHADO);
			}
			break;
		case FECHADO:
			LOGGER.info("Ja chegou no ponto? Deseja finalizar? (1 - sim / 0 - nao)");
			if (console.getScanner().nextInt() != 0) {
				carona.setEstado(Estado.FINALIZADO);
			}
			break;
		default:
			LOGGER.info(SEM_OPCOES);
			break;
		}
	}

	private static void verOpcoesPassageiro(Carona carona, Console console) {
		if (carona.getEstado() == Estado.ABERTO) {
			LOGGER.info("Deseja sair dessa corrida? (1 - sim / 0 - nao)");
			if (console.getScanner().nextInt() == 0) {
				return;
			}
			try {
				carona.removePassageiro(console.getUser());
			} catch (UsuarioNaoEncontradoException e) {
				LOGGER.log(Level.WARNING, "Nao foi possivel remover o passageiro da carona.", e);
			}
		} else {
			LOGGER.info(SEM_OPCOES);
		}
	}
}