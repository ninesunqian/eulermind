package eulermind.view;

import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

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

public class TreeCursor extends NodeControl {

    MindView m_mindView;
    VisualTree m_tree;

    NodeItem m_originCursor;

    public ArrayList<NodeItem> m_selectedNodes = new ArrayList<>();

    ArrayList<NodeItem> m_xAxis;
    ArrayList<NodeItem> m_yAxis;

    int m_originXIndex;
    int m_originYIndex;

    int m_currentXIndex;
    int m_currentYIndex;

    boolean m_isHeld = false;

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    enum SelectMode {ONLY_ONE, ADD_ONE, SERIES};
    private SelectMode m_selectMode = SelectMode.ONLY_ONE;

    public TreeCursor(MindView mindView) {
        super(mindView);

        m_mindView = mindView;
        m_tree = mindView.m_visualTree;
        setCursorNodeItem((NodeItem) m_tree.getRoot());
    }

    public NodeItem getCursorNodeItem()
    {
        assert (m_selectedNodes.size() > 0);
        return m_selectedNodes.get(m_selectedNodes.size() - 1);
    }

    void copyNodeBetweenPreviousAndCurrentCursor(ArrayList<NodeItem> axis, NodeItem prevCursor, NodeItem currentCursor)
    {
        int preIndex = axis.indexOf(prevCursor);
        int curIndex = axis.indexOf(currentCursor);
        if (preIndex < curIndex) {
            m_selectedNodes.addAll(axis.subList(preIndex + 1, curIndex));

        } else {
            List<NodeItem> newSelectedNode = new ArrayList<>();
            newSelectedNode.addAll(axis.subList(curIndex + 1, preIndex));
            Collections.reverse(newSelectedNode);

            m_selectedNodes.addAll(newSelectedNode);
        }
    }

    private void selectNodeItem(NodeItem item)
    {
        if (m_selectMode == SelectMode.ONLY_ONE) {
            m_selectedNodes.clear();
            m_selectedNodes.add(item);

        } else if (m_selectMode == SelectMode.ADD_ONE) {
            if (! m_selectedNodes.contains(item)) {
                m_selectedNodes.add(item);

            } else {

                if (m_selectedNodes.size() > 1) {
                    m_selectedNodes.remove(item);
                }
            }

        } else {
            int currentCursorIndex = m_selectedNodes.lastIndexOf(item);

            if (currentCursorIndex == -1) {

                NodeItem prevCursor = m_selectedNodes.get(m_selectedNodes.size() - 1);

                if (m_xAxis == null || m_yAxis == null) {
                    buildXYAxis(prevCursor);
                }

                //只要两者在一个光标十字上，就可以。不管当前光标十字是 prevCursor的还是item的
                if (m_xAxis.contains(prevCursor) && m_xAxis.contains(item)) {
                    copyNodeBetweenPreviousAndCurrentCursor(m_xAxis, prevCursor, item);
                }
                if (m_yAxis.contains(prevCursor) && m_yAxis.contains(item)) {
                    copyNodeBetweenPreviousAndCurrentCursor(m_yAxis, prevCursor, item);
                }

                m_selectedNodes.add(item);

            } else {
                m_selectedNodes.subList(currentCursorIndex + 1, m_selectedNodes.size()).clear();
            }
        }
    }

    public void clearMultiSelectedNodeItems() {
        m_selectMode = SelectMode.ONLY_ONE;
        NodeItem cursor = m_selectedNodes.get(m_selectedNodes.size() - 1);
        m_selectedNodes.clear();
        m_selectedNodes.add(cursor);
    }

    public void setCursorNodeItem(NodeItem node)
    {
        m_originCursor = node;
        selectNodeItem(node);

        if (node == null) {
            int debug = 1;
        }

        //build m_xAxis and m_yAxis by lazy mode.
        //so we can setCurserNode before layout tree
        m_xAxis = null;
        m_yAxis = null;

        m_mindView.m_mindController.updateAllMindViews();

        //FIXME: 放在这里合适吗
        m_mindView.m_mindController.updateMindPropertyComponents(node);
    }

    private void buildXYAxis(NodeItem originCursor)
    {
        m_originCursor = originCursor;

        m_xAxis = new ArrayList<NodeItem>();
        m_yAxis = new ArrayList<NodeItem>();

        NodeItem root = (NodeItem)m_tree.getRoot();
        NodeItem lefter = m_originCursor;

        //add m_originCursor and lefters
        while (lefter != root) {
            m_xAxis.add(0, lefter);
            lefter = (NodeItem)lefter.getParent();
        }
        m_xAxis.add(0, root);

        //add righters
        NodeItem righterParent = m_originCursor;
        while (righterParent.getChildCount() > 0 && righterParent.isExpanded() == true) {
            Iterator children = righterParent.children();
            NodeItem righter = null;

            while (children.hasNext()) {
                NodeItem child = (NodeItem)children.next();

                if (righter == null ||
                        Math.abs(child.getY() - originCursor.getY()) < Math.abs(righter.getY() - originCursor.getY())) {
                    righter = child;
                }
            }
            m_xAxis.add(righter);

            righterParent = righter;
        }

        m_originXIndex = m_xAxis.indexOf(originCursor);

        //上下键 插入同级别的节点
        m_tree.depthFirstTraverse(m_tree.getRoot(), new Tree.DepthFirstTraverseProcessor() {
            @Override
            public boolean run(Node parent, Node node, int level) {
                NodeItem nodeItem = (NodeItem) node;
                if (level >= m_originXIndex || nodeItem.isExpanded() == false || nodeItem.getChildCount() == 0) {
                    m_yAxis.add(nodeItem);
                    return false;
                } else {
                    return true;
                }
            }
        });

        m_originYIndex = m_yAxis.indexOf(originCursor);

        m_currentXIndex = m_originXIndex;
        m_currentYIndex = m_originYIndex;
    }

    void moveLeft()
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (m_xAxis == null || m_currentYIndex != m_originYIndex) {
            buildXYAxis(getCursorNodeItem());
        }

        if (m_currentXIndex > 0) {
            m_currentXIndex--;

            selectNodeItem(m_xAxis.get(m_currentXIndex));
            m_mindView.renderTree();
            m_mindView.panToExposeItem(getCursorNodeItem());
        }
    }

    void moveRight()
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (m_xAxis == null || m_currentYIndex != m_originYIndex) {
            buildXYAxis(getCursorNodeItem());
        }

        if (m_currentXIndex < m_xAxis.size() - 1) {
            //如果该节点闭合了，焦点就不能右移动了。
            if (!getCursorNodeItem().isExpanded()) {
                return;
            }

        } else if (m_currentXIndex == m_xAxis.size() - 1) {
            //如果最右边的节点张开了，重新计算
            if (getCursorNodeItem().getChildCount() > 0 && getCursorNodeItem().isExpanded()) {
                buildXYAxis(m_originCursor);
            }
        }

        if (m_currentXIndex < m_xAxis.size() - 1) {
            m_currentXIndex++;

            selectNodeItem(m_xAxis.get(m_currentXIndex));
            m_mindView.renderTree();
            m_mindView.panToExposeItem(getCursorNodeItem());
        }
    }

    void moveUp()
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (m_xAxis == null || m_currentXIndex != m_originXIndex) {
            buildXYAxis(getCursorNodeItem());
        }
        if (m_currentYIndex > 0) {
            m_currentYIndex--;

            selectNodeItem(m_yAxis.get(m_currentYIndex));
            m_mindView.renderTree();
            m_mindView.panToExposeItem(getCursorNodeItem());
        }
    }

    void moveDown()
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (m_xAxis == null || m_currentXIndex != m_originXIndex) {
            buildXYAxis(getCursorNodeItem());
        }

        if (m_currentYIndex < m_yAxis.size() - 1) {
            m_currentYIndex++;

            selectNodeItem(m_yAxis.get(m_currentYIndex));
            m_mindView.renderTree();
            m_mindView.panToExposeItem(getCursorNodeItem());
        }
    }

    @Override
    public void nodeItemEntered(NodeItem item, MouseEvent e) {
        if (m_isHeld) {
            return;
        }

        if (e.isControlDown() || e.isShiftDown() || e.isMetaDown()) {
            return;
        }

        startCursorTimer(item);
    }

    @Override
    public void nodeItemExited(NodeItem item, MouseEvent e) {
        stopCursorTimer();
    }

    @Override
    public void nodeItemPressed(NodeItem item, MouseEvent e) {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (e.isControlDown() || e.isMetaDown()) {
            m_selectMode = SelectMode.ADD_ONE;

        } else if (e.isShiftDown()) {
            m_selectMode = SelectMode.SERIES;

        } else {
            m_selectMode = SelectMode.ONLY_ONE;
        }

        if (m_selectMode == SelectMode.ONLY_ONE && getCursorNodeItem() == item) {
            return;
        }

        setCursorNodeItem(item);
        m_mindView.renderTree();
    }

    private Timer m_cursorTimer;

    private void startCursorTimer(final NodeItem nodeItem)
    {
        m_cursorTimer = new Timer(100, new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                setCursorNodeItem(nodeItem);
            }
        });
        m_cursorTimer.setRepeats(false);
        m_cursorTimer.setCoalesce(true);
        m_cursorTimer.start();
    }

    private void stopCursorTimer()
    {
        if (m_cursorTimer != null) {
            m_cursorTimer.stop();
            m_cursorTimer = null;
        }
    }

    private void processKey(KeyEvent e) {
        if (m_isHeld) {
            return;
        }

        if (e.isControlDown() || e.isMetaDown()) {
            return;
        }

        m_selectMode = e.isShiftDown() ? SelectMode.SERIES : SelectMode.ONLY_ONE;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
                moveUp();
                break;

            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
                moveDown();
                break;

            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
                moveLeft();
                break;

            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
                moveRight();
                break;
        }

    }

    @Override
    public void nodeItemKeyPressed(NodeItem nodeItem, KeyEvent e) {
        processKey(e);
    }

    //当鼠标没有在节点上时，这个函数响应按键。 FIXME：当鼠标在一个边上呢
    @Override
    public void keyPressed(KeyEvent e) {
        processKey(e);
    }


    public void hold() {
        stopCursorTimer();
        m_isHeld = true;
    }

    public void free() {
        m_isHeld = false;
    }

    public List<NodeItem> getSelectedNodeItems() {
        return Collections.unmodifiableList(m_selectedNodes);
    }
}
