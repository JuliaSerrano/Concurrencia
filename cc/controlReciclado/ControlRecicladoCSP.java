
package cc.controlReciclado;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.jcsp.lang.*;


public class ControlRecicladoCSP implements ControlReciclado, CSProcess {

  // constantes varias
  private enum Estado { LISTO, SUSTITUIBLE, SUSTITUYENDO }

  private final int MAX_P_CONTENEDOR; 
  private final int MAX_P_GRUA;       
  
  // canales para comunicacion con el servidor y RPC
  // uno por operacion (peticiones aplazadas)
  private final Any2OneChannel chNotificarPeso;
  private final Any2OneChannel chIncrementarPeso;
  private final Any2OneChannel chNotificarSoltar;
  private final One2OneChannel chPrepararSustitucion;
  private final One2OneChannel chNotificarSustitucion;
  
  
  PetIncrementarPeso petIncrementarPeso;
  
  // para aplazar peticiones de incrementarPeso
  private static class PetIncrementarPeso {
    public int p;
    public One2OneChannel chACK;

    PetIncrementarPeso (int p) {
      this.p = p;
      this.chACK = Channel.one2one();
    }
  }

  public ControlRecicladoCSP(int max_p_contenedor,
                             int max_p_grua) {
    // constantes del sistema
    MAX_P_CONTENEDOR = max_p_contenedor;
    MAX_P_GRUA       = max_p_grua;
    
    // creacion de los canales 
    this.chNotificarPeso = Channel.any2one();
    this.chIncrementarPeso = Channel.any2one();
    this.chNotificarSoltar = Channel.any2one();
    this.chPrepararSustitucion = Channel.one2one();
    this.chNotificarSustitucion = Channel.one2one();
    this.petIncrementarPeso = new PetIncrementarPeso(0);
    
    // arranque del servidor 
    new ProcessManager(this).start();
  }



  //  PRE: 0 < p < MAX_P_GRUA
  // CPRE: self.estado =/= SUSTITUYENDO
  // notificarPeso(p)
  public void notificarPeso(int p) throws IllegalArgumentException {
    //PRE
    if(p <= 0 || p > MAX_P_GRUA)
        throw new IllegalArgumentException();
    
    // PRE OK, enviar peticion
    System.out.println("Grua pide peso "+p);
    // enviamos peticion
    chNotificarPeso.out().write(p);
    System.out.println("Se ha notificado el peso de la grua");
  }

  //  PRE: 0 < p < MAX_P_GRUA
  // CPRE: self.estado =/= SUSTITUYENDO /\
  //       self.peso + p <= MAX_P_CONTENEDOR
  // incrementarPeso(p)
  public void incrementarPeso(int p) throws IllegalArgumentException {
    // tratar PRE

    if(p <= 0 || p > MAX_P_GRUA)
        throw new IllegalArgumentException();
      
    // PRE OK, creamos peticion para el servidor
    PetIncrementarPeso peticionIncrementarPeso = new PetIncrementarPeso(p);
    
    // enviamos peticion
    System.out.println("Grua va a incrementarse de peso: "+p);
    chIncrementarPeso.out().write(peticionIncrementarPeso);

    // esperar confirmacion
    peticionIncrementarPeso.chACK.in().read();
    System.out.println("Se ha incrementado el peso de la grua: "+p);
  }

  //  PRE: --
  // CPRE: --
  // notificarSoltar()
  public void notificarSoltar() {
    // enviar peticion
    System.out.println("Grua pide soltar");
    chNotificarSoltar.out().write(null);
    System.out.println("Se ha soltado la grua");
  }

  //  PRE: --
  // CPRE: self = (_, sustituible, 0)
  // prepararSustitucion()
  public void prepararSustitucion() {
    // enviar peticion
      System.out.println("Grua prepara sustitucion");
      chPrepararSustitucion.out().write(null);
      System.out.println("Se ha sustituido la grua");
  }

  //  PRE: --
  // CPRE: --
  // notificarSustitucion()
  public void notificarSustitucion() {
    // enviar peticion
    System.out.println("Grua pide sustitucion");
    chNotificarSustitucion.out().write(null);
    System.out.println("Se ha notificado la sustitucion de la grua");
  }

  // SERVIDOR
  public void run() {
    System.out.println("--------------------------------------------------------------------------");
      
    // estado del recurso

    int peso = 0;
    Estado estado = Estado.LISTO;
    int accediendo = 0;
      
    // para recepcion alternativa condicional
    Guard[] entradas = {
      chNotificarPeso.in(),
      chIncrementarPeso.in(),
      chNotificarSoltar.in(),
      chPrepararSustitucion.in(),
      chNotificarSustitucion.in()
    };
    Alternative servicios =  new Alternative (entradas);

    final int NOTIFICAR_PESO = 0;
    final int INCREMENTAR_PESO = 1;
    final int NOTIFICAR_SOLTAR = 2;
    final int PREPARAR_SUSTITUCION = 3;
    final int NOTIFICAR_SUSTITUCION = 4;
    
    // condiciones de recepcion
    final boolean[] sincCond = new boolean[5];

    sincCond[NOTIFICAR_SOLTAR] = true; 
    sincCond[NOTIFICAR_SUSTITUCION] = true;

    // creamos coleccion para almacenar peticiones aplazadas

    Queue<PetIncrementarPeso> peticionesAplazadas = new LinkedList<PetIncrementarPeso>();

    // bucle de servicio
    while (true) {

        
      // actualizacion de condiciones de recepcion

      sincCond[NOTIFICAR_PESO] = estado != Estado.SUSTITUYENDO;
      sincCond[INCREMENTAR_PESO] = estado != Estado.SUSTITUYENDO;
      sincCond[PREPARAR_SUSTITUCION] = estado == Estado.SUSTITUIBLE && accediendo == 0;

      switch (servicios.fairSelect(sincCond)) {
      case NOTIFICAR_PESO:
        System.out.println("NOTIFICAR_PESO");
        
        // estado != Estado.SUSTITUYENDO
        // leer peticion
        int pesoNotificado = (Integer) chNotificarPeso.in().read();
        
        // procesar peticion
        if(peso + pesoNotificado > MAX_P_CONTENEDOR)
            estado = Estado.SUSTITUIBLE;
        else
            estado = Estado.LISTO;

        break;
      case INCREMENTAR_PESO:
        System.out.println("INCREMENTAR_PESO");
        
        // leer peticion 
        PetIncrementarPeso peticionIncrementarPeso = (PetIncrementarPeso) chIncrementarPeso.in().read();
        

        //Tratamos peticion, si no se cumple CPRE la aplazamos
        if (peso + peticionIncrementarPeso.p <= MAX_P_CONTENEDOR) {
            peso += peticionIncrementarPeso.p;
            accediendo++;
            peticionIncrementarPeso.chACK.out().write(null);
        } else {
        	peticionesAplazadas.add(peticionIncrementarPeso);
        }

        break;
      case NOTIFICAR_SOLTAR:
        System.out.println("NOTIFICAR_SOLTAR");
        
        // accediendo > 0 (por protocolo de llamada)
        // leer peticion
        chNotificarSoltar.in().read();


        // tratar peticion
        accediendo--;
        
        break;
      case PREPARAR_SUSTITUCION:
        System.out.println("PREPARAR_SUSTITUCION");
        
        // estado == Estado.SUSTITUIBLE && accediendo == 0
        // leer peticion
        chPrepararSustitucion.in().read();

        // tratar peticion
        estado = Estado.SUSTITUYENDO;
        
        break;
      case NOTIFICAR_SUSTITUCION:
        System.out.println("NOTIFICAR_SUSTITUCION");
        
        // estado == Estado.SUSTITUYENDO && accediendo == 0 
        // leer peticion
        chNotificarSustitucion.in().read();

        // tratar peticion
        peso = 0;
        estado = Estado.LISTO;
        accediendo = 0;
        
        break;
      } // switch
      
      System.out.println("peso=" + peso);
      System.out.println("estado=" + estado);
      System.out.println("accediendo=" + accediendo);

      // tratamiento de peticiones aplazadas

      // Nos guardamos el size de peticionesAplazadas, ya que este se modifica dentro del for
      int numeroDePeticionesAplazadas = peticionesAplazadas.size();
      for (int i = 0; i < numeroDePeticionesAplazadas; i++) {
          PetIncrementarPeso peticionIncrementarPeso = peticionesAplazadas.poll();
          // Si se cumple la CPRE
          if (peso + peticionIncrementarPeso.p < MAX_P_CONTENEDOR && estado != Estado.SUSTITUYENDO) {
        	  peso += peticionIncrementarPeso.p;
        	  accediendo++;
        	  System.out.println("Cumple CPRE peso=" + peso + " pesoAIncrementar=" + peticionIncrementarPeso.p);
              System.out.println("peso=" + peso);
              System.out.println("estado=" + estado);
              System.out.println("accediendo=" + accediendo);
              peticionIncrementarPeso.chACK.out().write(null);
          }
          else {
        	  System.out.println("No cumple CPRE peso=" + peso + " pesoAIncrementar=" + peticionIncrementarPeso.p);
              System.out.println("peso=" + peso);
              System.out.println("estado=" + estado);
              System.out.println("accediendo=" + accediendo);
              peticionesAplazadas.add(peticionIncrementarPeso);
          }
      }
      
      //no quedan peticiones aplazadas que podrian ser atendidas
        
    } // bucle servicio
  } // run() SERVER
} // class ControlRecicladoCSP