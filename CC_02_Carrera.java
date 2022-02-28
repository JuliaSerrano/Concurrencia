public class CC_02_Carrera {


	public static volatile int x = 0;
	public static final int N = 1000000;


	public static class Inc extends Thread{
		public void run() {
			x++;
		}
	}
	public static class Dec extends Thread{
		public void run() {
			x--;
		}
	}



	public static void main(String args[]) throws Exception{

		for (int i = 0; i < N; i++) {
			System.out.println(x);
			Inc in = new Inc();
			Dec de = new Dec();
			de.start();	
			in.start();
		}
		
		System.out.println(x);

	}
}