
public class CC_01_Threads implements Runnable {

	int N;
	int T;	
	int threadNumber;
	
	@Override
	public void run() {
		
		
		try { 
			System.out.println( "Thread:" + threadNumber );

			Thread.sleep(T); 
		} 
		catch (InterruptedException e) { 
			System.out.println(e); 
		} 








	}
	//constructor N threads, T milisegundos
	public CC_01_Threads(int N, int T, int threadNumber) {
		
		this.N = N;
		this.T= T;
		this.threadNumber = threadNumber;
	}




	public static void main(String args[]) {
		
		int N = 6;
		int T = 10;
		Thread[] threads = new Thread[N];
		

		for (int i = 0; i < N; i++) {
			threads[i] = new Thread(new CC_01_Threads(N,T,i));
			threads[i].start();
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				
				System.out.println(e);
			}
		}
		System.out.println("Terminado");
	



	



}
}