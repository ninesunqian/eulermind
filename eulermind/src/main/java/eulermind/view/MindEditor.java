package eulermind.view;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.tinkerpop.blueprints.Vertex;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
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

public class MindEditor extends JTextField {

    boolean m_hasPromptList;

    MindEditorListener m_mindEditorListener;

    Logger m_logger = LoggerFactory.getLogger(this.getClass());

    private JList m_promptList = new JList(new DefaultListModel());
    private JScrollPane m_promptScrollPane = new JScrollPane(m_promptList);
    JPopupMenu m_popupMenu = new JPopupMenu();

    private MindDB m_mindDb;

    private ArrayList<PromptedNode> m_promptedNodes = new ArrayList<>();
    private SwingWorker<Boolean, PromptedNode> m_queryWorker;

    public MindEditor() {
        super();
        m_popupMenu.setLayout(new BorderLayout());
        m_popupMenu.add(m_promptScrollPane);

        m_promptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_promptList.setLayoutOrientation(JList.VERTICAL);
        m_promptList.setPrototypeCellValue("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        m_promptList.setVisibleRowCount(10);
        m_promptList.setFocusable(false);
        m_promptScrollPane.setFocusable(false);
        m_promptScrollPane.getVerticalScrollBar().setFocusable(false);
        m_promptScrollPane.getHorizontalScrollBar().setFocusable(false);

        //FIXME: here or after show ?
        m_popupMenu.setFocusable(false);

        addKeyListener(m_editorKeyListener);
        addMouseListener(m_editorMouseListener);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e)
            {
                if (m_hasPromptList) {
                    showPrompt();
                }
            }
            @Override
            public void componentHidden(ComponentEvent e)
            {
                m_popupMenu.setVisible(false);
            }
        });

        m_promptList.addMouseListener(m_prompterMouseListener);

        getDocument().addDocumentListener(m_editTextListener);
    }

    public void setMindDb(MindDB mindDb) {
        m_mindDb = mindDb;
    }

    public void setHasPromptList(boolean hasPromptList)
    {
        m_hasPromptList = hasPromptList;
    }


    Point computePopupPoint(int px,int py,int pw,int ph) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Rectangle screenBounds;

        // Calculate the desktop dimensions relative to the combo box.
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Point p = new Point();
        SwingUtilities.convertPointFromScreen(p, this);
        if (gc != null) {
            Insets screenInsets = toolkit.getScreenInsets(gc);
            screenBounds = gc.getBounds();
            screenBounds.width -= (screenInsets.left + screenInsets.right);
            screenBounds.height -= (screenInsets.top + screenInsets.bottom);
            screenBounds.x += (p.x + screenInsets.left);
            screenBounds.y += (p.y + screenInsets.top);
        }
        else {
            screenBounds = new Rectangle(p, toolkit.getScreenSize());
        }

        Rectangle rect = new Rectangle(px,py,pw,ph);
        if (py+ph > screenBounds.y+screenBounds.height
            && ph < screenBounds.height) {
            rect.y = -rect.height;
        }
        return rect.getLocation();
    }

    void showPrompt() {
        Point popupLocation = computePopupPoint(0, MindEditor.this.getBounds().height,
                m_promptScrollPane.getWidth(),
                m_promptScrollPane.getHeight());

        m_popupMenu.show(MindEditor.this, popupLocation.x, popupLocation.y);
    }


    MouseListener m_editorMouseListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e)
        {
            if (m_hasPromptList) {
                showPrompt();
            }
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
        ((DefaultListModel) m_promptList.getModel()).removeAllElements();
        m_promptedNodes.clear();

        //SwingWorker 被设计为只执行一次。多次执行 SwingWorker 将不会调用两次 doInBackground 方法。
        //所以每次要 new一个新对象
        m_queryWorker = new QueryWorker();
        m_queryWorker.execute();
    }

    private Timer m_queryDelayTimer;

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

        if (getText().isEmpty()) {
            ((DefaultListModel) m_promptList.getModel()).removeAllElements();
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

    KeyListener m_editorKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e)
        {
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case KeyEvent.VK_ENTER:
                    if (e.isShiftDown()) {
                        int selectedIndex = m_promptList.getSelectedIndex();
                        PromptedNode selected = m_promptedNodes.get(selectedIndex);
                        firePromptListOk(selected.m_dbId, selected.m_text, selected.m_parentDBId, selected.m_parentText);
                    } else {
                        fireEditorOk(getText());
                    }
                    break;

                case KeyEvent.VK_ESCAPE:
                    fireCancel();
                    break;

                case KeyEvent.VK_KP_UP:
                case KeyEvent.VK_UP:
                case KeyEvent.VK_KP_DOWN:
                case KeyEvent.VK_DOWN:
                    //TODO:
                    break;
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
        //doInBackground是在一个单独线程中运行的函数， 结果放在m_promptNodes中。 这里不能添加ListModel
        @Override
        protected Boolean doInBackground()
        {
            //整个数据库查找, 如果打开orientdb的线程与操作数据库的线程不是同一个, 需要调用:
            ODatabaseRecordThreadLocal.INSTANCE.set(m_mindDb.m_graph.getRawGraph());

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
                    listModel.addElement(promptedNode.m_text + " @ " + promptedNode.m_parentText);
                } else  {
                    listModel.addElement("root: " + promptedNode.m_text);
                }
            }
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


}
