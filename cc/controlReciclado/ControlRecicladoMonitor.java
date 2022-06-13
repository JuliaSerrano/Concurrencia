package cc.controlReciclado;

import es.upm.babel.cclib.Monitor;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Iterator;
public final class ControlRecicladoMonitor implements ControlReciclado {
  private enum Estado { LISTO, SUSTITUIBLE, SUSTITUYENDO }

  
  private Monitor mutex = new Monitor();
  
  
  //DOMINIO
  private int peso = 0;
  private Estado estado = Estado.LISTO;
  private int accediendo = 0;                 //numero de gruas accediendo 
  
  private final int MAX_P_CONTENEDOR;
  private final int MAX_P_GRUA;
  
  //cola peticiones aplazadas
  Queue<IncrementarPeso> listaPeticiones = new LinkedList<IncrementarPeso>();
  
  //SEMANTICA
  
  
  //bloquear notificarPeso hasta que CPRE se cumpla 
  private Monitor.Cond condAct = mutex.newCond(); 
  

  
  //bloquear prepararSustitucion hasta que CPRE se cumpla
  private Monitor.Cond condSust = mutex.newCond(); 
  
  
  // para aplazar peticiones de incrementarPeso
  private static class IncrementarPeso {
	public Monitor.Cond condPeso; 
    public int p;
    

    IncrementarPeso (Monitor m,int p) {
    	
    	this.p = p;
    	this.condPeso = m.newCond();
    	
      
    }

  }


  //constructor
  public ControlRecicladoMonitor (int max_p_contenedor,
                                  int max_p_grua) {
    MAX_P_CONTENEDOR = max_p_contenedor;
    MAX_P_GRUA = max_p_grua;
    this.peso = 0;
    this.estado = Estado.LISTO;
    this.accediendo = 0; 
    
  }

  
  
  public void notificarPeso(int p) throws IllegalArgumentException{
	  
	  //PRE: p > 0 ∧ p ≤ MAX_P_GRUA
	  if(p <= 0 ||  p > MAX_P_GRUA) throw new IllegalArgumentException();
	  

	  mutex.enter();
	  

	  //CPRE:  self.estado != sustituyendo
	  if(this.estado.compareTo(Estado.SUSTITUYENDO) == 0) {
		  condAct.await();
		  
		  
	  }
	  
	  

	  
	  // POST: self.peso = self pre .peso ∧ self.accediendo = self pre .accediendo
	  if((this.peso + p) > MAX_P_CONTENEDOR) {
		  this.estado = Estado.SUSTITUIBLE;
	  }
	  if((this.peso + p) <= MAX_P_CONTENEDOR) {
		  this.estado = Estado.LISTO;
	  }
	  
	  
	  realizarDesbloqueos();
	  mutex.leave();
	  
	  
	    
  }

  public void incrementarPeso(int p) throws IllegalArgumentException{
	
	  //PRE: p > 0 ∧ p ≤ MAX_P_GRUA
	if(p <= 0 || p > MAX_P_GRUA) throw new IllegalArgumentException();
	
	mutex.enter();
	
	
	//CPRE: self.peso + p ≤ MAX_P_CONTENEDOR ∧ self.estado , sustituyendo
	if(((this.peso + p > MAX_P_CONTENEDOR) || (this.estado.compareTo(Estado.SUSTITUYENDO)==0))) {
		IncrementarPeso pet = new IncrementarPeso(mutex,p);
		listaPeticiones.add(pet);
		pet.condPeso.await();
		
	}
	
	
	

	  
	  
	
	//POST: self = (peso + p, e, a + 1)
	this.peso = peso + p;
	this.accediendo++;
	
	
	realizarDesbloqueos();
	mutex.leave();
	  
  }

  public void notificarSoltar() {
	
	//no hay PRE ni CPRE 
	mutex.enter();

	//POST: self = (p, e, a − 1)
	this.accediendo--;
	  
	realizarDesbloqueos();
	mutex.leave();
	  
  }

  public void prepararSustitucion() {
	  
	 //no hay PRE
	 mutex.enter();
	 
	 //CPRE: self = (_, sustituible , 0)
	 if(((this.estado.compareTo(Estado.SUSTITUIBLE)!=0) || (this.accediendo!=0))) {
		 
		 condSust.await();
	 }
	 //POST: self = (_, sustituyendo, 0)
	 this.estado = Estado.SUSTITUYENDO;
	 this.accediendo = 0;
	 
	 realizarDesbloqueos();
	 mutex.leave();
  }

  public void notificarSustitucion() {
	 mutex.enter();
	 
	 this.peso = 0;
	 this.estado = Estado.LISTO;
	 this.accediendo = 0;
	 
	 
	 realizarDesbloqueos();
	 mutex.leave();
	  
  }
  
  
  private void realizarDesbloqueos() {
	  boolean signaled = false;
	  
	  //notificar peso
	  if(!signaled && (condAct.waiting()>0) && (this.estado.compareTo(Estado.SUSTITUYENDO)!=0) ) {
	  		
	  		condAct.signal();
	  		signaled = true;
	  	}
	  	
	  	//sustitucion
	  if (!signaled && (condSust.waiting()>0) && (this.estado.compareTo(Estado.SUSTITUIBLE)==0 && this.accediendo==0) ) {
	  		
	  		condSust.signal();
	  		signaled = true;
	  		
	  	}
	  	//incrementar Peso
		  for(int i = 0; i < listaPeticiones.size() && !signaled; i++) {
			  IncrementarPeso peticion = listaPeticiones.peek();
			  if(this.peso + peticion.p <= MAX_P_CONTENEDOR && this.estado.compareTo(Estado.SUSTITUYENDO)!=0) {
				  peticion.condPeso.signal();
				  listaPeticiones.remove();
				  signaled = true;
			  }
			  else {
				  listaPeticiones.remove();
				  listaPeticiones.add(peticion);
			}
		  
	  }

  }	


	  
}
