package eulermind;

import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import eulermind.MindDB.EdgeVertex;

import java.awt.*;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import com.tinkerpop.blueprints.Vertex;


import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/*
The MIT License (MIT)
Copyright (c) 2012-2014 wangxuguang ninesunqian@163.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

public class Mindmap {
    static Logger m_logger = LoggerFactory.getLogger(Mindmap.class);

    public static void deleteDir(String path)
    {
        File file = new File(path);
        file.setWritable(true);
        if (file.exists())
        {
            if (file.isDirectory())
            {
                File[] files = file.listFiles();
                for (File subFile : files)
                {
                    if (subFile.isDirectory())
                        deleteDir(subFile.getPath());
                    else {
                        subFile.setWritable(true);
      //                  boolean ret = subFile.delete();
       //                 s_logger.info("delete file: " + ret);

                        try {
                            Files.delete(Paths.get(subFile.getPath()));
                        } catch (NoSuchFileException x) {
                            System.err.format("%s: no such" + " file or directory%n", path);
                        } catch (DirectoryNotEmptyException x) {
                            System.err.format("%s not empty%n", path);
                        } catch (IOException x) {
                            // File permission problems are caught here.
                            System.err.println(x);
                        }
                    }
                }
            }
            file.delete();
        }
    }


    static void testXML()
    {

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        //Load and parse XML file into DOM
        Document document = null;
        try {
            //DOM parser instance
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            //parse an XML file into a DOM tree
            document = builder.parse(new File("/home/wangxuguang/excitedmind尽快可用.mm"));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //get root element
        Element rootElement = document.getDocumentElement();

        //traverse child elements
        NodeList nodes = rootElement.getChildNodes();
        for (int i=0; i < nodes.getLength(); i++)
        {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                //process child element
            }
        }

        NodeList nodeList = rootElement.getElementsByTagName("node");
        if(nodeList != null)
        {
            for (int i = 0 ; i < nodeList.getLength(); i++)
            {
                Element element = (Element)nodeList.item(i);
                String id = element.getAttribute("ID");
                String text = element.getAttribute("TEXT");
                m_logger.info("freemind node {}: {}", id, text);
            }
        }
    }

    static void testJava()
    {
        deleteDir("/tmp/test/aaa");
        String pathToDatabase = "plocal:/tmp/test/aaa";

        OrientTransactionalGraph graph = new OrientGraph(pathToDatabase, false);

        OrientVertexType type = graph.getVertexBaseType();
        type.createProperty("text", OType.STRING);
        OProgressListener oProgressListener = null;
        type.createIndex("textIndex", "FULLTEXT", oProgressListener, null, "LUCENE", new String[]{"text"});

        Vertex v0 = graph.addVertex(null, "text", "我们是好人" );
        Vertex v1 = graph.addVertex(null, "text", "Rome");
        v1.setProperty("text", "我们不是好人");

        graph.addEdge(null, v0, v1, "E");

        graph.commit();

        v0 = graph.getVertex(v0.getId());
        v1 = graph.getVertex(v1.getId());
        Vertex v2 = graph.addVertex(null, "text", "我们是好人-2" );
        Vertex v3 = graph.addVertex(null, "text", "Rome");
        graph.addEdge(null, v0, v2, "E");
        graph.addEdge(null, v0, v3, "E");

        v3.setProperty("text", "我们不是好人-2");
        System.out.println(graph.isUseClassForEdgeLabel());
        System.out.println(graph.isUseClassForVertexLabel());
        graph.commit();

        Iterable<Vertex> vertexes = graph.getVertices("V", new String[]{"text"}, new Object[]{"(我们 好人)"});
        for (Vertex v : vertexes) {
            System.out.println(v.getId());
            System.out.println(v.getProperty("text"));
        }

        graph.shutdown();
        /*
 OrientVertexType type = graph.createVertexType("City");
    type.createProperty("latitude", OType.DOUBLE);
    type.createProperty("longitude", OType.DOUBLE);
    type.createProperty("name", OType.STRING);

    ODocument metadata = new ODocument();
    metadata.field("analyzer", "org.apache.lucene.analysis.en.EnglishAnalyzer");
    type.createIndex("City.name", "FULLTEXT", null, metadata, "LUCENE", new String[] { "name" });

    graph.addVertex("class:City", new Object[] { "name", "London" });
    graph.addVertex("class:City", new Object[] { "name", "Rome" });

    graph.commit();
    Iterable<Vertex> vertexes = graph.getVertices("City.name", "London");
    for (Vertex v : vertexes) {
      System.out.println(v.getId());
    }
    graph.shutdown();
    */

    }

	public static void main(String argv[]) {
        /*
        testJava();
        testXML();
        */

        try {
            //UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        String dbPath = System.getProperty("user.home") + "/.eulermind/mind_db";
        //TODO: for debug
        deleteDir(dbPath);

		final String dbUrl = "local:" + dbPath.replace(File.separatorChar, '/');
        m_logger.info ("dbUrl = " + dbUrl);

		MindDB mindDb = new MindDB(dbUrl);

		mindDb = null;

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame frame = new MainFrame(dbUrl);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();

            }
        });
	}
	
	static private Vertex m_rootVertex;

	static private void createTree (MindDB mindDb, Vertex parent, String parentText, int level)
	{
		if (level >= 3)
		{
			return;
			
		} else if (level == 0) {
			Vertex root = mindDb.getVertex(mindDb.getRootId());
			root.setProperty(MindModel.sm_textPropName, "a");
			m_rootVertex = root;
			
			createTree (mindDb, root, "a", 1);
			
		} else {

			EdgeVertex edgeVertex = mindDb.addChild(parent, 0);
			edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, parentText + "a");
			createTree (mindDb, edgeVertex.m_vertex, parentText + "a", level + 1);

			edgeVertex = mindDb.addChild(parent, 1);
			edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, parentText + "b");
			createTree (mindDb, edgeVertex.m_vertex, parentText + "b", level + 1);
			
			edgeVertex = mindDb.addChild(parent, 2);
			edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, parentText + "c");
			createTree (mindDb, edgeVertex.m_vertex, parentText + "c", level + 1);
			
			mindDb.addRefEdge(parent, m_rootVertex, 3);
		}
	}

} // end of class TreeMap
