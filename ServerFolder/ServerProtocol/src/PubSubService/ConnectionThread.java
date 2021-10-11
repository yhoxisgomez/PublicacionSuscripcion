package PubSubService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionThread extends Thread {
  ServerSocket listeningSocket;
  public ConnectionThread(ServerSocket listeningSocket){
    super();
    this.listeningSocket = listeningSocket;
  }
  @Override
  public void run(){
    try{
      while(true){
        Socket clientConnection = listeningSocket.accept();

        // observa si fue interrumpido, si es así, para
        if (this.isInterrupted()){
          System.err.println("se detuvo.");
          return;
        }
        //añade el socket a la lista de sockets
        Hashy.addSocket(clientConnection);

        ClientThread handleClient = new ClientThread(clientConnection);
        handleClient.start();
      }
    }catch(IOException e){}
  }
}