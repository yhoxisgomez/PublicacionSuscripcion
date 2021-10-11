package PubSubClient;

import java.io.BufferedReader;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class ServerListener extends Thread {

  /* Formato de hora  HH:MM:SS */
  private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
  /* variable de tiempo actual */
  LocalDateTime currentTime;
  /* inicializa el stream object que lee desde el server */
  BufferedReader brNetwork = null;

  public ServerListener(BufferedReader brNetwork) {
    super();
    this.brNetwork = brNetwork;
  }

  @Override
  public void run() {

    try {
      while (true) {
        try {
          /* el array de resultados entra en la secuencia de acuerdo con la cantidad de espacios que se separe*/
          String[] result = brNetwork.readLine().split(" ");
          if (result.length == 1) {
            /* si el servidor envió un mensaje de protocolo de 1 palabra - puede ser Error / OK */
            switch (result[0].toUpperCase()) {
              case "OK":
                System.out.println("Ejecutado exitosamente\n");
                break;
              case "ERROR":
                System.out.println("Error\n");
                break;
            }
          } else {
            /* Si el servidor envió un mensaje de protocolo de más de 1 palabra, puede ser forward MSG */
            String rebuildMsg = "";
            rebuildMsg = String.join(" ", Arrays.copyOfRange(result, 3, result.length));
            String formatMSG = "(" + result[1] + ") " + result[2] + " " + dtf.format(LocalDateTime.now()) + " - "
                    + rebuildMsg;
            System.out.println(formatMSG);
          }
        } catch (NullPointerException ex) {
          /* la comunicación de red no funciona, mientras que el servidor se cierra */
          System.out.println("El servidor cerró la conexión contigo");
          return;
        } catch (SocketException sc) {
          if (!helperClass.disconnet) /* mientras el servidor tiene una salida impredecible */ {
            System.out.println("El servidor desapareció");
          }
          return;
        }
      }
    } catch (Exception ex) {
      System.err.println("Error" + ex.getMessage());
      return;
    }
  }
}
