package mindworld;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-10-7
 * Time: 上午6:28
 * To change this template use File | Settings | File Templates.
 */
import com.tinkerpop.blueprints.Vertex;
import org.apache.tika.exception.TikaException;
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


import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.*;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Importer {
    static Logger s_logger = LoggerFactory.getLogger(Importer.class);

    MindDB m_mindDb;
    Importer(MindDB mindDB)
    {
        m_mindDb = mindDB;
    }

    private Object importFreemindNode(Object parentDBId, int pos, org.w3c.dom.Element element,
                                    HashMap<String, Object> mmId2dbIdMap,
                                    HashMap<String, String> mmLinkMap)
    {
        String mmId = element.getAttribute("ID");
        String text = element.getAttribute("TEXT");

        s_logger.info("import freemind Nod {} : {}", mmId, text);

        Vertex dbParent = m_mindDb.getVertex(parentDBId);
        MindDB.EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);
        edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, text);

        Object dbId = edgeVertex.m_vertex.getId();

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

    public List importFreemind(Object parentDBId, int pos, final File file)
            throws IOException, SAXException, ParserConfigurationException
    {
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
                m_mindDb.addRefEdge(vertexSource, vertexTarget, MindDB.ADDING_EDGE_END);
            }
        }

        return newChildren;
    }

    List importPlainText(Object parentDBId, int pos, String text)
    {
        ArrayList newChildren = new ArrayList();
        BreakIterator boundary = BreakIterator.getSentenceInstance();
        //text = "发达。发达\n，发达。I am a student. You say:\" hi. hello\"";
        //System.out.println(text);
        boundary.setText(text);

        int start = boundary.first();
        int end = boundary.next();
        Vertex dbParent = m_mindDb.getVertex(parentDBId);

        for (; end != BreakIterator.DONE; start = end, end = boundary.next()) {
            MindDB.EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);
            edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, text.substring(start, end));
            newChildren.add(edgeVertex.m_vertex.getId());
            pos++;
        }

        return newChildren;
    }

    String getPlainTextByTika(File file) throws IOException, TikaException, SAXException
    {
        Detector detector = new DefaultDetector();
        Parser parser = new AutoDetectParser(detector);
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        Metadata metadata = new Metadata();

        TikaInputStream input = TikaInputStream.get(file, metadata);
        String text = null;
        try {
            Writer output = new CharArrayWriter();
            parser.parse(input, new BodyContentHandler(output), metadata, context);
            text = output.toString();
        } finally {
            input.close();
        }

        return text;
    }

    public List importFile(Object parentDBId, final String path) throws Exception
    {
        File file = new File(path);
        Vertex parent = m_mindDb.getVertex(parentDBId);
        int pos = m_mindDb.getChildOrReferentCount(parent);

        if (path.endsWith(".mm")) {
            return importFreemind(parentDBId, pos, file);
        } else {
            String plainText = getPlainTextByTika(file);
            if (plainText != null && !plainText.isEmpty()) {
                return importPlainText(parentDBId, pos, plainText);
            }
            return new ArrayList();
        }
    }

}
