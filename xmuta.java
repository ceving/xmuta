import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.w3c.dom.Element;

public class xmuta
{
    static final String nl = System.getProperty("line.separator");

    static void die (String message)
    {
        System.err.println (message);
        System.exit(1);
    }

    static public class Arguments
    {
        private String[] args = null;
        private int i = 0;

        public Arguments(String[] args_array) { args = args_array; }

        public void    skip    ()      { i++; }
        public boolean anyleft ()      { return i < args.length; }
        public boolean left    (int n) { return i-1+n < args.length; }

        public boolean in (String... options)
        {
            boolean result = false;
            if (anyleft())
                for (int j = 0; j < options.length; j++)
                    result = result || args[i].equals (options[j]);
            return result;
        }

        public boolean equals (String arg)
        {
            boolean result = anyleft ()  && args[i].equals (arg);
            if (result) skip();
            return result;
        }
        public String next ()
        {
            String result = args[i];
            skip();
            return result;
        }
        public String require (String name)
        {
            if (!anyleft())
                die ("Argument '" + name + "' is missing.");
            return next();
        }
    }

    static Document read_document (String xml_filename)
        throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware (true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource source;
        if (xml_filename == null)
            source = new InputSource (System.in);
        else
            source = new InputSource (xml_filename);
        Document document = null;
        try {
            document = builder.parse (source);
        }
        catch (Exception e) {
            die ("Can not read XML file '"+ xml_filename + "': " + e);
        }
        Element root = document.getDocumentElement();
        return document;
    }

    static XPath make_xpath (final Document document,
                             final String new_default_namespace)
        throws Exception
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext (new NamespaceContext ()
            {
                String default_namespace;
                Map<String,String> namespace;
                {
                    String old_default_namespace = 
                        document.lookupNamespaceURI(null);
                    if (new_default_namespace == null)
                        default_namespace = old_default_namespace;
                    else
                        default_namespace = new_default_namespace;
                    namespace = new HashMap<String,String>();
                    NodeList nodelist = document.getElementsByTagName("*");
                    for (int i = 0, l = nodelist.getLength(); i < l; i++)
                    {
                        Node node = nodelist.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE)
                        {
                            String element_namespace = node.getNamespaceURI();
                            if (element_namespace == null)
                                document.renameNode (node,
                                                     new_default_namespace,
                                                     node.getLocalName());
                            else if (!element_namespace.equals(default_namespace))
                                namespace.put (node.getPrefix(), element_namespace);
                        }
                    }
                }
                public String getNamespaceURI (String prefix)
                {
                    if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX))
                        return default_namespace;
                    else
                        return namespace.get (prefix);
                }
                public String   getPrefix   (String uri) { return null; }
                public Iterator getPrefixes (String uri) { return null; }
            });
        return xpath;
    }

    public enum OutputFormat { TEXT, XML };

    static String evaluate_xpath (Document document,
                                  XPath xpath,
                                  String xpath_string,
                                  OutputFormat output_format)
        throws Exception
    {
        XPathExpression expr = xpath.compile (xpath_string);
        NodeList nodes = (NodeList) expr.evaluate
            (document.getDocumentElement(),
             XPathConstants.NODESET);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter sw = new StringWriter();
        for (int i = 0, l = nodes.getLength(); i < l; i++)
        {
            Node node = nodes.item(i);
            if (output_format == OutputFormat.TEXT)
                sw.write (node.getTextContent());
            else
            {
                StreamResult sr = new StreamResult(sw);
                Source source = new DOMSource(node);
                trans.transform (source, sr);
            }
        }
        return sw.toString();
    }

    static void test_xpath (String xpath_string,
                            OutputFormat output_format,
                            String new_default_namespace)
        throws Exception
    {
        final Document document = read_document (null);
        XPath xpath = make_xpath (document, new_default_namespace);
        System.out.print (evaluate_xpath (document, xpath, xpath_string,
                                          output_format));
    }

    static void test_regexp (String regexp_string,
                             OutputFormat output_format)
        throws Exception
    {
    }

    static void substitute (String xml_filename,
                            Map<String,String> substitutions,
                            OutputFormat output_format,
                            String new_default_namespace)
        throws Exception
    {
        final Document document = read_document (xml_filename);
        XPath xpath = make_xpath (document, new_default_namespace);
        for (Map.Entry<String,String> entry : substitutions.entrySet())
            entry.setValue (evaluate_xpath
                            (document, xpath, entry.getValue(),
                             output_format));
        BufferedReader in =
            new BufferedReader (new InputStreamReader (System.in));
        String line;
        while ((line = in.readLine()) != null)
        {
            for (Map.Entry<String,String> entry : substitutions.entrySet())
                line = line.replaceAll (entry.getKey(), entry.getValue());
            System.out.println (line);
        }
    }

    public static void main (String[] args_array)
    {
        try {
            Arguments args = new Arguments(args_array);

            OutputFormat output_format = OutputFormat.TEXT;
            String new_default_namespace = null;

            while (args.in ("-d", "-x"))
            {
                if (args.equals("-d"))
                    new_default_namespace = args.require("Default name space");
                if (args.equals("-x"))
                    output_format = OutputFormat.XML;
            }

            if (args.equals("-h"))
            {
                System.out.print
                    ("XMUTA - Substitute regular expression matches with XPath" +
                     " matches." + nl +
                     "Usage:" + nl +
                     "    xmuta [-x] <xmlfile> (<regexp> <xpath>)+" + nl + 
                     "Options:" + nl +
                     "    -d NS   Add a default name space if the XML file does not have any." + nl +
                     "    -x      Generate XML output. Default is TEXT." + nl +
                     "Test XPath:" + nl +
                     "    xmuta [-x] -p <xpath>" + nl + 
                     "Test regular expression:" + nl +
                     "    xmuta -r <regexp>" + nl);
            }
            else if (args.equals("-p"))
            {
                test_xpath (args.require ("XPath expression"),
                            output_format,
                            new_default_namespace);
            }
            else if (args.equals("-r"))
            {
                test_regexp (args.require ("Regular expression"), output_format);
            }
            else 
            {
                String xml_filename = args.require ("XML file name");
                Map<String,String> substitutions = new HashMap<String,String>();
                while (args.left(2))
                    substitutions.put(args.next(), args.next());
                substitute (xml_filename, substitutions, output_format,
                            new_default_namespace);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
