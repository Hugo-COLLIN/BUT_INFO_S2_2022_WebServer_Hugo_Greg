import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServerGreg {

    public static final String RESSOURCEPATH = "ressource" + File.separator;

    public static void main(String[] args) throws IOException {
        // Initialisation du port
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            port = 80;
            System.out.println("Le parametre n'est pas un nombre");
        } catch (IndexOutOfBoundsException e) {
            port = 80;
        }


        // Initialisation des flux
        BufferedReader in;
        OutputStream out;

        // Initialisation du socket
        ServerSocket socketServeur = new ServerSocket(port);
        Socket socketClient;
        while (true) {

            socketClient = socketServeur.accept();
            System.out.println("Connexion avec : " + socketClient.getInetAddress());
            in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            out = socketClient.getOutputStream();


            String line = in.readLine();

            if(line == null) return;
            boolean isRequest = line.contains("GET");
            if (!isRequest) return;

            String[] requestSepared = line.split(" ");

            String folder = requestSepared[1];
            folder = folder.substring("/".length());

            if(folder.contains("favicon.ico")) continue;
            for (String s : requestSepared) {
                System.out.println(s);
            }

            System.out.println("folder: "+folder + "\n");

            FileInputStream fis;
            try {
                fis = new FileInputStream(RESSOURCEPATH + folder);

                byte[] fileBytes = fis.readAllBytes();
                if (folder.contains("html"))
                    out.write("HTTP/1.1 200 OK".getBytes());
                out.write(fileBytes);
                out.flush();
            } catch (FileNotFoundException e) {
                System.out.println("this file not found so isn't generated");
            }
            out.close();
        }

    }
}
