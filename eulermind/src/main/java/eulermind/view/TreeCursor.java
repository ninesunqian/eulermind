package eulermind.view;

import prefuse.data.Table;
import prefuse.util.collections.IntIterator;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;
import prefuse.visual.expression.VisiblePredicate;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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

    public NodeItem m_currentCursor;

    ArrayList<NodeItem> m_xAxis;
    ArrayList<NodeItem> m_yAxis;

    int m_originXIndex;
    int m_originYIndex;

    int m_currentXIndex;
    int m_currentYIndex;

    boolean m_isHeld = false;

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    public TreeCursor(MindView mindView) {
        super(mindView);

        m_mindView = mindView;
        m_tree = mindView.m_visualTree;
        setCursorNodeItem((NodeItem) m_tree.getRoot());
    }

    public NodeItem getCursorNodeItem()
    {
        return m_currentCursor;
    }

    public void setCursorNodeItem(NodeItem node)
    {
        m_originCursor = node;
        m_currentCursor = node;

        if (node == null) {
            int debug = 1;
        }

        //build m_xAxis and m_yAxis by lazy mode.
        //so we can setCurserNode before layout tree
        m_xAxis = null;
        m_yAxis = null;

        m_mindView.renderTree();

        //FIXME: 放在这里合适吗
        m_mindView.m_mindController.updateNodePropertyComponent(node);
    }

    private void buildXYAxis(NodeItem originCursor)
    {
        m_originCursor = originCursor;
        m_currentCursor = originCursor;

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
                        Math.abs(child.getY() - righterParent.getY()) < Math.abs(righter.getY() - righterParent.getY())) {
                    righter = child;
                }
            }
            m_xAxis.add(righter);

            righterParent = righter;
        }

        //fill uper and downer
        Table nodeTable = m_tree.getNodeTable();
        IntIterator allRows = nodeTable.rows(new VisiblePredicate());
        NodeItem upDowner;

        while (allRows.hasNext()) {
            int row = allRows.nextInt();
            upDowner = (NodeItem) m_tree.getNode(row);
            if (overlayInXAxis(upDowner, m_originCursor) || upDowner.isExpanded() == false) {
                if (overlayInXAxis(upDowner, m_originCursor)) {
                    m_logger.debug(m_originCursor.getString("text") + ": overlay upDowner: " + upDowner.getString("text"));
                }
                m_yAxis.add(upDowner);
            }
        }

        Collections.sort(m_yAxis, new Comparator<NodeItem>() {
            @Override
            public int compare(NodeItem n1, NodeItem n2) {
                return (int)(n1.getY() - n2.getY());
            }
        });

        m_originXIndex = m_xAxis.indexOf(originCursor);
        m_originYIndex = m_yAxis.indexOf(originCursor);

        m_currentXIndex = m_originXIndex;
        m_currentYIndex = m_originYIndex;
    }


    boolean overlayInXAxis(NodeItem n1, NodeItem n2) {
        Rectangle2D bounds1 = n1.getBounds();
        Rectangle2D bounds2 = n2.getBounds();

        double left1 = bounds1.getMinX();
        double right1 = bounds1.getMaxX();

        double left2 = bounds2.getMinX();
        double right2 = bounds2.getMaxX();

        if (left1 < left2) {
            return left2 < right1;
        } else {
            return left1 < right2;
        }
    }

    void moveLeft()
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (m_xAxis == null || m_currentYIndex != m_originYIndex) {
            buildXYAxis(m_currentCursor);
        }

        if (m_currentXIndex > 0) {
            m_currentXIndex--;

            m_currentCursor = m_xAxis.get(m_currentXIndex);
            m_mindView.renderTree();
        }
    }

    void moveRight()
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (m_xAxis == null || m_currentYIndex != m_originYIndex) {
            buildXYAxis(m_currentCursor);
        }

        if (m_currentXIndex < m_xAxis.size() - 1) {
            //如果该节点闭合了，焦点就不能右移动了。
            if (!m_currentCursor.isExpanded()) {
                return;
            }

        } else if (m_currentXIndex == m_xAxis.size() - 1) {
            //如果最右边的节点张开了，重新计算
            if (m_currentCursor.getChildCount() > 0 && m_currentCursor.isExpanded()) {
                buildXYAxis(m_originCursor);
            }
        }

        if (m_currentXIndex < m_xAxis.size() - 1) {
            m_currentXIndex++;

            m_currentCursor = m_xAxis.get(m_currentXIndex);
            m_mindView.renderTree();
        }
    }

    void moveUp()
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (m_xAxis == null || m_currentXIndex != m_originXIndex) {
            buildXYAxis(m_currentCursor);
        }
        if (m_currentYIndex > 0) {
            m_currentYIndex--;

            m_currentCursor = m_yAxis.get(m_currentYIndex);
            m_mindView.renderTree();
        }
    }

    void moveDown()
    {
        stopCursorTimer();

        if (m_isHeld) {
            return;
        }

        if (m_xAxis == null || m_currentXIndex != m_originXIndex) {
            buildXYAxis(m_currentCursor);
        }

        if (m_currentYIndex < m_yAxis.size() - 1) {
            m_currentYIndex++;

            m_currentCursor = m_yAxis.get(m_currentYIndex);
            m_mindView.renderTree();
        }
    }

    @Override
    public void nodeItemEntered(NodeItem item, MouseEvent e) {
        if (m_isHeld) {
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

        if (item != m_currentCursor) {
            setCursorNodeItem(item);
            m_mindView.renderTree();
        }
    }

    private Timer m_cursorTimer;

    private void startCursorTimer(final NodeItem nodeItem)
    {
        m_cursorTimer = new Timer(500, new ActionListener() {
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

}
