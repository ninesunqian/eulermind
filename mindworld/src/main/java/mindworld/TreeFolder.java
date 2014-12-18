package mindworld;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;

import prefuse.Visualization;
import prefuse.data.*;

import prefuse.util.PrefuseLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-2-28
 * Time: 下午10:42
 * To change this template use File | Settings | File Templates.
 */
public class TreeFolder extends NodeControl{

    private LinkedHashSet<Integer> m_foldedNodes = new LinkedHashSet<Integer>();
    VisualTree m_tree;
    MindView m_mindView;

    TreeFolder(MindView mindView)
    {
        super(mindView);
        m_mindView = mindView;
        m_tree = mindView.m_visualTree;
    }

    void unfoldNode(NodeItem node)
    {
        if (! MindModel.childrenAttached(m_mindView.toSource(node))) {
            m_mindView.m_mindModel.attachChildren(m_mindView.toSource(node));
        }

        if (node.getChildCount() == 0 || node.isExpanded()) {
            return;
        }

        assert (m_foldedNodes.contains(node.getRow()));

        m_foldedNodes.remove(node.getRow());

        final Visualization vis = node.getVisualization();
        final Node unfoldTreeRoot = node;
        final String group = node.getGroup();

        //unfold descendants deeply, to the folded descendants
        m_tree.deepTraverse(node,new Tree.Processor() {
            public boolean run(Node node, int level) {

                if (node == unfoldTreeRoot) {
                    return true;
                }

                TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
                TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();

                //s_logger.info ( "visiableNode " + m_mindTree.getText(node));
                PrefuseLib.updateVisible(visualNode, true);
                PrefuseLib.updateVisible(visualEdge, true);

                if (m_foldedNodes.contains(node.getRow())) {
                    return false;
                } else {
                    return true;
                }
            }
        }, 0);

        node.setExpanded(true);
    }

    public void foldNode(NodeItem node)
    {
        final Visualization vis = node.getVisualization();
        final String group = node.getGroup();

        if (!node.isExpanded())
        {
            return;
        }

        m_foldedNodes.add(node.getRow());

        final Node foldTreeRoot = node;

        //set descendants unvisible deeply, to the folded descendants
        m_tree.deepTraverse(node, new Tree.Processor() {
            public boolean run(Node node, int level) {
                if (node == foldTreeRoot)
                {
                    return true;
                }

                TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
                TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();

                PrefuseLib.updateVisible(visualNode, false);
                PrefuseLib.updateVisible(visualEdge, false);

                //s_logger.info ( "invisiableNode " + text);
                if (m_foldedNodes.contains(node.getRow())) {
                    return false;
                } else {
                    return true;
                }
            }
        }, 0);


        node.setExpanded(false);
    }

    public boolean isFolded(NodeItem node)
    {
        if (node.getChildCount() > 0) {
            return ! node.isExpanded();
        } else {
            return MindModel.getDBChildCount(node) > 0;
        }
    }

    public void toggleFoldNode(NodeItem node)
    {
        if (m_mindView.getCursorNodeItem() != node) {
            return;
        }

        if (! m_mindView.beginChanging()) {
            return;
        }

        if (isFolded(node)) {
            unfoldNode(node);
        }
        else {
            foldNode(node);
        }

        m_mindView.renderTreeToEndChanging();
    }

    @Override
    public void nodeItemClicked(NodeItem item, MouseEvent e) {
        toggleFoldNode(item);
    }

    @Override
    public void nodeItemKeyPressed(NodeItem item, KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            toggleFoldNode(item);
        }
    }
}
