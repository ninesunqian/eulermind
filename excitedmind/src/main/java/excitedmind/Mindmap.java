package excitedmind;

import excitedmind.MindDB.EdgeVertex;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.*;

import com.tinkerpop.blueprints.Vertex;

import javax.swing.UIManager.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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


        String dbPath = "d://tmp/mind_db";

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
                MindmapFrame frame = new MindmapFrame(dbUrl, m_rootVertex.getId());
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

            final String label = MindModel.sm_textPropName;

            final MindModel mindModel = new MindModel(dbUrl);

            final JTabbedPane tabbedPane = new JTabbedPane();
            final MindController mindController = new MindController(mindModel, tabbedPane);
            mindController.findOrAddMindView(m_rootVertex.getId());
            mindController.findOrAddMindView(m_rootVertex1.getId());
            tabbedPane.addChangeListener(new ChangeListener()
            {
                public void stateChanged(ChangeEvent e)
                {
                    Component comp = tabbedPane.getSelectedComponent();
                    comp.requestFocusInWindow();
                }
            });

            Box box = new Box(BoxLayout.X_AXIS);
            box.add(Box.createHorizontalStrut(10));
            box.add(Box.createHorizontalGlue());
            box.add(Box.createHorizontalStrut(3));
            box.setBackground(BACKGROUND);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(BACKGROUND);
            panel.setForeground(FOREGROUND);
            panel.add(tabbedPane, BorderLayout.CENTER);
            panel.add(box, BorderLayout.SOUTH);

            MindIcons mindIcons = new MindIcons(mindController);
           // panel.add(mindIcons.getToolbar(), BorderLayout.WEST);
            panel.add(new MindToolBar(mindModel), BorderLayout.NORTH);
            add(panel);
        }
    }
} // end of class TreeMap
