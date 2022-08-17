
public class CC_02_Threads extends Thread{

	public static int n=0;
	public static int Num=20;
	public static int M=50;

	public static class Inc  extends Thread{
		@Override
		public void run() {
			for(int i=0; i<Num; i++) {
				n++;
			}
		}
	}

	public static class Dec  extends Thread{
		@Override
		public void run() {
			for(int i=0; i<Num; i++) {
				n--;
			}
		}
	}


	public static void main(String[] args) {
		Inc  th1 []= new Inc[M];
		Dec  th2 []= new Dec[M];
		for(int i=0; i<M; i++) {
			th1[i]= new Inc();
			th2[i]= new Dec();
		}
		for(int i=0; i<M; i++) {
			th1[i].start();
			th2[i].start();		
		}
		for(int i=0; i<M; i++) {
			try {
				th1[i].join();
				th2[i].join();
			}
			catch(InterruptedException e) {
			}
		}
		System.out.println("El valor de n es: "+ n);
	}
}
