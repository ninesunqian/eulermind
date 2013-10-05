package excitedmind;

import java.util.ArrayList;

import prefuse.Visualization;
import prefuse.visual.NodeItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.Graph;
import prefuse.data.Tuple;
import prefuse.util.PrefuseLib;
import prefuse.util.collections.IntIterator;


public class VisualMindTree extends MindTree {
    public final static String sm_treeGroupName = "tree";
    public final static String sm_treeNodesGroupName = PrefuseLib.getGroupName(sm_treeGroupName, Graph.NODES);
    public final static String sm_treeEdgesGroupName = PrefuseLib.getGroupName(sm_treeGroupName, Graph.EDGES);

    //MindTree
    Visualization m_vis;

    private LinkedHashSet<Node> m_foldedNodes = new LinkedHashSet<Node>();

    public VisualMindTree(String dbPath, Object rootId, Visualization vis)
    {
        //= new MindTree (dbPath, rootId);
        super(dbPath, rootId);
        m_vis = vis;
        //m_vis.add(sm_treeGroupName, m_tree);
        m_vis.add(sm_treeGroupName, m_tree);

        //TODO: add table Litener, remove event, remove frome m_foldedNodes
    }

    private NodeItem toVisual (Node node)
    {
        return (NodeItem) m_vis.getVisualItem(sm_treeNodesGroupName, node);
    }

    private EdgeItem toVisual (Edge edge)
    {
        return (EdgeItem) m_vis.getVisualItem(sm_treeEdgesGroupName, edge);
    }

    private Node toSource (NodeItem nodeItem)
    {
        return (Node) m_vis.getSourceTuple (nodeItem);
    }

    private Edge toSource (EdgeItem edgeItem)
    {
        return (Edge) m_vis.getSourceTuple (edgeItem);
    }

    public boolean isNode(VisualItem item) {
        return item.isInGroup(sm_treeNodesGroupName);
    }


    public void unfoldNode (VisualItem visualItem)
    {
        Node node = (Node)visualItem.getSourceTuple();

        if (node.getChildCount() > 0){ // node is not a leaf node

            if (visualItem.isExpanded()) {
                return;
            }

            assert (m_foldedNodes.contains(node));

            m_foldedNodes.remove(node);

            final Visualization vis = visualItem.getVisualization();
            final Node unfoldTreeRoot = node;
            final String group = visualItem.getGroup();

            //unfold descendants deeply, to the folded descendants
            m_tree.deepTraverse(node,new Tree.Processor() {
                public boolean run(Node node, int level) {

                    if (node == unfoldTreeRoot) {
                        return true;
                    }

                    TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
                    TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();

                    System.out.println ( "visiableNode " + getText(node));
                    PrefuseLib.updateVisible(visualNode, true);
                    PrefuseLib.updateVisible(visualEdge, true);

                    if (m_foldedNodes.contains(node)) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }, 0);
        }
        else // node  is the leaf of prefuse Tree
        {
            attachChildren(node);
        }

        visualItem.setExpanded(true);
    }

    public void foldNode (VisualItem visualItem)
    {
        final Visualization vis = visualItem.getVisualization();
        Node node = (Node)visualItem.getSourceTuple();
        final String group = visualItem.getGroup();

        System.out.println ( "foldNode " + getText(node));
        if (! visualItem.isExpanded())
        {
            return;
        }

        m_foldedNodes.add(node);

        final Node foldTreeRoot = node;

        //set descendants unvisible deeply, to the folded descendants
        m_tree.deepTraverse(node,new Tree.Processor() {
            public boolean run(Node node, int level) {
                if (node == foldTreeRoot)
                {
                    return true;
                }

                TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
                TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();

                PrefuseLib.updateVisible(visualNode, false);
                PrefuseLib.updateVisible(visualEdge, false);

                String text = getText(node);

                System.out.println ( "invisiableNode " + text);
                if (m_foldedNodes.contains(node)) {
                    System.out.println ( "m_foldedNodes contain: " + node + " " + text);
                    return false;
                } else {
                    return true;
                }
            }
        }, 0);

        // detach the descendants of the earliest unfold node
        if (m_foldedNodes.size() > 5)
        {
            Node toRemovedNode = m_foldedNodes.iterator().next();
            m_foldedNodes.remove(toRemovedNode);
            detachChildern(toRemovedNode);
        }

        visualItem.setExpanded(false);
    }

    public void ToggleFoldNode (VisualItem visualItem )
    {
        Node node = (Node)visualItem.getSourceTuple();
        String text = getText(node);
        if (node.getChildCount() == 0)
        {
            System.out.println ( "----leaf node un fold " + text);
            unfoldNode(visualItem);
        }
        else
        {
            if (visualItem.isExpanded())
            {
                System.out.println ( "---- fold " + text);
                foldNode(visualItem);
            }
            else
            {
                System.out.println ( "----un fold " + text);
                unfoldNode(visualItem);
            }
        }
    }

    public void setText (NodeItem node, String text)
    {
        super.setText(toSource(node), text);
    }
}
