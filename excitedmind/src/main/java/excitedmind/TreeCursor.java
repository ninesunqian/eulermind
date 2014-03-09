package excitedmind;

import prefuse.data.Table;
import prefuse.util.collections.IntIterator;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;
import prefuse.visual.expression.VisiblePredicate;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.logging.Logger;

public class TreeCursor {
    VisualTree m_tree;

    NodeItem m_originCursor;

    NodeItem m_currentCursor;

    ArrayList<NodeItem> m_xAxis;
    ArrayList<NodeItem> m_yAxis;

    int m_originXIndex;
    int m_originYIndex;

    int m_currentXIndex;
    int m_currentYIndex;

    final Logger m_logger = Logger.getLogger(this.getClass().getName());

    public TreeCursor(VisualTree tree) {
        m_tree = tree;
        setCursorNodeItem((NodeItem) tree.getRoot());
    }

    public NodeItem getCursorNodeItem()
    {
        return m_currentCursor;
    }

    public void setCursorNodeItem(NodeItem node)
    {
        m_originCursor = node;
        m_currentCursor = node;

        //build m_xAxis and m_yAxis by lazy mode.
        //so we can setCurserNode before layout tree
        m_xAxis = null;
        m_yAxis = null;
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
        NodeItem righter = m_originCursor;
        while (righter.getChildCount() > 0 && righter.isExpanded() == true) {
            Iterator children = righter.children();
            NodeItem right = null;

            while (children.hasNext()) {
                NodeItem child = (NodeItem)children.next();

                if (right == null ||
                        Math.abs(child.getY() - righter.getY()) < Math.abs(right.getY() - righter.getY())) {
                    right = child;
                }
            }
            m_xAxis.add(right);

            righter = right;
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
                    m_logger.fine(m_originCursor.getString("text") + ": overlay upDowner: " + upDowner.getString("text"));
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


    public NodeItem moveLeft()
    {
        if (m_xAxis == null || m_currentYIndex != m_originYIndex) {
            buildXYAxis(m_currentCursor);
        }

        if (m_currentXIndex > 0) {
            m_currentXIndex--;
        }

        m_currentCursor = m_xAxis.get(m_currentXIndex);

        return m_currentCursor;
    }

    public NodeItem moveRight()
    {
        if (m_xAxis == null || m_currentYIndex != m_originYIndex) {
            buildXYAxis(m_currentCursor);
        }

        if (m_currentXIndex < m_xAxis.size() - 1) {
            m_currentXIndex++;
        }

        m_currentCursor = m_xAxis.get(m_currentXIndex);

        return m_currentCursor;
    }

    public NodeItem moveUp()
    {
        if (m_xAxis == null || m_currentXIndex != m_originXIndex) {
            buildXYAxis(m_currentCursor);
        }
        if (m_currentYIndex > 0) {
            m_currentYIndex--;
        }

        m_currentCursor = m_yAxis.get(m_currentYIndex);
        return m_currentCursor;
    }

    public NodeItem moveDown()
    {
        if (m_xAxis == null || m_currentXIndex != m_originXIndex) {
            buildXYAxis(m_currentCursor);
        }

        if (m_currentYIndex < m_yAxis.size() - 1) {
            m_currentYIndex++;
        }

        m_currentCursor = m_yAxis.get(m_currentYIndex);
        return m_currentCursor;
    }
}
