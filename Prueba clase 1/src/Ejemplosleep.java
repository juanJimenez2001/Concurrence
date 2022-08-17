
public class Ejemplosleep {
	public static class MiThread extends Thread{
		@Override
		public void run() {
			long time= System.currentTimeMillis();
			try {
				Thread.sleep(6000);
			}
			catch(InterruptedException e) {
			}
			time=System.currentTimeMillis()-time;
			System.out.println("Terminado el THREAD correctamente en "+time +"ms");
			
			/*MiThread t= new MiThread();
			t.start();*/
		}
	}
	public static void main(String[] args) {
		MiThread t= new MiThread();
		t.start();
		long time= System.currentTimeMillis();
		try {
			Thread.sleep(3000);
		}
		catch(InterruptedException e) {
		}
		time=System.currentTimeMillis()-time;
		System.out.println("Terminado el MAIN correctamente en "+time +"ms");
	}
}
