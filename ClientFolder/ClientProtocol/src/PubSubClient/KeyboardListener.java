package PubSubClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class KeyboardListener{

  public static void main(String[] args) throws IOException {

    while (true) {
      /* El thread escucha al servicio, notifica cuando el hilo principial recibe por teclado y notifica al servicio */
      ServerListener listener;
      /* Socket con el servicio */
      Socket serverSocket = null;
      /* Dirección IP del objeto */
      InetAddress serverIP = null;      
      int port = 0;
      /* Como streamWriter -> en el servidor mientras el cliente desea enviar mensajes al servidor, por ejemplo, comandos de protocolo */
      PrintWriter pwNetwork = null;
      /* Lector del teclado */
      BufferedReader brKeyboard = null;
      /* Instancia del objeto que obtendr{a mensajes desde servicio dentro del hilo oyente */
      BufferedReader brNetwork = null;
      /* flag para saber el estado del programa */
      boolean isConnect = false;
      helperClass.disconnet = false;
      
      while (!isConnect) {
        /* leemos desde el teclado */
        brKeyboard = new BufferedReader(new InputStreamReader(System.in));
        /* imprime: connect / disconnect / quit */
        printUserConnectMenue();
        String answer = "";
        try {
          /* lee la respuesa del usuario */
          answer = brKeyboard.readLine().toUpperCase();
        } catch (IOException e1) {
          System.out.println("Error no fue posible leer del teclado: " + e1.getMessage());
        }
        /* multiples opciones en el menú principal */
        switch (answer) {
          case "CONNECT":
            System.out.println("Conectando...\n");
            isConnect = true;

            serverIP = InetAddress.getByName("127.0.0.1");
            port = Integer.parseInt("6556");
            /* Abrimos el socket */
            serverSocket = new Socket(serverIP, port);
            if (serverSocket != null) {
                    System.out.println("Conectado al servicio.\n");
                    brNetwork = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            }
            /* abre el writer al socket abierto */
            pwNetwork = new PrintWriter(serverSocket.getOutputStream());
            
            break;

          case "DISCONNECT":
            isConnect = false;
            System.out.println("Not logged-in yet\n");
            break;

          case "QUIT":
            System.out.println("Good bye!\n");
            return;
          
          default:
            illegalUsage();
        }
      }

      /* apertura del hilo que esta oyendo a SERVER */
      listener = new ServerListener(brNetwork);
      listener.start();
      System.out.println("\nElija el rol que desee:" + "\n1. publisher" + "\n2. subscriber");
      String userOpc = brKeyboard.readLine();
      
      while (isConnect) {
        /* segundo menú, lo que el usuario puede hacer como pub/sub */

      switch (userOpc){
        case "publisher":
            printUserInterfaceConnectedPub();
        break;
          
        case "subscriber":
          printUserInterfaceConnectedSub();
        break;
      }
        try {
          /* lee la decisión del usuario - join / quit / disconnect / message to delivery / */
          String userIn = brKeyboard.readLine();
          String[] sentence = userIn.split(" ");
          /* En caso de presionar en blanco */
          if (sentence.length == 0)
            System.out.println("Comando en blanco, intente de neuvo");
          /* En caso de que sea una sola palabra: chequea que sea "quit"/ "connect" / "disconnect" */
          if (sentence.length == 1) {
            switch (sentence[0].toUpperCase()) {
              case "QUIT": {
                try { 
                  /* enviando una señal de cerrar al server */
                  pwNetwork.println("CLOSE");
                  pwNetwork.flush();
                  helperClass.disconnet = true;
                  // cerrar socket al salir del programa - se lanzará el cierre del hilo por la excepción
                  serverSocket.close();
                } catch (IOException | NullPointerException ioe) {
                  System.out.println("Error: " + ioe.getMessage());
                  break;
                }
                System.out.println("Nos vemos pronto!");
                return;
              }
              case "CONNECT": {
              /* verifique si el oyente no está encendido o  alguna salida impredecible del servidor por ejemplo, 
              el servidor se ejecutó cerca de algún cliente, por lo tanto verificamos si existe, o todo debe comenzar de nuevo */
                if (!listener.isAlive()) {
                  try {
                    serverSocket = new Socket(serverIP, port);
                    /* abre el socket entre el client y el servicio */
                    if (serverSocket != null) {
                      System.out.println("Conectado al servidor.");
                      brNetwork = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                    }
                    /* abre el writer al servidor por el socket abierto */
                    pwNetwork = new PrintWriter(serverSocket.getOutputStream());
                  } catch (UnknownHostException unx) {
                    System.out.println("Error: No se encontró host: " + unx.getMessage());
                    isConnect = false;
                    break;
                  } catch (NumberFormatException nfe) {
                    System.out.println("Error: Puerto invalido: " + nfe.getMessage());
                    isConnect = false;
                    break;
                  } catch (ConnectException ce) {
                    System.out.println("Error: No hay procesos esuchcando el hilo \n");
                    isConnect = false;
                    break;
                  } catch (NullPointerException e) {
                    e.getMessage();
                    isConnect = false;
                    break;
                  } catch (IOException ex) {
                    System.out.println("error: " + ex.getMessage());
                    break;
                  }
                  /* el serverSocket está abierto para comunicarse, tenemos que escuchar con el hilo del oyente */
                  listener = new ServerListener(brNetwork);
                  listener.start();
                }
                /* si ya esta oonectado y trata de conectarse */
                else
                        System.out.println("Ya conectado");
                break;

              }
              case "DISCONNECT": {
                System.out.println("Desconectando del server...");
                /* enviar close al servidor */
                pwNetwork.println("CLOSE");
                pwNetwork.flush();
                /* cerrar el socket en lugar del hilo de escucha porque no es seguro */
                try {
                  serverSocket.close();
                } catch (NullPointerException | SocketException e) {
                  e.getMessage();
                  return;
                } catch (IOException e) {
                  System.out.println(e.getMessage());
                  return;
                }
                isConnect = false;
                helperClass.disconnet = true;
                System.out.println("Desconectado!");
                Thread.sleep(200);
                break;
              }
             
              default:
                illegalUsage();
                break;
            }
          }
          /* En caso de que el cliente haya escrito comandos del pubsub */
          else if (sentence.length == 2) {
            switch (sentence[0].toUpperCase()) {
              case "JOIN": {
                if (sentence[1].toUpperCase().equals("JOIN")) {
                  specialTopic();
                  break;
                }

                pwNetwork.println("REGISTER " + sentence[1]);
                pwNetwork.flush();
                break;
              }

              default: {
                pwNetwork.println("SEND " + userIn);
                pwNetwork.flush();
                break;
              }
            }
          }
          /*
           * en cualquier otro caso, la primera palabra es el tema
           * 
           */
          else if (sentence.length >= 3) {
            /* si la oración tiene más de 2 palabras si la primera palabra es una palabra de protocolo, imprimiremos el comando no
            encontrado, de lo contrario, enviará toda la oración al servidor con el comando SEND */
            switch (sentence[0].toUpperCase()) {
              case "JOIN":
                specialTopic();
                break;
              case "LEAVE":
                specialTopic();
                break;
              case "QUIT":
                specialTopic();
                break;
              default: {
                pwNetwork.println("SEND " + userIn);
                pwNetwork.flush();
                break;
              }
            }
          }
        } catch (IOException | InterruptedException iox) {
            System.out.println("Error en la comunicación con la red: " + iox.getMessage());
        }
      }
    }

  }

  /* Primer menú - connect , disconnect or quit antes de conectar al servidro */
  private static void printUserConnectMenue() {
    System.out.println("\nPorfavor escriba el comando que desea realizar:" + "\n1. connect" + "\n2. disconnect" + "\n3. quit");
  }

  /* En caso de comando no encontrado */
  private static void illegalUsage() {
    System.out.println("Comando no encontrado");
  }

  /* algunos topics que no se puede usar como comandos */
  private static void specialTopic() {
    System.out.println("No se puede usar comando como topics.");
  }

  /* Menús */  
  private static void printUserInterfaceConnectedPub() {
    System.out.println("\nSiga las instrucciones de abajo:"
                    + "\n*** Acción ***************************** Comando ******************** Ejemplo *******************"
                    + "\n1) Registrar una categoría de noticia:\t\tjoin\ttopic \t\t ej. join deportes"
                    + "\n2) Enviar una noticia por una categoría:\ttopic \"your sentence\" \t ej. deportes Cristiano Ronaldo se une al Manchester United"
                    + "\n3) Desconectarse del servidor:\t\t\tdisconnect \t\t ej. disconnect"
                    + "\n4) Salir:\t\t\t\tquit\n");
  }
  
  private static void printUserInterfaceConnectedSub() {
    System.out.println("\nFollow the instructions below:"
                    + "\n*** Acción ***************************** Comando ******************** Ejemplo *******************"
                    + "\n1) Unirse a una categoría de noticia:\t\tjoin\ttopic \t\t ej. join deportes"
                    + "\n4) Desconectarse del servidor:\t\t\tdisconnect \t\t ej. disconnect"
                    + "\n5) Salir:\t\t\t\tquit\n");
  }
}