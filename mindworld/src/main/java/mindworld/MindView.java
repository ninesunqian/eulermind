package mindworld;

import java.awt.event.*;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import mindworld.operator.*;
import prefuse.Display;
import prefuse.Visualization;

import prefuse.controls.*;
import prefuse.data.*;
import prefuse.util.PrefuseLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;
import prefuse.visual.sort.TreeDepthItemSorter;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindView extends Display {

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());
    final String m_treeGroupName = "tree";

    final public MindModel m_mindModel;
    MindController m_mindController;

    TreeCursor m_cursor;
    Node m_savedCursor = null;

    TreeFolder m_folder;

    Tree m_tree;
	MindTreeRenderEngine m_renderEngine;

    private MindPrompter m_prompter;

    MouseListener m_prompterMouseListener = new MouseAdapter() {
        public void mouseClicked(MouseEvent mouseEvent) {
            stopInserting(true, true);
        }
    };

    KeyListener m_editorKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e)
        {
            m_newOperator = null;

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                //TODO
                if (true) {  //inserting
                    stopInserting(true, false);
                } else { //editing
                    stopEditing(true);
                }

                if (m_newOperator != null) {
                    m_mindController.does(m_newOperator);
                    m_newOperator = null;
                }

            }

            else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)  {
                //TODO
                if (true) {  //inserting
                    stopInserting(false, false);
                } else { //editing
                    stopEditing(false);
                }
            }
        }
    };

    VisualTree m_visualTree;

	public MindView(MindModel mindModel, MindController undoManager, Object rootId) {
		super(new Visualization());

        //s_logger.setLevel(Level.OFF);
		setSize(700, 600);
		setHighQuality(true);

        m_mindModel = mindModel;
        m_mindController = undoManager;

        m_tree = mindModel.findOrPutTree(rootId, 1);
        m_visualTree = (VisualTree)m_vis.add(m_treeGroupName, m_tree);
        MindModel.addNodeMirrorXYColumn(m_tree, m_visualTree);

        setItemSorter(new TreeDepthItemSorter());
        m_renderEngine = new MindTreeRenderEngine(this, m_treeGroupName);

        m_cursor = new TreeCursor(this);
        m_folder = new TreeFolder(this);

        getTextEditor().addKeyListener(m_editorKeyListener);
		setMouseControlListener();
		setKeyControlListener();

        m_prompter = new MindPrompter(this, m_mindModel.m_mindDb);
        m_prompter.addMouseListener(m_prompterMouseListener);

        //m_testTimer.setRepeats(true);
        //m_testTimer.start();
	}

    public NodeItem toVisual (Node node)
    {
        if (node instanceof NodeItem) {
            return  (NodeItem) node;
        } else {
            String treeNodesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.NODES);
            return (NodeItem) m_vis.getVisualItem(treeNodesGroupName, node);
        }
    }

    public EdgeItem toVisual (Edge edge)
    {
        if (edge instanceof EdgeItem) {
            return (EdgeItem) edge;
        } else {
            String treeEdgesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.EDGES);
            return (EdgeItem) m_vis.getVisualItem(treeEdgesGroupName, edge);
        }
    }

    public Node toSource (NodeItem nodeItem)
    {
        return (Node) m_vis.getSourceTuple (nodeItem);
    }

    public Edge toSource (EdgeItem edgeItem)
    {
        return (Edge) m_vis.getSourceTuple (edgeItem);
    }

	public void renderTree() {
        if (m_renderEngine != null) {
            m_renderEngine.run(null);
        }
	}

    public void renderTree(Runnable runAfterRePaint) {
        m_renderEngine.run(runAfterRePaint);
    }

    ControlAdapter m_zoomToFitControl;
    ControlAdapter m_zoomControl;
    ControlAdapter m_wheelZoomControl;
    ControlAdapter m_panControl;
    NodeDraggingControl m_dragControl;

    public NodeItem getDragHitNode()
    {
        return m_dragControl.m_hitNode;
    }

	private void setMouseControlListener()
    {
		m_zoomToFitControl = new ZoomToFitControl(Control.MIDDLE_MOUSE_BUTTON);
		m_zoomControl = new ZoomControl();
		m_wheelZoomControl = new WheelZoomControl();
		m_panControl = new PanControl();

		m_dragControl = new NodeDraggingControl(this);

        addControlListener(m_zoomToFitControl);
        addControlListener(m_zoomControl);
        addControlListener(m_wheelZoomControl);
        addControlListener(m_panControl);
        addControlListener(m_dragControl);

        addControlListener(m_cursor);
        addControlListener(m_dragControl);
	}

    void setMouseControlEnabled(boolean enabled)
    {
        m_zoomToFitControl.setEnabled(enabled);
        m_zoomControl.setEnabled(enabled);
        m_wheelZoomControl.setEnabled(enabled);
        m_panControl.setEnabled(enabled);
        m_dragControl.setEnabled(enabled);
    }

    void startEditing()
    {
        showEditor(false);
    }

    void stopEditing(boolean confirm)
    {
        m_logger.warn("++++++++++++++ stopEditing");
        if (confirm) {
            String text = getTextEditor().getText();
            SettingProperty settingProperty = new SettingProperty(m_mindModel,
                    getCursorSourceNode(), MindModel.sm_textPropName, text);

            settingProperty.does();
            m_mindController.does(settingProperty);
        }

        hideEditor();
    }

    private void showEditor(boolean withPrompter)
    {
        if (isEditing())
            return;

        editText(toVisual(getCursorSourceNode()), MindModel.sm_textPropName) ;

        if (withPrompter) {
            m_prompter.show(getTextEditor());
        }
    }

    private void hideEditor()
    {
        super.stopEditing2(false);
        m_prompter.hide();
    }

    public void setCursorNodeByPath(ArrayList<Integer> path)
    {
        m_cursor.setCursorNodeItem(toVisual(m_mindModel.getNodeByPath(m_tree, path)));
    }

    void startInserting(boolean asChild)
    {
        NodeItem cursorItem = m_cursor.getCursorNodeItem();
        if (asChild) {
            if (m_folder.isFolded(cursorItem)) {
                m_folder.unfoldNode(cursorItem);
            }
        } else {
            //TODO 防止 NodeIem与Node比较
            if (toSource(cursorItem) == m_tree.getRoot()) {
                assert(false);
                return;
            }
        }

        addPlaceholder(asChild);

        renderTree(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        m_logger.warn("++++++++++++++ show Editor");
                        showEditor(true);
                        m_logger.warn("--------------- after show Editor");
                    }
                });
            }
        });
    }

    void stopInserting(boolean confirm, boolean fromPrompter)
    {
        m_logger.warn("++++++++++++++ stopInserting");
        if (confirm) {
            MindOperator operator;

            if (fromPrompter) {
                int selectedIndex = m_prompter.getSelectedIndex();
                MindPrompter.PromptedNode selected = m_prompter.getPromptedNode(selectedIndex);

                operator = new AddingReference(m_mindModel, getCursorSourceNode().getParent(),
                        selected.m_dbId, getCursorSourceNode().getIndex());
            } else {
                String text = getTextEditor().getText();
                operator = new AddingChild(m_mindModel, getCursorSourceNode().getParent(), getCursorSourceNode().getIndex(), text);
            }

            removePlaceholder();
            m_mindController.does(operator);

        } else {
            removePlaceholder();
        }

        hideEditor();
    }

    void importFile()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        String path;

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            path = chooser.getSelectedFile().getPath();
        } else {
            return;
        }

        MindOperator operator = new ImportingFile(m_mindModel, getCursorSourceNode(), path);

        m_mindController.does(operator);
    }

    NormalStateAction m_importAction = new NormalStateAction()  {

        @Override
        public void NormalStateActionPerformed(ActionEvent e) {
            importFile();
        }
    };

    abstract class NormalStateAction extends AbstractAction {
        final public void actionPerformed(ActionEvent e) {
            m_cursor.hold();
            NormalStateActionPerformed(e);
            renderTree();
        }

        //this memthod must not change state
        abstract public void NormalStateActionPerformed(ActionEvent e);
    }

    NormalStateAction m_undoAction = new NormalStateAction() {
        @Override
        public void NormalStateActionPerformed(ActionEvent e) {
            if (m_mindController.canUndo()) {
                m_mindController.undo();
            }
        }
    };

    NormalStateAction m_redoAction = new NormalStateAction() {
        @Override
        public void NormalStateActionPerformed(ActionEvent e) {
            if (m_mindController.canRedo()) {
                m_mindController.redo();
            }
        }
    };

    NormalStateAction m_saveAction = new NormalStateAction() {
        @Override
        public void NormalStateActionPerformed(ActionEvent e) {
            m_mindModel.m_mindDb.commit();
        }
    };

    boolean canRemove()
    {
        return Removing.canDo(m_mindModel, m_tree, getCursorSourceNode());
    }

    public void removeCursor()
    {
        Node cursorNode = getCursorSourceNode();
        MindOperator operator = new Removing(m_mindModel, cursorNode);
        m_mindController.does(operator);
    }

    NormalStateAction m_removeAction = new NormalStateAction()  {

        @Override
        public void NormalStateActionPerformed(ActionEvent e) {
            if (canRemove())
            {
                removeCursor();
            }
        }
    };

    public AbstractAction m_addChildAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            startInserting(true);
        }
    };

    public AbstractAction m_editAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            startEditing();
        }
    };

    private void alert(String msg)
    {
        JOptionPane.showMessageDialog(null, msg);
    }

    boolean canStartInserting(boolean asChild)
    {
        if (asChild) {
            return true;

        } else {
            if (getCursorSourceNode() == m_tree.getRoot()) {
                alert("you must open the root parent");
                return false;
            } else {
                return true;
            }
        }
    }

    public AbstractAction m_addSiblingAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            startInserting(false);
        }
    };

    final static String sm_editActionName = "edit";
    final static String sm_undoActionName = "undo";
    final static String sm_redoActionName = "redo";
    final static String sm_saveActionName = "save";

    final static String sm_addChildActionName = "addChild";
    final static String sm_addSiblingActionName = "addSibling";
    final static String sm_removeActionName = "remove";


    public void setKeyControlListener() {
        /*
        ActionMap m_mindActionMap = new ActionMap();
        ActionMap defaultActionMap = getActionMap();
        m_mindActionMap.setParent(defaultActionMap);
        setActionMap(m_mindActionMap);
        */
        ActionMap m_mindActionMap = getActionMap();

        m_mindActionMap.put(sm_editActionName, m_editAction);
        m_mindActionMap.put(sm_removeActionName, m_removeAction);
        m_mindActionMap.put(sm_addChildActionName, m_addChildAction);
        m_mindActionMap.put(sm_addSiblingActionName, m_addSiblingAction);

        m_mindActionMap.put(sm_undoActionName, m_undoAction);
        m_mindActionMap.put(sm_redoActionName, m_redoAction);
        m_mindActionMap.put(sm_saveActionName, m_saveAction);

        InputMap inputMap = getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("F2"), sm_editActionName);
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), sm_removeActionName);
        inputMap.put(KeyStroke.getKeyStroke("INSERT"), sm_addChildActionName);
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), sm_addSiblingActionName);

        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), sm_undoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl Y"), sm_redoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl S"), sm_saveActionName);

    }

    private void addPlaceholder(boolean asChild)
    {
        Node cursorNode = getCursorSourceNode();
        m_savedCursor = cursorNode;

        Node newNode;

        if (asChild) {

            newNode = m_tree.addChild(cursorNode, cursorNode.getChildCount());
            m_folder.unfoldNode(toVisual(cursorNode));

        } else {
            newNode = m_tree.addChild(cursorNode.getParent(), cursorNode.getIndex() + 1);
        }

        //NOTE: newNode.setString(MindModel.sm_textPropName, "") error

        newNode.set(MindModel.sm_textPropName, "");

        m_cursor.setCursorNodeItem(toVisual(newNode));
    }

    private void removePlaceholder()
    {
        Node placeholderNode = getCursorSourceNode();
        assert(isPlaceholer(placeholderNode));
        assert(placeholderNode != m_tree.getRoot());

        m_tree.removeChild(placeholderNode);
        m_cursor.setCursorNodeItem(toVisual(m_savedCursor));
    }

    //include node and edge, the edge is used rendering
    public boolean isPlaceholer(Tuple tuple)
    {
        return (m_mindModel.getDBId(tuple) == null);
    }

    Timer m_testTimer = new Timer(500, new ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            m_logger.info("test timer$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        }
    });

    public Node getCursorSourceNode()
    {
        return toSource(m_cursor.getCursorNodeItem());
    }

    public void setCursorProperty(String key, Object value)
    {
        //called by toolbar controls' action listener,
        m_cursor.hold();

        Node cursorNode = getCursorSourceNode();
        MindOperator operator = new SettingProperty(m_mindModel, cursorNode, key, value);

        m_mindController.does(operator);
    }

    MindOperator m_newOperator;

} // end of class TreeMap
