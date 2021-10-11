package PubSubService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class ServerThread {

  public static void main(String[] args) {
    // puerto al que conectar
    int port = 6556;
    // Inicializa el log
    Hashy.initiateLogFile();
    boolean connected = false;
    ServerSocket listeningSocket = null;
    ConnectionThread listener = null;
    // inicizaliza el buffer
    BufferedReader brIn = new BufferedReader(new InputStreamReader(System.in));
    printRules();

    while (true) {
      try {
        // leer por teclado
        String userAnswer = brIn.readLine().trim().toUpperCase();
        switch (userAnswer) {
          case "CONNECT": {
            try {
              if (!connected) {
                InetAddress ipAddr = InetAddress.getByName("127.0.0.1");//chooseIPAddress(uniqueIPs);
                listeningSocket = new ServerSocket(port, 10, ipAddr);
                listener = new ConnectionThread(listeningSocket);
                listener.start();
                System.out.println("Escuchando: " + listeningSocket.getLocalSocketAddress());
                Hashy.writeLog("Connected to: " + listeningSocket.getLocalSocketAddress());
                connected = true;
              } else {
                  System.out.println("Ya esta conectado: " + listeningSocket.getLocalSocketAddress());
              }
            } catch (BindException be) {
                System.out.println("No pudo conectar. " + be.getMessage());
                Hashy.writeLog("BindException occurred: " + be.getMessage());
                printRules();
                continue;
            } catch (IOException ioe) {
                System.out.println("falló al conectar. " + ioe.getMessage());
                Hashy.writeLog("IOException occurred: " + ioe.getMessage());
                printRules();
                continue;
            } catch (Exception e) {
                System.out.println("falló al conectar. " + e.getMessage());
                Hashy.writeLog("IOException occurred: " + e.getMessage());
            }
            break;
          }
          case "CLOSE": {
            if (connected) {
              try {
                Hashy.removeAllSockets();
                listeningSocket.close();
              } catch (IOException e) {
                  System.out.println("falló al cerrar.");
              }
              connected = false;
              // cerrando el sevidor
              System.out.println("Server cerrado.");
              Hashy.writeLog("Server closed.");
              Hashy.cleanTopics();
            } else {
                System.out.println("Server ya esta cerrado.");
            }
            break;
          }
          case "QUIT": {
            try {
              if (connected) {
                listeningSocket.close();
              }
            } catch (IOException e) {
                System.out.println("falló cerrando.");
            } finally {
                // el programa se apaga
                Hashy.removeAllSockets();
                Hashy.cleanTopics();
            }
            System.out.println("Bye bye.");
            Hashy.writeLog("Server terminated.");
            return;
          }
          default: {
            System.out.println("Ilegal input!");
            printRules();
          }
        }
      } catch (IOException ioe) {
          System.out.println("falló al leer. " + ioe.getMessage());
          Hashy.writeLog("IOException occured, couldnt read keyboard: " + ioe.getMessage());
      }
    }
  }

  /* menu */
  private static void printRules() {
    System.out.println("Menú [escribe tu opción]:\n"
                    + "\"Connect\" para conectar el servicio, \n\"Close\" para cerrar el servicio, \n\"Quit\" para cerrar el progama.");
  }
}
