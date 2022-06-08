import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Hugo COLLIN 20220529
 */
public class HttpServer
{
    public static final String SITE_PATH = "ressource" + File.separator;
    public static final String IMG_PATH = SITE_PATH + "images" + File.separator;


    public static void main(String[] args) {
        try
        {
            //XML launch
            //JAXBContext

            int port = setPort(args);

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
                    //Process of request path
                    String path = setPath(data);

                    //Send data from server files to client
                    if (!accessFile(SITE_PATH + path, toClient))
                        accessFile(IMG_PATH + path , toClient);
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
            System.out.println("In-Out error");
        }
    }

    public static int setPort (String[] args)
    {
        int port;

        if (args.length == 0)
            port = 80;
        else if (args.length == 1)
            port = Integer.parseInt(args[0]);
        else
            throw new ArrayIndexOutOfBoundsException();

        return port;
    }

    private static String setPath(String data)
    {
        String[] requestSplit = data.split(" ");

        String path = requestSplit[1].substring(File.separator.length());
        System.out.println("Requested : " + path);

        String [] tmp = path.split("/");
        if (path.equals(""))
            path = "index.html";
        else if (!tmp[tmp.length - 1].contains("."))
            path += File.separator + "index.html";

        System.out.println("Try to send : " + path);
        return path;
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
            System.out.println("In-Out error");
        }
        return res;
    }

    public void readXML(String path) throws Exception {
        File file = new File(path);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        NodeList nodeList = doc.getElementsByTagName("webconf");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                System.out.println(element.getElementsByTagName("port").item(0).getTextContent());
                System.out.println(element.getElementsByTagName("root").item(0).getTextContent());
                System.out.println(element.getElementsByTagName("index").item(0).getTextContent());

                try {
                    System.out.println(element.getElementsByTagName("accepts").item(0).getTextContent());
                } catch (NullPointerException e) {
                    System.out.println("don't have accepted ip");
                }

                try {
                    System.out.println(element.getElementsByTagName("rejects").item(0).getTextContent());
                } catch (NullPointerException e) {
                    System.out.println("don't have rejected ip");
                }
            }
        }
    }

}
