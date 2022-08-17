
public class Indeterminismo {
	
	public static final int N_ITER=10;
	
	public static class MiThread extends Thread{
		@Override
		public void run() {
			for(int i=0; i<N_ITER; i++) {
				System.out.println("THR1: Ejecuta el bucle "+i);
			}
			System.out.println("THR1: El thread ha terminado");
		}
	}
	public static void main(String[] args) {
		MiThread t= new MiThread();
		t.start();
		for(int i=0; i<N_ITER; i++) {
			System.out.println("MAIN: Ejecuta el bucle "+i);
		}
		System.out.println("MAIN: El main ha terminado");
	}
}
