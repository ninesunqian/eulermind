package infinitmindmap.importer;

import infinitmindmap.MindDB;
import com.tinkerpop.blueprints.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FreemindImporter extends Importer{

    static Logger s_logger = LoggerFactory.getLogger(Importer.class);

    public FreemindImporter(MindDB mindDB)
    {
        super(mindDB);
    }

    private Object importFreemindNode(Object parentDBId, int pos, org.w3c.dom.Element element,
                                      HashMap<String, Object> mmId2dbIdMap,
                                      HashMap<String, String> mmLinkMap)
    {
        String mmId = element.getAttribute("ID");
        String text = element.getAttribute("TEXT");

        s_logger.info("import freemind Nod {} : {}", mmId, text);

        Object dbId = addTextDBChild(parentDBId, pos, text);

        mmId2dbIdMap.put(mmId, dbId);

        String linkAttribute = element.getAttribute("LINK");
        if (linkAttribute.length() > 1 && linkAttribute.substring(0, 4).equals("#ID_")) {
            mmLinkMap.put(mmId, linkAttribute.substring(1));
        }

        NodeList nodes = element.getChildNodes();
        int childPos = 0;

        for (int i=0; i < nodes.getLength(); i++)
        {
            org.w3c.dom.Node node = nodes.item(i);
            if (node.getNodeName().equals("node")) {
                importFreemindNode(dbId, childPos, (Element)node, mmId2dbIdMap, mmLinkMap);
                childPos++;
                //process child element
            } else if (node.getNodeName().equals("arrowlink")) {
                mmLinkMap.put(mmId, ((Element)node).getAttribute("DESTINATION"));
            }
        }

        return dbId;
    }

    public List importFile(Object parentDBId, int pos, String path)
            throws IOException, SAXException, ParserConfigurationException
    {
        File file = new File(path);

        ArrayList newChildren = new ArrayList();

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        //Load and parse XML file into DOM
        Document document = null;
        //DOM parser instance
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        //parse an XML file into a DOM tree
        document = builder.parse(file);


        //get root element
        Element rootElement = document.getDocumentElement();

        HashMap<String, Object> mmId2dbIdMap = new HashMap<String, Object>();
        HashMap<String, String> mmLinkMap = new HashMap<String, String>();

        //traverse child elements
        NodeList nodes = rootElement.getChildNodes();
        for (int i=0; i < nodes.getLength(); i++)
        {
            org.w3c.dom.Node node = nodes.item(i);
            if (node.getNodeName().equals("node")) {
                Object newChild = importFreemindNode(parentDBId, pos, (Element)node, mmId2dbIdMap, mmLinkMap);
                newChildren.add(newChild);
                //process child element
            }
        }

        for(String mmLinkSource : mmLinkMap.keySet()) {
            String mmLinkTarget = mmLinkMap.get(mmLinkSource);

            //s_logger.info("import freemind link {} -> {}", mmLinkSource, mmLinkTarget);
            Object dbLinkSource = mmId2dbIdMap.get(mmLinkSource);
            Object dbLinkTarget = mmId2dbIdMap.get(mmLinkTarget);

            if (dbLinkSource != null && dbLinkTarget != null) {
                //s_logger.info("import link {} -> {}", dbLinkSource, dbLinkTarget);

                Vertex vertexSource = m_mindDb.getVertex(dbLinkSource);
                Vertex vertexTarget = m_mindDb.getVertex(dbLinkTarget);
                m_mindDb.addRefEdge(vertexSource, vertexTarget);
            }
        }

        return newChildren;
    }
}
