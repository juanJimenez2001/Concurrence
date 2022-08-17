

public class EjemploJoin extends Thread{
	@Override
	public void run() {
		long time= System.currentTimeMillis();
		try {
			Thread.sleep(1000);
		}
		catch(InterruptedException e) {
		}
		time=System.currentTimeMillis()-time;
		System.out.println("Terminado el THREAD correctamente en "+time +"ms");
	}
	public static void main(String[] args) {
		EjemploJoin t= new EjemploJoin();
		t.start();
		long time= System.currentTimeMillis();
		try {
			Thread.sleep(3000);
			t.join();
		}
		catch(InterruptedException e) {
		}
		time=System.currentTimeMillis()-time;
		System.out.println("Terminado el MAIN correctamente en "+time +"ms");
	}
}
