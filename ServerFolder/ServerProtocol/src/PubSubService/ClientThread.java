package PubSubService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Vector;

public class ClientThread extends Thread {
  //el socket del client
  Socket clientConnection;
  public ClientThread(Socket clientConnection){
          super();
          this.clientConnection = clientConnection;
  }

  @Override
  public void run(){
    //guarde el socket del cliente como una cadena para mayor conveniencia.
    String clientIP = clientConnection.getRemoteSocketAddress().toString();
    //notificar al servidor y obviamente el archivo de registro.
    System.out.println("Conexión recibida de: " + clientIP);			
    Hashy.writeLog(clientIP + ": Received connection from: " + clientIP);
    BufferedReader brIn = null;
    PrintWriter pwOut = null;
    try {
      //inicializa el stream.
      brIn = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
      pwOut = new PrintWriter(clientConnection.getOutputStream());
      //BUCLE.
      while (true){
        String recievedString = brIn.readLine().trim();
        if ( recievedString.length() == 0){
          Hashy.writeLog("Protocol failure, received empty stream.");
          throw new Exception("Recibido stream vacío.");					
        }
        //divide en espacio separado.
        String[] stringParts = recievedString.split(" ");
        String sentence = "";
        //compruebe si el número de palabras de protocolo es mayor que 2, entonces es un mensaje de "send" con una oración..
        if (stringParts.length > 2){
          sentence = String.join(" ", Arrays.copyOfRange(stringParts,2,stringParts.length));
        }
        if (stringParts.length >= 2){
          stringParts[1] = stringParts[1].toLowerCase();
        }
        switch(stringParts[0]){
          case "REGISTER":
            //write to log
            Hashy.writeLog(clientIP + ": " + recievedString);
            //trata de añadir un topic
            if (Hashy.addToTopic(stringParts[1], pwOut)){
              pwOut.println("OK");
              pwOut.flush();
              Hashy.writeLog(clientIP + ": OK");
            }
            else{//ya registrado
              pwOut.println("ERROR");
              pwOut.flush();
              Hashy.writeLog(clientIP + ": ERROR");
            }
            break;
          case "LEAVE": 
            //write log
            Hashy.writeLog(clientIP + ": " + recievedString);
            if (Hashy.removeFromTopic(stringParts[1], pwOut)){
              pwOut.println("OK");
              pwOut.flush();
              Hashy.writeLog(clientIP + ": OK");
            }
            else{
              //user not registered to the topic
              pwOut.println("ERROR");
              pwOut.flush();
              Hashy.writeLog(clientIP + ": ERROR");
            }
            break;
          case "SEND":
            //write log
            Hashy.writeLog(clientIP + ": " + recievedString);
            pwOut.println("OK");
            pwOut.flush();	
            Hashy.writeLog(clientIP + ": OK");
            Vector<PrintWriter> pwForward = Hashy.getVector(stringParts[1]);
            if(pwForward == null){
              continue;
            }
            //iterar sobre todos los clientes registrados al tema y enviar un protocolo de reenvío de mensajes.
            for (PrintWriter pw : pwForward) {
              if(pw != pwOut){
                pw.println("FORWARD " + stringParts[1] + " " + clientIP + " " + sentence);
                pw.flush();
                Hashy.writeLog(clientIP + ": " + "FORWARD " + stringParts[1] + " " + clientIP + " " + sentence);
              }
            }
            break;
          case "CLOSE":
            clientConnection.close();
            Hashy.removeSocket(clientConnection);
            System.out.println("Closed connection with: "+ clientIP);
            Hashy.removeFromAllTopics(pwOut);
            Hashy.writeLog(clientIP + ": CLOSE");
            return;
          default:
            pwOut.println("ERROR");
            pwOut.flush();
            Hashy.writeLog(clientIP + ": ERROR");
        }				
      }
    }
    catch (SocketException se){
      System.out.println("La conexión con el cliente "+ clientIP + " ha terminado.");
      Hashy.writeLog(clientIP + ": The connection with client "+ clientIP + " has been terminated.");
    }
    catch (NullPointerException npe){
      System.out.println("Error: cliente cerró este socket.");
    }
    catch(Exception e) {
      System.out.println("Error: "+e.getMessage());
    }
    finally{
      Hashy.removeFromAllTopics(pwOut);
      Hashy.removeSocket(clientConnection);
    }
  }
}