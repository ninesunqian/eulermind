package excitedmind;

import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.util.collections.IntIterator;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;
import prefuse.visual.expression.VisiblePredicate;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Logger;

public class TreeCursor {
    VisualTree m_tree;

    NodeItem m_originCursor;

    NodeItem m_currentCursor;

    ArrayList<NodeItem> m_xAxis = new ArrayList<NodeItem>();
    ArrayList<NodeItem> m_yAxis = new ArrayList<NodeItem>();

    int m_originXIndex;
    int m_originYIndex;

    int m_currentXIndex;
    int m_currentYIndex;

    final Logger m_logger = Logger.getLogger(this.getClass().getName());

    public TreeCursor(VisualTree tree) {
        m_tree = tree;
        m_originCursor = (NodeItem)tree.getRoot();
        m_currentCursor = m_originCursor;
    }

    public NodeItem getCursorNode()
    {
        return m_currentCursor;
    }

    public void setCursorNode(NodeItem node)
    {
        buildXYAxis(node);
    }

    private void buildXYAxis(NodeItem originCursor)
    {
        m_originCursor = originCursor;
        m_currentCursor = originCursor;

        m_xAxis.clear();
        m_yAxis.clear();

        NodeItem root = (NodeItem)m_tree.getRoot();
        NodeItem node = m_originCursor;

        while (node != root) {
            m_xAxis.add(0, node);
            node = (NodeItem)node.getParent();
        }
        m_xAxis.add(0, root);
        //TODO: add right child nodes


        Table nodeTable = m_tree.getNodeTable();
        IntIterator allRows = nodeTable.rows(new VisiblePredicate());

        while (allRows.hasNext()) {
            int row = allRows.nextInt();
            node = (NodeItem) m_tree.getNode(row);
            if (overlayInXAxis(node, m_originCursor) || node.isExpanded() == false) {
                if (overlayInXAxis(node, m_originCursor)) {
                    m_logger.info(m_originCursor.getString("text") + ": overlay node: " + node.getString("text"));
                }
                m_yAxis.add(node);
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
        if (m_currentYIndex != m_originYIndex) {
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
        if (m_currentYIndex != m_originYIndex) {
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
        if (m_currentXIndex != m_originXIndex) {
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
        if (m_currentXIndex != m_originXIndex) {
            buildXYAxis(m_currentCursor);
        }

        if (m_currentYIndex < m_yAxis.size() - 1) {
            m_currentYIndex++;
        }

        m_currentCursor = m_yAxis.get(m_currentYIndex);
        return m_currentCursor;
    }
}
