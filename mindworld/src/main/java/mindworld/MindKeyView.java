package mindworld;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-12-16
 * Time: 上午6:46
 * To change this template use File | Settings | File Templates.
 */
public class MindKeyView extends MindView {

    public MindKeyView(MindModel mindModel, MindController undoManager, Object rootId) {
        super(mindModel, undoManager, rootId);
        setKeyControlListener();
    }

    final static String sm_editActionName = "edit";
    final static String sm_undoActionName = "undo";
    final static String sm_redoActionName = "redo";
    final static String sm_saveActionName = "save";

    final static String sm_addChildActionName = "addChild";
    final static String sm_addSiblingActionName = "addSibling";
    final static String sm_removeActionName = "remove";

    final static String sm_cursorMoveUpActionName = "cursorMoveUp";
    final static String sm_cursorMoveDownActionName = "cursorMoveDown";
    final static String sm_cursorMoveLeftActionName = "cursorMoveLeft";
    final static String sm_cursorMoveRightActionName = "cursorMoveRight";

    final static String sm_toggleFoldNodeName = "toggleFoldNode";

    public void setKeyControlListener() {
        ActionMap m_mindActionMap = getActionMap();

        m_mindActionMap.put(sm_editActionName, m_editAction);
        m_mindActionMap.put(sm_removeActionName, m_removeAction);
        m_mindActionMap.put(sm_addChildActionName, m_addChildAction);
        m_mindActionMap.put(sm_addSiblingActionName, m_addSiblingAction);

        m_mindActionMap.put(sm_undoActionName, m_undoAction);
        m_mindActionMap.put(sm_redoActionName, m_redoAction);
        m_mindActionMap.put(sm_saveActionName, m_saveAction);

        m_mindActionMap.put(sm_cursorMoveUpActionName, m_cursorMoveUpAction);
        m_mindActionMap.put(sm_cursorMoveDownActionName, m_cursorMoveDownAction);
        m_mindActionMap.put(sm_cursorMoveLeftActionName, m_cursorMoveLeftAction);
        m_mindActionMap.put(sm_cursorMoveRightActionName, m_cursorMoveRightAction);

        m_mindActionMap.put(sm_toggleFoldNodeName, m_toggleFoldNodeAction);

        InputMap inputMap = getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("F2"), sm_editActionName);
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), sm_removeActionName);
        inputMap.put(KeyStroke.getKeyStroke("INSERT"), sm_addChildActionName);
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), sm_addSiblingActionName);

        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), sm_undoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl Y"), sm_redoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl S"), sm_saveActionName);

        inputMap.put(KeyStroke.getKeyStroke("UP"), sm_cursorMoveUpActionName);
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), sm_cursorMoveDownActionName);
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), sm_cursorMoveLeftActionName);
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), sm_cursorMoveRightActionName);

        inputMap.put(KeyStroke.getKeyStroke("SPACE"), sm_toggleFoldNodeName);

    }

    AbstractAction m_importAction = new AbstractAction()  {
        @Override
        public void actionPerformed(ActionEvent e) {
            importFile();
        }
    };

    AbstractAction m_undoAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            undo();
        }
    };

    AbstractAction m_redoAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            redo();
        }
    };

    AbstractAction m_saveAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            save();
        }
    };

    AbstractAction m_removeAction = new AbstractAction()  {

        @Override
        public void actionPerformed(ActionEvent e) {
            remove();
        }
    };

    public AbstractAction m_addChildAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            addChild();
        }
    };

    public AbstractAction m_addSiblingAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            addSibling();
        }
    };

    public AbstractAction m_editAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            edit();
        }
    };

    public AbstractAction m_cursorMoveUpAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cursorMoveUp();
        }
    };

    public AbstractAction m_cursorMoveDownAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cursorMoveDown();
        }
    };
    public AbstractAction m_cursorMoveLeftAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cursorMoveLeft();
        }
    };
    public AbstractAction m_cursorMoveRightAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cursorMoveRight();
        }
    };

    public AbstractAction m_toggleFoldNodeAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            toggleFoldNode();
        }
    };
}