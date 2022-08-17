// BancoCSP_skel.java
// Lars feat. Julio -- 2021
// esqueleto de codigo para JCSP
// (peticiones aplazadas)

package cc.banco;

import java.util.HashMap;
import java.util.Map;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.ProcessManager;

import es.upm.aedlib.Position;
import es.upm.aedlib.fifo.FIFOList;
import es.upm.aedlib.indexedlist.ArrayIndexedList;
import es.upm.aedlib.positionlist.NodePositionList;
import es.upm.aedlib.positionlist.PositionList;

// otras librerias: estructuras de datos, etc.
// COMPLETAD

public class codigoIwi implements Banco, CSProcess {

	// canales: uno por operacion
	// seran peticiones aplazadas
	private Any2OneChannel chIngresar;
	private Any2OneChannel chDisponible;
	private Any2OneChannel chTransferir;
	private Any2OneChannel chAlertar;

	// clases para peticiones
	// regalamos una como ejemplo
	public class TransferirReq {
		// atributos (pueden ser publicos)
		String from;
		String to;
		int value;
		One2OneChannel resp;

		// constructor
		public TransferirReq(String from, String to, int value) {
			this.from = from;
			this.to = to;
			this.value = value;
			this.resp = Channel.one2one();
		}
	}

	public class AlertarReq {
		// atributos (pueden ser publicos)

		String numCuentaAlertar;
		int valueAlertar;
		One2OneChannel respAlertar;

		// constructor
		public AlertarReq(String numCuentaAlertar, int valueAlertar) {
			this.numCuentaAlertar = numCuentaAlertar;
			this.valueAlertar = valueAlertar;
			this.respAlertar = Channel.one2one();
		}
	}

	public class IngresarReq {
		// atributos (pueden ser publicos)
		String numCuentaIngresar;
		int valueIngresar;

		// constructor
		public IngresarReq(String numCuentaIngresar, int valueIngresar) {
			this.numCuentaIngresar = numCuentaIngresar;
			this.valueIngresar = valueIngresar;
		}

	}

	public class DisponibleReq {
		// atributos (pueden ser publicos)
		String numCuentaDisponible;
		One2OneChannel respDisponible;

		// constructor
		public DisponibleReq(String numCuentaDisponible) {
			this.numCuentaDisponible = numCuentaDisponible;
			this.respDisponible = Channel.one2one();

		}
	}

	// constructor de BancoCSP
	public codigoIwi() {
		this.chIngresar = Channel.any2one();
		this.chAlertar = Channel.any2one();
		this.chDisponible = Channel.any2one();
		this.chTransferir = Channel.any2one();
		new ProcessManager(this).start();
	}

	// interfaz Banco
	/**
	 * Un cajero pide que se ingrese una determinado valor v a una cuenta c. Si la
	 * cuenta no existe, se crea.
	 * 
	 * @param c numero de cuenta
	 * @param v valor a ingresar
	 */
	public void ingresar(String c, int v) {
		IngresarReq p = new IngresarReq(c, v);
		chIngresar.out().write(p);

	}

	/**
	 * Un ordenante pide que se transfiera un determinado valor v desde una cuenta o
	 * a otra cuenta d.
	 * 
	 * @param o numero de cuenta origen
	 * @param d numero de cuenta destino
	 * @param v valor a transferir
	 * 
	 * @throws IllegalArgumentException si o y d son las mismas cuentas
	 *
	 */
	public void transferir(String o, String d, int v) {
		if (o.equals(d))
			throw new IllegalArgumentException();
		TransferirReq p = new TransferirReq(o, d, v);
		chTransferir.out().write(p);
		p.resp.in().read();
	}

	/**
	 * Un consultor pide el saldo disponible de una cuenta c.
	 * 
	 * @param c numero de la cuenta
	 * @return saldo disponible en la cuenta id
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public int disponible(String c) {
		DisponibleReq p = new DisponibleReq(c);
		chDisponible.out().write(p);
		int sol = (int) p.respDisponible.in().read();
		if (sol == -1)
			throw new IllegalArgumentException();
		return sol;
	}

	/**
	 * Un avisador establece una alerta para la cuenta c. La operacion termina
	 * cuando el saldo de la cuenta c baja por debajo de m.
	 * 
	 * @param c numero de la cuenta
	 * @param m saldo minimo
	 * @throws IllegalArgumentException si la cuenta c no existe
	 */
	public void alertar(String c, int v) {
		AlertarReq p = new AlertarReq(c, v);
		chAlertar.out().write(p);
		int sol = (Integer) p.respAlertar.in().read();
		if (sol == -1)
			throw new IllegalArgumentException();
	}

	// Codigo del servidor
	public void run() {
		// nombres simbolicos para las entradas
		final int INGRESAR = 0;
		final int DISPONIBLE = 1;
		final int TRANSFERIR = 2;
		final int ALERTAR = 3;

		// construimos la estructura para recepcion alternativa
		final Guard[] guards = new AltingChannelInput[4];
		guards[INGRESAR] = chIngresar.in();
		guards[DISPONIBLE] = chDisponible.in();
		guards[TRANSFERIR] = chTransferir.in();
		guards[ALERTAR] = chAlertar.in();
		Alternative servicios = new Alternative(guards);

		// El estado del recurso debe ir en el servidor (aqui)
		// Reutilizar de la practica de monitores
		// COMPLETAD
		String cuenta;
		String c1;
		String c2;
		int cantidad;
		int n = 0;
		final Map<String, Integer> cuentas = new HashMap<String, Integer>();
		// colecciones para peticiones aplazadas
		// reutilizar de monitores si es posible
		// COMPLETAD
		IngresarReq petIng;
		DisponibleReq petDisp;
		TransferirReq petTrans;
		AlertarReq petAler;
		One2OneChannel chResp;
		FIFOList<TransferirReq> bloqueoTrans = new FIFOList<TransferirReq>();
		FIFOList<AlertarReq> bloqueoAler = new FIFOList<AlertarReq>();

		// Bucle principal del servicio
		while (true) {
			int servicio = servicios.fairSelect();
			switch (servicio) {
			case INGRESAR: {
				petIng = (IngresarReq) chIngresar.in().read();
				if (!cuentas.containsKey(petIng.numCuentaIngresar))
					cuentas.put(petIng.numCuentaIngresar, petIng.valueIngresar);
				else
					cuentas.put(petIng.numCuentaIngresar, cuentas.get(petIng.numCuentaIngresar) + petIng.valueIngresar);

				break;
			}
			case DISPONIBLE: {
				petDisp = (DisponibleReq) chDisponible.in().read();
				if (cuentas.containsKey(petDisp.numCuentaDisponible)) {
					cantidad = cuentas.get(petDisp.numCuentaDisponible);
					petDisp.respDisponible.out().write(cantidad);
				} else
					petDisp.respDisponible.out().write(-1);

				break;
			}
			case TRANSFERIR: {
				petTrans = (TransferirReq) chTransferir.in().read();
				bloqueoTrans.enqueue(petTrans);

				break;
			}
			case ALERTAR: {
				petAler = (AlertarReq) chAlertar.in().read();
				if (cuentas.containsKey(petAler.numCuentaAlertar))
					bloqueoAler.enqueue(petAler);
				else
					petAler.respAlertar.out().write(-1);

				break;
			}
			}// END SWITCH


			boolean exito = true;
			do {
				exito = true;
				n = bloqueoTrans.size();
				int k = 0;
				ArrayIndexedList<String> transfPendientes = new ArrayIndexedList<String>();
				while (k < n) {
					TransferirReq pet = bloqueoTrans.dequeue();
					if (exito) {
						c1 = pet.from;
						c2 = pet.to;
						cantidad = pet.value;
						chResp = pet.resp;

						if (transfPendientes.indexOf(pet.from) == -1 && transferirAux(c1, c2, cantidad, cuentas)) {
							chResp.out().write(0);
							exito = false;

						} else {
							bloqueoTrans.enqueue(pet);
							transfPendientes.add(transfPendientes.size(), pet.from);
						}
					} else
						bloqueoTrans.enqueue(pet);
					k++;
				}
			} while (!exito);

			n = bloqueoAler.size();
			for (int i = 0; i < n; i++) {
				AlertarReq pet = bloqueoAler.dequeue();
				cuenta = pet.numCuentaAlertar;
				cantidad = pet.valueAlertar;
				chResp = pet.respAlertar;

				if (cuentas.containsKey(cuenta) && cuentas.get(cuenta) < cantidad)
					chResp.out().write(0);
				else
					bloqueoAler.enqueue(pet);
			}

			// no debemos volver al inicio del bucle
			// de servicio mientras haya alguna
			// peticion pendiente que se pueda atender !!
		} // END BUCLE SERVICIO
	}// END run SERVIDOR

	// cualquier otro codigo auxiliar que necesiteis...
	private boolean transferirAux(String c1, String c2, int cantidad, Map<String, Integer> cuentas) {
		boolean exito = true;
		if (cuentas.containsKey(c1) && cuentas.containsKey(c2) && cuentas.get(c1) >= cantidad) {
			cuentas.put(c1, cuentas.get(c1) - cantidad);
			cuentas.put(c2, cuentas.get(c2) + cantidad);
		} else
			exito = false;
		return exito;
	}
}
