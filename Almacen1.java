import es.upm.babel.cclib.Producto;
import es.upm.babel.cclib.Almacen;

// TODO: importar la clase de los semáforos.
import es.upm.babel.cclib.Semaphore;

/**
 * Implementación de la clase Almacen que permite el almacenamiento
 * de producto y el uso simultáneo del almacen por varios threads.
 */
class Almacen1 implements Almacen {
   // Producto a almacenar: null representa que no hay producto
   private Producto almacenado = null;

   // TODO: declaración e inicialización de los semáforos
   // necesarios
   private volatile Semaphore pSem = new Semaphore(1);
   private volatile Semaphore cSem = new Semaphore(0);

   public Almacen1() {
   }

   public void almacenar(Producto producto) {
      // TODO: protocolo de acceso a la sección crítica y código de
      // sincronización para poder almacenar.
	   pSem.await();

      // Sección crítica
      almacenado = producto;

      // TODO: protocolo de salida de la sección crítica y código de
      // sincronización para poder extraer.
      cSem.signal();
   }

   public Producto extraer() {
      Producto result;

      // TODO: protocolo de acceso a la sección crítica y código de
      // sincronización para poder extraer.
      cSem.await();

      // Sección crítica
      result = almacenado;
      almacenado = null;

      // TODO: protocolo de salida de la sección crítica y código de
      // sincronización para poder almacenar.
      pSem.signal();

      return result;
   }
}
