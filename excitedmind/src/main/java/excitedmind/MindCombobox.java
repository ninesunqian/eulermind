package excitedmind;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-3-30
 * Time: 下午2:04
 * To change this template use File | Settings | File Templates.
 */
public class MindCombobox extends JComboBox{

    class MindComboBoxEditor implements ComboBoxEditor {
        final protected JTextField m_editor;

        public MindComboBoxEditor() {
            m_editor = new JTextField();
            //TODO m_editor->textchanged, call prompter
        }

        public Component getEditorComponent() {
            return m_editor;
        }

        public Object getItem() {
            return m_editor.getText();
        }

        public void addActionListener(ActionListener l) {
            m_editor.addActionListener(l);
        }

        public void removeActionListener(ActionListener l) {
            m_editor.removeActionListener(l);
        }

        public void selectAll() {
        }

        public void setItem(Object newValue) {
        }
    }

    MindCombobox()
    {
        MindComboBoxEditor editor = new MindComboBoxEditor();
        this.setEditor(editor);

        editor.getEditorComponent().

    }


    Logger m_logger = Logger.getLogger(this.getClass().getName());

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
        m_followedEditor = editor;

        m_jScrollPane.setLocation(editor.getX(), editor.getY() + editor.getHeight());
        m_jScrollPane.setSize(100, 100);
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

            MindDB.EdgeVertex edgeVertex = m_mindDb.getParent(vertex);
            if (edgeVertex == null) {
                m_parentDBId = null;
                m_parentText = null;
            } else {
                m_parentDBId = edgeVertex.m_vertex.getId();
                m_parentText = edgeVertex.m_vertex.getProperty(MindModel.sm_textPropName);
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

            for (Vertex vertex : m_mindDb.getVertices(MindModel.sm_textPropName, inputed)) {
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
}
