package excitedmind;

import java.awt.event.*;

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
                m_fsm.ok();
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
        //TODO m_fsm.setState(m_fsm.MindViewStateMap.Normal);
        m_fsm.enterStartState();
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
				if (m_mindTreeController.isNode(item)) {
                    m_logger.info("mouse entered event---");
                    m_fsm.mouseInNode((NodeItem)item);
				}
			}

            public void itemExited(VisualItem item, MouseEvent e) {
                if (m_mindTreeController.isNode(item)) {
                    m_logger.info("mouse exit event---");
                    m_fsm.mouseOutNode();
                }
            }

			public void itemPressed(VisualItem item, MouseEvent e) {
				if (m_mindTreeController.isNode(item)) {
                    m_logger.info("mouse click event---");
                    m_fsm.press((NodeItem)item);
				}
			}

		});
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

    void startMoving()
    {

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


    AbstractAction m_startLinkAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            m_fsm.startLinking();
        }
    };

    AbstractAction m_startMoveAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            m_fsm.startMoving();
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

        m_mindActionMap.put(sm_startLinkActionName, m_startLinkAction);
        m_mindActionMap.put(sm_startMoveActionName, m_startMoveAction);


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
