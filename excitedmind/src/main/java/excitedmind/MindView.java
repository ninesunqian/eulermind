package excitedmind;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;

import excitedmind.operators.EditAction;
import excitedmind.operators.RemoveAction;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.distortion.Distortion;
import prefuse.action.distortion.FisheyeDistortion;
import prefuse.action.layout.Layout;
import prefuse.controls.AnchorUpdateControl;
import prefuse.controls.ControlAdapter;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.util.ColorLib;
import prefuse.util.PrefuseLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.TreeDepthItemSorter;
import prefuse.visual.tuple.TableNodeItem;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindView extends Display {

	public final String m_treeGroupName = "tree";
	public final String m_treeNodesGroupName;
	public final String m_treeEdgesGroupName;

	private MindTree m_mindTree;
	private TableNodeItem m_curFocus;

	MindTreeRenderEngine m_renderEngine;

	public MindView(String path, Object rootId) {
		super(new Visualization());
		setSize(700, 600);

		setHighQuality(true);

		m_mindTree = new MindTree(path, rootId);
		m_treeNodesGroupName = PrefuseLib.getGroupName(m_treeGroupName,
				Graph.NODES);
		m_treeEdgesGroupName = PrefuseLib.getGroupName(m_treeGroupName,
				Graph.EDGES);

		m_vis.add(m_treeGroupName, m_mindTree.m_tree);
		setItemSorter(new TreeDepthItemSorter());

		m_renderEngine = new MindTreeRenderEngine(this, m_treeGroupName);

		setMouseControlListener();
		setKeyControlListener();

		renderTree();
	}

	public boolean isNode(VisualItem item) {
		return item.isInGroup(m_treeNodesGroupName);
	}

	public boolean isEdge(VisualItem item) {
		return item.isInGroup(m_treeEdgesGroupName);
	}

	public void renderTree() {
		m_renderEngine.run();
	}

	boolean m_needPan;

	private void setMouseControlListener() {
		addControlListener(new ZoomToFitControl());
		addControlListener(new ZoomControl());
		addControlListener(new WheelZoomControl());
		addControlListener(new PanControl());
		addControlListener(new ControlAdapter() {

			public void itemEntered(VisualItem item, MouseEvent e) {
				if (isNode(item)) {
					m_curFocus = (TableNodeItem) item;
					renderTree();

                    /*
					Rectangle2D bounds = item.getBounds();
					MindTreeLayout.Params params = MindTreeLayout
							.getParams((NodeItem) item);
					System.out.println("    mod = " + params.mod);
					System.out.println("    prelim = " + params.prelim);
					System.out.println("    breadth = " + params.breadth);
					System.out.println("    bounds.y = " + bounds.getY());
                    */

				}

			}

			public void itemClicked(VisualItem item, MouseEvent e) {
				System.out.println("mouse Clicked");

				if (isNode(item)) {
					m_curFocus = (TableNodeItem) item;

					m_renderEngine.holdItem(item);

					m_mindTree.ToggleFoldNode(item);
					renderTree();

				}
			}
		});
	}

	public void setKeyControlListener() {
		registerKeyboardAction(new EditAction(this), "edit",
				KeyStroke.getKeyStroke("F2"), WHEN_FOCUSED);
		registerKeyboardAction(new RemoveAction(this), "remove",
				KeyStroke.getKeyStroke("DELETE"), WHEN_FOCUSED);

		registerKeyboardAction(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_undoManager.canUndo())
					m_undoManager.undo();

			}
		}, "back", KeyStroke.getKeyStroke("F3"), WHEN_FOCUSED);

		registerKeyboardAction(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_undoManager.canRedo())
					m_undoManager.redo();

			}
		}, "redo", KeyStroke.getKeyStroke("F4"), WHEN_FOCUSED);
	}

	private UndoManager m_undoManager = new UndoManager();

	public UndoManager getUndoManager() {
		return m_undoManager;
	}

	public MindTree getMindTree() {
		return m_mindTree;
	}

	public TableNodeItem getFocusNode() {
		return m_curFocus;
	}

} // end of class TreeMap
