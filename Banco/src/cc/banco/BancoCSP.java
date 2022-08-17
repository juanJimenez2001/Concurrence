// BancoCSP_skel.java
// Lars feat. Julio -- 2021
// esqueleto de codigo para JCSP
// (peticiones aplazadas)

package cc.banco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.ProcessManager;

import es.upm.aedlib.Pair;
import es.upm.aedlib.fifo.FIFO;
import es.upm.aedlib.fifo.FIFOArray;
import es.upm.aedlib.indexedlist.ArrayIndexedList;


public class BancoCSP implements Banco, CSProcess {

	// canales: uno por operacion
	// seran peticiones aplazadas
	private Any2OneChannel chIngresar; //Canal ingresar 
	private Any2OneChannel chDisponible; //Canal disponible 
	private Any2OneChannel chTransferir; //Canal transferir 
	private Any2OneChannel chAlertar; //Canal alertar 


	// clases para peticiones
	// regalamos una como ejemplo
	public class TransferirReq {
		String origen; 
		String destino; 
		int value; 
		One2OneChannel resp; 

		// constructor
		public TransferirReq(String from, String to, int value) {
			this.origen = from; this.destino = to; this.value = value; this.resp = Channel.one2one();
		}
	}

	public class AlertarReq {
		String cuenta; 
		int value; 
		One2OneChannel resp; 

		// constructor
		public AlertarReq(String cuenta, int value) {
			this.cuenta = cuenta; this.value = value; this.resp = Channel.one2one();
		}
	}

	public class IngresarReq {
		String cuenta; 
		int value; 
		One2OneChannel resp; 

		// constructor
		public IngresarReq(String cuenta, int value) {
			this.cuenta = cuenta; this.value = value; this.resp = Channel.one2one();
		}
	}

	public class DisponibleReq {
		String cuenta; 
		One2OneChannel resp; 

		// constructor
		public DisponibleReq(String cuenta) {
			this.cuenta = cuenta; this.resp = Channel.one2one();
		}
	}

	// constructor de BancoCSP
	public BancoCSP() {
		this.chIngresar = Channel.any2one(); 
		this.chAlertar = Channel.any2one(); 
		this.chDisponible = Channel.any2one(); 
		this.chTransferir = Channel.any2one(); 
		new ProcessManager(this).start(); 
	}

	// interfaz Banco
	/**
	 * Un cajero pide que se ingrese una determinado valor v a una
	 * cuenta c. Si la cuenta no existe, se crea.
	 * @param c numero de cuenta
	 * @param v valor a ingresar
	 */
	public void ingresar(String c, int v) {
		IngresarReq ingreso= new IngresarReq(c, v); 
		chIngresar.out().write(ingreso); 
		ingreso.resp.in().read();
	}

	/**
	 * Un ordenante pide que se transfiera un determinado valor v desde
	 * una cuenta o a otra cuenta d.
	 * @param o numero de cuenta origen
	 * @param d numero de cuenta destino
	 * @param v valor a transferir
	 * @throws IllegalArgumentException si o y d son las mismas cuentas
	 *
	 */
	public void transferir(String o, String d, int v) {
		if (o.equals(d)) throw new IllegalArgumentException(); 
		TransferirReq transferencia= new TransferirReq(o, d, v); 
		chTransferir.out().write(transferencia); 
		transferencia.resp.in().read();
	}

	/**
	 * Un consultor pide el saldo disponible de una cuenta c.
	 * @param c numero de la cuenta
	 * @return saldo disponible en la cuenta id
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public int disponible(String c) {
		DisponibleReq disponible= new DisponibleReq(c); 
		chDisponible.out().write(disponible); 
		int n=(int) disponible.resp.in().read(); 
		if(n==-1) throw new IllegalArgumentException(); 
		return n; 
	}

	/**
	 * Un avisador establece una alerta para la cuenta c. La operacion
	 * termina cuando el saldo de la cuenta c baja por debajo de m.
	 * @param c numero de la cuenta
	 * @param m saldo minimo
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public void alertar(String c, int v) {
		AlertarReq alertar= new AlertarReq(c, v);
		chAlertar.out().write(alertar);
		int n= (int) alertar.resp.in().read();
		if(n==-1) throw new IllegalArgumentException();
	}

	// Codigo del servidor
	public void run() {
		// nombres simbolicos para las entradas
		final int INGRESAR   = 0;
		final int DISPONIBLE = 1;
		final int TRANSFERIR = 2;
		final int ALERTAR    = 3;

		// construimos la estructura para recepcion alternativa
		final Guard[] guards = new AltingChannelInput[4];
		guards[INGRESAR]   = chIngresar.in();
		guards[DISPONIBLE] = chDisponible.in();
		guards[TRANSFERIR] = chTransferir.in();
		guards[ALERTAR]    = chAlertar.in();
		Alternative servicios = new Alternative(guards);
		
		Map<String, Integer> cuentas= new HashMap<String, Integer>(); //Mapa con las distintas cuentas y sus respectivos saldos

		ArrayList<AlertarReq> listaAlertas = new ArrayList<AlertarReq>(); //Lista donde vamos a guardar las distintas alertas
		ArrayList<TransferirReq> listaTransferencias = new ArrayList<TransferirReq>(); //Lista donde vamos a guardar las distintas transferencias

		while(true) {
			int servicio = servicios.fairSelect();

			switch (servicio) {
			case INGRESAR: {
				IngresarReq  ingreso = (IngresarReq) chIngresar.in().read();
				if(!cuentas.containsKey(ingreso.cuenta))
					cuentas.put(ingreso.cuenta, ingreso.value);
				else
					cuentas.put(ingreso.cuenta, cuentas.get(ingreso.cuenta)+ingreso.value);
				ingreso.resp.out().write(null);
				break;
			}
			case DISPONIBLE: {
				DisponibleReq disponible=(DisponibleReq) chDisponible.in().read();
				if(cuentas.containsKey(disponible.cuenta)) { //Comprobamos si la cuenta existe, si no existe devolvemos menos uno y si existe devolvemos el saldo
					int saldo = cuentas.get(disponible.cuenta);
					disponible.resp.out().write(saldo);
				}
				else
					disponible.resp.out().write(-1);	
				break;
			}
			case TRANSFERIR: {
				TransferirReq transferir=(TransferirReq) chTransferir.in().read();
				listaTransferencias.add(transferir);
				break;
			}
			case ALERTAR: {
				AlertarReq alerta=(AlertarReq) chAlertar.in().read();
				if(!cuentas.containsKey(alerta.cuenta)) { //Comprobamos si la cuenta existe, si no existe devolvemos menos uno para lanzar la excepcion 
					alerta.resp.out().write(-1);
				}	
				else
					listaAlertas.add(alerta);
				break;
			}
			}// END SWITCH
			boolean cambio = true;

			while(cambio) {
				cambio = false;
				for(AlertarReq alerta : listaAlertas){					
					if(cuentas.containsKey(alerta.cuenta) && cuentas.get(alerta.cuenta) < alerta.value){
						alerta.resp.out().write(0);
						listaAlertas.remove(alerta);
						cambio=true;
						break;
					}
				}
				ArrayList<String> pendientes = new ArrayList<String>(); //Lista para guardar las cuentas origen de las transferencias que ya hemos comprobado
				for(TransferirReq transferir : listaTransferencias){
					if(!pendientes.contains(transferir.origen) && cuentas.containsKey(transferir.origen) && cuentas.containsKey(transferir.destino) && cuentas.get(transferir.origen)>=transferir.value){
						cuentas.put(transferir.origen, cuentas.get(transferir.origen) - transferir.value);
						cuentas.put(transferir.destino, cuentas.get(transferir.destino) + transferir.value);
						transferir.resp.out().write(null);
						listaTransferencias.remove(transferir);
						cambio=true;
						break;
					}
					else
						pendientes.add(pendientes.size(), transferir.origen);
				}  		        
			}
		}// END BUCLE SERVICIO
	}// END run SERVIDOR
}

