import es.upm.babel.cclib.Producto;
import es.upm.babel.cclib.Semaphore;
import es.upm.babel.cclib.Almacen;


/**
 * Implementación de la clase Almacen que permite el almacenamiento
 * de producto y el uso simultáneo del almacen por varios threads.
 */
class Almacen1 implements Almacen {

	private Producto almacenado = null;
	static volatile Semaphore productores = new Semaphore(1);
	static volatile Semaphore consumidores = new Semaphore(1);

	public Almacen1() {
	}

	public void almacenar(Producto producto) {
		productores.await();
		almacenado = producto;
		consumidores.signal();
	}

	public Producto extraer() {
		Producto result;
		consumidores.await();
		result = almacenado;
		almacenado = null;
		productores.signal();	
		return result;
	}
}
