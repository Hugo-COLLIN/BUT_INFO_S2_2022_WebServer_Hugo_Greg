import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hugo COLLIN 20220608
 */
public class HttpServer
{
    private static int port;
    public static String rootPath, imgPath, actualPath;
    public static boolean isIndex;
    public static List<String> acceptIPList, rejectIPList;
    public static List<Integer> acceptMaskList, rejectMaskList;

    public static void main(String[] args)
    {
        try
        {
            readXML(args);
            System.out.println(port + "\n" + rootPath + "\n" + isIndex + "\n" + acceptIPList + "\n" + rejectIPList);

            BufferedReader fromClient;
            OutputStream toClient;
            String data;
            while (true) {
                //Server connexion
                ServerSocket servSocket = new ServerSocket(port);
                System.out.println("\nConnected on port " + port + ", pending client request...");

                //Client connexion to server
                Socket cliSocket = servSocket.accept();
                String cliAdr = cliSocket.getInetAddress() + ":" + cliSocket.getPort();
                System.out.println("Client " + cliAdr + " connected");
                toClient = cliSocket.getOutputStream();

                fromClient = new BufferedReader(
                        new InputStreamReader(cliSocket.getInputStream())
                );

                while ((data = fromClient.readLine()) != null && data.contains("GET"))
                {
                    if (!isAccepted(cliSocket.getInetAddress()) ) {
                        forbiddenPage(toClient);
                        break;
                    }
                    //Process of path request
                    actualPath = setPath(data);
                    if (actualPath != null && new File(rootPath +actualPath).isDirectory()) {
                        folderIndexPage(rootPath + actualPath, toClient);
                        continue;
                    }
                    else if (actualPath == null) {
                        actualPath = "";
                        folderIndexPage(rootPath, toClient);
                        continue;
                    }

                    String [] tmp = actualPath.split("/");
                    //Send data from server files to client
                    if (!accessFile(rootPath + actualPath, toClient))
                        if(!accessFile(imgPath + actualPath , toClient))
                            if (tmp[tmp.length - 1].contains(".html") || !tmp[tmp.length - 1].contains(".")) {
                                System.out.println("Not found");
                                toClient.write("HTTP/1.1 404 Not Found".getBytes());
                                errorPage(toClient);
                            }
                }

                //Close connexions
                toClient.close();
                fromClient.close();

                cliSocket.close();
                servSocket.close();

                System.out.println("Connexion closed with " + cliAdr);
            }
        }
        catch (IOException e)
        {
            System.out.println("In-Out error in server");
            //e.printStackTrace();
        }
        catch (ParserConfigurationException | SAXException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Verifie si l'adresse du client est dans la liste des adresses acceptees
     * @param client adresse IP du client
     * @return si l'adresse est autorisee ou non
     */
    private static boolean isAccepted(InetAddress client) {

        if (acceptIPList.isEmpty() || acceptMaskList.isEmpty()) return true;

        String temp = client.toString().substring(File.separator.length());     // Suppression du "/" de debut

        String[] numbersCli, masque;
        int[] adresseReseau;
        if (temp.contains(".")) {
            numbersCli = temp.split("\\.");                               // Separation des chiffres grace au point

            masque = getMasque(acceptMaskList.get(0)).split("\\.");           // Separation des chiffres grace au point
            adresseReseau = new int[numbersCli.length];                       // Initialisation du tableau

            // Recherche de l'adresse reseaux
            for (int i = 0; i < numbersCli.length; i++) {
                int numberCli = Integer.parseInt(numbersCli[i]);                    // Conversion de la chaine en int
                int nMasque = HttpServer.binaryToInt(masque[i]);                    // Conversion de la chaine en int
                adresseReseau[i] = numberCli & nMasque;                             // Et logique entre le client et le masque
            }
        }
        else {
            return true;
            //numbersCli = temp.split(":");
            //adresseReseau = new int[numbersCli.length];
        }


        boolean accepted = false;                                               // Initialisation
        for (String s : acceptIPList) {                                         // for each pour parcourir tout la liste des ips accepter
            String[] nS = s.split("\\.");                                 // Separation des chiffres grace au point
            for (int i = 0; i < nS.length; i++) {                               // parcours de chaque chiffres
                int numberS = Integer.parseInt(nS[i]);                          // conversion vers un int
                if (numberS == adresseReseau[i]) {                              // Si le nombre est equal a l'adresse reseau
                    accepted = true;                                            // alors il est accepte tant que c'est equal
                } else {                                                        // sinon
                    accepted = false;                                           // il est refuse
                    break;                                                      // arret de boucle pour eviter qu'accepted depend du dernier chiffre
                }
            }
            if (accepted) break;                                                // Si l'ip est accepte pas besoin de regarder les autres ip donc arret de boucle
        }
        return accepted;                                                        // retourn s'il est accepte ou non
    }

    /**
     * permet d'obtenir un masque a partir d'une taille
     * de la partie reseau
     * @param partieReseau taille de la partie reseau (ex : 24)
     * @return retoune le masque en fonction de la partie reseau (ex : 11111111.11111111.11111111.00000000)
     */
    public static String getMasque(int partieReseau) {
        final int max = 32;                         // init de la taille max du masque
        if (partieReseau > max) return null;        // si la taille de la partie reseau est superieur a la taille max d'un max on s"arrete
        int nb = 0;                                 // init pour savoir combien de valeur il a ajoute
        StringBuilder sb = new StringBuilder();     // init string builder
        for (int i = 0; i<partieReseau; i++) {      // ajout de la partie reseau dans le string
            if(nb == 8) { sb.append("."); nb = 0;}  // ajout a point apres 8 ajouts consecutif pour obtenir (12345678.12345678...)
            sb.append("1");                         // ajout des un etant la partie reseau
            nb++;                                   // incremente nb
        }

        for (int i = 0; i < max-partieReseau; i++) {// partie machine
            if(nb == 8) { sb.append("."); nb = 0;}  // mettre un point tout le 8 ajouts
            sb.append("0");                         // ajout d'un 0 etant la partie machine
            nb++;                                   // incrmeent nb
        }
        return sb.toString();                       // retourne le string correspondant au masque
    }


    private static void errorPage (OutputStream os) throws IOException
    {
        os.write("HTTP/1.1 404 ERROR".getBytes());

        //Head
        StringBuilder sb = new StringBuilder(
                "<!DOCTYPE html>\n<html lang=\"fr\">\n\n<head>\n" +
                "\t<meta http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n" +
                "\t<title>Page introuvable</title>\n" +
                "</head>\n"
        );

        // Body
        sb.append("<body>\n" +
                "    <main style = \"margin:2em; text-align:center;\">\n" +
                "        <section>\n" +
                "        <h1>Erreur 404</h1>\n" +
                "        <p>Hé oui, c'est pas de bol !</p>\n" +
                "        <h3>La page n'existe pas sur le serveur.</h3>\n" +
                "        </section>\n" +
                "    </main>\n" +
                "</body>\n" +
                "</html>");

        os.write(sb.toString().getBytes());
        os.flush();
    }


    private static void forbiddenPage(OutputStream os) throws IOException
    {
        os.write("HTTP/1.1 403 Forbidden".getBytes());

        StringBuilder sb = new StringBuilder("<!DOCTYPE html>\n<html lang=\"fr\">\n\n<head>\n\t" +
                "<meta http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n" +
                "\t<title>Accès refusé</title>\n</head>\n");

        // Body
        sb.append("<body>\n" +
                "    <main style = \"margin:2em; text-align:center;\">\n" +
                "        <section>\n" +
                "        <h1>Erreur 403</h1>\n" +
                "        <p>On ne passe pas !</p>\n" +
                "        <h3>Vous n'avez pas l'autorisation d'accéder à ce fichier.</h3>\n" +
                "        </section>\n" +
                "    </main>\n" +
                "</body>\n" +
                "</html>");

        os.write(sb.toString().getBytes());
        System.out.println("Forbidden");
    }

    private static void folderIndexPage(String globalPath, OutputStream os) throws IOException {
        File folder = new File(globalPath); // Recupere le dossier
        File[] files = folder.listFiles(); // recupere tout les fichiers du dossier

        os.write("HTTP/1.1 202 OK".getBytes());

        // code html
        // Header
        StringBuilder sb = new StringBuilder("<!DOCTYPE html>\n" +
                "<html lang=\"fr\">\n\n" +
                "<head>\n\t" +
                "<meta http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n" +
                "\t<title>Index de " + File.separator + actualPath + "</title>\n</head>\n");

        // Body
        sb.append("<body>\n" +
                "    <main style = \"margin:2em;\">\n" +
                "        <section>\n" +
                "           <ul style = \"list-style:none;\">\n");

        // ajout des liens vers chaque fichier
        for (File f : files) {
            if(f.getName().equals("images")) continue; // Si c'est le dossier image il l'affiche pas
            sb.append("\t\t<li><a href=\""+ actualPath + File.separator + f.getName() + "\">"+f.getName()+"</a></li>\n"); // ajout du lien entre chaque fichier
        }
        sb.append("         </ul>\n" +
                "        <section>\n" +
                "</main>\n" +
                "</body>\n" +
                "</html>");
        // fin code html
        os.write(sb.toString().getBytes());
        os.flush();
    }

    private static String setPath(String data)
    {
        String[] requestSplit = data.split(" ");

        String path = requestSplit[1].substring(File.separator.length());


        System.out.println("Requested : " + path);

        String [] tmp = path.split("/");
        if (path.equals("") && isIndex) {
            path = null;
        }
        else if (path.equals("") && !isIndex) {
            path = "index.html";
        }
        else if (!tmp[tmp.length - 1].contains(".") && !isIndex)
            path += File.separator + "index.html";

        //if (isIndex && !tmp[tmp.length - 1].contains(".")) showIndex = true;

        //if (isIndex)
        //else


        System.out.println("Try to send : " + path);
        return path;
    }

    public static String valueToBinary(int val) {
        // Search in first the power of 2 upper than val
        StringBuilder binary= new StringBuilder();
        while(val != 0 && val != 1) { // Divise val par 2 tant qu'il n'est pas egal a 0 ou 1
            binary.append(val % 2); // ajout de 0 ou 1
            val /= 2; // divise val par deux
        }
        binary.append(val); // ajoute la valeur de val etant la derniere donc 0 ou 1
        binary.reverse(); // retourne le pour pour passer de 0001 a 1000 pour le chiffre 8
        return binary.toString();
    }

    public static int binaryToInt(String binary) {
        int valeur = 0;
        // parcours toute la chaine de charactere
        for (int i = 0; i < binary.length(); i++) {
            if (binary.charAt(i) == '1') // si la valeur du char est 1 alors
                valeur += Math.pow(2, (binary.length()-1)-i); // on ajoute la puissance de deux correspondante
            // binary.length() -1 car un octect a pour taille 8 mais sa derniere puissance est 7
            // -i pour aller du point fort au point faible
        }
        return valeur; // retourne la valeur
    }

    private static boolean accessFile(String path, OutputStream os)
    {
        FileInputStream fis;
        boolean res = false;
        try
        {
            fis = new FileInputStream(path);

            byte[] fileBytes = fis.readAllBytes();
            if (path.contains("html"))
                os.write("HTTP/1.1 200 OK".getBytes());

            os.write(fileBytes);
            os.flush();
            os.close();
            res = true;
            System.out.println("Found in " + File.separator + path);
            System.out.println("Resource sent");
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Not found in " + File.separator + path);
        }
        catch (IOException e)
        {
            System.out.println("In-Out error in acces file");
        }
        return res;
    }

    public static void readXML(String [] args) throws ParserConfigurationException, IOException, SAXException {
        File file;
        if (args.length != 0)
            file = new File(args[0]);
        else
            file = new File("src/myweb.conf");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        NodeList nodeList = doc.getElementsByTagName("webconf");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                try {
                    port = Integer.parseInt(element.getElementsByTagName("port").item(0).getTextContent());
                } catch (NullPointerException e) {
                    port = 80;
                    System.out.println("No port defined, set on port 80.");
                }


                rootPath = element.getElementsByTagName("root").item(0).getTextContent();
                if (rootPath.equals(""))
                {
                    rootPath = "ressource";
                    System.out.println("No root defined, default used");
                }
                rootPath += File.separator;
                imgPath = rootPath + "images" + File.separator;

                //
                try {
                    isIndex = Boolean.parseBoolean(element.getElementsByTagName("index").item(0).getTextContent());
                }
                catch (NullPointerException e)
                {
                    isIndex = false;
                    System.out.println("No index state defined, set to false.");
                }


                NodeList accepts = element.getElementsByTagName("accept");
                acceptIPList = new ArrayList<>();
                acceptMaskList = new ArrayList<>();
                for (int j = 0 ; j < accepts.getLength() ; j ++)
                    try {
                        String [] tmp = accepts.item(j).getTextContent().split("/");
                        acceptIPList.add(tmp[0]);
                        acceptMaskList.add(Integer.parseInt(tmp[1]));
                    }
                    catch (NullPointerException e)
                    {
                        System.out.println("No accepted IPs defined");
                    }

                NodeList rejects = element.getElementsByTagName("reject");
                rejectIPList = new ArrayList<>();
                rejectMaskList = new ArrayList<>();
                for (int j = 0 ; j < rejects.getLength() ; j ++)
                    try {
                        String [] tmp = rejects.item(j).getTextContent().split("/");
                        rejectIPList.add(tmp[0]);
                        rejectMaskList.add(Integer.parseInt(tmp[1]));
                    }
                    catch (NullPointerException e)
                    {
                        System.out.println("No rejected IPs defined");
                        e.printStackTrace();
                    }
            }
        }
    }

}
