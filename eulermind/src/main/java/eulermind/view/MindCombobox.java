package eulermind.view;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.tinkerpop.blueprints.Vertex;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import eulermind.MindDB;
import eulermind.MindModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class MindCombobox extends JComboBox {

    MindComboBoxEditor m_editor = new MindComboBoxEditor();
    JTextField m_editorComponent;

    private MindDB m_mindDb;
    boolean m_hasPromptList;

    private ArrayList<PromptedNode> m_promptedNodes = new ArrayList<PromptedNode>();
    private SwingWorker<Boolean, PromptedNode> m_queryWorker;
    private Timer m_queryDelayTimer;

    MindEditorListener m_mindEditorListener;

    MindCombobox()
    {
        this.setEditable(true);
        m_editor = new MindComboBoxEditor();
        this.setEditor(editor);

        m_editorComponent = m_editor.m_textField;
        m_editorComponent.getDocument().addDocumentListener(m_editTextListener);
        m_editorComponent.addKeyListener(m_editorKeyListener);

        this.setPrototypeDisplayValue("WWW");
        this.setMaximumRowCount(8);

    }

    KeyListener m_editorKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e)
        {
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case KeyEvent.VK_ESCAPE:
                    fireCancel();
                    break;
            }
        }
    };

    ActionListener m_actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            String command = e.getActionCommand();
            if ("comboBoxEdited".equals(command)) {
                fireEditorOk(m_editorComponent.getText());
            } else if ("comboBoxChanged".equals(command)) {
                int selectedIndex = getSelectedIndex();
                PromptedNode selected = m_promptedNodes.get(selectedIndex);
                firePromptListOk(selected.m_dbId, selected.m_text, selected.m_parentDBId, selected.m_parentText);
            }
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };

    public void setMindDb(MindDB mindDb) {
        m_mindDb = mindDb;
    }

    public void setHasPromptList(boolean hasPromptList)
    {
        m_hasPromptList = hasPromptList;
    }

    Logger m_logger = LoggerFactory.getLogger(this.getClass());


    private void stopQueryWorker()
    {
        if (m_queryWorker != null && ! m_queryWorker.isDone()) {
            m_queryWorker.cancel(true);
        }
        m_queryWorker = null;
    }

    private void startQueryWorker()
    {
        removeAllItems();
        m_promptedNodes.clear();

        //SwingWorker 被设计为只执行一次。多次执行 SwingWorker 将不会调用两次 doInBackground 方法。
        //所以每次要 new一个新对象
        m_queryWorker = new QueryWorker();
        m_queryWorker.execute();
    }


    private void startDelayedQuery()
    {
        m_queryDelayTimer = new Timer(500, new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                startQueryWorker();
            }
        });
        m_queryDelayTimer.setRepeats(false);
        m_queryDelayTimer.setCoalesce(true);
        m_queryDelayTimer.start();
    }

    private void updatePromptList()
    {
        if (m_queryDelayTimer != null) {
            m_queryDelayTimer.stop();
            m_queryDelayTimer = null;
        }

        stopQueryWorker();

        if (m_editorComponent.getText().isEmpty()) {
            removeAllItems();
            m_promptedNodes.clear();

        } else {
            startDelayedQuery();
        }
    }

    private DocumentListener m_editTextListener = new DocumentListener() {

        @Override
        public void insertUpdate(DocumentEvent documentEvent)
        {
            m_logger.info("insert update");
            if (m_hasPromptList) {
                updatePromptList();
            }
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent)
        {
            m_logger.info("remove update");
            if (m_hasPromptList) {
                updatePromptList();
            }
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent)
        {
            //do nothing
        }
    };

    //FIXME: override BasicComboBoxEditor
    class MindComboBoxEditor implements ComboBoxEditor {
        final protected JTextField m_textField;

        public MindComboBoxEditor() {
            m_textField = new JTextField();
        }

        @Override
        public Component getEditorComponent() {
            return m_textField;
        }

        @Override
        public Object getItem() {
            return m_textField.getText();
        }

        @Override
        public void addActionListener(ActionListener l) {
            m_textField.addActionListener(l);
        }

        @Override
        public void removeActionListener(ActionListener l) {
            m_textField.removeActionListener(l);
        }

        @Override
        public void selectAll() {
        }

        @Override
        public void setItem(Object newValue) {
        }
    }

    private class PromptedNode {
        final public Object m_dbId;
        final public String m_text;
        final public Object m_parentDBId;
        final public String m_parentText;

        PromptedNode(Vertex vertex)
        {
            m_dbId = vertex.getId();
            m_text = vertex.getProperty(MindModel.sm_textPropName);

            Vertex parent = m_mindDb.getParent(vertex);
            if (parent == null) {
                m_parentDBId = null;
                m_parentText = null;
            } else {
                m_parentDBId = parent.getId();
                m_parentText = parent.getProperty(MindModel.sm_textPropName);
            }
        }
    }

    private class QueryWorker extends SwingWorker<Boolean, PromptedNode> {
        //doInBackground是在一个单独线程中运行的函数， 结果放在m_promptNodes中。 这里不能添加ListModel
        @Override
        protected Boolean doInBackground()
        {
            //整个数据库查找, 如果打开orientdb的线程与操作数据库的线程不是同一个, 需要调用:
            ODatabaseRecordThreadLocal.INSTANCE.set(m_mindDb.m_graph.getRawGraph());

            String inputed = m_editorComponent.getText();

            m_logger.info("query vertex: " + inputed);

            for (Vertex vertex : m_mindDb.getVertices("V", new String[]{MindModel.sm_textPropName},
                    new String[]{inputed}))  {

                if (m_mindDb.isVertexTrashed(vertex)) {
                    continue;
                }

                PromptedNode promptedNode = new PromptedNode(vertex);
                publish(promptedNode);
                m_promptedNodes.add(promptedNode);

                if (isCancelled()) {
                    return false;
                }
            }

            return true;
        }

        @Override
        protected void process(List<PromptedNode> promptedNodes)
        {
            for (PromptedNode promptedNode : promptedNodes) {
                m_logger.info("get promptedNode " + promptedNode.m_dbId);

                if (promptedNode.m_parentText != null) {
                    addItem(promptedNode.m_text + " @ " + promptedNode.m_parentText);
                } else  {
                    addItem("root: " + promptedNode.m_text);
                }
            }
        }
    };

    public void setMindEditorListener(MindEditorListener listener)
    {
        m_mindEditorListener = listener;
    }

    void fireEditorOk(String text) {
        if (m_mindEditorListener != null) {
            m_mindEditorListener.editorOk(text);
        }
    }

    void fireCancel() {
        if (m_mindEditorListener != null) {
            m_mindEditorListener.cancel();
        }
    }
    void firePromptListOk(Object dbId, String text, Object parentDBId, String parentText) {
        if (m_mindEditorListener != null) {
            m_logger.info("LLLLLLLLLLLLL: {}: {},  {}: {}", dbId, text, parentDBId, parentText);
            m_mindEditorListener.promptListOk(dbId, text, parentDBId, parentText);
        }
    }

    static public class MindEditorListener {
        public void editorOk(String text) {

        }

        public void cancel() {

        }

        public void promptListOk(Object dbId, String text, Object parentDBId, String parentText) {

        }
    }

}
