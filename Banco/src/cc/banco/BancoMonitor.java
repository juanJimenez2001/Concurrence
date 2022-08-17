
package cc.banco;


import es.upm.aedlib.Pair;
import es.upm.babel.cclib.Monitor;

import java.util.*;

public class BancoMonitor implements Banco {


	private Map<String, Integer> cuentas;
	private Map<String,Pair<Monitor.Cond,Monitor.Cond>> no_creadas;
	private Monitor mutex;
	private ArrayList<Transferir> listaTransferencias;
	private ArrayList<Alertar> listaAlertas;
	private Map<String, Integer> transferencias;



	public BancoMonitor() {
		cuentas = new HashMap<String, Integer>();
		no_creadas = new HashMap<String,Pair<Monitor.Cond,Monitor.Cond>>();
		mutex = new Monitor();
		listaTransferencias=new ArrayList<Transferir>();
		listaAlertas=new ArrayList<Alertar>();   
		transferencias = new HashMap<String,Integer>();
	}

	/**
	 * Un cajero pide que se ingrese una determinado valor v a una
	 * cuenta  Entregas disponibles: ∞ c. Si la cuenta no existe, se crea.
	 *
	 * @param c número de cuenta
	 * @param v valor a ingresar
	 */
	public void ingresar(String c, int v) {
		mutex.enter();
		if (!cuentas.containsKey(c)) {
			cuentas.put(c,v);
			if(!no_creadas.containsKey(c)) no_creadas.put(c,new Pair<Monitor.Cond,Monitor.Cond>(mutex.newCond(),mutex.newCond()));
		} else {
			cuentas.put(c,cuentas.get(c)+v);
		}
		desbloqueo_transf();
		mutex.leave();
	}


	/**
	 * Un orde Entregas disponibles: ∞ nante pide que se transfiera un determinado valor v desde
	 * una cuenta o a otra cuenta d.
	 *
	 * @param o número de cuenta origen
	 * @param d número de cuenta destino
	 * @param v valor a transferir
	 * @throws IllegalArgumentException si o y d son las mismas cuentas
	 */
	public void transferir(String o, String d, int v) {
		if (o.equals(d)) throw new IllegalArgumentException();
		mutex.enter();
		if(!cuentas.containsKey(o) || !cuentas.containsKey(d) || cuentas.get(o)<v || transferencias.containsKey(o)) {
			if(transferencias.containsKey(o))
				transferencias.put(o, transferencias.get(o)+1);
			else
				transferencias.put(o,1);
			if(!no_creadas.containsKey(o)) no_creadas.put(o,new Pair<Monitor.Cond,Monitor.Cond>(mutex.newCond(),mutex.newCond()));
			Transferir transferencia=new Transferir(o,d,v);
			listaTransferencias.add(transferencia);
			no_creadas.get(o).getLeft().await();
		}
		cuentas.put(o,cuentas.get(o)-v);
		desbloqueo_minimo();
		cuentas.put(d,cuentas.get(d)+v);
		desbloqueo_transf();
		mutex.leave();
	}

	private void desbloqueo_transf(){
		ArrayList<String> pendientes = new ArrayList<String>();
		for(Transferir transferencia: listaTransferencias){
			if(!pendientes.contains(transferencia.origen) && cuentas.containsKey(transferencia.origen) && cuentas.containsKey(transferencia.destino) && cuentas.get(transferencia.origen)>=transferencia.value){
				no_creadas.get(transferencia.origen).getLeft().signal();
				listaTransferencias.remove(transferencia);
				if(transferencias.get(transferencia.origen)==1)
					transferencias.remove(transferencia.origen);
				else
					transferencias.put(transferencia.origen, transferencias.get(transferencia.origen)-1);
				break;
			}
			else
				pendientes.add(pendientes.size(), transferencia.origen);
		}
	}

	private void desbloqueo_minimo(){
		for(Alertar alerta: listaAlertas){
			if(cuentas.containsKey(alerta.cuenta) && cuentas.get(alerta.cuenta) < alerta.value){
				listaAlertas.remove(alerta);
				no_creadas.get(alerta.cuenta).getRight().signal();
				break;
			}
		}
	}

	/**
	 * Un consultor pide el saldo disponible de una cuenta c.
	 *
	 * @param c número de la cuenta
	 * @return saldo disponible en la cuenta id
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public int disponible(String c) {
		mutex.enter();
		if (!cuentas.containsKey(c)) {
			mutex.leave();
			throw new IllegalArgumentException();
		}
		int aux = cuentas.get(c);
		mutex.leave();
		return aux;
	}

	/**
	 * Un avisador establece una alerta para la cuenta c. La operación
	 * termina cuando el saldo de la cuenta c baja por debajo de m.
	 *
	 * @param c número de la cuenta
	 * @param m saldo mínimo
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public void alertar(String c, int m) {
		mutex.enter();
		if (!cuentas.containsKey(c)) {
			mutex.leave();
			throw new IllegalArgumentException();
		}
		if (cuentas.get(c) >= m) {
			Alertar alerta= new Alertar(c,m);
			listaAlertas.add(alerta);
			no_creadas.get(c).getRight().await();
		}
		desbloqueo_minimo();
		mutex.leave();
	}

	public class Transferir {
		String origen;
		String destino;
		int value;

		public Transferir(String from, String to, int value) {
			this.origen = from; this.destino = to; this.value = value;
		}
	}

	public class Alertar {
		String cuenta;
		int value;

		public Alertar(String cuenta, int value) {
			this.cuenta = cuenta; this.value = value;
		}
	}
}
