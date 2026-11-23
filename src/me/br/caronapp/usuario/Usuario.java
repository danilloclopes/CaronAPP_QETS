package me.br.caronapp.usuario;

import java.io.Serializable;
import java.util.ArrayList;

import me.br.caronapp.carona.Carona;

/* Implementado por Eperson Cardoso Mayrink Xavier Filho */

public class Usuario implements Comparable<Usuario>, Serializable {
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String username;
	private String cpf;
	private String senha;
	private int stars;
	private int avaliacoes;
	private boolean adm;
	private ArrayList<Carona> caronas;
	private ArrayList<Carona> historico;

	public Usuario(String username, String name, String cpf, String senha) {
		validarCampo(username, "username");
		validarCampo(name, "name");
		validarCampo(cpf, "cpf");
		validarCampo(senha, "senha");
		this.username = username;
		this.name = name;
		this.cpf = cpf;
		this.senha = senha;
		this.stars = 10;
		this.avaliacoes = 0;
		this.caronas = new ArrayList<Carona>();
		this.historico = new ArrayList<Carona>();
	}

	private static void validarCampo(String valor, String campo) {
		if (valor == null || valor.trim().isEmpty()) {
			throw new IllegalArgumentException(campo + " nao pode ser nulo ou vazio");
		}
	}
	
	public boolean podeLogar(String username, String senha) {
		if (username == null || senha == null) return false;
		if (!username.equalsIgnoreCase(this.username)) return false;
		return this.senha.equals(senha);
	}
	
	@Override
	public int compareTo(Usuario o) {
		return o.getUsername().compareTo(this.getUsername());
	}
	
	public boolean equals(Usuario o) {
		return o.getUsername().equalsIgnoreCase(this.getUsername());
	}
	
	/* GETTERS AND SETTERS */
	
	public Carona getPeloId(int id) {
		for (Carona c : this.caronas) if (c.getID() == id) return c;
		return null;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getCpf() {
		return cpf;
	}
	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getSenha() {
		return senha;
	}
	
	public void setSenha(String senha) {
		this.senha = senha;
	}
	
	public boolean isAdm() {
		return adm;
	}
	
	public int getStars() {
		return this.stars;
	}
	public void giveStars(int amt) {
		if (amt < 0 || amt > 5) return;
		this.avaliacoes++;
		this.stars = ((this.stars * (this.avaliacoes - 1)) + amt)/(this.avaliacoes);
	}
	
	public void addCarona(Carona c) {
		if (caronas.contains(c)) return;
		caronas.add(c);
	}
	
	public  void cancelaCarona(Carona c) {
		caronas.remove(c);
	}
	
	public void finalizaCarona(Carona c) {
		if (!caronas.remove(c)) return;
		historico.add(c);
	}
	
	public ArrayList<Carona> getCaronas() {
		return this.caronas;
	}
	
	public ArrayList<Carona> getHistorico() {
		return this.historico;
	}
}
