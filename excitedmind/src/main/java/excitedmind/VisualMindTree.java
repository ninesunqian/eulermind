package excitedmind;

import java.util.*;

import prefuse.Visualization;
import prefuse.visual.NodeItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.data.Graph;
import prefuse.util.PrefuseLib;

import javax.swing.undo.AbstractUndoableEdit;

public class VisualMindTree extends MindTree {
    public final static String sm_treeGroupName = "tree";
    public final static String sm_treeNodesGroupName = PrefuseLib.getGroupName(sm_treeGroupName, Graph.NODES);
    public final static String sm_treeEdgesGroupName = PrefuseLib.getGroupName(sm_treeGroupName, Graph.EDGES);

    //MindTree
    final Visualization m_vis;

    Node m_cursor;
    Stack<Integer> m_cursorPath = new Stack<Integer>();
    int m_cursorDepth = 0;

    private LinkedHashSet<Node> m_foldedNodes = new LinkedHashSet<Node>();


    public VisualMindTree(String dbPath, Object rootId, Visualization vis)
    {
        //= new MindTree (dbPath, rootId);
        super(dbPath, rootId);
        m_vis = vis;
        //m_vis.add(sm_treeGroupName, m_tree);
        m_vis.add(sm_treeGroupName, m_tree);

        m_cursor = m_tree.getRoot();

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

    private HashSet<NodeItem> m_selectedNode;


    public void moveCursorLeft ()
    {
        if (m_cursor == m_tree.getRoot())
            return;

        m_cursor = m_tree.getParent(m_cursor);

        m_cursorPath.pop();
        m_cursorDepth = m_cursorPath.size ();
    }

    public void moveCursorRight ()
    {
        if (m_cursor.getChildCount() == 0)
            return;

        m_cursor = m_tree.getChild(m_cursor, 0);

        m_cursorPath.push(0);
        m_cursorDepth = m_cursorPath.size ();
    }


    public void moveCursorUp ()
    {
        Node cur = m_cursor;
        int depth = m_cursorDepth;
        Stack<Integer> path = (Stack<Integer>)m_cursorPath.clone();

        while (!m_tree.hasPreviousSibling(cur) && depth > 0)
        {
            cur = cur.getParent();
            path.pop();
            depth --;
        }

        if (depth == 0) {
            return;
        }

        cur = cur.getPreviousSibling();

        while (cur.getChildCount()>0 && depth < m_cursorDepth)
        {
            int lastChildIndex = cur.getChildCount() - 1;
            path.push(lastChildIndex);
            cur = cur.getChild(lastChildIndex);
            depth ++;
        }

        m_cursor = cur;
        m_cursorPath = path;
    }

    public void moveCursorDown ()
    {
        Node cur = m_cursor;
        int depth = m_cursorDepth;
        Stack<Integer> path = (Stack<Integer>)m_cursorPath.clone();

        while (!m_tree.hasNextSibling(cur) && depth > 0)
        {
            cur = cur.getParent();
            path.pop();
            depth --;
        }

        if (depth == 0) {
            return;
        }

        cur = cur.getNextSibling();

        while (cur.getChildCount()>0 && depth < m_cursorDepth)
        {
            path.push(0);
            cur = cur.getChild(0);
            depth ++;
        }

        m_cursor = cur;
        m_cursorPath = path;
    }

    public final NodeItem getCursor ()
    {
        return toVisual(m_cursor);
    }

    private void setCursor (Node node)
    {
        m_cursor = node;
        m_cursorPath.clear();
        Node climber = m_cursor;

        while (climber != m_tree.getRoot())
        {
            m_cursorPath.add(0, climber.getIndex());
            climber = climber.getParent();
        }

        m_cursorDepth = m_cursorPath.size();

    }


    public void setCursor (NodeItem nodeItem)
    {
        setCursor(toSource(nodeItem));
    }



    private void setCursorByPath (Stack<Integer> path)
    {
        m_cursor = m_tree.getRoot();

        for (int pos : path)
        {
            m_cursor = m_cursor.getChild(pos);

            //setCursor by path used in undo queue, fold/unfold is regared a operator.
            //so when setCursorPath, the visual node must be exist.
            assert (m_cursor != null);
        }

        m_cursorPath = path;
        m_cursorDepth = path.size();
    }


    private Stack<Integer> copyCursorPath ()
    {
        return (Stack<Integer>) m_cursorPath.clone();
    }

    class AddingChildUndoer
    {
        AddingChildUndoer (Stack<Integer> nodePath)
        {
            Stack<Integer> m_nodePath;
            m_dbId = getDBElementId(m_cursor);
        }
        public void undo () {
            setCursorByPath(m_nodePath);
            Node newCursor = getNextSiblingOrParent();
            moveNodeToTrash(m_dbId); //  m_dbId --> parent, pos
            setCursor(newCursor);



        }
        public void redo () {

        }

        Stack<Integer> m_nodePath;
        Object m_dbId;
    }

    public AbstractUndoableEdit addChild ()
    {
        final Stack<Integer> oldCursorPath = copyCursorPath();
        addChild(getDBElementId(m_cursor), -1);
        setCursor(m_cursor.getChild(m_cursor.getChildCount() - 1));

        return new AbstractUndoableEdit() {
            public void undo () {
                moveNodeToTrash(m_cursor)

            }
            public void redo () {

            }
        };
    }

    public void addSibling ()
    {

    }

    public void removeCursorNode ()
    {

    }

    public void addReference (Object refereeDBId)
    {
        addReference(getDBElementId(m_cursor), refereeDBId, -1);
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

    class SetPropertyUndoer extends AbstractUndoableEdit
    {
        SetPropertyUndoer (Stack<Integer> nodePath, String property, Object oldValue, Object newValue)
        {
            m_nodePath = nodePath;

            m_property = property;
            m_oldValue = oldValue;
            m_newValue = newValue;
        }

        public void undo ()
        {
            setCursorByPath(m_cursorPath);
            setCursorProperty(m_property, m_newValue);
        }

        public void redo ()
        {
            setCursorByPath(m_cursorPath);
            setCursorProperty(m_property, m_oldValue);
        }

        final Stack<Integer> m_nodePath;

        final String m_property;
        final Object m_oldValue;
        final Object m_newValue;
    }


    private Object setCursorProperty (String property, Object value)
    {
        Object oldValue = m_cursor.get(property);
        setNodeProperty(getDBElementId(m_cursor), property, value);
        return oldValue;
    }

    //cursor move the next silbling ,
    //return old cursor
    private AbstractUndoableEdit setCursorPropertyUndoable  (String property, Object value)
    {
        Object oldValue = setCursorProperty(property, value);
        return new SetPropertyUndoer((Stack<Integer>) m_cursorPath.clone(), property, value, oldValue);
    }

    public AbstractUndoableEdit setText(String text)
    {
        return setCursorPropertyUndoable(sm_textPropName, text);
    }

    //TODO setFontFamliy setSize setColor

}
