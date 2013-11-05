package excitedmind;

import java.util.*;

import prefuse.Visualization;
import prefuse.data.*;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.util.ColorLib;
import prefuse.visual.NodeItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

import prefuse.util.PrefuseLib;

import javax.swing.undo.AbstractUndoableEdit;
import java.util.logging.Logger;

public class VisualMindTree extends MindTree {
    final Logger m_logger = Logger.getLogger(this.getClass().getName());
    public final static String sm_treeGroupName = "tree";
    public final static String sm_treeNodesGroupName = PrefuseLib.getGroupName(sm_treeGroupName, Graph.NODES);
    public final static String sm_treeEdgesGroupName = PrefuseLib.getGroupName(sm_treeGroupName, Graph.EDGES);

    //MindTree
    final Visualization m_vis;


    Node m_cursor;
    int m_cursorDepth = 0;

    private LinkedHashSet<Node> m_foldedNodes = new LinkedHashSet<Node>();

    abstract class NodeOperatorUndoer extends AbstractUndoableEdit {
        NodeOperatorUndoer (Stack<Integer> nodePath)
        {
            m_nodePath = nodePath;
        }

        final Stack<Integer> m_nodePath;
    }

    public VisualMindTree(String dbPath, Object rootId, Visualization vis)
    {
        super(dbPath, rootId);
        m_vis = vis;
        m_vis.add(sm_treeGroupName, m_tree);

        m_cursor = m_tree.getRoot();

        m_tree.getNodeTable().addTableListener(new TableListener() {
            @Override
            public void tableChanged(Table t, int start, int end, int col, int type) {
                if (type ==  EventConstants.DELETE) {
                    for (int i=start; i<=end; i++) {
                        m_foldedNodes.remove(m_tree.getNode(i));
                    }
                }
            }
        });
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

    private Stack getDisplayPath(Node node)
    {
        Stack path = new Stack();

        Node climber = node;

        while (climber != m_tree.getRoot())
        {
            path.add(0, climber.getIndex());
            climber = climber.getParent();
        }

        return path;
    }


    public void moveCursorLeft ()
    {
        if (m_cursor == m_tree.getRoot())
            return;

        m_cursor = m_tree.getParent(m_cursor);
        m_cursorDepth = getDisplayPath(m_cursor).size();
    }

    public void moveCursorRight ()
    {
        if (m_cursor.getChildCount() == 0)
            return;

        m_cursor = m_tree.getChild(m_cursor, 0);
        m_cursorDepth = getDisplayPath(m_cursor).size();
    }


    public void moveCursorUp ()
    {
        Node cur = m_cursor;
        int depth = m_cursorDepth;

        while (!m_tree.hasPreviousSibling(cur) && depth > 0) {
            cur = cur.getParent();
            depth --;
        }

        if (depth == 0) {
            return;
        }

        cur = cur.getPreviousSibling();

        while (cur.getChildCount()>0 && depth < m_cursorDepth) {
            cur = cur.getChild(cur.getChildCount() - 1);
            depth ++;
        }

        m_cursor = cur;
    }

    public void moveCursorDown ()
    {
        Node cur = m_cursor;
        int depth = m_cursorDepth;

        while (!m_tree.hasNextSibling(cur) && depth > 0) {
            cur = cur.getParent();
            depth --;
        }

        if (depth == 0) {
            return;
        }

        cur = cur.getNextSibling();

        while (cur.getChildCount()>0 && depth < m_cursorDepth) {
            cur = cur.getChild(0);
            depth ++;
        }

        m_cursor = cur;
    }

    public NodeItem getCursor ()
    {
        return toVisual(m_cursor);
    }

    private void setCursor (Node node)
    {
        m_cursor = node;
        Node climber = m_cursor;

        while (climber != m_tree.getRoot())
        {
            climber = climber.getParent();
        }
    }

    public void setCursor (NodeItem nodeItem)
    {
        setCursor(toSource(nodeItem));
    }

    private void setCursorByPath (Stack<Integer> path)
    {
        m_cursor = m_tree.getRoot();

        for (int pos : path) {
            if (m_cursor.getChild(pos) == null) {
                unfoldNode(toVisual(m_cursor));
            }
            m_cursor = m_cursor.getChild(pos);
        }

        m_cursorDepth = path.size();
    }

    private boolean isInDBSubTree(Node node, Node treeRoot)
    {
        DBTree.InheritDirection inheritDirection = getInheritDirection(node, treeRoot);
        return inheritDirection == DBTree.InheritDirection.LINEAL_ANCESTOR ||
                inheritDirection == DBTree.InheritDirection.SELF;

    }

    private void removeCursorNodeAndCursorNext()
    {
        final Node root = m_tree.getRoot();
        if (getDBElementId(root) == getDBElementId(m_cursor))
            return;

        Node parent = m_cursor.getParent();
        m_logger.info ("remove'd parent : " + getDisplayPath(parent));
        Node topParent = parent;
        m_logger.info ("remove'd parent's parent : " + getDisplayPath(parent.getParent()));
        for (Node n=parent.getParent(); n!=root ;n=n.getParent())
        {
            m_logger.info ("remove clim path : " + getDisplayPath(n));
            if (getDBElementId(n).equals(getDBElementId(parent))) {
                topParent = n;
                break;
            }
        }

        int i;
        Node newCursor = topParent;
        for (i=m_cursor.getIndex()+1; i<topParent.getChildCount(); i++) {
            newCursor = topParent.getChild(i);
            if (! isInDBSubTree(newCursor, m_cursor))
                break;
        }

        if (i == topParent.getChildCount()) {
            for (i=m_cursor.getIndex()-1; i<=0; i--) {
                newCursor = topParent.getChild(i);
                if (! isInDBSubTree(newCursor, m_cursor))
                    break;
            }

            if (i == -1) {
                newCursor = topParent;
            }
        }

        //using node path, compute the removed node in highest level;
        moveNodeToTrash(getDBElementId(m_cursor.getParent()), m_cursor.getIndex());

        setCursor(newCursor);
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
        Object childDBId = addChild(getDBElementId(m_cursor), DBTree.ADDING_EDGE_END);
        setCursor(m_cursor.getChild(m_cursor.getChildCount() - 1));

        return new AddingChildUndoer(getDisplayPath(m_cursor), getDBElementId(m_cursor));
    }

    public AbstractUndoableEdit addSibling ()
    {
        Node parent = m_cursor.getParent();
        int newSiblingIndex = m_cursor.getIndex() + 1;
        Object newSiblingDBId = addChild(getDBElementId(parent), newSiblingIndex);
        setCursor(parent.getChild(newSiblingIndex));

        return new AddingChildUndoer(getDisplayPath(m_cursor), getDBElementId(m_cursor));
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
        addReference(getDBElementId(m_cursor), refereeDBId, DBTree.ADDING_EDGE_END);

        // add a reference from m_cursor to other node, the m_cursor does not move the referered node

        return new AddingReferenceUndoer(getDisplayPath(m_cursor), refereeDBId, m_cursor.getChildCount()-1);
    }

    public AbstractUndoableEdit addReference (Node node)
    {
        Object refereeDBId = getDBElementId(node);
        return addReference(refereeDBId);
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

    public AbstractUndoableEdit removeCursorNode()
    {
        Node parent = m_cursor.getParent();
        Edge edge = m_tree.getEdge(parent, m_cursor);
        if (isRefEdge(edge)) {
            Object refereeDBId =  getDBElementId(m_cursor);
            int pos = m_cursor.getIndex();

            removeReference(getDBElementId(parent), m_cursor.getIndex());
            setCursor(parent);

            return new RemovingReferenceUndoer(getDisplayPath(m_cursor), refereeDBId, pos);
        }
        else {
            RemovingChildUndoer undoer = new RemovingChildUndoer(getDisplayPath(m_cursor), getDBElementId(m_cursor));
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

                    m_logger.info ( "visiableNode " + getText(node));
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

        m_logger.info ( "foldNode " + getText(node));
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

                m_logger.info ( "invisiableNode " + text);
                if (m_foldedNodes.contains(node)) {
                    m_logger.info ( "m_foldedNodes contain: " + node + " " + text);
                    return false;
                } else {
                    return true;
                }
            }
        }, 0);

        // detach the descendants of the earliest unfold node
        /* TODO  now disable it
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
            setCursorByPath(m_nodePath);
            if (m_foldIt)
                unfoldNode(toVisual(m_cursor));
            else
                foldNode(toVisual(m_cursor));
        }

        public void redo () {
            setCursorByPath(m_nodePath);
            if (m_foldIt)
                foldNode(toVisual(m_cursor));
            else
                unfoldNode(toVisual(m_cursor));
        }

        boolean m_foldIt;
    }

    public AbstractUndoableEdit ToggleFoldNode ()
    {
        String text = getText(m_cursor);
        VisualItem visualItem = toVisual(m_cursor);
        boolean foldIt = false;
        if (m_cursor.getChildCount() == 0)
        {
            m_logger.info ( "----leaf node un fold " + text);
            unfoldNode(visualItem);
            foldIt = false;
        }
        else
        {
            if (visualItem.isExpanded())
            {
                m_logger.info ( "---- fold " + text);
                foldNode(visualItem);
                foldIt = true;
            }
            else
            {
                m_logger.info ( "----un fold " + text);
                unfoldNode(visualItem);
                foldIt = false;
            }
        }

        return new TogglingFoldUndoer (getDisplayPath(m_cursor), false);
    }

    class SetPropertyUndoer extends NodeOperatorUndoer
    {
        SetPropertyUndoer (Stack<Integer> nodePath, String property, Object newValue, Object oldValue)
        {
            super(nodePath);
            m_property = property;
            m_oldValue = oldValue;
            m_newValue = newValue;
            m_logger.info("nodePath="+ nodePath + ",   newValue="+m_newValue + ",   oldValue="+m_oldValue);
        }

        public void undo ()
        {
            setCursorByPath(m_nodePath);
            setCursorProperty(m_property, m_oldValue);
            m_logger.info("nodePath="+ m_nodePath + ",   DBId="+getDBElementId(m_cursor) + ",   oldValue="+m_oldValue);
        }

        public void redo ()
        {
            setCursorByPath(m_nodePath);
            setCursorProperty(m_property, m_newValue);
            System.out.print("nodePath="+ m_nodePath + ",   DBId="+getDBElementId(m_cursor) + ",   newValue="+m_newValue);
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

    private AbstractUndoableEdit setCursorPropertyUndoable  (String property, Object value)
    {
        Object oldValue = setCursorProperty(property, value);
        return new SetPropertyUndoer(getDisplayPath(m_cursor), property, value, oldValue);
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
}
