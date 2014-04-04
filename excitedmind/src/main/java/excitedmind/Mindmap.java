package excitedmind;

import excitedmind.MindDB.EdgeVertex;

import java.awt.*;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.*;

import com.tinkerpop.blueprints.Vertex;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Mindmap {
    static Logger m_logger = Logger.getLogger(Mindmap.class.getName());

    public static void deleteDir(String path)
    {
        File file = new File(path);
        if (file.exists())
        {
            if (file.isDirectory())
            {
                File[] files = file.listFiles();
                for (File subFile : files)
                {
                    if (subFile.isDirectory())
                        deleteDir(subFile.getPath());
                    else
                        subFile.delete();
                }
            }
            file.delete();
        }
    }

	public static void main(String argv[]) {

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        String dbPath = System.getProperty("user.home") + "/.excitedmind/mind_db";

		final String dbUrl = "local:" + dbPath.replace(File.separatorChar, '/');
        m_logger.info ("dbUrl = " + dbUrl);

        deleteDir(dbPath);

		MindDB mindDb = new MindDB(dbUrl);

        mindDb.createFullTextVertexKeyIndex(MindModel.sm_textPropName);

		createTree (mindDb, null, "", 0);
		mindDb = null;


        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame frame = new MainFrame(dbUrl, m_rootVertex.getId());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);

            }
        });
	}
	
	static private Vertex m_rootVertex;
    static private Vertex m_rootVertex1;

	static private void createTree (MindDB mindDb, Vertex parent, String parentText, int level)
	{
		if (level >= 3)
		{
			return;
			
		} else if (level == 0) {
			Vertex root = mindDb.addRoot();
			root.setProperty(MindModel.sm_textPropName, "a");
			m_rootVertex = root;
			
			createTree (mindDb, root, "a", 1);
			
		} else {

			EdgeVertex edgeVertex = mindDb.addChild(parent, 0);
			edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, parentText + "a");
			createTree (mindDb, edgeVertex.m_vertex, parentText + "a", level + 1);
            if (level == 1) {
                m_rootVertex1 = edgeVertex.m_vertex;
            }

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
