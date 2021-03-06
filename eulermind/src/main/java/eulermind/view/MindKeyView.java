package eulermind.view;

import eulermind.MindController;
import eulermind.MindModel;
import prefuse.data.Tree;

import javax.swing.*;
import java.awt.event.ActionEvent;

/*
The MIT License (MIT)
Copyright (c) 2012-2014 wangxuguang ninesunqian@163.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

public class MindKeyView extends MindView {

    public MindKeyView(MindModel mindModel, MindController undoManager, Tree tree) {
        super(mindModel, undoManager, tree);
        setKeyControlListener();
    }

    final static String sm_editActionName = "edit";
    final static String sm_undoActionName = "undo";
    final static String sm_redoActionName = "redo";
    final static String sm_saveActionName = "save";

    final static String sm_copyActionName = "copy";
    final static String sm_pasteActionName = "paste";

    final static String sm_markToBeLinkedActionName = "markToBeLinked";
    final static String sm_linkActionName = "link";

    final static String sm_addChildActionName = "addChild";
    final static String sm_addSiblingActionName = "addSibling";

    final static String sm_addChildWithPromptActionName = "addChildWithPrompt";
    final static String sm_addSiblingWithPromptActionName = "addSiblingWithPrompt";

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
        m_mindActionMap.put(sm_addChildWithPromptActionName, m_addChildWithPromptAction);
        m_mindActionMap.put(sm_addSiblingWithPromptActionName, m_addSiblingWithPromptAction);

        m_mindActionMap.put(sm_undoActionName, m_undoAction);
        m_mindActionMap.put(sm_redoActionName, m_redoAction);
        m_mindActionMap.put(sm_saveActionName, m_saveAction);

        m_mindActionMap.put(sm_copyActionName, m_copyAction);
        m_mindActionMap.put(sm_pasteActionName, m_pasteAction);

        m_mindActionMap.put(sm_markToBeLinkedActionName, m_markToBeLinkedAction);
        m_mindActionMap.put(sm_linkActionName, m_linkAction);

        m_mindActionMap.put(sm_toggleFoldNodeName, m_toggleFoldNodeAction);

        InputMap inputMap = getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("F2"), sm_editActionName);
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), sm_removeActionName);
        inputMap.put(KeyStroke.getKeyStroke("INSERT"), sm_addChildActionName);
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), sm_addSiblingActionName);

        inputMap.put(KeyStroke.getKeyStroke("ctrl INSERT"), sm_addChildWithPromptActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl ENTER"), sm_addSiblingWithPromptActionName);

        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), sm_undoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl Y"), sm_redoActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl S"), sm_saveActionName);

        inputMap.put(KeyStroke.getKeyStroke("ctrl C"), sm_copyActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl V"), sm_pasteActionName);
        inputMap.put(KeyStroke.getKeyStroke("ctrl L"), sm_linkActionName);

        /*
        inputMap.put(KeyStroke.getKeyStroke("UP"), sm_cursorMoveUpActionName);
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), sm_cursorMoveDownActionName);
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), sm_cursorMoveLeftActionName);
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), sm_cursorMoveRightActionName);

        inputMap.put(KeyStroke.getKeyStroke("shift UP"), sm_cursorMoveUpActionName);
        inputMap.put(KeyStroke.getKeyStroke("shift DOWN"), sm_cursorMoveDownActionName);
        inputMap.put(KeyStroke.getKeyStroke("shift LEFT"), sm_cursorMoveLeftActionName);
        inputMap.put(KeyStroke.getKeyStroke("shift RIGHT"), sm_cursorMoveRightActionName);
        */

        inputMap.put(KeyStroke.getKeyStroke("SPACE"), sm_toggleFoldNodeName);

    }

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

    AbstractAction m_pasteAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            pasteAsSubTree();
        }
    };

    AbstractAction m_markToBeLinkedAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
        }
    };

    AbstractAction m_linkAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            linkMarkedVertexToCursor();
        }
    };

    AbstractAction m_copyAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            copySubTree();
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
            addSibling(false);
        }
    };

    public AbstractAction m_addChildWithPromptAction = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            addChildWithPrompt();
        }
    };

    public AbstractAction m_addSiblingWithPromptAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            addSiblingWithPrompt(false);
        }
    };

    public AbstractAction m_editAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            edit();
        }
    };

    public AbstractAction m_toggleFoldNodeAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            toggleFoldNode();
        }
    };
}
