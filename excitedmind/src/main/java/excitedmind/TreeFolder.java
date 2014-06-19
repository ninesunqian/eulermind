package excitedmind;
import java.util.*;

import prefuse.Visualization;
import prefuse.data.*;

import prefuse.util.PrefuseLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTree;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-2-28
 * Time: 下午10:42
 * To change this template use File | Settings | File Templates.
 */
public class TreeFolder {

    private LinkedHashSet<Integer> m_foldedNodes = new LinkedHashSet<Integer>();
    VisualTree m_tree;

    TreeFolder(VisualTree tree)
    {
        m_tree = tree;
    }

    public void unfoldNode(NodeItem node)
    {
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
}
