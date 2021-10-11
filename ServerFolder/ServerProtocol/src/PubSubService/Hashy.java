package PubSubService;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

public final class Hashy{

  //una tabla para topics + print writers.
  private static Hashtable<String, Vector<PrintWriter>> htTopics = new Hashtable<>();
  //vector of all the online sockets.
  private static Vector<Socket> vSockets = new Vector<>();
  private static FileWriter fw = null;
  private static BufferedWriter bw = null;
  // path files
  private static final String LOG_PATH = "Log.txt";

  /* añade un cliente a un topic que registró. */
  public static synchronized boolean addToTopic(String topicName, PrintWriter pt){
    Vector<PrintWriter> temp = null;
    //obtiene el vector de topic
    temp = htTopics.get(topicName);
    if(temp == null){  				//si no hay tal vector, hace uno
      temp = new Vector<>();
    }
    else if(temp.contains(pt)){    //Si el cliente ya está registrado, devuelva false
      return false;
    }
    //agregue el cliente al vector y devuelva el vector al hash - return true.
    temp.add(pt); 
    htTopics.put(topicName, temp);
    return true;
  }

  /**
   * remueve el cliente que se fue del topic. */
  public static synchronized boolean removeFromTopic(String topicName, PrintWriter pt){
    Vector<PrintWriter> temp = null;
    if(!htTopics.containsKey(topicName)){	
      return false;	
    }
    //obtener el vector del mismo tema de hash
    temp = htTopics.get(topicName);
    if(!temp.contains(pt)){				
      return false;
    }
    //Eliminar el escritor cliente del vector
    temp.remove(pt);
    if(!temp.isEmpty()){
      htTopics.put(topicName, temp);
    }
    else{
      htTopics.remove(topicName);
    }
    return true;
  }

  /* Eliminando a algún client que se fue de todos los topics por los que se unió .*/
  public static void removeFromAllTopics(PrintWriter pt){
    //sconjunto de cadenas para contener todos los topics
    Set<String> df = null;
    //obtenga todos los temas de la tabla hash.
    synchronized(Hashy.class){
      df =  htTopics.keySet();
    }
    Object[] topics = df.toArray();
    int length = topics.length;
    for(int i = 0; i < length ; i++){
      removeFromTopic(topics[i].toString(),pt); //ejecutar en cada topic y eliminar el client de él con la función anterior.
    }
  }

  /* obtiene el vector completo de un topic específico - para reenviar el mensaje a todos los clientes registrados. */
  public static synchronized Vector<PrintWriter> getVector(String topic){
    return htTopics.get(topic);
  }

  /* agregue un nuevo socket de cliente a la lista de sockets - para cerrar todos los sockets a pedido. */
  public static boolean addSocket(Socket s) {
    return vSockets.add(s);
  }

  /* elimine un socket de la lista si el cliente se ha ido. */
  public static synchronized boolean removeSocket(Socket s) {
    return vSockets.remove(s);
  }

  /* eliminar todos los sockets porque el servidor se está cerrando */
  public static synchronized void removeAllSockets() {
    //iterar sobre cada socket y cerrarlo
    for (Socket socket : vSockets) {
      try {
        socket.close();
      } catch (Exception e) {}
    }
    try {
      //iterar sobre cada socket y eliminarlo de la lista.
      for(int i = 0; i < vSockets.size(); i++) {
        vSockets.remove(0);
      }
    }catch (Exception e) {}
  }

  /**
* Inicializando el archivo de log en el que vamos a escribir. */
  public static void initiateLogFile() {
    try {
      fw = new FileWriter(LOG_PATH,true);
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
    bw = new BufferedWriter(fw);
  }

  /* escribe el log en el archivo */
  public static synchronized void writeLog(String s) {
    try {
      //cadena específica solicitada para el archivo de registro.
      bw.write(LocalDate.now() + " " + LocalTime.now() + ": " + s + '\n');
      bw.flush();
    } catch (IOException e) {
        System.out.println("Error: " + e.getMessage());
    }
  }

  /** Limpiar toda la tabla hash, porque el servidor se desconectó y no debería recordar a los clientes antiguos. */
  public static void cleanTopics() {
    htTopics.clear();
  }
}