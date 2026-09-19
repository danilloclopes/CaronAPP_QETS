package me.br.caronapp.storage;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import me.br.caronapp.carona.Carona;
import me.br.caronapp.usuario.Usuario;

/*
 * Author: S�rgio Ricardo
 */

public class StorageManager implements Storable{

	@Override
	public void salva(ArrayList<Usuario> listaUsuarios, ArrayList<Carona> listaCaronas) {
		try {
			FileOutputStream usuariosFile = new FileOutputStream("usuarios.bin");
			FileOutputStream caronasFile = new FileOutputStream("caronas.bin");
			ObjectOutputStream usuariosOutputStream = new ObjectOutputStream(usuariosFile);
			ObjectOutputStream caronasOutputStream = new ObjectOutputStream(caronasFile);
			caronasOutputStream.writeInt(Carona.getSerial());
			for (Usuario usuario : listaUsuarios) {
				usuariosOutputStream.writeObject(usuario);
			}
			for (Carona carona : listaCaronas) {
				caronasOutputStream.writeObject(carona);
			}
			usuariosOutputStream.close();
			caronasOutputStream.close();
			usuariosFile.close();
			caronasFile.close();
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}	
	}

	@Override
	public void carrega(ArrayList<Usuario> listaUsuarios, ArrayList<Carona> listaCaronas) {
		if (listaUsuarios == null || listaCaronas == null) return;

		try {
			FileInputStream usuariosFile = new FileInputStream("usuarios.bin");
			FileInputStream caronasFile = new FileInputStream("caronas.bin");
			ObjectInputStream usuariosInputStream = new ObjectInputStream(usuariosFile);
			ObjectInputStream caronasInputStream = new ObjectInputStream(caronasFile);
			try {
				while(true) {
					listaUsuarios.add((Usuario) usuariosInputStream.readObject());
				}
			} catch (EOFException e) {
				
			} catch (ClassNotFoundException e) {
				System.out.println(e.getMessage());
			}
			try {
				Carona.setSerial(caronasInputStream.readInt());
				while(true) {
					listaCaronas.add((Carona) caronasInputStream.readObject());
				}
			} catch (EOFException e) {
				
			} catch (ClassNotFoundException e) {
				System.out.println(e.getMessage());
			}
			usuariosInputStream.close();
			caronasInputStream.close();
			usuariosFile.close();
			caronasFile.close();
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
}
