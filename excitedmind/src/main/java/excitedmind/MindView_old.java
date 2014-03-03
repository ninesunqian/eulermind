package excitedmind;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.*;
import prefuse.visual.NodeItem;
import prefuse.visual.sort.TreeDepthItemSorter;
import statemap.State;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import java.awt.dnd.DragSource;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindView_old extends Display {

    Logger m_logger = Logger.getLogger(this.getClass().getName());

    final MindTree m_mindTree;
	final MindTreeController m_mindTreeController;
    UndoManager m_undoManager = new UndoManager();

	MindTreeRenderEngine m_renderEngine;

    private MindViewFSM m_fsm;

    private MindPrompter m_prompter;

    private PropertyChangeListener m_fsmStateChangeListener =  new PropertyChangeListener ()
    {

        public void propertyChange(PropertyChangeEvent event) {
            String propertyName = event.getPropertyName();
            State previousState = (State) event.getOldValue();
            State newState = (State) event.getNewValue();

            m_logger.info( "FSM: " + "  event: " + propertyName + ": " + "[" + previousState  + " -> " + newState + "]");

        }
    };

    MouseListener m_prompterMouseListener = new MouseAdapter() {
        public void mouseClicked(MouseEvent mouseEvent) {
            m_fsm.ok(true);
        }
    };

    KeyListener m_editorKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e)
        {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Inserting) {
                    m_fsm.ok(false);
                } else {
                    m_fsm.ok();
                }
            }
            else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)  {
                m_fsm.cancel();
            }
        }
    };

	public MindView_old(String path, Object rootId) {
		super(new Visualization());
		setSize(700, 600);
		setHighQuality(true);

        String treeGroup = "tree";
        m_mindTree = new MindTree(path, rootId);
        m_vis.add(treeGroup, m_mindTree.m_displayTree);
		m_mindTreeController = new MindTreeController(m_mindTree, m_vis, treeGroup);

        setItemSorter(new TreeDepthItemSorter());
        m_renderEngine = new MindTreeRenderEngine(this, treeGroup);

        m_prompter = new MindPrompter(this, m_mindTree.m_mindDb);
        m_prompter.addMouseListener(m_prompterMouseListener);

        getTextEditor().addKeyListener(m_editorKeyListener);
		setMouseControlListener();
		setKeyControlListener();

        m_fsm = new MindViewFSM(this, MindViewFSM.MindViewStateMap.Normal);
        m_fsm.addStateChangeListener(m_fsmStateChangeListener);
        m_fsm.enterStartState();
	}

	public void renderTree() {
		m_renderEngine.run(null);
	}

    public void renderTree(Runnable runAfterRePaint) {
        m_renderEngine.run(runAfterRePaint);
    }

    ControlAdapter m_zoomToFitContol;
    ControlAdapter m_zoomControl;
    ControlAdapter m_wheelZoomControl;
    ControlAdapter m_panControl;
    MouseControl m_mouseControl;

    class MouseControl extends RobustNodeItemController {

        public NodeItem m_hittedNode;
        public HittedPosition m_hittedPosition;

        MouseControl() {
            super(MindView_old.this);
            m_hittedNode = null;
            m_hittedPosition = HittedPosition.OUTSIDE;
        }

        @Override
        public void nodeItemEntered(NodeItem item, MouseEvent e) {
            m_fsm.itemEntered(item);
        }

        @Override
        public void nodeItemExited(NodeItem item, MouseEvent e) {
            m_fsm.itemExited(item);
        }

        @Override
        public void nodeItemPressed(NodeItem item, MouseEvent e) {
            m_fsm.itemPressed(item);
        }

        @Override
        public void nodeItemClicked(NodeItem item, MouseEvent e) {
            m_fsm.itemClicked();
        }

        @Override
        public void nodeItemDragged(NodeItem item, MouseEvent e) {
            if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Normal) {
                m_fsm.itemDragged(item);
                setCursor(e.isControlDown() ? DragSource.DefaultLinkDrop : DragSource.DefaultMoveDrop);
            }
        }

        private boolean notMoveToOtherNode(NodeItem sourceNode, NodeItem hittedNode) {
            return hittedNode == null || hittedNode == sourceNode;
        }

        private boolean operatorEnabled(NodeItem sourceNode, NodeItem hittedNode, boolean ctrlDowned)
        {
            if (ctrlDowned) {
                return m_mindTreeController.canAddReference(m_mindTreeController.toSource(hittedNode));
            } else {
                return m_mindTreeController.canResetParent(m_mindTreeController.toSource(hittedNode));
            }
        }

        private void setCursorByNode(NodeItem sourceNode, NodeItem hittedNode, boolean ctrlDowned)
        {
            boolean cursorEnabled = notMoveToOtherNode(sourceNode, hittedNode) ||
                    operatorEnabled(sourceNode, hittedNode, ctrlDowned);

            if (ctrlDowned) {
                setCursor(cursorEnabled ? DragSource.DefaultLinkDrop : DragSource.DefaultLinkNoDrop);
            } else {
                setCursor(cursorEnabled ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
            }
        }

        //if dropNode or dropPostion changed, give the event
        @Override
        public void nodeItemHitted(NodeItem item, NodeItem hittedNode,
                                   HittedPosition hittedPosition, boolean ctrlDowned) {
            if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Dragging) {
                m_hittedNode = hittedNode;
                m_hittedPosition = hittedPosition;
                setCursorByNode(item, hittedNode, ctrlDowned);
                renderTree();
                System.out.print((ArrayList)hittedNode.get(MindTree.sm_inheritPathPropName));
            }
        }

        private void clearHittedNode()
        {
            m_hittedNode = null;
            m_hittedPosition = HittedPosition.OUTSIDE;
        }

        @Override
        public void nodeItemMissed(NodeItem item, NodeItem dropNode, boolean ctrlDowned) {
            if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Dragging) {
                setCursorByNode(item, null, ctrlDowned);
                renderTree();
            }
            clearHittedNode();
        }

        @Override
        public void nodeItemDropped(NodeItem item, NodeItem dropNode, HittedPosition hittedPosition, boolean ctrlDowned) {
            m_logger.info("nodeItemDropped");
            if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Dragging) {

                if (notMoveToOtherNode(item, dropNode)) {
                    m_fsm.cancel();
                } else {
                    m_logger.info(String.format("--- ctrlDown %s, release time %dms", ctrlDowned ?"true":"false", System.currentTimeMillis() - m_ctrlReleaseTime));
                    if (operatorEnabled(item, dropNode, ctrlDowned)) {
                        m_fsm.itemDropped(dropNode, ctrlDowned);
                    } else {
                        m_fsm.cancel();
                    }
                }
            }

            clearHittedNode();
        }

        @Override
        public void nodeItemKeyPressed(NodeItem item, KeyEvent e)
        {
            if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Dragging) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    setCursorByNode(item, m_hittedNode, true);
                }
            }
        }

        @Override
        public void nodeItemKeyReleased(NodeItem item, KeyEvent e)
        {
            if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Dragging) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    setCursorByNode(item, m_hittedNode, false);
                }
            }
        }
    };

	private void setMouseControlListener() {
		m_zoomToFitContol = new ZoomToFitControl();
		m_zoomControl = new ZoomControl();
		m_wheelZoomControl = new WheelZoomControl();
		m_panControl = new PanControl();

		m_mouseControl = new MouseControl();

        addControlListener(m_zoomToFitContol);
        addControlListener(m_zoomControl);
        addControlListener(m_wheelZoomControl);
        addControlListener(m_panControl);
        addControlListener(m_mouseControl);
	}

    void mouse_control_set_enabled(boolean enabled)
    {
        m_zoomToFitContol.setEnabled(enabled);
        m_zoomControl.setEnabled(enabled);
        m_wheelZoomControl.setEnabled(enabled);
        m_panControl.setEnabled(enabled);
        m_mouseControl.setEnabled(enabled);
    }

    void startEditing()
    {
        showEditor(false);
    }

    void stopEditing(boolean confirm)
    {
        if (confirm) {
            String text = getTextEditor().getText();
            AbstractUndoableEdit undoer = m_mindTreeController.setCursorText(text);
            m_undoManager.addEdit(undoer);
        }

        hideEditor();
    }

    private void showEditor(boolean withPrompter)
    {
        if (getTextEditor().isVisible())
            return;

        editText(m_mindTreeController.toVisual(m_mindTreeController.getCursorNode()), MindTree.sm_textPropName) ;

        if (withPrompter) {
            m_prompter.show(getTextEditor());
        }
    }

    private void hideEditor()
    {
        super.stopEditing2(false);
        m_prompter.hide();
    }

    void startInserting(boolean asChild)
    {
        if (asChild) {
            if (m_mindTreeController.cursorIsFolded()) {
                m_undoManager.addEdit(m_mindTreeController.toggleFoldCursorUndoable());
            }
        } else {
            if (m_mindTreeController.getCursorNode() == m_mindTreeController.getRoot()) {
                assert(false);
                return;
            }
        }

        m_mindTreeController.addPlaceholder(asChild);

        renderTree(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        showEditor(true);
                    }
                });
            }
        });
    }

    void stopInserting(boolean confirm, boolean fromPrompter)
    {
        if (confirm) {
            if (fromPrompter) {
                int selectedIndex = m_prompter.getSelectedIndex();
                MindPrompter.PromptedNode selected = m_prompter.getPromptedNode(selectedIndex);

                AbstractUndoableEdit undoer = m_mindTreeController.placeRefereeUndoable(selected.m_dbId);
                m_undoManager.addEdit(undoer);

            } else {
                String text = getTextEditor().getText();
                m_mindTreeController.setPlaceholderCursorText(text);

                AbstractUndoableEdit undoer = m_mindTreeController.placeNewNodeUndoable();
                m_undoManager.addEdit(undoer);
            }

        } else {
            m_mindTreeController.removePlaceholder();
        }

        hideEditor();
    }

    private Timer m_cursorTimer;

    void startCursorTimer(final NodeItem nodeItem) {
        m_cursorTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_fsm.cursorTimeout(nodeItem);
                m_cursorTimer = null;
            }
        });
        m_cursorTimer.start();
    }

    void stopCursorTimer()
    {
        if (m_cursorTimer != null) {
            m_cursorTimer.stop();
            m_cursorTimer = null;
        }
    }


    AbstractAction m_cursorLeftAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.cursorLeft();
        }
    };

    AbstractAction m_cursorRightAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.cursorRight();
        }
    };

    AbstractAction m_cursorUpAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.cursorUp();
        }
    };

    AbstractAction m_cursorDownAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.cursorDown();
        }
    };

    AbstractAction m_undoAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.undo();
        }
    };

    AbstractAction m_redoAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.redo();
        }
    };

    boolean canRemove()
    {
        if (m_mindTreeController.getCursorNode() == m_mindTreeController.getRoot()) {
            alert("can't remove the root");
            return false;
        } else {
            return true;
        }
    }
    AbstractAction m_removeAction = new AbstractAction()  {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.remove();
        }
    };

    public AbstractAction m_addChildAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.startInserting(true);
        }
    };

    public AbstractAction m_editAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            m_fsm.startEditing();
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
            if (m_mindTreeController.getCursorNode() == m_mindTreeController.getRoot()) {
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
            m_fsm.startInserting(false);
        }
    };

    final static String sm_editActionName = "edit";
    final static String sm_undoActionName = "undo";
    final static String sm_redoActionName = "redo";

    final static String sm_addChildActionName = "addChild";
    final static String sm_addSiblingActionName = "addSibling";
    final static String sm_removeActionName = "remove";

    final static String sm_startLinkActionName = "startLink";
    final static String sm_startMoveActionName = "startMove";

    final static String sm_toNormalActionAction = "toNormal";

    final static String sm_cursorLeft = "cursorLeft";
    final static String sm_cursorRight = "cursorRight";
    final static String sm_cursorUp = "cursorUp";
    final static String sm_cursorDown = "cursorDown";

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

        m_mindActionMap.put(sm_cursorLeft, m_cursorLeftAction);
        m_mindActionMap.put(sm_cursorRight, m_cursorRightAction);
        m_mindActionMap.put(sm_cursorUp, m_cursorUpAction);
        m_mindActionMap.put(sm_cursorDown, m_cursorDownAction);

        InputMap inputMap = getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("F2"), sm_editActionName);
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), sm_removeActionName);
        inputMap.put(KeyStroke.getKeyStroke("INSERT"), sm_addChildActionName);
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), sm_addSiblingActionName);

        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), sm_undoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl Y"), sm_redoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl L"), sm_startLinkActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl M"), sm_startMoveActionName);
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), sm_toNormalActionAction);

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), sm_cursorLeft);
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), sm_cursorRight);
        inputMap.put(KeyStroke.getKeyStroke("UP"), sm_cursorUp);
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), sm_cursorDown);
    }
} // end of class TreeMap
