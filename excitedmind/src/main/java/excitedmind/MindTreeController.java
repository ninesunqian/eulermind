package excitedmind;

import java.util.*;

import prefuse.Visualization;
import prefuse.data.*;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.visual.NodeItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

import prefuse.util.PrefuseLib;

import javax.swing.undo.AbstractUndoableEdit;
import java.util.logging.Logger;

public class MindTreeController {
    final Logger m_logger = Logger.getLogger(this.getClass().getName());
    public final String m_treeGroupName;
    public final String m_treeNodesGroupName;
    public final String m_treeEdgesGroupName;

    //MindTree
    final Visualization m_vis;
    final MindTree m_mindTree;

    Node m_cursor;
    int m_cursorDepth;
    Node m_savedCursor = null;

    private LinkedHashSet<Node> m_foldedNodes = new LinkedHashSet<Node>();

    public void setCursor(Node node)
    {
        m_cursor = node;
        m_cursorDepth = m_mindTree.getNodePath(m_cursor).size();
    }

    public void setCursorByPath(Stack<Integer> path)
    {
        m_cursor = m_mindTree.getNodeByPath(path);
        m_cursorDepth = path.size();
    }

    public void moveCursorLeft ()
    {
        if (m_cursor != m_mindTree.m_displayTree.getRoot()) {
            m_cursor = m_cursor.getParent();
            m_cursorDepth ++;
        }
    }

    public void moveCursorRight ()
    {
        if (m_cursor != m_mindTree.m_displayTree.getRoot()) {
            m_cursor = m_cursor.getParent();
            m_cursorDepth ++;
        }
    }


    public void moveCursorUp ()
    {
        /* TODO
        Node cur = m_cursor;
        int depth = m_cursorDepth;

        while (!m_displayTree.hasPreviousSibling(cur) && depth > 0) {
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
        */
    }

    public void moveCursorDown ()
    {
        /* TODO
        Node cur = m_cursor;
        int depth = m_cursorDepth;

        while (!m_displayTree.hasNextSibling(cur) && depth > 0) {
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
        */
    }


    abstract class NodeOperatorUndoer extends AbstractUndoableEdit {
        NodeOperatorUndoer (Stack<Integer> nodePath)
        {
            m_nodePath = nodePath;
        }

        final Stack<Integer> m_nodePath;
    }

    public MindTreeController(MindTree mindTree, Visualization vis, String treeGroupName)
    {
        m_mindTree = mindTree;
        m_vis = vis;

        m_treeGroupName = treeGroupName;
        m_treeNodesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.NODES);
        m_treeEdgesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.EDGES);

        m_mindTree.m_displayEdgeTable.addTableListener(new TableListener() {
            @Override
            public void tableChanged(Table t, int start, int end, int col, int type) {
                if (type ==  EventConstants.DELETE) {
                    for (int i=start; i<=end; i++) {
                        m_foldedNodes.remove(m_mindTree.m_displayTree.getNode(i));
                    }
                }
            }
        });
    }

    private NodeItem toVisual (Node node)
    {
        return (NodeItem) m_vis.getVisualItem(m_treeNodesGroupName, node);
    }

    private EdgeItem toVisual (Edge edge)
    {
        return (EdgeItem) m_vis.getVisualItem(m_treeEdgesGroupName, edge);
    }

    private Node toSource (NodeItem nodeItem)
    {
        return (Node) m_vis.getSourceTuple (nodeItem);
    }

    private Edge toSource (EdgeItem edgeItem)
    {
        return (Edge) m_vis.getSourceTuple (edgeItem);
    }

    public boolean isNode(VisualItem item)
    {
        return item.isInGroup(m_treeNodesGroupName);
    }

    public NodeItem getCursor ()
    {
        return toVisual(m_cursor);
    }

    public void setCursor (NodeItem nodeItem)
    {
        setCursor(toSource(nodeItem));
    }

    private void trashNodeAndCursorNext(Node node)
    {
        final Node root = m_mindTree.getRoot();
        if (m_mindTree.sameDBNode(root, node)) {
            return;
        }

        //从要删除的节点，向根节点找，找到最接近根的相同dbID的parent节点
        Node parent = node.getParent();
        Node topParent = m_mindTree.topSameDBNode(parent);

        Node newCursor = m_mindTree.getFamiliarNode(topParent.getChild(node.getIndex()));

        //using node path, compute the removed node in highest level;
        m_mindTree.trashNode(m_mindTree.getDBId(m_cursor.getParent()), m_cursor.getIndex());

        setCursor(newCursor);
    }

    private void restoreNodeAndSetCursor (Object dbId, Stack<Integer> nodePath)
    {
        m_mindTree.restoreNodeFromTrash(dbId);
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
            trashNodeAndCursorNext(m_mindTree.getNodeByPath(m_nodePath));
        }
        public void redo () {
            restoreNodeAndSetCursor(m_dbId, m_nodePath);
        }

        Object m_dbId;
    }

    private AbstractUndoableEdit addChildUndoable(Node parent, int pos, String text)
    {
        assert(!isPlaceholer(parent));
        Object childDBId = m_mindTree.addChild(m_mindTree.getDBId(parent), pos, text);

        Node newNode = parent.getChild(pos);
        setCursor(newNode);

        return new AddingChildUndoer(m_mindTree.getNodePath(newNode), m_mindTree.getDBId(newNode));
    }

    class AddingReferenceUndoer extends NodeOperatorUndoer
    {
        AddingReferenceUndoer (Stack<Integer> nodePath, int pos, Object refereeDBId)
        {
            super(nodePath);
            m_refereeDBId = refereeDBId;
            m_pos = pos;
        }
        public void undo () {
            setCursorByPath(m_nodePath);
            m_mindTree.removeReference(m_refereeDBId, m_pos);
        }
        public void redo () {
            setCursorByPath(m_nodePath);
            m_mindTree.addReference(m_mindTree.getDBId(m_cursor), m_pos, m_refereeDBId);
        }

        Object m_refereeDBId;
        int m_pos;
    }

    public AbstractUndoableEdit addReferenceUndoable(Node referer, int pos, Object refereeDBId)
    {
        m_mindTree.addReference(m_mindTree.getDBId(referer), pos, refereeDBId);

        // add a reference from m_cursor to other node, the m_cursor does not move the referered node

        return new AddingReferenceUndoer(m_mindTree.getNodePath(referer), pos, refereeDBId);
    }

    public AbstractUndoableEdit addReferenceUndoable(Node refereeNode)
    {
        return addReferenceUndoable(m_cursor, m_cursor.getChildCount(), m_mindTree.getDBId(refereeNode));
    }

    public void addPlaceholder(boolean asChild)
    {
        m_savedCursor = m_cursor;
        if (asChild) {
            //FIXME: prefuse function using dbTree's argument
            m_cursor = m_mindTree.m_displayTree.addChild(m_cursor, m_mindTree.getChildCount(m_cursor));
        } else {
            Node parent = m_cursor.getParent();
            int pos = m_cursor.getIndex() + 1;
            m_cursor = m_mindTree.m_displayTree.addChild(parent, pos);
        }
        m_cursor.set(m_mindTree.sm_textPropName, "");
    }

    public void removePlaceholder()
    {
        assert(isPlaceholer(m_cursor));
        assert(m_cursor != m_mindTree.m_displayTree.getRoot());

        Node placeholder = m_cursor;
        m_cursor = m_savedCursor;

        m_mindTree.m_displayTree.removeChild(placeholder);
    }

    //include node and edge, the edge is used rendering
    public boolean isPlaceholer(Tuple tuple)
    {
        return (m_mindTree.getDBId(tuple) == null);
    }


    //node has other property except dbId;
    public AbstractUndoableEdit placeNewNodeUndoable()
    {
        assert(m_cursor != m_mindTree.m_displayTree.getRoot());
        assert(isPlaceholer(m_cursor));

        Node parent = m_cursor.getParent();
        int pos = m_cursor.getIndex();
        String text = m_cursor.getString(m_mindTree.sm_textPropName);

        m_mindTree.m_displayTree.removeChild(m_cursor);
        AbstractUndoableEdit undor =  addChildUndoable(parent, pos, text);
        m_cursor = parent.getChild(pos);
        return  undor;
    }

    //node has only dbId
    public AbstractUndoableEdit placeRefereeUndoable(Object refereeDBId)
    {
        assert(m_cursor != m_mindTree.m_displayTree.getRoot());
        assert(isPlaceholer(m_cursor));

        Node sourceNode = m_cursor.getParent();
        int pos = m_cursor.getIndex();
        m_mindTree.m_displayTree.removeChild(m_cursor);

        AbstractUndoableEdit undor = addReferenceUndoable(sourceNode, pos, refereeDBId);
        m_cursor = sourceNode.getChild(pos);
        return undor;
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
            trashNodeAndCursorNext(m_mindTree.getNodeByPath(m_nodePath));
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
        public void undo ()
        {
            setCursorByPath(m_nodePath);
            m_mindTree.removeReference(m_refereeDBId, m_pos);
        }
        public void redo () {
            setCursorByPath(m_nodePath);
            m_mindTree.addReference(m_mindTree.getDBId(m_cursor), m_pos, m_refereeDBId);
        }

        Object m_refereeDBId;
        int m_pos;
    }

    public AbstractUndoableEdit removeCursorUndoable()
    {
        Node parent = m_cursor.getParent();
        Edge edge = m_mindTree.m_displayTree.getEdge(parent, m_cursor);
        if (m_mindTree.isRefEdge(edge)) {
            Object refereeDBId = m_mindTree.getDBId(m_cursor);
            int pos = m_cursor.getIndex();

            m_mindTree.removeReference(m_mindTree.getDBId(parent), m_cursor.getIndex());
            setCursor(parent);

            return new RemovingReferenceUndoer(m_mindTree.getNodePath(m_cursor), refereeDBId, pos);
        }
        else {
            RemovingChildUndoer undor = new RemovingChildUndoer(m_mindTree.getNodePath(m_cursor),
                    m_mindTree.getDBId(m_cursor));
            trashNodeAndCursorNext(m_cursor);
            return undor;
        }
    }

    private void moveChildImpl(Node oldParent, int oldPos, Node newParent, int newPos)
    {
        if (newPos == DBTree.ADDING_EDGE_END) {
            newPos = newParent.getChildCount();
        }

        m_mindTree.moveChild(m_mindTree.getDBId(oldParent), oldPos,
                m_mindTree.getDBId(newParent), newPos);
        setCursor(newParent.getChild(newPos));
    }

    class MovingChildUndoer extends AbstractUndoableEdit {
        MovingChildUndoer(Stack<Integer> oldParentPath, int oldPos, Stack<Integer> newParentPath, int newPos)
        {
            m_oldParentPath = oldParentPath;
            m_oldPos = oldPos;
            m_newParentPath = newParentPath;
            m_newPos = newPos;
        }

        public void undo()
        {
            moveChild(m_newParentPath, m_newPos, m_oldParentPath, m_oldPos);
        }

        public void redo()
        {
            moveChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);
        }

        private void moveChild(Stack<Integer> oldParentPath, int oldPos, Stack<Integer> newParentPath, int newPos)
        {
            Node oldParent = m_mindTree.getNodeByPath(oldParentPath);
            Node newParent = m_mindTree.getNodeByPath(newParentPath);
            moveChildImpl(oldParent, oldPos, newParent, newPos);
        }

        Stack<Integer> m_oldParentPath;
        int m_oldPos;
        Stack<Integer> m_newParentPath;
        int m_newPos;
    }

    public AbstractUndoableEdit resetParentUndoable(NodeItem newParentItem)
    {
        if (m_cursor == m_mindTree.m_displayTree.getRoot()) {
            return null;
        }

        Node newParent = toSource(newParentItem);
        Node oldParent = m_cursor.getParent();

        if (m_mindTree.sameDBNode(newParent, oldParent)) {
            return null;
        }

        DBTree.InheritDirection inheritDirection = m_mindTree.getInheritDirection(m_cursor, newParent);
        if (inheritDirection == DBTree.InheritDirection.LINEAL_DESCENDANT) {
            return null;
        }

        Stack<Integer> oldParentPath = m_mindTree.getNodePath(oldParent);
        Stack<Integer> newParentPath = m_mindTree.getNodePath(newParent);
        int oldPos = m_cursor.getIndex();
        int newPos = newParent.getChildCount();

        MovingChildUndoer undor = new MovingChildUndoer(oldParentPath, oldPos, newParentPath, newPos);

        moveChildImpl(oldParent, oldPos, newParent, newPos);

        return undor;
    }

    public boolean cursorIsFolded()
    {
        if (m_cursor.getChildCount() > 0) {
            NodeItem item = toVisual(m_cursor);
            return ! item.isExpanded();
        } else {
            return m_mindTree.getChildCount(m_cursor) > 0;
        }
    }

    private void unfoldNode (VisualItem visualItem)
    {
        Node node = (Node)visualItem.getSourceTuple();

        if (node.getChildCount() > 0) { // node is not a leaf node

            if (visualItem.isExpanded()) {
                return;
            }

            assert (m_foldedNodes.contains(node));

            m_foldedNodes.remove(node);

            final Visualization vis = visualItem.getVisualization();
            final Node unfoldTreeRoot = node;
            final String group = visualItem.getGroup();

            //unfold descendants deeply, to the folded descendants
            m_mindTree.m_displayTree.deepTraverse(node,new Tree.Processor() {
                public boolean run(Node node, int level) {

                    if (node == unfoldTreeRoot) {
                        return true;
                    }

                    TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
                    TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();

                    m_logger.info ( "visiableNode " + m_mindTree.getText(node));
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
            m_mindTree.attachChildren(node);
        }

        visualItem.setExpanded(true);
    }

    private void foldNode (VisualItem visualItem)
    {
        final Visualization vis = visualItem.getVisualization();
        Node node = (Node)visualItem.getSourceTuple();
        final String group = visualItem.getGroup();

        m_logger.info ( "foldNode " + m_mindTree.getText(node));
        if (! visualItem.isExpanded())
        {
            return;
        }

        m_foldedNodes.add(node);

        final Node foldTreeRoot = node;

        //set descendants unvisible deeply, to the folded descendants
        m_mindTree.m_displayTree.deepTraverse(node,new Tree.Processor() {
            public boolean run(Node node, int level) {
                if (node == foldTreeRoot)
                {
                    return true;
                }

                TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
                TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();

                PrefuseLib.updateVisible(visualNode, false);
                PrefuseLib.updateVisible(visualEdge, false);

                String text = m_mindTree.getText(node);

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

    public AbstractUndoableEdit toggleFoldCursorUndoable()
    {
        String text = m_mindTree.getText(m_cursor);
        VisualItem visualItem = toVisual(m_cursor);
        if (m_cursor.getChildCount() == 0)
        {
            m_logger.info ( "----leaf node un fold " + text);
            unfoldNode(visualItem);
        }
        else
        {
            if (visualItem.isExpanded())
            {
                m_logger.info ( "---- fold " + text);
                foldNode(visualItem);
            }
            else
            {
                m_logger.info ( "----un fold " + text);
                unfoldNode(visualItem);
            }
        }

        return new TogglingFoldUndoer (m_mindTree.getNodePath(m_cursor), false);
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
            m_logger.info("nodePath="+ m_nodePath + ",   DBId="+m_mindTree.getDBId(m_cursor) + ",   oldValue="+m_oldValue);
        }

        public void redo ()
        {
            setCursorByPath(m_nodePath);
            setCursorProperty(m_property, m_newValue);
            System.out.print("nodePath="+ m_nodePath + ",   DBId="+m_mindTree.getDBId(m_cursor) + ",   newValue="+m_newValue);
        }

        final String m_property;
        final Object m_oldValue;
        final Object m_newValue;
    }

    private Object setCursorProperty (String property, Object value)
    {
        Object oldValue = m_cursor.get(property);
        m_mindTree.setNodeProperty(m_mindTree.getDBId(m_cursor), property, value);
        return oldValue;
    }

    private AbstractUndoableEdit setCursorPropertyUndoable(String property, Object value)
    {
        Object oldValue = setCursorProperty(property, value);
        return new SetPropertyUndoer(m_mindTree.getNodePath(m_cursor), property, value, oldValue);
    }

    public AbstractUndoableEdit setCursorText(String text)
    {
        return setCursorPropertyUndoable(m_mindTree.sm_textPropName, text);
    }

    public boolean cursorIsPlaceholder()
    {
        return isPlaceholer(m_cursor);
    }

    public void setPlaceholderCursorText(String text)
    {
        assert(isPlaceholer(m_cursor));
        m_cursor.set(m_mindTree.sm_textPropName, text);
    }

    //TODO setFontFamliy setSize setColor

}
