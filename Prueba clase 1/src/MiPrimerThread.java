
public class MiPrimerThread extends Thread{
	
	@Override
	public void run() {
		int iter= (int) (Math.random()*100)*10000000;
		for(int i=0; i<iter; i++) {
			for(int j=0; j<iter; j++) {
				for(int k=0; k<iter; k++);
			}
		}
		System.out.println("El thread ha terminado");
	}
	
	public static void main(String []args) {
		MiPrimerThread t= new MiPrimerThread();
		t.start();
		int iter= (int) (Math.random()*100)*10000000;
		for(int i=0; i<iter; i++) {
			for(int j=0; j<iter; j++) {
				for(int k=0; k<iter; k++);
			}
		}
		System.out.println("El main ha terminado");
	}
	
}
