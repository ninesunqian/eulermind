package excitedmind;

import excitedmind.DBTree.EdgeVertex;
import groovy.util.logging.Log4j;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

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

	public static void main(String argv[]) {
		
		String dbUrl = "local://tmp/mind_db";
		
        Runtime rt =Runtime.getRuntime();
        String str[ ] = {"/bin/rm","-rf","/tmp/mind_db"};
        try {
			rt.exec(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		DBTree dbTree = new DBTree (dbUrl);
		
		createTree (dbTree, null, "", 0);
		dbTree = null;
		
		
		JComponent Mindmap = demo(dbUrl, m_rootVertex.getId());
		
		
		JFrame frame = new JFrame("mindmap core");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(Mindmap);
		frame.pack();
		frame.setVisible(true);
	}
	
	static private Vertex m_rootVertex;
	static private void createTree (DBTree dbTree, Vertex parent, String parentText, int level)
	{
		if (level >= 4)
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

	public static JComponent demo(String dbUrl, Object rootId) {
		Color BACKGROUND = Color.WHITE;
		Color FOREGROUND = Color.BLACK;
		
		final String label = MindTree.sm_textPropName;
		
		// create a new treemap
		final MindView mindView = new MindView (dbUrl, rootId);
		mindView.setBackground(BACKGROUND);
		mindView.setForeground(FOREGROUND);

		// create a search panel for the tree map
		/*
		JSearchPanel search = new JSearchPanel(mindView.getVisualization(),
				MindView.sm_treeNodesGroupName, Visualization.SEARCH_ITEMS, label, true, true);
		search.setShowResultCount(true);
		search.setBorder(BorderFactory.createEmptyBorder(5, 5, 4, 0));
		search.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 11));
		search.setBackground(BACKGROUND);
		search.setForeground(FOREGROUND);
		*/

		final JFastLabel title = new JFastLabel("                 ");
		title.setPreferredSize(new Dimension(350, 20));
		title.setVerticalAlignment(SwingConstants.BOTTOM);
		title.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
		title.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 16));
		title.setBackground(BACKGROUND);
		title.setForeground(FOREGROUND);

		mindView.addControlListener(new ControlAdapter() {
			public void itemEntered(VisualItem item, MouseEvent e) {
				if (item.canGetString(label))
					title.setText(item.getString(label));
					
			}

			public void itemExited(VisualItem item, MouseEvent e) {
				title.setText(null);
			}
		});

		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalStrut(10));
		box.add(title);
		box.add(Box.createHorizontalGlue());
		//box.add(search);
		box.add(Box.createHorizontalStrut(3));
		box.setBackground(BACKGROUND);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(BACKGROUND);
		panel.setForeground(FOREGROUND);
		panel.add(mindView, BorderLayout.CENTER);
		panel.add(box, BorderLayout.SOUTH);
		return panel;
	}
} // end of class TreeMap
