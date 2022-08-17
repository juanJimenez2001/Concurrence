import es.upm.babel.cclib.Producto;
import es.upm.babel.cclib.MultiAlmacen;
import es.upm.babel.cclib.Monitor;

// importar la librería de monitores


class MultiAlmacenMon implements MultiAlmacen {
	private int capacidad = 0;
	private Producto almacenado[] = null;
	private int aExtraer = 0;
	private int aInsertar = 0;
	private int nDatos = 0;

	boolean HayDato = false;
	Monitor mutex;
	Monitor.Cond almacenar;
	Monitor.Cond extraer;

	// Para evitar la construcción de almacenes sin inicializar la
	// capacidad 
	private MultiAlmacenMon() {
		
		mutex = new Monitor();
		almacenar = mutex.newCond();
		extraer = mutex.newCond();
		
	}

	public MultiAlmacenMon(int n) {
		
		almacenado = new Producto[n];
		aExtraer = 0;
		aInsertar = 0;
		capacidad = n;
		nDatos = 0;

		mutex = new Monitor();
		almacenar = mutex.newCond();
		extraer = mutex.newCond();
		
	}

	private int nDatos() {
		
		return nDatos;
		
	}

	private int nHuecos() {
		
		return capacidad - nDatos;
		
	}

	public void almacenar(Producto[] productos) {

		mutex.enter();
		if(HayDato) {
			almacenar.await();
			// Sección crítica
			for (int i = 0; i < productos.length; i++) {
				almacenado[aInsertar] = productos[i];
				nDatos++;
				aInsertar++;
				aInsertar %= capacidad;
			}
		}
		HayDato = true;
		extraer.signal();  
		mutex.leave();
		
	}

	public Producto[] extraer(int n) {
		
		Producto[] result = new Producto[n];
		mutex.enter();
		if(!HayDato) {
			extraer.await();
			// Sección crítica
			for (int i = 0; i < result.length; i++) {
				result[i] = almacenado[aExtraer];
				almacenado[aExtraer] = null;
				nDatos--;
				aExtraer++;
				aExtraer %= capacidad;
			}
		}
		HayDato = false;
		almacenar.signal();  
		mutex.leave();
		return result;
		
	}
}