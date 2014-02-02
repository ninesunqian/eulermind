package excitedmind;

import excitedmind.DBTree.EdgeVertex;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

import javax.swing.*;

import com.tinkerpop.blueprints.Vertex;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.util.FontLib;
import prefuse.util.ui.JFastLabel;
import prefuse.util.ui.JSearchPanel;
import prefuse.visual.VisualItem;

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

        String dbPath = "d://tmp/mind_db";

		final String dbUrl = "local:" + dbPath.replace(File.separatorChar, '/');
        m_logger.info ("dbUrl = " + dbUrl);

        deleteDir(dbPath);

		DBTree dbTree = new DBTree (dbUrl);

        dbTree.createFullTextVertexKeyIndex(MindTree.sm_textPropName);


        /*
        {
            for (int i=0; i<10; i++) {
                Vertex a = dbTree.m_graph.addVertex(null);
                a.setProperty(MindTree.sm_textPropName, "abc def");
            }

            for (int i=0; i<10; i++) {
                Vertex a = dbTree.m_graph.addVertex(null);
                a.setProperty(MindTree.sm_textPropName, "abcdef");
            }
        }
        */

		createTree (dbTree, null, "", 0);
        /*
        for (Vertex v : dbTree.getVertices(MindTree.sm_textPropName, "def")) {
            System.out.println("find :" + v + ": " + v.getProperty(MindTree.sm_textPropName));
        }
        */
		dbTree = null;


        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MindmapFrame frame = new MindmapFrame(dbUrl, m_rootVertex.getId());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);

            }
        });
	}
	
	static private Vertex m_rootVertex;
	static private void createTree (DBTree dbTree, Vertex parent, String parentText, int level)
	{
		if (level >= 3)
		{
			return;
			
		} else if (level == 0) {
			Vertex root = dbTree.addRoot();
			root.setProperty(MindTree.sm_textPropName, "a");
			m_rootVertex = root;
			
			createTree (dbTree, root, "a", 1);
			
		} else {
			
			EdgeVertex edgeVertex = dbTree.addChild(parent, 0);
			edgeVertex.m_vertex.setProperty(MindTree.sm_textPropName, parentText + "a");
			createTree (dbTree, edgeVertex.m_vertex, parentText + "a", level + 1);
			
			edgeVertex = dbTree.addChild(parent, 1);
			edgeVertex.m_vertex.setProperty(MindTree.sm_textPropName, parentText + "b");
			createTree (dbTree, edgeVertex.m_vertex, parentText + "b", level + 1);
			
			edgeVertex = dbTree.addChild(parent, 2);
			edgeVertex.m_vertex.setProperty(MindTree.sm_textPropName, parentText + "c");
			createTree (dbTree, edgeVertex.m_vertex, parentText + "c", level + 1);
			
			dbTree.addRefEdge(parent, m_rootVertex, 3);
		}
	}

    static class MindmapFrame extends JFrame {
        public MindmapFrame(String dbUrl, Object rootId) {

            JMenuBar menuBar = new JMenuBar();
            setJMenuBar(menuBar);
            JMenu fileMenu = new JMenu("文件");
            fileMenu.setMnemonic('F');
            menuBar.add(fileMenu);

            JMenuItem openMenuItem = new JMenuItem("open", KeyEvent.VK_O);
            fileMenu.add(openMenuItem);

            setJMenuBar(menuBar);

            Color BACKGROUND = Color.WHITE;
            Color FOREGROUND = Color.BLACK;

            final String label = MindTree.sm_textPropName;

            // create a new treemap
            final MindView mindView = new MindView (dbUrl, rootId);
            mindView.setBackground(BACKGROUND);
            mindView.setForeground(FOREGROUND);


            Box box = new Box(BoxLayout.X_AXIS);
            box.add(Box.createHorizontalStrut(10));
            box.add(Box.createHorizontalGlue());
            box.add(Box.createHorizontalStrut(3));
            box.setBackground(BACKGROUND);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(BACKGROUND);
            panel.setForeground(FOREGROUND);
            panel.add(mindView, BorderLayout.CENTER);
            panel.add(box, BorderLayout.SOUTH);
            add(panel);
        }
    }
} // end of class TreeMap
