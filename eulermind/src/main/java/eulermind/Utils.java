package eulermind;

import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 15-1-13
 * Time: 下午9:35
 * To change this template use File | Settings | File Templates.
 */
public class Utils {

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

    static private Vertex createTree (MindDB mindDb, Vertex parent, String parentText, int level, int maxLevel)
    {
        if (level >= maxLevel)
        {
            return null;

        }

        if (level == 0) {
            Vertex root = mindDb.getVertex(mindDb.getRootId());
            root.setProperty(MindModel.sm_textPropName, "a");
            parent = root;

        } else {
            MindDB.EdgeVertex edgeVertex = mindDb.addChild(parent, 0);
            edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, parentText + "a");

            edgeVertex = mindDb.addChild(parent, 1);
            edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, parentText + "b");

            edgeVertex = mindDb.addChild(parent, 2);
            edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, parentText + "c");

            mindDb.addRefEdge(parent, parent, 3);
        }

        createTree (mindDb, parent, "a", 1, maxLevel);
        createTree (mindDb, parent, "b", 1, maxLevel);
        createTree (mindDb, parent, "c", 1, maxLevel);

        return parent;
    }

    static public Vertex createTree (MindDB mindDb, int maxLevel) {
        return createTree(mindDb, null, "", 0, maxLevel);

    }
}
