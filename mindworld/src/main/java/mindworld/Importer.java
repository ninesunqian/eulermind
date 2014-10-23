package mindworld;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-10-7
 * Time: 上午6:28
 * To change this template use File | Settings | File Templates.
 */
import com.ibm.icu.text.BreakIterator;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.tinkerpop.blueprints.Vertex;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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

    private Object addTextDBChild(Object parentDBId, int pos, String text)
    {
        Vertex dbParent = m_mindDb.getVertex(parentDBId);
        MindDB.EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);
        edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, text);
        return edgeVertex.m_vertex.getId();

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

    List importFreemind(Object parentDBId, int pos, final File file)
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

    private  class LineNode extends DefaultMutableTreeNode{
        LineNode(String line) {
            super();

            for (m_indent=0; m_indent<line.length() && Character.isWhitespace(line.charAt(m_indent)); m_indent++) {
            }

            for (m_tail=line.length(); m_tail>m_indent && Character.isWhitespace(line.charAt(m_tail-1)); m_tail--) {
            }

            m_trimLine = line.substring(m_indent, m_tail);
        }

        int m_indent;
        int m_tail;
        String m_trimLine;
    }

    private LineNode plainText2TreeModel(String rootText, String text)
    {
        LineNode root = new LineNode(rootText);
        LineNode prevNode = root;

        //String lines[] = text.split("[\n\r\f\u2029\u2028\u0003]");
        String lines[] = text.split("[\r\f]");

        for (String line : lines) {

            LineNode curNode = new LineNode(line);

            if (curNode.m_trimLine.length() == 0) {
                continue;
            }

            LineNode parent = prevNode;
            while (parent.m_indent >= curNode.m_indent && parent != root ) {
                parent = (LineNode)parent.getParent();
            }

            parent.add(curNode);

            prevNode = curNode;
        }

        return root;
    }

    private Object importLineNode(Object parentDBId, int pos, LineNode root)
    {
        BreakIterator boundary = BreakIterator.getSentenceInstance();
        boundary.setText(root.m_trimLine);

        int start = boundary.first();
        int end = boundary.next();
        Vertex dbParent = m_mindDb.getVertex(parentDBId);

        Object dbId;

        int childPos = 0;

        if (end == root.m_trimLine.length()) {
           dbId = addTextDBChild(parentDBId, pos, root.m_trimLine);
           s_logger.info("import line {}", root.m_trimLine);


        } else {
            dbId = addTextDBChild(parentDBId, pos, "paragraph");

            for (; end != BreakIterator.DONE; start = end, end = boundary.next()) {
                LineNode lineNode = new LineNode(root.m_trimLine.substring(start, end));

                if (lineNode.m_trimLine.length() == 0) {
                    continue;
                }

                addTextDBChild(dbId, childPos, lineNode.m_trimLine);

                s_logger.info("import sentence {}", lineNode.m_trimLine);
                childPos++;
            }
        }

        for (int i=0; i<root.getChildCount(); i++) {
            importLineNode(dbId, childPos, (LineNode)root.getChildAt(i));
            childPos++;
        }

        return dbId;
    }

    //TODO: return List -> return dbId
    List importPlainText(Object parentDBId, int pos, String text, String rootText)
    {
        LineNode root = plainText2TreeModel(rootText, text);
        Object dbId = importLineNode(parentDBId, pos, root);
        List list = new ArrayList();
        list.add(dbId);
        return list;
    }

    String getPlainTextByTika(File file) throws IOException, TikaException, SAXException
    {
        Detector detector = new DefaultDetector();
        Parser parser = new AutoDetectParser(detector);
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, file.getName());

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

    List importHtml(Object parentDBId, int pos, String text)
    {
        return null;
    }


    public List importFile(Object parentDBId, final String path) throws Exception
    {
        //OGlobalConfiguration.FILE_MMAP_AUTOFLUSH_UNUSED_TIME.setValue(100);
        File file = new File(path);
        Vertex parent = m_mindDb.getVertex(parentDBId);
        int pos = m_mindDb.getChildOrReferentCount(parent);

        if (path.endsWith(".mm")) {
            return importFreemind(parentDBId, pos, file);
        } else {
            String plainText = getPlainTextByTika(file);
            if (plainText != null && !plainText.isEmpty()) {
                return importPlainText(parentDBId, pos, plainText, file.getName());
            }
            return new ArrayList();
        }
    }

}
