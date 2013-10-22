package excitedmind;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

import excitedmind.operators.EditAction;
import excitedmind.operators.SimpleMindTreeAction;
import prefuse.Display;
import prefuse.Visualization;

import prefuse.controls.ControlAdapter;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.TreeDepthItemSorter;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindView extends Display {

	private VisualMindTree m_visMindTree;

	MindTreeRenderEngine m_renderEngine;

	public MindView(String path, Object rootId) {
		super(new Visualization());
		setSize(700, 600);

		setHighQuality(true);

		m_visMindTree = new VisualMindTree(path, rootId, m_vis);

		setItemSorter(new TreeDepthItemSorter());

		m_renderEngine = new MindTreeRenderEngine(this, VisualMindTree.sm_treeGroupName);

		setMouseControlListener();
		setKeyControlListener();

		renderTree();
	}

	public void renderTree() {
		m_renderEngine.run();
	}

	private void setMouseControlListener() {
		addControlListener(new ZoomToFitControl());
		addControlListener(new ZoomControl());
		addControlListener(new WheelZoomControl());
		addControlListener(new PanControl());
		addControlListener(new ControlAdapter() {

			public void itemEntered(VisualItem item, MouseEvent e) {
				if (m_visMindTree.isNode(item)) {
                    m_visMindTree.setCursor((NodeItem)item);
					renderTree();
				}

			}

			public void itemClicked(VisualItem item, MouseEvent e) {
				System.out.println("mouse Clicked");

				if (m_visMindTree.isNode(item)) {

					//FIXME:
					// m_renderEngine.holdItem(item);

					m_visMindTree.ToggleFoldNode((NodeItem)item);
					renderTree();
				}
			}
		});
	}

	public void setKeyControlListener() {
		registerKeyboardAction(new EditAction(this), "edit",
				KeyStroke.getKeyStroke("F2"), 0);

		registerKeyboardAction(new SimpleMindTreeAction(this) {
                    @Override
                    public AbstractUndoableEdit operateMindTree(ActionEvent e) {
                        return m_visMindTree.removeCursorNodeUndoable();
                    }
        }, "remove", KeyStroke.getKeyStroke("D"), 0);

		registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_undoManager.canUndo()) {
					m_undoManager.undo();
                    renderTree();
                }

			}
		}, "undo", KeyStroke.getKeyStroke("U"), 0);

		registerKeyboardAction(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_undoManager.canRedo()) {
					m_undoManager.redo();
                    renderTree();
                }
			}
		}, "redo", KeyStroke.getKeyStroke("R"), 0);

        registerKeyboardAction(new SimpleMindTreeAction(this) {
                    @Override
                    public AbstractUndoableEdit operateMindTree(ActionEvent e) {
                        return m_visMindTree.addChild();
                    }
        }, "add_child", KeyStroke.getKeyStroke("I"), 0);
	}

	private UndoManager m_undoManager = new UndoManager();

	public UndoManager getUndoManager() {
		return m_undoManager;
	}

	public VisualMindTree getVisMindTree() {
		return m_visMindTree;
	}

} // end of class TreeMap
