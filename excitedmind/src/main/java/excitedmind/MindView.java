package excitedmind;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

import com.tinkerpop.blueprints.Vertex;
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

    private MindTree m_mindTree;
	private MindTreeController m_mindTreeController;

	MindTreeRenderEngine m_renderEngine;
    NodeItem m_clickedNode;

    static enum State {NORMAL, LINKING, MOVING};

    private JList m_prompter = new JList(new DefaultListModel());
    private JScrollPane m_promptScrollPane = new JScrollPane(m_prompter);

    final static String sm_editActionName = "edit";
    final static String sm_removeActionName = "remove";
    final static String sm_undoActionName = "undo";
    final static String sm_redoActionName = "redo";
    final static String sm_addChildActionName = "addChild";
    final static String sm_addSiblingActionName = "addSibling";
    final static String sm_addLinkActionName = "addLink";
    final static String sm_moveActionName = "move";
    final static String sm_prepareLinkActionName = "prepareLink";
    final static String sm_prepareMoveActionName = "prepareMove";
    final static String sm_toNormalActionAction = "toNormal";

    AbstractAction m_removeAction = new SimpleMindTreeAction() {
        @Override
        public AbstractUndoableEdit operateMindTree(ActionEvent e) {
            return m_mindTreeController.removeCursorUndoable();
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

    public AbstractAction m_addChildAction = new AbstractAction() {

        //TODO:
        @Override
        public void actionPerformed(ActionEvent e) {

            if (m_mindTreeController.cursorIsFolded()) {
                getUndoManager().addEdit(m_mindTreeController.toggleFoldCursorUndoable());
            }

            m_mindTreeController.addPlaceholder(true);
            renderTree(new Runnable() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            m_editAction.actionPerformed(null);
                        }
                    });
                }
            });
            /*
            try {
                //TODO renderer tree and display editor thread sync
                Thread.sleep(10);
            } catch (InterruptedException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    m_editAction.actionPerformed(null);
                }
            });
            */
        }
    };

    public AbstractAction m_addSiblingAction = new AbstractAction() {

        //TODO:
        @Override
        public void actionPerformed(ActionEvent e) {

            if (m_mindTreeController.getCursor() == m_mindTreeController.m_mindTree.getRoot()) {
                return;
            }

            m_mindTreeController.addPlaceholder(false);
            renderTree();
            try {
                //TODO renderer tree and display editor thread sync
                Thread.sleep(10);
            } catch (InterruptedException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    m_editAction.actionPerformed(null);
                }
            });
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

    MouseListener m_prompterMouseListener = new MouseAdapter() {
        public void mouseClicked(MouseEvent mouseEvent) {

            m_prompter.removeMouseListener(this);

            int idx = m_prompter.getSelectedIndex();
            QueriedNode selected = m_queriedNodes.get(idx);

            stopEditing();

            AbstractUndoableEdit undoer = m_mindTreeController.placeRefereeUndoable(selected.m_dbId);
            getUndoManager().addEdit(undoer);
            renderTree();
        }
    };

    KeyListener m_editorKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER)
            {
                String text = getTextEditor().getText();

                //stopEditing will set text of cursor
                stopEditing();

                AbstractUndoableEdit undoer;

                if (m_mindTreeController.isPlaceholer(m_mindTreeController.getCursor())) {
                    m_mindTreeController.setPlaceholderCursorText(text);
                    undoer = m_mindTreeController.placeNewNodeUndoable();
                } else {
                    undoer = m_mindTreeController.setCursorText(text);
                }

                m_undoManager.addEdit(undoer);

                getTextEditor().removeKeyListener(this);

                renderTree();
            }
        }
    };

    AbstractAction m_editAction =  new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            editText(m_mindTreeController.getCursor(), MindTree.sm_textPropName) ;
        }
    };


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

        String treeGroup = "tree";
        m_mindTree = new MindTree(path, rootId);
        m_vis.add(treeGroup, m_mindTree.m_displayTree);

		m_mindTreeController = new MindTreeController(m_mindTree, m_vis, treeGroup);

        setItemSorter(new TreeDepthItemSorter());
        m_renderEngine = new MindTreeRenderEngine(this, treeGroup);

        m_promptScrollPane.setVisible(false);
        add(m_promptScrollPane);

        m_prompter.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_prompter.setLayoutOrientation(JList.VERTICAL);
        m_prompter.setPrototypeCellValue("WWW");
        m_prompter.setVisibleRowCount(8);


		setMouseControlListener();
		setKeyControlListener();

		renderTree();
	}

    public JList getPromptJList()
    {
        return m_prompter;
    }

	public void renderTree() {
		m_renderEngine.run(null);
	}

    public void renderTree(Runnable runAfterRePaint) {
        m_renderEngine.run(runAfterRePaint);
    }

	private void setMouseControlListener() {
		addControlListener(new ZoomToFitControl());
		addControlListener(new ZoomControl());
		addControlListener(new WheelZoomControl());
		addControlListener(new PanControl());
		addControlListener(new ControlAdapter() {

			public void itemEntered(VisualItem item, MouseEvent e) {
                if (isEditing()) {
                    return;
                }

				if (m_mindTreeController.isNode(item)) {
                    if (m_state == State.NORMAL) {
                        m_mindTreeController.setCursor((NodeItem)item);
                        renderTree();
                    }
				}
			}

			public void itemClicked(VisualItem item, MouseEvent e) {
                if (isEditing()) {
                    return;
                }

                m_logger.info("mouse Clicked");

				if (m_mindTreeController.isNode(item)) {
                    m_clickedNode = (NodeItem)item;

					m_renderEngine.holdItem(item);

                    AbstractUndoableEdit undoer;

                    switch (m_state) {
                        case NORMAL:
                            undoer = m_mindTreeController.toggleFoldCursorUndoable();
                            break;
                        case LINKING:
                            undoer = m_mindTreeController.addReferenceUndoable(m_clickedNode);
                            break;
                        case MOVING:
                            undoer = m_mindTreeController.resetParentUndoable(m_clickedNode);
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
        m_mindActionMap.put(sm_addSiblingActionName, m_addSiblingAction);

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
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), sm_removeActionName);
        inputMap.put(KeyStroke.getKeyStroke("INSERT"), sm_addChildActionName);
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), sm_addSiblingActionName);

        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), sm_undoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl Y"), sm_redoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl L"), sm_prepareLinkActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl M"), sm_prepareMoveActionName);
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), sm_toNormalActionAction);
	}

	private UndoManager m_undoManager = new UndoManager();

	public UndoManager getUndoManager() {
		return m_undoManager;
	}

	public MindTreeController getMindTreeController() {
		return m_mindTreeController;
	}

    @Override
    public void editText(String txt, Rectangle r) {
        super.editText(txt, r);
        JTextComponent editor = getTextEditor();
        editor.getDocument().addDocumentListener(m_editTextListener);
        editor.addKeyListener(m_editorKeyListener);

        if (m_mindTreeController.cursorIsPlaceholder()) {
            m_promptScrollPane.setLocation(editor.getX(), editor.getY()+editor.getHeight());
            m_promptScrollPane.setSize(100, 100);
            m_promptScrollPane.setVisible(true);
            m_prompter.addMouseListener(m_prompterMouseListener);
        }

    }

    @Override
    public void stopEditing() {
        m_prompter.removeMouseListener(m_prompterMouseListener);
        getTextEditor().removeKeyListener(m_editorKeyListener);

        super.stopEditing();

        m_promptScrollPane.setVisible(false);
        m_queriedNodes.clear();
        DefaultListModel listModel = (DefaultListModel) m_prompter.getModel();
        listModel.clear();

        JTextComponent editor = getTextEditor();
        editor.getDocument().removeDocumentListener(m_editTextListener);
    }

    public class QueriedNode {
        public Object m_dbId;
        public String m_text;
        public Object m_parentDBId;
        public String m_parentText;

        QueriedNode (Vertex vertex)
        {
            m_dbId = vertex.getId();
            m_text = vertex.getProperty(MindTree.sm_textPropName);

            DBTree.EdgeVertex edgeVertex = m_mindTreeController.m_mindTree.m_dbTree.getParent(vertex);
            m_parentDBId = edgeVertex.m_vertex.getId();
            m_parentText = edgeVertex.m_vertex.getProperty(MindTree.sm_textPropName);
        }
    }

    public ArrayList<QueriedNode> m_queriedNodes = new ArrayList<QueriedNode>();

    class QueryWorker extends SwingWorker<Boolean, QueriedNode>
    {
        @Override
        protected Boolean doInBackground() {
            ((DefaultListModel) m_prompter.getModel()).removeAllElements();

            String inputed = getTextEditor().getText();

            m_logger.info("query vertex: " + inputed);

            for (Vertex vertex : m_mindTreeController.m_mindTree.m_dbTree.getVertices(MindTree.sm_textPropName, inputed)) {
                QueriedNode queriedNode = new QueriedNode(vertex);
                publish(queriedNode);
                m_queriedNodes.add(queriedNode);

                if (isCancelled()) {
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void process(List<QueriedNode> queriedNodes) {
            DefaultListModel listModel = (DefaultListModel) m_prompter.getModel();

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
                m_queryWorker.cancel(true);
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

    public abstract class SimpleMindTreeAction extends AbstractAction {

        public abstract AbstractUndoableEdit operateMindTree(ActionEvent e);


        @Override
        public void actionPerformed(ActionEvent e) {
            AbstractUndoableEdit undoer = operateMindTree(e);
            if (undoer != null) {
                getUndoManager().addEdit(undoer);
            }
            renderTree ();
        }
    }

} // end of class TreeMap
