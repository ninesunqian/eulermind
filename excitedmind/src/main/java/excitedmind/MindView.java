package excitedmind;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.tinkerpop.blueprints.Vertex;
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

    Logger m_logger = Logger.getLogger(this.getClass().getName());

	private VisualMindTree m_visMindTree;

	MindTreeRenderEngine m_renderEngine;
    NodeItem m_clickedNode;

    static enum State {NORMAL, LINKING, MOVING};

    EditAction m_editAction = new EditAction(this);

    private JList m_promptList = new JList(new DefaultListModel());
    private JScrollPane m_promptScrollPane = new JScrollPane(m_promptList);

    final static String sm_editActionName = "edit";
    final static String sm_removeActionName = "remove";
    final static String sm_undoActionName = "undo";
    final static String sm_redoActionName = "redo";
    final static String sm_addChildActionName = "addChild";
    final static String sm_addLinkActionName = "addLink";
    final static String sm_moveActionName = "move";
    final static String sm_prepareLinkActionName = "prepareLink";
    final static String sm_prepareMoveActionName = "prepareMove";
    final static String sm_toNormalActionAction = "toNormal";

    AbstractAction m_removeAction = new SimpleMindTreeAction(this) {
        @Override
        public AbstractUndoableEdit operateMindTree(ActionEvent e) {
            return m_visMindTree.removeCursorNode();
        }
    };

    AbstractAction m_undoAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (m_undoManager.canUndo()) {
                m_undoManager.undo();
                renderTree();
            }
        }
    };

    AbstractAction m_redoAction = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (m_undoManager.canRedo()) {
                m_undoManager.redo();
                renderTree();
            }
        }
    };

    AbstractAction m_addChildAction = new AbstractAction() {

        //TODO:
        @Override
        public void actionPerformed(ActionEvent e) {
            m_visMindTree.addPlaceholder();
            renderTree();
        }
    };


    AbstractAction m_prepareLinkAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            translate (State.LINKING);
        }
    };

    AbstractAction m_prepareMoveAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            translate (State.MOVING);
        }
    };

    AbstractAction m_toNormalAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            translate (State.NORMAL);
        }
    };

    ActionMap m_mindActionMap = new ActionMap();

    private void setEnabledAllMindActions(boolean enabled)
    {
        Object [] keys = m_mindActionMap.keys();
        for (Object key : keys) {
            m_mindActionMap.get(key).setEnabled(enabled);
        }
    }

    private void setEnabledMindActions(String keys[], boolean enabled)
    {
        for (Object key : keys) {
            m_mindActionMap.get(key).setEnabled(enabled);
        }
    }

    State m_state = State.NORMAL;

    void translate(State newState)
    {
        if (m_state == newState) {
            return;
        }

        m_state = newState;

        switch (newState) {
            case NORMAL:
                setEnabledAllMindActions(true);
                break;
            case LINKING:
                setEnabledAllMindActions(false);
                setEnabledMindActions(new String []{sm_toNormalActionAction}, true);
                break;
            case MOVING:
                setEnabledAllMindActions(false);
                setEnabledMindActions(new String []{sm_toNormalActionAction}, true);
                break;
        }

    }

	public MindView(String path, Object rootId) {
		super(new Visualization());
		setSize(700, 600);

		setHighQuality(true);

		m_visMindTree = new VisualMindTree(path, rootId, m_vis);

        m_promptScrollPane.setVisible(false);
        add(m_promptScrollPane);

        m_promptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_promptList.setLayoutOrientation(JList.VERTICAL);
        m_promptList.setPrototypeCellValue("WWW");
        m_promptList.setVisibleRowCount(8);

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
                    if (m_state == State.NORMAL) {
                        m_visMindTree.setCursor((NodeItem)item);
                        renderTree();
                    }
				}
			}

			public void itemClicked(VisualItem item, MouseEvent e) {
				m_logger.info("mouse Clicked");

				if (m_visMindTree.isNode(item)) {
                    m_clickedNode = (NodeItem)item;

					m_renderEngine.holdItem(item);

                    AbstractUndoableEdit undoer;

                    switch (m_state) {
                        case NORMAL:
                            undoer = m_visMindTree.ToggleFoldNode();
                            break;
                        case LINKING:
                            undoer = m_visMindTree.addReference(m_clickedNode);
                            break;
                        case MOVING:
                            undoer = m_visMindTree.resetParent(m_clickedNode);
                            break;
                        default:
                            assert(false);
                            undoer = null;
                    }
                    m_undoManager.addEdit(undoer);

					renderTree();
				}
			}
		});
	}

	public void setKeyControlListener() {
        m_mindActionMap.put(sm_editActionName, m_editAction);
        m_mindActionMap.put(sm_removeActionName, m_removeAction);
        m_mindActionMap.put(sm_addChildActionName, m_addChildAction);

        m_mindActionMap.put(sm_undoActionName, m_undoAction);
        m_mindActionMap.put(sm_redoActionName, m_redoAction);

        m_mindActionMap.put(sm_prepareLinkActionName, m_prepareLinkAction);
        m_mindActionMap.put(sm_prepareMoveActionName, m_prepareMoveAction);
        m_mindActionMap.put(sm_toNormalActionAction, m_toNormalAction);


        ActionMap defaultActionMap = getActionMap();
        m_mindActionMap.setParent(defaultActionMap);
        setActionMap(m_mindActionMap);

        InputMap inputMap = getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("F2"), sm_editActionName);
        inputMap.put(KeyStroke.getKeyStroke('d'), sm_removeActionName);
        inputMap.put(KeyStroke.getKeyStroke('i'), sm_addChildActionName);

        inputMap.put(KeyStroke.getKeyStroke('u'), sm_undoActionName);
        inputMap.put(KeyStroke.getKeyStroke('r'), sm_redoActionName);
        inputMap.put(KeyStroke.getKeyStroke('l'), sm_prepareLinkActionName);
        inputMap.put(KeyStroke.getKeyStroke('m'), sm_prepareMoveActionName);
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), sm_toNormalActionAction);
	}

	private UndoManager m_undoManager = new UndoManager();

	public UndoManager getUndoManager() {
		return m_undoManager;
	}

	public VisualMindTree getVisMindTree() {
		return m_visMindTree;
	}

    @Override
    public void editText(String txt, Rectangle r) {
        super.editText(txt, r);
        JTextComponent editor = getTextEditor();

        m_promptScrollPane.setLocation(editor.getX(), editor.getY()+editor.getHeight());
        m_promptScrollPane.setSize(100, 100);
        m_promptScrollPane.setVisible(true);

        editor.getDocument().addDocumentListener(m_editTextListener);
    }

    @Override
    public void stopEditing() {
        super.stopEditing();
        m_promptScrollPane.setVisible(false);

        JTextComponent editor = getTextEditor();
        editor.getDocument().removeDocumentListener(m_editTextListener);
    }

    public class QueriedNode {
        Object m_dbId;
        String m_text;
        Object m_parentDBId;
        String m_parentText;

        QueriedNode (Vertex vertex)
        {
            m_dbId = vertex.getId();
            m_text = vertex.getProperty(MindTree.sm_textPropName);

            DBTree.EdgeVertex edgeVertex = m_visMindTree.m_dbTree.getParent(vertex);
            m_parentDBId = edgeVertex.m_vertex.getId();
            m_parentText = edgeVertex.m_vertex.getProperty(MindTree.sm_textPropName);
        }
    }

    class QueryWorker extends SwingWorker<Boolean, QueriedNode>
    {
        @Override
        protected Boolean doInBackground() {
            ((DefaultListModel) m_promptList.getModel()).removeAllElements();

            String inputed = getTextEditor().getText();

            m_logger.info("query vertex: " + inputed);

            for (Vertex vertex : m_visMindTree.m_dbTree.getVertices(MindTree.sm_textPropName, inputed)) {
                publish(new QueriedNode(vertex));

                if (isCancelled()) {
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void process(List<QueriedNode> queriedNodes) {
            DefaultListModel listModel = (DefaultListModel) m_promptList.getModel();

            for (QueriedNode queriedNode : queriedNodes) {
                m_logger.info("get queriedNode " + queriedNode.m_dbId);
                listModel.addElement(queriedNode.m_parentText + " -> " + queriedNode.m_text);
            }
        }
    };

    SwingWorker<Boolean, QueriedNode> m_queryWorker;

    DocumentListener m_editTextListener = new DocumentListener() {

        private void restartQueryWorker()
        {
            if (m_queryWorker != null) {
                m_queryWorker.cancel(false);
            }

            m_queryWorker = new QueryWorker();
            m_queryWorker.execute();
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            m_logger.info("insert update");
            restartQueryWorker();
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            m_logger.info("remove update");
            restartQueryWorker();
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            //do nothing
        }
    };
} // end of class TreeMap
