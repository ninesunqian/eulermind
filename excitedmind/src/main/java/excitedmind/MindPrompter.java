package excitedmind;

import com.tinkerpop.blueprints.Vertex;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 13-12-25
 * Time: 上午7:10
 * To change this template use File | Settings | File Templates.
 */
public class MindPrompter {

    Logger m_logger = Logger.getLogger(this.getClass().getName());

    private JList m_jList = new JList(new DefaultListModel());
    private JScrollPane m_jScrollPane = new JScrollPane(m_jList);

    private JTextComponent m_followedEditor;
    private DBTree m_dbTree;

    private ArrayList<PromptedNode> m_promptedNodes = new ArrayList<PromptedNode>();

    private SwingWorker<Boolean, PromptedNode> m_queryWorker;

    public MindPrompter(JComponent parentView, DBTree dbTree)
    {
        m_jList = new JList(new DefaultListModel());
        m_jScrollPane = new JScrollPane(m_jList);

        m_jScrollPane.setVisible(false);
        parentView.add(m_jScrollPane);

        m_jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_jList.setLayoutOrientation(JList.VERTICAL);
        m_jList.setPrototypeCellValue("WWW");
        m_jList.setVisibleRowCount(8);
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

        m_jScrollPane.setLocation(editor.getX(), editor.getY() + editor.getHeight());
        m_jScrollPane.setSize(100, 100);
        m_jScrollPane.setVisible(true);
    }

    public void hide()
    {
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
            m_text = vertex.getProperty(MindTree.sm_textPropName);

            DBTree.EdgeVertex edgeVertex = m_dbTree.getParent(vertex);
            m_parentDBId = edgeVertex.m_vertex.getId();
            m_parentText = edgeVertex.m_vertex.getProperty(MindTree.sm_textPropName);
        }
    }


    private class QueryWorker extends SwingWorker<Boolean, PromptedNode> {
        @Override
        protected Boolean doInBackground()
        {
            ((DefaultListModel) m_jList.getModel()).removeAllElements();

            String inputed = m_followedEditor.getText();

            m_logger.info("query vertex: " + inputed);

            for (Vertex vertex : m_dbTree.getVertices(MindTree.sm_textPropName, inputed)) {
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
                listModel.addElement(promptedNode.m_parentText + " -> " + promptedNode.m_text);
            }
        }
    };
};