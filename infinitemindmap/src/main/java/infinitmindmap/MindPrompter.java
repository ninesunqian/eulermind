package infinitmindmap;

import com.tinkerpop.blueprints.Vertex;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
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

public class MindPrompter {

    Logger m_logger = LoggerFactory.getLogger(this.getClass());

    private JList m_jList = new JList(new DefaultListModel());
    private JScrollPane m_jScrollPane = new JScrollPane(m_jList);

    private JTextComponent m_followedEditor;
    private MindDB m_mindDb;

    private ArrayList<PromptedNode> m_promptedNodes = new ArrayList<PromptedNode>();

    private SwingWorker<Boolean, PromptedNode> m_queryWorker;

    public MindPrompter(JComponent parentView, MindDB mindDb)
    {
        m_mindDb = mindDb;

        m_jList = new JList(new DefaultListModel());
        m_jScrollPane = new JScrollPane(m_jList);

        m_jScrollPane.setVisible(false);
        parentView.add(m_jScrollPane);

        m_jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_jList.setLayoutOrientation(JList.VERTICAL);
        m_jList.setPrototypeCellValue("WWW");
        m_jList.setVisibleRowCount(10);
    }

    public void addMouseListener(MouseListener mouseListener)
    {
        m_jList.addMouseListener(mouseListener);
    }

    public void removeMouseListener(MouseListener mouseListener)
    {
        m_jList.removeMouseListener(mouseListener);
    }

    public int getSelectedIndex()
    {
        return m_jList.getSelectedIndex();
    }

    public PromptedNode getPromptedNode(int index)
    {
        return m_promptedNodes.get(index);
    }

    public void show(JTextComponent editor)
    {
        editor.getDocument().addDocumentListener(m_editTextListener);
        m_followedEditor = editor;

        m_jScrollPane.setLocation(editor.getX(), editor.getY() + editor.getHeight());
        m_jScrollPane.setSize(400, 100);
        m_jScrollPane.setVisible(true);
    }

    public void hide()
    {
        if (m_jScrollPane.isVisible() == false)
            return;

        m_jScrollPane.setVisible(false);
        m_promptedNodes.clear();

        DefaultListModel listModel = (DefaultListModel) m_jList.getModel();
        listModel.clear();

        m_followedEditor.getDocument().removeDocumentListener(m_editTextListener);
    }

    private DocumentListener m_editTextListener = new DocumentListener() {

        private void restartQueryWorker()
        {
            if (m_queryWorker != null) {
                m_queryWorker.cancel(true);
            }

            m_queryWorker = new QueryWorker();
            m_queryWorker.execute();
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent)
        {
            m_logger.info("insert update");
            restartQueryWorker();
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent)
        {
            m_logger.info("remove update");
            restartQueryWorker();
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent)
        {
            //do nothing
        }
    };


    public class PromptedNode {
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
        @Override
        protected Boolean doInBackground()
        {
            ((DefaultListModel) m_jList.getModel()).removeAllElements();

            String inputed = m_followedEditor.getText();

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
            DefaultListModel listModel = (DefaultListModel) m_jList.getModel();

            for (PromptedNode promptedNode : promptedNodes) {
                m_logger.info("get promptedNode " + promptedNode.m_dbId);

                if (promptedNode.m_parentText != null) {
                    listModel.addElement(promptedNode.m_parentText + " -> " + promptedNode.m_text);
                } else  {
                    listModel.addElement("root: " + promptedNode.m_text);
                }
            }
        }
    };
};
