package excitedmind;

import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTree;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 13-12-8
 * Time: 下午8:30
 * To change this template use File | Settings | File Templates.
 */
public class TreeCursor {
    VisualTree m_tree;

    NodeItem m_originCursor;
    NodeItem m_currentCursor;

    ArrayList<NodeItem> m_xAxis;
    ArrayList<NodeItem> m_yAxis;

    int m_xIndex;
    int m_yIndex;

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
        m_originCursor = node;
        m_currentCursor = node;
    }

    private void buildXAxis(NodeItem node)
    {

    }

    private void buildYAxis(NodeItem node)
    {

    }

    public NodeItem moveLeft()
    {
        return m_currentCursor;

    }

    public NodeItem moveRight()
    {
        return m_currentCursor;

    }

    public NodeItem moveUp()
    {
        return m_currentCursor;

    }

    public NodeItem moveDown()
    {
        return m_currentCursor;

    }
}
