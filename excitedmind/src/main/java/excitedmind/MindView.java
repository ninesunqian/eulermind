package excitedmind;

import java.awt.event.*;

import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.text.JTextComponent;
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

    static enum State {NORMAL, LINKING, MOVING};

    final static String sm_editActionName = "edit";

    final static String sm_undoActionName = "undo";
    final static String sm_redoActionName = "redo";

    final static String sm_addChildActionName = "addChild";
    final static String sm_addSiblingActionName = "addSibling";
    final static String sm_addLinkActionName = "addLink";
    final static String sm_removeActionName = "remove";

    final static String sm_prepareLinkActionName = "prepareLink";
    final static String sm_prepareMoveActionName = "prepareMove";
    final static String sm_moveActionName = "move";

    final static String sm_toNormalActionAction = "toNormal";

    final static String sm_cursorLeft = "cursorLeft";
    final static String sm_cursorRight = "cursorRight";
    final static String sm_cursorUp = "cursorUp";
    final static String sm_cursorDown = "cursorDown";

    Logger m_logger = Logger.getLogger(this.getClass().getName());

	final public MindTreeController m_mindTreeController;
    final public MindTree m_mindTree;

	MindTreeRenderEngine m_renderEngine;
    NodeItem m_clickedNode;

    State m_state = State.NORMAL;

    private MindViewFSM m_fsm;

    private MindPrompter m_prompter;

    AbstractAction m_removeAction = new SimpleMindTreeAction() {
        @Override
        public AbstractUndoableEdit operateMindTree(ActionEvent e) {
            return m_mindTreeController.removeCursorUndoable();
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

    void alert(String msg)
    {
        JOptionPane.showMessageDialog(null, msg);
    }

    public AbstractAction m_addSiblingAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {

            if (m_mindTreeController.getCursorNode() == m_mindTreeController.getRoot()) {
                alert("you must open the root parent");
                return;
            }
            startInserting(false);
        }
    };


    AbstractAction m_prepareLinkAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            m_fsm.startLinking();
        }
    };

    AbstractAction m_prepareMoveAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            m_fsm.startMoving();
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

    AbstractAction m_cursorLeftAction = new MovingCursorAction(Direction.LEFT);
    AbstractAction m_cursorRightAction = new MovingCursorAction(Direction.RIGHT);
    AbstractAction m_cursorUpAction = new MovingCursorAction(Direction.UP);
    AbstractAction m_cursorDownAction = new MovingCursorAction(Direction.DOWN);

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

    //TODO: normal linking moving : disable key press
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

        m_prompter = new MindPrompter(this, m_mindTree.m_dbTree);
        m_prompter.addMouseListener(m_prompterMouseListener);

		setMouseControlListener();
		setKeyControlListener();

        getTextEditor().addKeyListener(m_editorKeyListener);

        m_fsm = new MindViewFSM(this);
	}

	public void renderTree() {
		m_renderEngine.run(null);
	}

    public void renderTree(Runnable runAfterRePaint) {
        m_renderEngine.run(runAfterRePaint);
    }

    //TODO
    NodeItem m_enteredNode = null;
    Timer m_setCursorTimer = new Timer(1000, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (m_state == State.NORMAL) {
                m_mindTreeController.setCursorNode((NodeItem) m_enteredNode);
            }
        }
    });

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
                        m_setCursorTimer.start();
                    }
				}
			}

            public void itemExited(VisualItem item, MouseEvent e) {
                m_setCursorTimer.stop();
            }

			public void itemClicked(VisualItem item, MouseEvent e) {
                if (isEditing()) {
                    return;
                }

                m_logger.info("mouse Clicked");
                m_setCursorTimer.stop();

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


        m_mindActionMap.put(sm_cursorLeft, m_cursorLeftAction);
        m_mindActionMap.put(sm_cursorRight, m_cursorRightAction);
        m_mindActionMap.put(sm_cursorUp, m_cursorUpAction);
        m_mindActionMap.put(sm_cursorDown, m_cursorDownAction);

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

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), sm_cursorLeft);
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), sm_cursorRight);
        inputMap.put(KeyStroke.getKeyStroke("UP"), sm_cursorUp);
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), sm_cursorDown);
	}

	UndoManager m_undoManager = new UndoManager();

	public MindTreeController getMindTreeController() {
		return m_mindTreeController;
	}

    public abstract class SimpleMindTreeAction extends AbstractAction {

        public abstract AbstractUndoableEdit operateMindTree(ActionEvent e);

        @Override
        public void actionPerformed(ActionEvent e) {
            AbstractUndoableEdit undoer = operateMindTree(e);
            if (undoer != null) {
                m_undoManager.addEdit(undoer);
                m_fsm.instantEvent();
            }
        }
    }

    enum Direction {LEFT, RIGHT, UP, DOWN};
    public class MovingCursorAction extends AbstractAction {

        Direction m_direction;
        public MovingCursorAction(Direction direction) {
            m_direction = direction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            m_logger.info("get a cusor event "  + m_direction);
            switch (m_direction)
            {
                case LEFT:
                    m_mindTreeController.moveCursorLeft();
                    break;
                case RIGHT:
                    m_mindTreeController.moveCursorRight();
                    break;
                case UP:
                    m_mindTreeController.moveCursorUp();
                    break;
                case DOWN:
                    m_mindTreeController.moveCursorDown();
                    break;
            }

            m_fsm.instantEvent();
        }
    }

    public void startEditing()
    {
        showEditor(false);
    }

    public void stopEditing(boolean confirm)
    {
        if (confirm) {
            String text = getTextEditor().getText();
            AbstractUndoableEdit undoer = m_mindTreeController.setCursorText(text);
            m_undoManager.addEdit(undoer);
        }

        hideEditor();
    }

    public void showEditor(boolean withPrompter)
    {
        if (getTextEditor().isVisible())
            return;

        editText(m_mindTreeController.toVisual(m_mindTreeController.getCursorNode()), MindTree.sm_textPropName) ;

        if (withPrompter) {
            m_prompter.show(getTextEditor());
        }
    }

    public void hideEditor()
    {
        super.stopEditing(false);
        m_prompter.hide();
    }

    public void startInserting(boolean asChild)
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

    public void stopInserting(boolean confirm, boolean fromPrompter)
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

    public void startMoving()
    {

    }

    Timer m_cursorTimer;
    public void startCursorTimer(final NodeItem nodeItem) {
        m_cursorTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                m_fsm.cursorTimeout(nodeItem);
            }
        });
        m_cursorTimer.start();
    }

    public void stopCursorTimer()
    {
        m_cursorTimer.stop();
        m_cursorTimer = null;
    }
} // end of class TreeMap
