package excitedmind;

import excitedmind.MindDB.EdgeVertex;

import java.awt.*;
import java.awt.event.KeyEvent;
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

        String dbPath = "d://tmp/mind_db";

		final String dbUrl = "local:" + dbPath.replace(File.separatorChar, '/');
        m_logger.info ("dbUrl = " + dbUrl);

        deleteDir(dbPath);

		MindDB mindDb = new MindDB(dbUrl);

        mindDb.createFullTextVertexKeyIndex(MindTree.sm_textPropName);

		createTree (mindDb, null, "", 0);
		mindDb = null;


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
	static private void createTree (MindDB mindDb, Vertex parent, String parentText, int level)
	{
		if (level >= 3)
		{
			return;
			
		} else if (level == 0) {
			Vertex root = mindDb.addRoot();
			root.setProperty(MindTree.sm_textPropName, "a");
			m_rootVertex = root;
			
			createTree (mindDb, root, "a", 1);
			
		} else {
			
			EdgeVertex edgeVertex = mindDb.addChild(parent, 0);
			edgeVertex.m_vertex.setProperty(MindTree.sm_textPropName, parentText + "a");
			createTree (mindDb, edgeVertex.m_vertex, parentText + "a", level + 1);
			
			edgeVertex = mindDb.addChild(parent, 1);
			edgeVertex.m_vertex.setProperty(MindTree.sm_textPropName, parentText + "b");
			createTree (mindDb, edgeVertex.m_vertex, parentText + "b", level + 1);
			
			edgeVertex = mindDb.addChild(parent, 2);
			edgeVertex.m_vertex.setProperty(MindTree.sm_textPropName, parentText + "c");
			createTree (mindDb, edgeVertex.m_vertex, parentText + "c", level + 1);
			
			mindDb.addRefEdge(parent, m_rootVertex, 3);
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

            final MindModel mindModel = new MindModel(dbUrl);

            final MindUndoManager undoManager = new MindUndoManager(mindModel) {
                @Override
                public void exposeMindView(MindView mindView) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            };

            // create a new treemap
            final MindView mindView = new MindView(mindModel, undoManager, rootId);
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
