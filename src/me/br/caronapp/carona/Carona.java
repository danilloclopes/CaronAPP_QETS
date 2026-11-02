package me.br.caronapp.carona;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import me.br.caronapp.usuario.Usuario;
import me.br.caronapp.carona.Rota.Sentido;
import me.br.caronapp.carona.excecoes.*;

/*
 * Author: Sérgio Ricardo
 * Modificado por Eperson Filho
 */

public class Carona implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static int serial = 0;
	
	public enum Estado {ABERTO, AGUARDANDO, FECHADO, FINALIZADO};	// ABERTO: Aceita guests.
	 																// AGUARDANDO, FECHADO e FINALIZADO: Não aceita guests.
	
	private Usuario host;
	private ArrayList<Usuario> guests = new ArrayList<Usuario>();
	private int maximoGuests;
	private Calendar dataHora;
	private Rota rota;
	private Estado estado;
	
	private int id;
	
	public Carona(Usuario host, int maximoGuests, LocalDateTime dataHora, Rota rota) {
		this(host, maximoGuests, GregorianCalendar.from(dataHora.atZone(ZoneId.systemDefault())), rota);
	}

	public Carona(Usuario host, int maximoGuests, Calendar dataHora, Rota rota) {
		super();
		this.host = host;
		this.maximoGuests = maximoGuests;
		this.dataHora = dataHora;
		this.rota = rota;
		this.estado = Estado.ABERTO;
		
		serial++;
		this.id = serial;
		
		this.host.addCarona(this);
	}
	
	public boolean adicionaPassageiro(Usuario user) throws VagasExcedidasException {
		if (estaNaCorrida(user)) return false;
		if (estado != Estado.ABERTO) return false;
		
		if (getVagasPreenchidas() < maximoGuests) {
			guests.add(user);
			user.addCarona(this);
		} else {
			throw new VagasExcedidasException();
		}
		
		return true;
	}
	
	public void removePassageiro(Usuario user) throws UsuarioNaoEncontradoException {
		if (!this.estado.equals(Estado.ABERTO)) return;
		if (!guests.remove(user)) throw new UsuarioNaoEncontradoException();
		else
			user.cancelaCarona(this);
	}
	
	public void removePassageiro(String username) throws UsuarioNaoEncontradoException {
		for (Usuario u : guests) if (u.getUsername().equalsIgnoreCase(username)) {removePassageiro(u);return;}
		throw new UsuarioNaoEncontradoException();
	}
	
	public void atualiza() {
		long tempoAtual = Calendar.getInstance().getTime().getTime();	
		long tempoCarona = this.dataHora.getTime().getTime();
		if(tempoCarona - tempoAtual <= 1800000) {
			setEstado(Estado.AGUARDANDO);
		} else if(tempoAtual - tempoCarona >= 21600000) {
			setEstado(Estado.FINALIZADO);
		}
	}
	
	public boolean estaNaCorrida(Usuario user) {
		if (user.equals(this.host)) return true;
		if (this.guests.contains(user)) return true;
		return false;
	}
	
	private void finaliza() {
		host.finalizaCarona(this);
		for (int i = 0; i < guests.size(); i++)
			guests.get(i).finalizaCarona(this);
	}
	
	@Override
	public String toString() {
		if (getRota().getSentido() == Sentido.IDA) return getRota().getDestino().getPontoDeEncontro().getNome();
		else return getRota().getOrigem().getPontoDeEncontro().getNome();
	}
	
	// Getters and Setters
	
	public int getVagasPreenchidas() {
		return guests.size();
	}
	
	public Usuario getHost() {
		return host;
	}

	public int getMaximoGuests() {
		return maximoGuests;
	}

	public void setMaximoGuests(int maximoGuests) {
		this.maximoGuests = maximoGuests;
	}

	public Calendar getDataHora() {
		return dataHora;
	}

	public void setDataHora(Calendar dataHora) {
		this.dataHora = dataHora;
	}

	public Rota getRota() {
		return rota;
	}

	public void setRota(Rota rota) {
		this.rota = rota;
	}
	
	public Estado getEstado() {
		return estado;
	}
	
	public void setEstado(Estado estado) {
		if(this.estado.equals(Estado.FINALIZADO)) return;
		this.estado = estado;
		if (this.estado.equals(Estado.FINALIZADO))
			this.finaliza();
	}
	
	public int getID() {
		return id;
	}
	
	public static int getSerial() {
		return serial;
	}
	
	public static void setSerial(int number) {
		serial = number;
	}
	
}
