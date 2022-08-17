
public class CC_01_Threads {
	
	public static class ThreadHijo  extends Thread{
		private int id;
		public ThreadHijo (int id) {
			this.id=id;
		}
		@Override
		public void run() {
			System.out.println(id+": Comienza el thread");
			try {
				Thread.sleep(10000);
			}
			catch(InterruptedException e) {
			}
			System.out.println(id+": Termina el thread");
		}
	}
	
	public static void main(String[] args) {
		int n=10;
		Thread  th []= new ThreadHijo[n];
		for(int i=0; i<n; i++) {
			th[i]= new ThreadHijo (i);
		}
		for(int i=0; i<n; i++) {
			th[i].start();
		}
		for(int i=0; i<n; i++) {
			try {
				th[i].join();
			}
			catch(InterruptedException e) {
			}
		}
		System.out.println("Todos los threads han terminado");
	}
}
