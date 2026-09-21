package me.br.caronapp.console;

import java.util.Scanner;

import me.br.caronapp.console.util.CaronaManager;
import me.br.caronapp.console.util.HostJoin;
import me.br.caronapp.console.util.ListarCaronas;
import me.br.caronapp.console.util.Lobby;
import me.br.caronapp.console.util.LoginRegistro;
import me.br.caronapp.usuario.Usuario;

/* Implementado por Eperson Cardoso Mayrink Xavier Filho
 * Modificado por Sergio Ricardo
 *  */

public class Console {
	
	public enum Stage {
		/* COMANDOS INICIAIS */
		LOGIN_REGISTRO,
		LOGIN,
		REGISTRO,
		/* COMANDOS LOGADO */
		LOBBY,
		LISTAR_CARONAS,
		HOST_CARONA,
		ENTRAR_CARONA,
		VER_CORRIDAS,
		DESLOGAR,
		/* ADMINISTRADOR */
		
		/* OUTROS */
		HALT;
	}
	
	private Scanner scan;
	private Stage stage;
	private Usuario usuario;
	
	public Console() {
		scan = new Scanner(System.in);
		this.logout();
	}
	
	public boolean draw() {
		if (usuario != null)
			System.out.println("-----> Usuario: " + this.usuario.getName());
		switch (stage) {
		case LOGIN_REGISTRO:
			LoginRegistro.loginRegistro(this);
			break;
		case LOGIN:
			LoginRegistro.login(this);
			break;
		case REGISTRO:
			LoginRegistro.registro(this);
			break;
		/*COMANDOS LOGADO*/
		case LOBBY:
			Lobby.draw(this);
			break;
		case LISTAR_CARONAS:
			ListarCaronas.draw(this);
			break;
		case HOST_CARONA:
			HostJoin.host(this);
			break;
		case ENTRAR_CARONA:
			HostJoin.join(this);
			break;
		case VER_CORRIDAS:
			CaronaManager.verCorridas(this);
			break;
		case DESLOGAR:
			this.logout();
			break;
		/* OUTROS */
		default:
			System.out.println("Finalizando console...");
			scan.close();
			return false;
		}
		
		
		return true;
	}
	
	public Scanner getScanner() {
		return this.scan;
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public Stage getStage() {
    return this.stage;
}
	
	public Usuario getUser() {
		return usuario;
	}
	
	public void setUser(Usuario usuario) {
		this.usuario = usuario;
	}
	
	public boolean isAdm() {
		return usuario.isAdm();
	}
	
	public void logout() {
		this.setUser(null);
		this.setStage(Stage.LOGIN_REGISTRO);
	}
}
