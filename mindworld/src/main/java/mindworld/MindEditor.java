package mindworld;

import com.tinkerpop.blueprints.Vertex;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-12-6
 * Time: 上午7:09
 * To change this template use File | Settings | File Templates.
 */
public class MindEditor extends JTextField {

    boolean m_hasPromptList;

    Set<MindEditorListener> m_mindEditorListeners = new LinkedHashSet<MindEditorListener>();

    Logger m_logger = LoggerFactory.getLogger(this.getClass());

    private Popup m_promptPopup;
    private JList m_promptList = new JList(new DefaultListModel());
    private JScrollPane m_promptScrollPane = new JScrollPane(m_promptList);

    private MindDB m_mindDb;

    private ArrayList<PromptedNode> m_promptedNodes = new ArrayList<PromptedNode>();
    private SwingWorker<Boolean, PromptedNode> m_queryWorker;

    MindEditor(MindDB mindDb) {
        super();
        init(mindDb);
    }

    MindEditor(int columns, MindDB mindDb)  {
        super(columns);
        init(mindDb);
    }

    MindEditor(String text, MindDB mindDb)  {
        super(text);
        init(mindDb);
    }

    MindEditor(String text, int columns, MindDB mindDb)  {
        super(text, columns);
        init(mindDb);
    }

    private void init(MindDB mindDb) {
        m_mindDb = mindDb;
        setHasPromptList(false);

        m_promptList = new JList(new DefaultListModel());
        m_promptScrollPane = new JScrollPane(m_promptList);

        m_promptScrollPane.setVisible(false);

        m_promptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_promptList.setLayoutOrientation(JList.VERTICAL);
        m_promptList.setPrototypeCellValue("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        m_promptList.setVisibleRowCount(10);
        m_promptList.setFocusable(false);

        addKeyListener(m_editorKeyListener);
        m_promptList.addMouseListener(m_prompterMouseListener);

        m_hasPromptList = false;

    }

    void setHasPromptList(boolean hasPromptList) {
        if (m_hasPromptList == hasPromptList) {
            return;
        }

        m_hasPromptList = hasPromptList;

        if (m_hasPromptList) {
            getDocument().addDocumentListener(m_editTextListener);
            //addHierarchyListener(m_hierarchyListener);
            addFocusListener(m_focusListener);
        } else {
            getDocument().removeDocumentListener(m_editTextListener);
            //removeHierarchyListener(m_hierarchyListener);
            removeFocusListener(m_focusListener);
        }
    }

    protected FocusListener m_focusListener = new FocusAdapter() {
        public void focusGained(FocusEvent e) {
            if (e.isTemporary()) {
                return;
            }

            showPrompter();

            //清空list放在focusGained，而不是focusLost, 防止focusLost与list的点击事件的处理冲突
            DefaultListModel listModel = (DefaultListModel) m_promptList.getModel();
            listModel.clear();
            m_logger.info("MindEditor: focusGained");
        }

        public void focusLost(FocusEvent e) {
            if (e.isTemporary()) {
                return;
            }

            hidePrompter();
            stopQueryWorker();
            m_logger.info("MindEditor: focusLost");
        }
    };

    private void stopQueryWorker()
    {
        if (m_queryWorker != null && ! m_queryWorker.isDone()) {
            m_queryWorker.cancel(true);
        }
        m_queryWorker = null;
    }

    private void startQueryWorker()
    {
        //SwingWorker 被设计为只执行一次。多次执行 SwingWorker 将不会调用两次 doInBackground 方法。
        //所以每次要 new一个新对象
        m_queryWorker = new QueryWorker();
        m_queryWorker.execute();
    }

    private void restartQueryWorker()
    {
        stopQueryWorker();
        startQueryWorker();
    }


    private DocumentListener m_editTextListener = new DocumentListener() {

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

    private class QueryWorker extends SwingWorker<Boolean, PromptedNode> {
        @Override
        protected Boolean doInBackground()
        {
            ((DefaultListModel) m_promptList.getModel()).removeAllElements();

            String inputed = getText();

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
            DefaultListModel listModel = (DefaultListModel) m_promptList.getModel();

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

    void showPrompter()
    {
        PopupFactory factory = PopupFactory.getSharedInstance();
        m_promptScrollPane.setSize(400, 100);
        m_promptScrollPane.setVisible(true);

        Point popupLocation = new Point(getX(),  getY() + getHeight());
        m_logger.info("promper local pos {}, {}", popupLocation.x, popupLocation.y);
        SwingUtilities.convertPointToScreen(popupLocation, this.getParent());
        m_logger.info("promper global pos {}, {}", popupLocation.x, popupLocation.y);
        m_promptPopup = factory.getPopup(getParent(), m_promptScrollPane, popupLocation.x, popupLocation.y);

        m_promptPopup.show();
    }

    void hidePrompter() {
        if (m_promptPopup != null) {
            m_promptPopup.hide();
            m_promptPopup = null;
        }
    }

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

    KeyListener m_editorKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e)
        {

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (e.isShiftDown()) {
                    int selectedIndex = m_promptList.getSelectedIndex();
                    PromptedNode selected = m_promptedNodes.get(selectedIndex);
                    firePromptListOk(selected.m_dbId, selected.m_text, selected.m_parentDBId, selected.m_parentText);
                } else {
                    m_logger.info("MindEditor: get enter key");
                    fireEditorOk(getText());
                }
            }

            else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)  {
                fireCancel();
            }
        }
    };


    MouseListener m_prompterMouseListener = new MouseAdapter() {
        public void mouseClicked(MouseEvent mouseEvent) {
            int selectedIndex = m_promptList.getSelectedIndex();
            PromptedNode selected = m_promptedNodes.get(selectedIndex);
            firePromptListOk(selected.m_dbId, selected.m_text, selected.m_parentDBId, selected.m_parentText);
        }
    };

    static public class MindEditorListener {
        public void editorOk(String text) {

        }

        public void cancel() {

        }

        public void promptListOk(Object dbId, String text, Object parentDBId, String parentText) {

        }
    }


    void addMindEditorListener(MindEditorListener listener)
    {
        m_mindEditorListeners.add(listener);
    }

    void fireEditorOk(String text) {
        for (MindEditorListener listener : m_mindEditorListeners) {
            listener.editorOk(text);
        }
    }

    void fireCancel() {
        for (MindEditorListener listener : m_mindEditorListeners) {
            listener.cancel();
        }
    }
    void firePromptListOk(Object dbId, String text, Object parentDBId, String parentText) {
        for (MindEditorListener listener : m_mindEditorListeners) {
            listener.promptListOk(dbId, text, parentDBId, parentText);
        }
    }
}
