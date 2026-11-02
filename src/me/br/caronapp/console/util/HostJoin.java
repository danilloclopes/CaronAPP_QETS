package me.br.caronapp.console.util;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.InputMismatchException;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.br.caronapp.Auxiliar;
import me.br.caronapp.carona.Campus;
import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.PontosDeEncontro;
import me.br.caronapp.carona.Posicao;
import me.br.caronapp.carona.Rota;
import me.br.caronapp.carona.excecoes.VagasExcedidasException;
import me.br.caronapp.console.Console;
import me.br.caronapp.console.Console.Stage;

/*
 * Author: Sérgio Ricardo
 */

public class HostJoin {

	private static final Logger LOGGER = Logger.getLogger(HostJoin.class.getName());

	private HostJoin() {
	}

	public static void host(Console console) {
		int sentido = 0;
		int numeroDePassageiros = 0;
		Campus campus;
		Posicao posicao;
		LocalDateTime dataHora;
		
		do {
			LOGGER.info("Insira a quantidade de passageiros:");
			numeroDePassageiros = definirPassageiros(console);
		} while (numeroDePassageiros < 1 || numeroDePassageiros > 4);
		
		do {
			LOGGER.info("Insira a data e a hora da carona que deseja criar (YYYY MM DD hh mm):");
			dataHora = definirData(console);
		} while (dataHora == null);
		
		do {
			LOGGER.info("1 - Sentido UFF\n2 - Saindo da UFF");
			sentido = selecionarSentido(console);
		} while (sentido != 1 && sentido != 2);
		
		do {
			LOGGER.log(Level.INFO, "Escolha o local de {0}", (sentido == 1) ? "destino:" : "partida:");
			campus = selecionarCampus(console);
		} while (campus == null);
		
		do {
			LOGGER.log(Level.INFO, "Entre com as informações do local de {0}", (sentido == 1) ? "partida:" : "destino:");
			posicao = definirPosicao(console);
		} while (posicao == null);
		
		Rota rota = (sentido == 1) ? new Rota(posicao, campus) : new Rota(campus, posicao);
	
		Auxiliar.criarCarona(new Carona(console.getUser(), numeroDePassageiros, dataHora, rota));
		
		console.setStage(Stage.LOBBY);
	}
	
	public static void join(Console console) {
		Carona carona = null;
		if(ListarCaronas.draw(console)) {
			do {
				LOGGER.info("Entre com a ID da carona que deseja participar:");
				carona = selecionarCarona(console);
				if (carona == null) continue;
				try {
					if(carona.adicionaPassageiro(console.getUser())) {
						LOGGER.info("Registro de participação efetuado com sucesso!");
					} else {
						LOGGER.info("Falha.");
					}
					
				} catch (VagasExcedidasException e) {
					LOGGER.info("Não há vagas disponíveis nessa carona...");
				}
			} while (carona == null);
		}
		console.setStage(Stage.LOBBY);
	}
		
	public static Campus selecionarCampus(Console console) {
		try {
			for (int i = 0; i < PontosDeEncontro.values().length; i++) {
				LOGGER.log(Level.INFO, "{0} - {1}", new Object[] { i, PontosDeEncontro.values()[i].getNome() });
			}
			LOGGER.info("-----------\nDigite o id nome do campus:");
			return new Campus(PontosDeEncontro.values()[console.getScanner().nextInt()]);
		} catch (IndexOutOfBoundsException e) {
			LOGGER.info("Id invalido...");
			console.setStage(Stage.LOBBY);
			return null;
		} catch (Exception e) {
			LOGGER.info("Id invalido...");
			console.setStage(Stage.LOBBY);
			console.getScanner().next();
			return null;
		}
	}
	
	public static Posicao definirPosicao(Console console) {
		double latitude;
		double longitude;
		String nome;
		int numero;
		String cep;
		
		try {
		LOGGER.info("Latitude: ");
			latitude = console.getScanner().nextDouble();
		LOGGER.info("Longitude: ");
			longitude = console.getScanner().nextDouble();
		LOGGER.info("Nome da Rua: ");
			nome = console.getScanner().next();
		LOGGER.info("Número: ");
			numero = console.getScanner().nextInt();
		LOGGER.info("Cep: ");
			cep = console.getScanner().next();
		} catch (Exception e) {
			LOGGER.info("Valor inválido...");
			console.getScanner().next();
			return null;
		}
		
		return new Posicao(latitude, longitude, nome, numero, cep);
	}
	
	public static LocalDateTime definirData(Console console) {
		int ano;
		int mes;
		int dia;
		int hora;
		int minuto;
		try {
			ano = console.getScanner().nextInt();
			mes = console.getScanner().nextInt();
			dia = console.getScanner().nextInt();
			hora = console.getScanner().nextInt();
			minuto = console.getScanner().nextInt();
		} catch (InputMismatchException e) {
			LOGGER.info("Data inválida...");
			console.getScanner().next();
			return null;
		}
		try {
			return LocalDateTime.of(ano, mes, dia, hora, minuto);
		} catch (DateTimeException e) {
			LOGGER.info("Data invalida...");
			return null;
		}
	}
	
	public static int definirPassageiros(Console console) {
		int numeroDePassageiros;
		try {
			numeroDePassageiros = console.getScanner().nextInt();
			if (numeroDePassageiros < 1 || numeroDePassageiros > 4) LOGGER.info("Quantidade inválida...");
		} catch (Exception e) {
			LOGGER.info("Quantidade inválida...");
			console.getScanner().next();
			return -1;
		}
		return numeroDePassageiros;
	}
	
	public static int selecionarSentido(Console console) {
		int sentido;
		try {
			sentido = console.getScanner().nextInt();
			if (sentido != 1 && sentido != 2) LOGGER.info("Sentido inválido...");
		} catch (Exception e) {
			LOGGER.info("Sentido inválido...");
			console.getScanner().next();
			return -1;
		}
		return sentido;
	}
	
	public static Carona selecionarCarona(Console console) {
		try {
			int id = console.getScanner().nextInt();
			for (Carona carona : Auxiliar.caronasAtivas) {
				if (carona.getID() == id) {
					return carona;
				}
			}
		} catch (Exception e) {
			LOGGER.info("ID inválida...");
			console.getScanner().next();
			return null;
		}
		LOGGER.info("ID inválida...");
		return null;
	}
}
