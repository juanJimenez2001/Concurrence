import es.upm.babel.cclib.Producto;
import es.upm.babel.cclib.Semaphore;
import es.upm.babel.cclib.Almacen;

// TODO: importar la clase de los semáforos.

/**
 * Implementación de la clase Almacen que permite el almacenamiento
 * FIFO de hasta un determinado número de productos y el uso
 * simultáneo del almacén por varios threads.
 */
class AlmacenN implements Almacen {
	private int capacidad = 0;
	private Producto[] almacenado = null;
	private int nDatos = 0;
	private int aExtraer = 0;
	private int aInsertar = 0;
	static volatile Semaphore productores;
	static volatile Semaphore consumidores;
	static volatile Semaphore mutex;
	

	public AlmacenN(int n) {
		capacidad = n;
		almacenado = new Producto[capacidad];
		nDatos = 0;
		aExtraer = 0;
		aInsertar = 0;
		productores = new Semaphore(n);
		consumidores = new Semaphore(1);
		mutex = new Semaphore(1);
	}

	public void almacenar(Producto producto) {
		productores.await();
		
		// Sección crítica
		mutex.await();
		almacenado[aInsertar] = producto;
		nDatos++;
		aInsertar++;
		aInsertar %= capacidad;
		mutex.signal();
		
		consumidores.signal();
	}

	public Producto extraer() {
		Producto result;
		consumidores.await();
		
		// Sección crítica
		mutex.await();
		result = almacenado[aExtraer];
		almacenado[aExtraer] = null;
		nDatos--;
		aExtraer++;
		aExtraer %= capacidad;
		mutex.signal();
		
		productores.signal();
		return result;
	}
}
