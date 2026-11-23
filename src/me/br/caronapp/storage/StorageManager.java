package me.br.caronapp.storage;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.br.caronapp.carona.Carona;
import me.br.caronapp.usuario.Usuario;

/*
 * Author: Sergio Ricardo
 */

public class StorageManager implements Storable{

	private static final Logger LOGGER = Logger.getLogger(StorageManager.class.getName());

	public static final String ARQUIVO_USUARIOS_PADRAO = "usuarios.bin";
	public static final String ARQUIVO_CARONAS_PADRAO = "caronas.bin";

	private final String arquivoUsuarios;
	private final String arquivoCaronas;

	/* Usa os arquivos padrao do sistema, no diretorio de trabalho. */
	public StorageManager() {
		this(ARQUIVO_USUARIOS_PADRAO, ARQUIVO_CARONAS_PADRAO);
	}

	/* Permite escolher onde os dados sao gravados (ex.: pasta temporaria nos testes). */
	public StorageManager(String arquivoUsuarios, String arquivoCaronas) {
		if (caminhoInvalido(arquivoUsuarios) || caminhoInvalido(arquivoCaronas)) {
			throw new IllegalArgumentException("Os caminhos dos arquivos nao podem ser nulos ou vazios");
		}
		if (arquivoUsuarios.equals(arquivoCaronas)) {
			throw new IllegalArgumentException("Usuarios e caronas precisam ser gravados em arquivos diferentes");
		}
		this.arquivoUsuarios = arquivoUsuarios;
		this.arquivoCaronas = arquivoCaronas;
	}

	private static boolean caminhoInvalido(String caminho) {
		return caminho == null || caminho.trim().isEmpty();
	}

	public String getArquivoUsuarios() {
		return arquivoUsuarios;
	}

	public String getArquivoCaronas() {
		return arquivoCaronas;
	}

	@Override
	public void salva(ArrayList<Usuario> listaUsuarios, ArrayList<Carona> listaCaronas) {
		// Sem esta verificacao os arquivos seriam abertos (e zerados) antes do NullPointerException.
		if (listaUsuarios == null || listaCaronas == null) {
			LOGGER.warning("Listas nulas: nada foi salvo");
			return;
		}

		// try-with-resources: os arquivos sao fechados mesmo se ocorrer erro no meio da gravacao.
		try (FileOutputStream usuariosFile = new FileOutputStream(arquivoUsuarios);
				FileOutputStream caronasFile = new FileOutputStream(arquivoCaronas);
				ObjectOutputStream usuariosOutputStream = new ObjectOutputStream(usuariosFile);
				ObjectOutputStream caronasOutputStream = new ObjectOutputStream(caronasFile)) {
			caronasOutputStream.writeInt(Carona.getSerial());
			for (Usuario usuario : listaUsuarios) {
				usuariosOutputStream.writeObject(usuario);
			}
			for (Carona carona : listaCaronas) {
				caronasOutputStream.writeObject(carona);
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Erro ao salvar os dados: {0}", e.getMessage());
		}
	}

	@Override
	public void carrega(ArrayList<Usuario> listaUsuarios, ArrayList<Carona> listaCaronas) {
		if (listaUsuarios == null || listaCaronas == null) return;

		try (FileInputStream usuariosFile = new FileInputStream(arquivoUsuarios);
				FileInputStream caronasFile = new FileInputStream(arquivoCaronas);
				ObjectInputStream usuariosInputStream = new ObjectInputStream(usuariosFile);
				ObjectInputStream caronasInputStream = new ObjectInputStream(caronasFile)) {
			carregaUsuarios(usuariosInputStream, listaUsuarios);
			carregaCaronas(caronasInputStream, listaCaronas);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Erro ao carregar os dados: {0}", e.getMessage());
		}
	}

	private static void carregaUsuarios(ObjectInputStream entrada, List<Usuario> listaUsuarios) throws IOException {
		try {
			boolean fimDoArquivo = false;
			while (!fimDoArquivo) {
				try {
					listaUsuarios.add((Usuario) entrada.readObject());
				} catch (EOFException e) {
					// EOFException e o fim normal do arquivo: todos os usuarios ja foram lidos.
					LOGGER.log(Level.FINE, "Fim do arquivo de usuarios", e);
					fimDoArquivo = true;
				}
			}
		} catch (ClassNotFoundException e) {
			LOGGER.log(Level.SEVERE, "Classe nao encontrada ao ler usuarios: {0}", e.getMessage());
		}
	}

	private static void carregaCaronas(ObjectInputStream entrada, List<Carona> listaCaronas) throws IOException {
		try {
			Carona.setSerial(entrada.readInt());
		} catch (EOFException e) {
			// Arquivo sem o serial (vazio): nao ha caronas para ler.
			LOGGER.log(Level.FINE, "Arquivo de caronas sem serial", e);
			return;
		}
		try {
			boolean fimDoArquivo = false;
			while (!fimDoArquivo) {
				try {
					listaCaronas.add((Carona) entrada.readObject());
				} catch (EOFException e) {
					// EOFException e o fim normal do arquivo: todas as caronas ja foram lidas.
					LOGGER.log(Level.FINE, "Fim do arquivo de caronas", e);
					fimDoArquivo = true;
				}
			}
		} catch (ClassNotFoundException e) {
			LOGGER.log(Level.SEVERE, "Classe nao encontrada ao ler caronas: {0}", e.getMessage());
		}
	}

}
