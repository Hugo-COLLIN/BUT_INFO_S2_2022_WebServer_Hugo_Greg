import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class XML_Creation_SAX
{
    public static void main(String[] args) {
        try {
            /*
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse("src/myweb.conf");
            System.out.println(document.getDocumentElement().getTextContent());
            /*
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse("src/myweb.conf", new DefaultHandler() {
                public void startDocument() throws SAXException
                {
                    System.out.println("\nstartDocument");
                }

                public void endDocument() throws SAXException
                {
                    System.out.println("\nendDocument");
                }

                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
                {
                    System.out.println("\nstartElement: \nqName : " + qName + "\nURI :" + uri + "\nlocalName : " + localName + "\nattributes : " + attributes);
                }

                public void endElement(String uri, String localName, String qName) throws SAXException
                {
                    System.out.println("\nendElement");
                }
            });

             */
        }
        catch (Exception e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }
}
