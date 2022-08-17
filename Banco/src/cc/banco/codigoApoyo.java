package cc.banco;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.ChannelOutput;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.ProcessInterruptedException;
import org.jcsp.lang.ProcessManager;

import es.upm.aedlib.Pair;
import es.upm.aedlib.Position;
import es.upm.aedlib.fifo.FIFO;
import es.upm.aedlib.fifo.FIFOArray;
import es.upm.aedlib.map.HashTableMap;
import es.upm.aedlib.map.Map;
import es.upm.aedlib.positionlist.NodePositionList;
import es.upm.aedlib.positionlist.PositionList;



public class codigoApoyo implements Banco, CSProcess {

	//Canales para enviar y recibir peticiones al/del servidor
	private final Any2OneChannel chIngresar = Channel.any2one();
	private final Any2OneChannel chTransferir = Channel.any2one();
	private final Any2OneChannel chDisponible = Channel.any2one();
	private final Any2OneChannel chAlertar = Channel.any2one();

	Map<String,Integer> bancoDB;
	
	public class petTransferir{
		private String origen = "";
		private String destino = "";
		private int monto = -1;
		public petTransferir(String origen, String destino, int v) {
			this.origen = origen;
			this.destino = destino;
			monto = v;
		}
	}


	public codigoApoyo() {
		bancoDB = new HashTableMap<String,Integer>();
		new ProcessManager(this).start();
	}

	/**
	 * Un cajero pide que se ingrese una determinado valor v a una
	 * cuenta c. Si la cuenta no existe, se crea.
	 * @param c número de cuenta
	 * @param v valor a ingresar
	 */
	public void ingresar(String c, int v) {
		petTransferir par = new petTransferir(c, "", v);	
		Any2OneChannel canal = Channel.any2one();
		Pair<Any2OneChannel,petTransferir> pair = new Pair<Any2OneChannel,petTransferir>(canal, par);
		chIngresar.out().write(pair);
		canal.in().read();


	}

	/**
	 * Un ordenante pide que se transfiera un determinado valor v desde
	 * una cuenta o a otra cuenta d.
	 * @param o número de cuenta origen
	 * @param d número de cuenta destino
	 * @param v valor a transferir
	 * @throws IllegalArgumentException si o y d son las mismas cuentas
	 *
	 */
	public void transferir(String o, String d, int v) {
		if(d.equals(o)) // si es la misma cuenta o es nula, da error
			throw new IllegalArgumentException();
		petTransferir par = new petTransferir(o, d, v);	
		Any2OneChannel canal = Channel.any2one();
		Pair<Any2OneChannel,petTransferir> pair = new Pair<Any2OneChannel,petTransferir>(canal, par);
		chTransferir.out().write(pair);
		canal.in().read();

	}

	/**
	 * Un consultor pide el saldo disponible de una cuenta c.
	 * @param c número de la cuenta
	 * @return saldo disponible en la cuenta id
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public int disponible(String c) {
		if(!bancoDB.containsKey(c))
			throw new IllegalArgumentException();
		petTransferir par = new petTransferir(c, "", 0);	
		Any2OneChannel canal = Channel.any2one();
		Pair<Any2OneChannel,petTransferir> pair = new Pair<Any2OneChannel,petTransferir>(canal, par);
		chDisponible.out().write(pair);
		return (int) canal.in().read();
	}

	/**
	 * Un avisador establece una alerta para la cuenta c. La operación
	 * termina cuando el saldo de la cuenta c baja por debajo de m.
	 * @param c número de la cuenta
	 * @param m saldo mínimo
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public void alertar(String c, int m) {
		if(!bancoDB.containsKey(c))
			throw new IllegalArgumentException();
		petTransferir par = new petTransferir(c, "", m);	
		Any2OneChannel canal = Channel.any2one();
		Pair<Any2OneChannel, petTransferir> pair = new Pair<Any2OneChannel,petTransferir>(canal, par);
		chAlertar.out().write(pair);
		canal.in().read();


	}

	/**
	 * Codigo para el servidor.
	 */
	private static final int INGRESAR = 0;
	private static final int TRANSFERIR = 1;
	private static final int DISPONIBLE = 2;
	private static final int ALERTAR = 3;
	public void run() {

		Guard[] entradas = {chIngresar.in(), chTransferir.in(), chDisponible.in(), chAlertar.in()};
		Alternative servicios = new Alternative(entradas);
		int choice = 0;


		PositionList<petTransferir> ordenTransferencias = new NodePositionList<petTransferir>();;
		FIFO<Pair<Any2OneChannel,petTransferir>> cola = new FIFOArray<Pair<Any2OneChannel,petTransferir>>();
		FIFO<Integer> turnos = new FIFOArray<Integer>();


		while(true) {
			try {
				choice = servicios.fairSelect();
			}
			catch (ProcessInterruptedException e) {}
			switch(choice) {
			case INGRESAR:
				Pair<Any2OneChannel,petTransferir> pairIngresar = (Pair<Any2OneChannel,petTransferir>) chIngresar.in().read();
				//System.out.println("Se anade a la cola INGRESAR: " + "[Cuenta:" + pairIngresar.getRight().origen + ", Monto: " + pairIngresar.getRight().monto + "]");


				String cuentaIng = pairIngresar.getRight().origen;
				if(!bancoDB.containsKey(cuentaIng)){ // si no existe la cuenta, se crea
					bancoDB.put(cuentaIng, 0);
					//System.out.println("Se crea la " + "[Cuenta:" + cuentaIng + ", Monto: " + pairIngresar.getRight().monto + "]");

				}
				bancoDB.put(cuentaIng, bancoDB.get(cuentaIng) + pairIngresar.getRight().monto);
				//System.out.println("Se desbloquea (ingresar): " + "[Cuenta:" + cuentaIng + ", Monto: " + pairIngresar.getRight().monto + "]");
				pairIngresar.getLeft().out().write(null);

				break;


			case TRANSFERIR:
				Pair<Any2OneChannel,petTransferir> pairTransferir = (Pair<Any2OneChannel,petTransferir>) chTransferir.in().read();
				//System.out.println("Se anade a la cola TRANSFERIR: " + "[CuentaO:" + pairTransferir.getRight().origen + ", CuentaD:" + pairTransferir.getRight().destino + ", Monto: " + pairTransferir.getRight().monto + "]");
				cola.enqueue(pairTransferir);
				turnos.enqueue(TRANSFERIR);
				ordenTransferencias.addLast(pairTransferir.getRight());
				break;

			case DISPONIBLE:
				Pair<Any2OneChannel,petTransferir> pairDisponible = (Pair<Any2OneChannel,petTransferir>) chDisponible.in().read();
				//System.out.println("Se anade a la cola DISPONIBLE: " + "[Cuenta:" + pairDisponible.getRight().origen + ", Monto: " + pairDisponible.getRight().monto + "]");

				String cuentaDisp = pairDisponible.getRight().origen;
				if(!bancoDB.containsKey(cuentaDisp))
					throw new IllegalArgumentException();
				int saldo = bancoDB.get(cuentaDisp);					
				pairDisponible.getLeft().out().write(saldo);
				System.out.println("Se desbloquea (disponibilidad): " + "[Cuenta:" + cuentaDisp + ", Monto: " + saldo + "]");
				break;

			case ALERTAR:
				Pair<Any2OneChannel,petTransferir> pairAlertar = (Pair<Any2OneChannel,petTransferir>) chAlertar.in().read();
				//System.out.println("Se anade a la cola ALERTAR: " + "[Cuenta:" + pairAlertar.getRight().origen + ", Monto: " + pairAlertar.getRight().monto + "]");
				cola.enqueue(pairAlertar);
				turnos.enqueue(ALERTAR);
				break;

			}


			boolean hayCambio = true;

			while(hayCambio) {
				hayCambio = false;


				for(int i = 0 ; i < cola.size() ; i++) {
					int TURNO = turnos.first();

					if(TURNO == TRANSFERIR) {
						Pair<Any2OneChannel,petTransferir> pair = cola.first();
						petTransferir transferencia = pair.getRight();


						Position<petTransferir> primero = null;
						boolean esNull = true;


						if(ordenTransferencias.size() > 0) {
							primero = primeraTransf(transferencia.origen, transferencia.destino, transferencia.monto, ordenTransferencias);
							esNull = primero == null;
						}

						if(ordenTransferencias.size() > 0 
								&& !esNull
								&& bancoDB.containsKey(transferencia.origen) 
								&& bancoDB.containsKey(transferencia.destino)
								&& (bancoDB.get(transferencia.origen) >= transferencia.monto)){
							bancoDB.put(transferencia.origen, bancoDB.get(transferencia.origen) - transferencia.monto);
							bancoDB.put(transferencia.destino, bancoDB.get(transferencia.destino) + transferencia.monto);
							//System.out.println("Se desbloquea (transferencia correcta): " + "[CuentaO:" + transferencia.origen + ", CuentaD:" + transferencia.destino + ", Monto: " + transferencia.monto + "]");
							pair.getLeft().out().write(null);
							ordenTransferencias.remove(primero);
							cola.dequeue();
							turnos.dequeue();
							esNull = true;
							hayCambio = true;
						}
						else {
							cola.enqueue(cola.first());
							cola.dequeue();
							turnos.enqueue(turnos.first());
							turnos.dequeue();
						}
					}


					if(TURNO == ALERTAR) {
						Pair<Any2OneChannel,petTransferir> pairAlert = cola.first();
						petTransferir alerta = pairAlert.getRight();

						
						if((bancoDB.get(alerta.origen) < alerta.monto)){
							//System.out.println("Se desbloquea (alertar) : "  + "[Cuenta:" + alerta.origen + ", Monto: " + alerta.monto + "]");
							pairAlert.getLeft().out().write(null);
							hayCambio = true;
							cola.dequeue();
							turnos.dequeue();
						}
						else {
							cola.enqueue(cola.first());
							cola.dequeue();
							turnos.enqueue(turnos.first());
							turnos.dequeue();
						}
					}
				}
			}
		}
	}


	private Position<petTransferir> primeraTransf(String o, String d, int v, PositionList<petTransferir> lista) {
		boolean res = false;
		Position<petTransferir> cursor;
		for(cursor = lista.first() ; cursor != null ; cursor = lista.next(cursor)) {
			petTransferir par =  cursor.element();
			if(o.equals(par.origen) && !res) {
				res = true;
				if(d.equals(par.destino) && v == par.monto) {
					return cursor;	
				}
			}	
		}
		return null;
	}
}