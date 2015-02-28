package eulermind;

import com.orientechnologies.common.listener.OProgressListener;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        File file = new File("/tmp/test/aaa");
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
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

    public static Vertex createTree (MindDB mindDb, int maxLevel) {
        return createTree(mindDb, null, "", 0, maxLevel);

    }

    public static void mkdir(String path) throws IOException
    {
        File file = new File(path);
        FileUtils.forceMkdir(file);
    }

    public static String mindMapNameToUrl(String name)
    {
        String path = Config.MAPS_DIR + File.separator + name;
        return "local:" +  path.replace(File.separatorChar, '/');
    }

    public static boolean mapExist(String name)
    {
        String path = Config.MAPS_DIR + File.separator + name;
        File file = new File(path);
        return file.exists();
    }

    public static String[] getAllMapNames()
    {
        File mapsDir = new File(Config.MAPS_DIR);
        String maps[] = mapsDir.list(DirectoryFileFilter.INSTANCE);
        return maps;
    }


    public static String getLastOpenedMap()
    {
        File file = new File(Config.LAST_OPENED_MAP_RECORD_FILE);
        if (file.isFile()) {
            try {
                String name = FileUtils.readFileToString(file);
                if (mapExist(name)) {
                    return name;
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static void removeMap(String name) {
        String path = Config.MAPS_DIR + File.separator + name;
        File mapDir = new File(path);
        try {
            FileUtils.deleteDirectory(mapDir);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public static void recordLastOpenedMap(String name)
    {
        if (!mapExist(name)) {
            return;
        }

        File file = new File(Config.LAST_OPENED_MAP_RECORD_FILE);
        try {
            FileUtils.writeStringToFile(file, name);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static void initFiles() throws IOException
    {
        Utils.mkdir(Config.MAPS_DIR);
        Style.load();
        Style.save();
    }

    public static Rectangle getScreenBounds(JComponent component) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        GraphicsConfiguration gc = component.getGraphicsConfiguration();
        Rectangle screenBounds;

        if (gc != null) {
            screenBounds = gc.getBounds();
            Insets screenInsets = toolkit.getScreenInsets(gc);

            screenBounds.width -= (screenInsets.left + screenInsets.right);
            screenBounds.height -= (screenInsets.top + screenInsets.bottom);
            screenBounds.x += screenInsets.left;
            screenBounds.y += screenInsets.top;
        }
        else {
            screenBounds = new Rectangle(new Point(0, 0), toolkit.getScreenSize());
        }

        return screenBounds;
    }

    public static String getSystemClipboardText()
    {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        DataFlavor flavor = DataFlavor.stringFlavor;
        if (clipboard.isDataFlavorAvailable(flavor)) {
            try {
                return (String) clipboard.getData(flavor);
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return null;
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return null;
            }
        } else {
            return null;
        }
    }

    public static void copyStringToSystemClipboard(String str)
    {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(str);
        clipboard.setContents(selection, null);
    }

}
