package excitedmind;

import java.util.*;

import prefuse.Visualization;
import prefuse.util.ColorLib;
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
import javax.swing.undo.CannotUndoException;

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

    abstract class NodeOperatorUndoer extends AbstractUndoableEdit {
        NodeOperatorUndoer (Stack<Integer> nodePath)
        {
            m_nodePath = (Stack<Integer>)nodePath.clone();
        }

        final Stack<Integer> m_nodePath;
    }

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

    public NodeItem getCursor (NodeItem nodeItem)
    {
        return toVisual(m_cursor);
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

    private Node getNextCursor ()
    {
        if (m_cursor == m_tree.getRoot())
            return m_cursor;
        else if (m_tree.hasNextSibling(m_cursor)) //TODO: tree.hasXXX,  node.getXXX
            return m_cursor.getNextSibling();
        else if (m_tree.hasPreviousSibling(m_cursor))
            return m_cursor.getPreviousSibling();
        else
            return m_cursor.getParent();
    }

    private void removeCursorNodeAndCursorNext()
    {
        Node next_cursor = getNextCursor();
        moveNodeToTrash(getDBElementId(m_cursor.getParent()), m_cursor.getIndex());
        setCursor(next_cursor);
    }

    private void restoreNodeAndSetCursor (Object dbId, Stack<Integer> nodePath)
    {
        restoreNodeFromTrash(dbId);
        setCursorByPath(nodePath);
    }

    class AddingChildUndoer extends NodeOperatorUndoer
    {
        AddingChildUndoer (Stack<Integer> nodePath, Object dbId)
        {
            super(nodePath);
            m_dbId = dbId;
        }
        public void undo () {
            setCursorByPath(m_nodePath);
            removeCursorNodeAndCursorNext();
        }
        public void redo () {
            restoreNodeAndSetCursor(m_dbId, m_nodePath);
        }

        Object m_dbId;
    }

    public AbstractUndoableEdit addChild ()
    {
        Object childDBId = addChild(getDBElementId(m_cursor), -1);
        setCursor(m_cursor.getChild(m_cursor.getChildCount() - 1));

        return new AddingChildUndoer(m_cursorPath, getDBElementId(m_cursor));
    }

    public AbstractUndoableEdit addSibling ()
    {
        Node parent = m_cursor.getParent();
        int newSiblingIndex = m_cursor.getIndex() + 1;
        Object newSiblingDBId = addChild(getDBElementId(parent), newSiblingIndex);
        setCursor(parent.getChild(newSiblingIndex));

        return new AddingChildUndoer(m_cursorPath, getDBElementId(m_cursor));
    }

    class AddingReferenceUndoer extends NodeOperatorUndoer
    {
        AddingReferenceUndoer (Stack<Integer> nodePath, Object refereeDBId, int pos)
        {
            super(nodePath);
            m_refereeDBId = refereeDBId;
            m_pos = pos;
        }
        public void undo () {
            setCursorByPath(m_nodePath);
            removeReference(m_refereeDBId, m_pos);
        }
        public void redo () {
            setCursorByPath(m_nodePath);
            addReference(getDBElementId(m_cursor), m_refereeDBId, m_pos);
        }

        Object m_refereeDBId;
        int m_pos;
    }

    public AbstractUndoableEdit addReference (Object refereeDBId)
    {
        addReference(getDBElementId(m_cursor), refereeDBId, -1);

        // add a reference from m_cursor to other node, the m_cursor does not move the referered node

        return new AddingReferenceUndoer(m_cursorPath, refereeDBId, m_cursor.getChildCount()-1);
    }


    class RemovingChildUndoer extends NodeOperatorUndoer
    {
        RemovingChildUndoer (Stack<Integer> nodePath, Object dbId)
        {
            super(nodePath);
            m_dbId = dbId;
        }
        public void undo () {
            restoreNodeAndSetCursor(m_dbId, m_nodePath);
        }
        public void redo () {
            setCursorByPath(m_nodePath);
            removeCursorNodeAndCursorNext();
        }

        Object m_dbId;
    }

    class RemovingReferenceUndoer extends NodeOperatorUndoer
    {
        //node path is the referer node
        RemovingReferenceUndoer (Stack<Integer> nodePath, Object refereeDBId, int pos)
        {
            super(nodePath);
            m_refereeDBId = refereeDBId;
            m_pos = pos;
        }
        public void undo () {
            setCursorByPath(m_nodePath);
            removeReference(m_refereeDBId, m_pos);
        }
        public void redo () {
            setCursorByPath(m_nodePath);
            addReference(getDBElementId(m_cursor), m_refereeDBId, m_pos);
        }

        Object m_refereeDBId;
        int m_pos;
    }

    public AbstractUndoableEdit removeCursorNodeUndoable ()
    {
        Node parent = m_cursor.getParent();
        Edge edge = m_tree.getEdge(parent, m_cursor);
        if (isRefEdge(edge)) {
            Object refereeDBId =  getDBElementId(m_cursor);
            int pos = m_cursor.getIndex();

            removeReference(getDBElementId(parent), m_cursor.getIndex());
            setCursor(parent);

            return new RemovingReferenceUndoer(m_cursorPath, refereeDBId, pos);
        }
        else {
            RemovingChildUndoer undoer = new RemovingChildUndoer(m_cursorPath, getDBElementId(m_cursor));
            removeCursorNodeAndCursorNext();
            return undoer;
        }
    }

    private void unfoldNode (VisualItem visualItem)
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

    private void foldNode (VisualItem visualItem)
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
        /* TODO  now disalble it
        if (m_foldedNodes.size() > 5)
        {
            Node toRemovedNode = m_foldedNodes.iterator().next();
            m_foldedNodes.remove(toRemovedNode);
            detachChildern(toRemovedNode);
        }
        */

        visualItem.setExpanded(false);
    }

    class TogglingFoldUndoer extends NodeOperatorUndoer{
        TogglingFoldUndoer (Stack<Integer> nodePath, boolean foldIt)
        {
            super(nodePath);
            m_foldIt = foldIt;
        }

        public void undo() {
            setCursorByPath(m_cursorPath);
            if (m_foldIt)
                unfoldNode(toVisual(m_cursor));
            else
                foldNode(toVisual(m_cursor));
        }

        public void redo () {
            setCursorByPath(m_cursorPath);
            if (m_foldIt)
                foldNode(toVisual(m_cursor));
            else
                unfoldNode(toVisual(m_cursor));
        }

        boolean m_foldIt;
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

    class SetPropertyUndoer extends NodeOperatorUndoer
    {
        SetPropertyUndoer (Stack<Integer> nodePath, String property, Object newValue, Object oldValue)
        {
            super(nodePath);
            m_property = property;
            m_oldValue = oldValue;
            m_newValue = newValue;
            System.out.println("nodePath="+ nodePath + ",   newValue="+m_newValue + ",   oldValue="+m_oldValue);
        }

        public void undo ()
        {
            setCursorByPath(m_nodePath);
            setCursorProperty(m_property, m_oldValue);
            System.out.println("nodePath="+ m_cursorPath + ",   DBId="+getDBElementId(m_cursor) + ",   oldValue="+m_oldValue);
        }

        public void redo ()
        {
            setCursorByPath(m_nodePath);
            setCursorProperty(m_property, m_newValue);
            System.out.print("nodePath="+ m_cursorPath + ",   DBId="+getDBElementId(m_cursor) + ",   newValue="+m_newValue);
        }

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
        return new SetPropertyUndoer(m_cursorPath, property, value, oldValue);
    }

    public AbstractUndoableEdit setCursorText(String text)
    {
        return setCursorPropertyUndoable(sm_textPropName, text);
    }

    //TODO setFontFamliy setSize setColor

    public boolean isRefEdge (Edge edge)
    {
        return DBTree.EdgeType.values()[(Integer)edge.get(sm_edgeTypePropName)] == DBTree.EdgeType.REFERENCE;
    }

    public String getText (Node node)
    {
        return node.getString(sm_textPropName);
    }

    public int getNodeColor (NodeItem nodeItem)
    {
        Node node = toSource(nodeItem);
        if (node == m_cursor)
            return ColorLib.rgb(255, 0, 0);
        else if (getDBElementId(node).equals(getDBElementId(m_cursor)))
            return ColorLib.rgb(255, 255, 0);
        else
            return ColorLib.rgb(255, 255, 255);
    }

    public String getFont (Node node)
    {
        return node.getString(sm_textPropName);
    }

    public String getSize (Node node)
    {
        return node.getString(sm_textPropName);
    }

    /*
    public String getText (Node node)
    {
        return node.getString(sm_textPropName);
    }
    */
}
