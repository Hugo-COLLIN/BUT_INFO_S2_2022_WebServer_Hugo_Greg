import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
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
    public static String sitePath;
    public static String imgPath;
    public static boolean isIndex;
    public static List<String> acceptList, rejectList;


    public static void main(String[] args) {
        try
        {
            //List<String> webConf = readXML("myweb.conf");

            //int port = setPort(args);

            readXML("src/myweb.conf");
            System.out.println(port + "\n" + sitePath + "\n" + isIndex + "\n" + acceptList + "\n" + rejectList);

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
                    if (!accessFile(sitePath + path, toClient))
                        accessFile(imgPath + path , toClient);
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
            //e.printStackTrace();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
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

    public static void readXML(String path) throws ParserConfigurationException, IOException, SAXException {
        File file = new File(path);
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


                sitePath = element.getElementsByTagName("root").item(0).getTextContent();
                if (sitePath.equals(""))
                {
                    sitePath = "ressource";
                    System.out.println("No root defined, default used");
                }
                sitePath += File.separator;
                imgPath = sitePath + "images" + File.separator;

                //
                try {
                    isIndex = Boolean.parseBoolean(element.getElementsByTagName("index").item(0).getTextContent());
                }
                catch (NullPointerException e)
                {
                    isIndex = false;
                    System.out.println("No index state defined, set to false.");
                }

                NodeList accepts = element.getElementsByTagName("accepts");
                acceptList = new ArrayList<>();
                for (int j = 0 ; j < accepts.getLength() ; j ++)
                    try {
                        acceptList.add(accepts.item(j).getTextContent());
                    }
                    catch (NullPointerException e)
                    {
                        System.out.println("No accepted IPs defined");
                    }

                NodeList rejects = element.getElementsByTagName("rejects");
                rejectList = new ArrayList<>();
                for (int j = 0 ; j < accepts.getLength() ; j ++)
                    try {
                        rejectList.add(rejects.item(j).getTextContent());
                    }
                    catch (NullPointerException e)
                    {
                        System.out.println("No rejected IPs defined");
                    }
            }
        }
    }

}
