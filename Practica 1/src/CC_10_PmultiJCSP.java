import es.upm.babel.cclib.MultiAlmacen;
import es.upm.babel.cclib.MultiProductor;
import es.upm.babel.cclib.MultiConsumidor;
import org.jcsp.lang.*;
/**
 * Programa concurrente para productor -buffer -consumidor con multialmacen
 * de capacidad N implementado con paso de mensajes (MultiAlmacenJCSP).
 */
class CC_10_PmultiCJCSP {
	public static final void main(final String[] args)
			throws InterruptedException {
		// Capacidad del multialmacen
		final int N = 10;
		// N�u mero de productores y consumidores
		final int N_PRODS = 2;
		final int N_CONSS = 2;
		// M�a xima cantidad de productos por paquete para producir y consumir
		final int MAX_PROD = N / 2;
		final int MAX_CONS = N / 2;
		// Almacen compartido
		MultiAlmacenJCSP almac = new MultiAlmacenJCSP(N);
		// OJO!!
		ProcessManager m_almac = new ProcessManager(almac);
		// Lanzamos el servidor del almac �en
		m_almac.start();
		// Declaraci �on de los arrays de productores y consumidores
		MultiProductor[] productores;
		MultiConsumidor[] consumidores;
		// Creaci �on de los arrays
		productores = new MultiProductor[N_PRODS];
		consumidores = new MultiConsumidor[N_CONSS];
		// Creaci �on de los productores
		for (int i = 0; i < N_PRODS; i++) {
			productores[i] = new MultiProductor(almac ,MAX_PROD);
		}
		// Creaci �on de los consumidores
		for (int i = 0; i < N_CONSS; i++) {
			consumidores[i] = new MultiConsumidor(almac ,MAX_CONS);
		}
		// Lanzamiento de los productores
		for (int i = 0; i < N_PRODS; i++) {
			productores[i].start();
		}
		// Lanzamiento de los consumidores
		for (int i = 0; i < N_CONSS; i++) {
			consumidores[i].start();
		}
		// Espera hasta la terminaci �on de los clientes
		try {
			for (int i = 0; i < N_PRODS; i++) {
				productores[i].join();
			}
			for (int i = 0; i < N_CONSS; i++) {
				consumidores[i].join();
			}
		} catch (Exception ex) {
			ex.printStackTrace ();
			System.exit (-1);
		}
	}
}