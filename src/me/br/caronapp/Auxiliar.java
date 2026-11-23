package me.br.caronapp;

import java.util.ArrayList;

import me.br.caronapp.carona.Carona;
import me.br.caronapp.carona.excecoes.UsuarioNaoEncontradoException;
import me.br.caronapp.usuario.Usuario;

/* Implementado por Eperson Cardoso Mayrink Xavier Filho */

public class Auxiliar {
	public static ArrayList<Carona> caronasAtivas = new ArrayList<Carona>();
	public static ArrayList<Usuario> usuarios = new ArrayList<Usuario>();
	
	/* Carona */
	
	public static boolean criarCarona(Carona c) {
		if (c == null || c.getHost() == null) return false;
		for (Usuario usuario : usuarios) {
			if (usuario.getUsername().equalsIgnoreCase(c.getHost().getUsername())) {
				caronasAtivas.add(c);
				return true;
			}
		}
		return false;
	}
	
	public static void removerCarona(Carona c) {
		caronasAtivas.remove(c);
	}
	
	public static void removerCarona(int id) {
		if (id < 0 || id >= caronasAtivas.size()) return;
		caronasAtivas.remove(id);
	}
	
	public static void removerPassageiro(String username, int caronaId) throws UsuarioNaoEncontradoException {
		if (caronaId < 0 || caronaId >= caronasAtivas.size()) return;
		caronasAtivas.get(caronaId).removePassageiro(username);
	}
	
	/* Login e registro */
	public static Usuario logar(String username, String senha) {
		for (Usuario u : usuarios) 
			if (u.podeLogar(username, senha)) return u;
		return null;
	}
	
	public static Usuario register(String username, String name, String cpf, String senha) {
		if (username == null || username.trim().isEmpty()) return null;
		if (existe(username)) return null;
		try {
			Usuario u = new Usuario(username, name, cpf, senha);
			usuarios.add(u);
			return u;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static boolean existe(String username) {
		if (username == null) return false;
		for (Usuario u : usuarios)
			if (username.equalsIgnoreCase(u.getUsername())) return true;
		return false;
	}
	
	/* Atualizacao de tempo */
	
	public static void updateTime() {
		for (Carona c : caronasAtivas) {
			c.atualiza();
		}
	}
}
