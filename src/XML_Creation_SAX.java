import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class XML_Creation_SAX
{
    public static void main(String[] args) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse("src/XML_résultat.xml", new DefaultHandler() {
                public void startDocument() throws SAXException { System.out.println("startDocument"); }
                public void endDocument() throws SAXException
                {
                    System.out.println("endDocument");
                }
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
                {
                    System.out.println("startElement: " + qName);
                }
                public void endElement(String uri, String localName, String qName) throws SAXException
                {
                    System.out.println("endElement");
                }
            });
        }
        catch (Exception e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }
}
