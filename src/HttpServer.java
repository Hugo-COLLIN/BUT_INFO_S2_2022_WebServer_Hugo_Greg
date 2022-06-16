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
    public static List<String> acceptIPList, rejectIPList;
    public static List<Integer> acceptMaskList, rejectMaskList;

    public static void main(String[] args) {

        try
        {

            readXML(args);
            System.out.println(port + "\n" + sitePath + "\n" + isIndex + "\n" + acceptIPList + "\n" + rejectIPList);

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
                    String [] tmp = path.split("/");

                    //Send data from server files to client
                    if (!accessFile(sitePath + path, toClient))
                        if(!accessFile(imgPath + path , toClient))
                            if (tmp[tmp.length - 1].contains(".html") || !tmp[tmp.length - 1].contains("."))
                                toClient.write("HTTP/1.1 404 Not Found".getBytes());
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
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private static String setPath(String data)
    {
        String[] requestSplit = data.split(" ");

        String path = requestSplit[1].substring(File.separator.length());
        System.out.println("Requested : " + path);

        String [] tmp = path.split("/");
        if (path.equals(""))
            path = "folder.html";
        else if (!tmp[tmp.length - 1].contains("."))
            path += File.separator + "index.html";

        System.out.println("Try to send : " + path);
        return path;
    }

    private static void generateFolderIndex(String path) throws IOException {
        File folder = new File(path);
        File[] files = folder.listFiles();
        FileWriter fw = new FileWriter("./ressource/folder.html");

       // Header
        StringBuilder sb = new StringBuilder("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("\t<meta charset=\"UTF-8\">\n");
        sb.append("\t<title>Title</title>\n");
        sb.append("</head>\n");

        // Body
        sb.append("<body>\n");
        for(File f : files) {
            sb.append("\t<a href=\""+ f.getName()+ "\">"+f.getName()+"</a>\n");
        }
        sb.append("</body>\n");
        sb.append("</html>");

        fw.write(sb.toString());

        fw.close();
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
