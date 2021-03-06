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
import eulermind.Utils;
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

public class MindEditor extends JTextArea {

    boolean m_hasPromptList;

    MindEditorListener m_mindEditorListener;

    Logger m_logger = LoggerFactory.getLogger(this.getClass());

    private JList m_promptList = new JList(new DefaultListModel());
    private JScrollPane m_promptScrollPane = new JScrollPane(m_promptList);
    JPopupMenu m_popupMenu = new JPopupMenu();

    private MindDB m_mindDb;

    private ArrayList<PromptedNode> m_promptedNodes = new ArrayList<>();
    private SwingWorker<Boolean, PromptedNode> m_queryWorker;

    JComponent m_innerFocus = this;

    int m_minWidth = 0;
    int m_minHeight = 0;

    public MindEditor() {
        super();
        m_popupMenu.setLayout(new BorderLayout());
        m_popupMenu.add(m_promptScrollPane);

        m_promptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_promptList.setLayoutOrientation(JList.VERTICAL);
        m_promptList.setPrototypeCellValue("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        m_promptList.setVisibleRowCount(15);

        //下拉菜单不能有输入焦点，否则会有焦点切换问题
        m_promptList.setFocusable(false);
        m_promptScrollPane.setFocusable(false);
        m_promptScrollPane.getVerticalScrollBar().setFocusable(false);
        m_promptScrollPane.getHorizontalScrollBar().setFocusable(false);
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
        getDocument().addDocumentListener(m_adjustSizeListener);

        setBorder(BorderFactory.createEtchedBorder());
        setLineWrap(true);
        setWrapStyleWord(true);
        setTabSize(4);
    }

    @Override
    protected int getRowHeight() {
        FontMetrics metrics = getFontMetrics(getFont());
        return metrics.getHeight();
    }

    @Override
    public void setMinimumSize(Dimension minimumSize) {
        m_minWidth = minimumSize.width;
        m_minHeight = minimumSize.height;
    }

    private void innerFocusEditor()
    {
        m_innerFocus = this;
        this.setForeground(new Color(0, 0, 0));
    }

    private void innerFocusPromptList()
    {
        m_innerFocus = m_promptList;
        this.setForeground(new Color(128, 128, 128));
    }

    public void setMindDb(MindDB mindDb) {
        m_mindDb = mindDb;
    }

    public void setHasPromptList(boolean hasPromptList)
    {
        m_hasPromptList = hasPromptList;
    }

    Point computePopupScreenPoint(Rectangle editorBounds,
                                  int popupWidth, int popupHeight,
                                  Rectangle screenBounds)
    {
        int editorLeft = editorBounds.x;
        int editorRight = editorBounds.x + editorBounds.width;
        int editorTop = editorBounds.y;
        int editorBottom = editorBounds.y + editorBounds.height;

        int popupLeft;
        int popupRight;

        int popupTop;
        int popupBottom;

        int screenRight = screenBounds.x + screenBounds.width;
        int screenBottom = screenBounds.y + screenBounds.height;

        if (editorLeft + popupWidth <= screenRight) {
            popupLeft = editorLeft;
            popupRight = popupLeft + popupWidth;
        } else {
            popupRight = screenRight;
            popupLeft = popupRight - popupWidth;
        }

        if (editorBottom + popupHeight <= screenBottom) {
            popupTop = editorBottom;
            popupBottom = popupTop + popupHeight;
        } else {
            popupBottom = editorTop;
            popupTop = popupBottom - popupHeight;
        }
        this.getSize();

        return new Point(popupLeft, popupTop);
    }

    void showPrompt() {
        Point p = new Point();
        SwingUtilities.convertPointToScreen(p, this);

        Rectangle editorBounds = new Rectangle(p.x, p.y, getWidth(), getHeight());
        Rectangle screenBounds = Utils.getScreenBounds(this);
        p = computePopupScreenPoint(editorBounds, m_popupMenu.getWidth(), m_popupMenu.getHeight(), screenBounds);

        SwingUtilities.convertPointFromScreen(p, this);
        m_logger.info("ppppppppppppp, show");

        //去除被删除的搜索结果
        {
            boolean hasTrashedNode = false;
            Iterator<PromptedNode> iter = m_promptedNodes.iterator();
            while(iter.hasNext()){
                PromptedNode promptedNode = iter.next();
                if(m_mindDb.isVertexTrashed(m_mindDb.getVertex(promptedNode.m_dbId))) {
                    iter.remove();
                    hasTrashedNode = true;
                }
            }
            if (hasTrashedNode) {
                DefaultListModel listModel = (DefaultListModel)m_promptList.getModel();
                listModel.removeAllElements();
                for (PromptedNode promptedNode : m_promptedNodes) {
                    if (promptedNode.m_parentText != null) {
                        listModel.addElement(promptedNode.m_text + " @ " + promptedNode.m_parentText);
                    } else  {
                        listModel.addElement("root: " + promptedNode.m_text);
                    }
                }
            }
        }

        m_popupMenu.show(MindEditor.this, p.x, p.y);
    }

    MouseListener m_editorMouseListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e)
        {
            innerFocusEditor();
            m_logger.info("ppppppppppppp, show");
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
        clearSearchResults();

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

    public void clearSearchResults()
    {
        ((DefaultListModel) m_promptList.getModel()).removeAllElements();
        m_promptedNodes.clear();

    }

    private void updatePromptList()
    {
        if (m_queryDelayTimer != null) {
            m_queryDelayTimer.stop();
            m_queryDelayTimer = null;
        }

        stopQueryWorker();

        if (getText().isEmpty()) {
            clearSearchResults();

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

    private final DocumentListener m_adjustSizeListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            adjustSizeByText();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            adjustSizeByText();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            adjustSizeByText();
        }
    };

    private void adjustSizeByText()
    {
        String text = getText();
        Dimension exceptedSize = MyLabelRenderer.computeTextDimensions(text, getFont(), 600);
        int rowHeight = getRowHeight();
        m_logger.info("rowHeight: {}", rowHeight);

        exceptedSize.width += 5; //插入符宽度

        Insets margin = getMargin();
        exceptedSize.width += margin.left + margin.right;
        exceptedSize.height += margin.top + margin.bottom;

        exceptedSize.width = Math.max(exceptedSize.width, m_minWidth);
        exceptedSize.height = Math.max(exceptedSize.height, m_minHeight);

        Dimension nowSize = getSize();
        if (!nowSize.equals(exceptedSize)) {
            setSize(exceptedSize);
        }
    }

    void afterFireMindEditorEvent()
    {
        m_popupMenu.setVisible(false);
        innerFocusEditor();
    }

    public void confirm()
    {
        if (m_innerFocus == MindEditor.this) {
            fireEditorOk(getText());
            afterFireMindEditorEvent();
        }
        else {
            int selectedIndex = m_promptList.getSelectedIndex();
            PromptedNode selected = m_promptedNodes.get(selectedIndex);
            firePromptListOk(selected.m_dbId, selected.m_text, selected.m_parentDBId, selected.m_parentText);
            afterFireMindEditorEvent();
        }
    }

    public void cancel()
    {
        if (m_innerFocus == m_promptList) {
            innerFocusEditor();
        } else {
            fireCancel();
            afterFireMindEditorEvent();
        }
    }

    KeyListener m_editorKeyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e)
        {
            int keyCode = e.getKeyCode();
            switch (keyCode) {
                case KeyEvent.VK_ENTER:
                    if (e.isShiftDown()) {
                        append("\n");

                    } else {
                        confirm();
                    }
                    break;

                case KeyEvent.VK_ESCAPE:
                    cancel();
                    break;

                case KeyEvent.VK_KP_UP:
                case KeyEvent.VK_UP:
                case KeyEvent.VK_KP_DOWN:
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_PAGE_DOWN:
                    if (m_hasPromptList) {
                        if (m_innerFocus != m_promptList) {
                            innerFocusPromptList();
                            if (m_promptList.getSelectedIndex() == -1) {
                                m_promptList.setSelectedIndex(0);
                            }
                        } else {
                            m_promptList.dispatchEvent(e);
                        }
                    }
                    break;

                default:
                    innerFocusEditor();
            }
        }
    };

    MouseListener m_prompterMouseListener = new MouseAdapter() {
        public void mouseClicked(MouseEvent mouseEvent) {
            int selectedIndex = m_promptList.getSelectedIndex();
            PromptedNode selected = m_promptedNodes.get(selectedIndex);
            firePromptListOk(selected.m_dbId, selected.m_text, selected.m_parentDBId, selected.m_parentText);
            m_popupMenu.setVisible(false);
            innerFocusEditor();
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
            m_text = vertex.getProperty(MindModel.TEXT_PROP_NAME);

            MindDB.EdgeVertex edgeParent = m_mindDb.getParentEge(vertex);
            if (edgeParent == null) {
                m_parentDBId = null;
                m_parentText = null;
            } else {
                m_parentDBId = edgeParent.m_source.getId();
                m_parentText = edgeParent.m_source.getProperty(MindModel.TEXT_PROP_NAME);
            }
        }
    }


    private class QueryWorker extends SwingWorker<Boolean, PromptedNode> {
        //doInBackground是在一个单独线程中运行的函数， 结果放在m_promptNodes中。 这里不能添加ListModel
        @Override
        protected Boolean doInBackground()
        {
            /*
            整个数据库查找, 如果打开orientdb的线程与操作数据库的线程不是同一个, 需要调用:
            ODatabaseRecordThreadLocal.INSTANCE.set(m_mindDb.m_graph.getRawGraph());
            */

            String inputed = getText();

            m_logger.info("query vertex: " + inputed);

            for (Vertex vertex : m_mindDb.getVertices(MindModel.TEXT_PROP_NAME, inputed))  {

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
