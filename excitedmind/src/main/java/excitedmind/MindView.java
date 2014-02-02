package excitedmind;

import java.awt.*;
import java.awt.dnd.DragSource;
import java.awt.event.*;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

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

import statemap.State;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindView extends Display {

    Logger m_logger = Logger.getLogger(this.getClass().getName());

    final MindTree m_mindTree;
	final MindTreeController m_mindTreeController;
    UndoManager m_undoManager = new UndoManager();

	MindTreeRenderEngine m_renderEngine;

    private MindViewFSM m_fsm;

    private MindPrompter m_prompter;

    NodeItem m_dropNode;
    enum DropPosition {OUTSIDE, TOP, BOTTOM, RIGHT};
    DropPosition m_dropPosition;

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

        m_prompter = new MindPrompter(this, m_mindTree.m_dbTree);
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
    ControlAdapter m_mouseControl;

    DropPosition getDropPosition(NodeItem node, double x, double y)
    {
        Rectangle2D bounds = node.getBounds();
        if (bounds.contains(x, y))
        {
            if (x > bounds.getCenterX()) {
                return DropPosition.RIGHT;
            }

            if (y > bounds.getCenterY()) {
                return DropPosition.BOTTOM;

            } else {
                return DropPosition.TOP;
            }
        } else {
            return DropPosition.OUTSIDE;
        }
    }

    class MouseControl extends ControlAdapter {

        long m_ctrlReleaseTime;
        boolean m_ctrlDowned;

        MouseControl() {
            m_ctrlReleaseTime = 0;
            m_ctrlDowned = false;
        }

        public void itemEntered(VisualItem item, MouseEvent e) {
            if (!m_mindTreeController.isNode(item)) {
                return;
            }
            m_fsm.itemEntered((NodeItem)item);
        }

        public void itemExited(VisualItem item, MouseEvent e) {
            if (!m_mindTreeController.isNode(item)) {
                return;
            }
            m_fsm.itemExited((NodeItem)item);
        }

        public void itemPressed(VisualItem item, MouseEvent e) {
            if (!m_mindTreeController.isNode(item)) {
                return;
            }
            m_fsm.itemPressed((NodeItem)item);
        }

        public void itemClicked(VisualItem item, MouseEvent e) {
            if (!m_mindTreeController.isNode(item)) {
                return;
            }
            m_fsm.itemClicked();
        }

        public void itemDragged(VisualItem item, MouseEvent e) {
            if (!m_mindTreeController.isNode(item)) {
                return;
            }

            m_logger.info("itemDragged : " + item.getString(MindTree.sm_textPropName));

            setCursor(m_ctrlDowned ? DragSource.DefaultLinkDrop : DragSource.DefaultMoveDrop);

            if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Normal) {
                m_fsm.itemDragged((NodeItem)item);

                m_dropNode = null;
                m_dropPosition = DropPosition.OUTSIDE;

            } else if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Dragging) {

                Point mousePoint = e.getPoint();
                VisualItem dropTarget = findItem(mousePoint);

                NodeItem dropNode;
                DropPosition dropPosition;

                if (dropTarget != null && m_mindTreeController.isNode(dropTarget)) {
                    dropNode = (NodeItem)dropTarget;
                    dropPosition = getDropPosition(dropNode, mousePoint.getX(), mousePoint.getY());
                } else {
                    dropNode = null;
                    dropPosition = DropPosition.OUTSIDE;
                }

                if (m_dropNode != dropNode || m_dropPosition != dropPosition) {
                    renderTree();
                    m_dropNode = dropNode;
                    m_dropPosition = dropPosition;
                }
            }
        }

        public void itemReleased(VisualItem item, MouseEvent e) {
            if (!m_mindTreeController.isNode(item)) {
                return;
            }
            m_logger.info("itemReleased : " + item.getString(MindTree.sm_textPropName));

            if (m_fsm.getState() == MindViewFSM.MindViewStateMap.Dragging) {
                VisualItem dropTarget = findItem(e.getPoint());
                if (dropTarget != null && m_mindTreeController.isNode(dropTarget)) {
                    NodeItem dropNode = (NodeItem)dropTarget;
                    m_logger.info(String.format("--- ctrlDown %s, release time %dms", m_ctrlDowned ?"true":"false", System.currentTimeMillis() - m_ctrlReleaseTime));
                    m_fsm.itemDropped(dropNode, m_ctrlDowned || System.currentTimeMillis() - m_ctrlReleaseTime < 500);
                    m_dropNode = null;
                    m_dropPosition = DropPosition.OUTSIDE;
                }else {
                    m_fsm.cancel();
                }
            }
        }

        public void itemKeyPressed(VisualItem item, KeyEvent e) {
            if (!m_mindTreeController.isNode(item)) {
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                m_ctrlDowned = true;
            }
        }

        public void itemKeyReleased(VisualItem item, KeyEvent e) {
            if (!m_mindTreeController.isNode(item)) {
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                //setCursor(DragSource.DefaultMoveDrop);
                //setCursor(DragSource.DefaultMoveDrop);
                m_ctrlReleaseTime = System.currentTimeMillis();
                m_ctrlDowned = false;
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

    void setMoveCursor()
    {
        setCursor(DragSource.DefaultMoveDrop);
    }

    void setLinkCursor()
    {
    }

    void setDefaultCursor()
    {
        setCursor(null);
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
