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

    public TreeCursor(MindView mindView) {
        super(mindView);

        m_mindView = mindView;
        m_tree = mindView.m_visualTree;
        moveToNodeItem((NodeItem) m_tree.getRoot(), SelectMode.ONLY_ONE);
    }

    private void addNodeBetweenPreviousAndCurrentCursor(ArrayList<NodeItem> axis, NodeItem prevCursor, NodeItem currentCursor)
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

    private void selectNodeItem(final NodeItem item, SelectMode m_selectMode)
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

                assert m_xAxis != null;
                assert m_yAxis != null;

                //只要两者在一个光标十字上，就可以。不管当前光标十字是 prevCursor的还是item的
                if (m_xAxis.contains(prevCursor) && m_xAxis.contains(item)) {
                    addNodeBetweenPreviousAndCurrentCursor(m_xAxis, prevCursor, item);
                }
                if (m_yAxis.contains(prevCursor) && m_yAxis.contains(item)) {
                    addNodeBetweenPreviousAndCurrentCursor(m_yAxis, prevCursor, item);
                }

                m_selectedNodes.add(item);

            } else {
                m_selectedNodes.subList(currentCursorIndex + 1, m_selectedNodes.size()).clear();
            }
        }

        m_mindView.renderTreeAfterCursorChanging(item);
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

    private void moveLeft(SelectMode selectMode)
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        //竖直移动后，又水平移动，需要更新光标十字
        if (m_xAxis == null || m_currentYIndex != m_originYIndex) {
            buildXYAxis(getLastSelectedNodeItem());
        }

        if (m_currentXIndex > 0) {
            m_currentXIndex--;

            selectNodeItem(m_xAxis.get(m_currentXIndex), selectMode);
        }
    }

    private void moveRight(SelectMode selectMode)
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        //竖直移动后，又水平移动，需要更新光标十字
        if (m_xAxis == null || m_currentYIndex != m_originYIndex) {
            buildXYAxis(getLastSelectedNodeItem());
        }

        if (m_currentXIndex < m_xAxis.size() - 1) {
            //如果该节点闭合了，焦点就不能右移动了。
            if (!getLastSelectedNodeItem().isExpanded()) {
                return;
            }

        } else if (m_currentXIndex == m_xAxis.size() - 1) {
            //如果最右边的节点张开了，重新计算
            if (getLastSelectedNodeItem().getChildCount() > 0 && getLastSelectedNodeItem().isExpanded()) {
                buildXYAxis(m_originCursor);
            }
        }

        if (m_currentXIndex < m_xAxis.size() - 1) {
            m_currentXIndex++;

            selectNodeItem(m_xAxis.get(m_currentXIndex), selectMode);
        }
    }

    private void moveUp(SelectMode selectMode)
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        //水平移动后又竖直移动，需要更新光标十字
        if (m_xAxis == null || m_currentXIndex != m_originXIndex) {
            buildXYAxis(getLastSelectedNodeItem());
        }
        if (m_currentYIndex > 0) {
            m_currentYIndex--;

            selectNodeItem(m_yAxis.get(m_currentYIndex), selectMode);
        }
    }

    private void moveDown(SelectMode selectMode)
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        //水平移动后又竖直移动，需要更新光标十字
        if (m_xAxis == null || m_currentXIndex != m_originXIndex) {
            buildXYAxis(getLastSelectedNodeItem());
        }

        if (m_currentYIndex < m_yAxis.size() - 1) {
            m_currentYIndex++;

            selectNodeItem(m_yAxis.get(m_currentYIndex), selectMode);
        }
    }

    private void moveToNodeItem(NodeItem node, SelectMode selectMode)
    {
        m_originCursor = node;
        buildXYAxis(node);
        selectNodeItem(node, selectMode);
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
    public void nodeItemReleased(NodeItem item, MouseEvent e) {
        stopCursorTimer();
        SelectMode m_selectMode;

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

        if (m_selectMode == SelectMode.ONLY_ONE && getLastSelectedNodeItem() == item) {
            return;
        }

        moveToNodeItem(item, m_selectMode);
    }

    private Timer m_cursorTimer;

    private void startCursorTimer(final NodeItem nodeItem)
    {
        m_cursorTimer = new Timer(100, new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
                //多选状态下，禁止鼠标跟随 (会无意中选中多个节点)
                if (m_selectedNodes.size() > 1) {
                    return;
                }

                moveToNodeItem(nodeItem, SelectMode.ONLY_ONE);
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

    public void keyPressed(KeyEvent e) {
        if (m_isHeld) {
            return;
        }

        if (e.isControlDown() || e.isMetaDown()) {
            return;
        }

        SelectMode selectMode = e.isShiftDown() ? SelectMode.SERIES : SelectMode.ONLY_ONE;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
                moveUp(selectMode);
                break;

            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
                moveDown(selectMode);
                break;

            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
                moveLeft(selectMode);
                break;

            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
                moveRight(selectMode);
                break;
        }
    }

    @Override
    public void nodeItemKeyPressed(NodeItem nodeItem, KeyEvent e) {
        keyPressed(e);
    }


    public void hold() {
        stopCursorTimer();
        m_isHeld = true;
    }

    public void free() {
        m_isHeld = false;
    }


    public NodeItem getLastSelectedNodeItem()
    {
        assert (m_selectedNodes.size() > 0);
        return m_selectedNodes.get(m_selectedNodes.size() - 1);
    }

    public List<NodeItem> getSelectedNodeItems() {
        return Collections.unmodifiableList(m_selectedNodes);
    }

    public void selectOnlyOneNodeItem(NodeItem nodeItem)
    {
        moveToNodeItem(nodeItem, SelectMode.ONLY_ONE);
    }

    public void selectNodeItems(List<NodeItem> nodeItems)
    {
        boolean hasSelected = false;
        for (NodeItem nodeItem : nodeItems) {
            if (!nodeItem.isValid()) {
                continue;
            }

            if (! hasSelected) {
                moveToNodeItem(nodeItem, SelectMode.ONLY_ONE);
                hasSelected = true;
            } else {
                moveToNodeItem(nodeItem, SelectMode.ADD_ONE);
            }
        }

        if (!hasSelected) {
            moveToNodeItem((NodeItem)m_tree.getRoot(), SelectMode.ONLY_ONE);
        }
    }

    public void checkSelectNodeItemsValid()
    {
        boolean allNodeValid = true;
        for (NodeItem nodeItem : m_selectedNodes) {
            if (!nodeItem.isValid()) {
                allNodeValid = false;
            }
        }

        if (allNodeValid) {
            return;
        }

        ArrayList<NodeItem> copySelection = new ArrayList<>();
        copySelection.addAll(m_selectedNodes);
        selectNodeItems(copySelection);
    }
}
