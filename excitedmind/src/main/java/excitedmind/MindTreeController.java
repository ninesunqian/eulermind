package excitedmind;

import java.util.*;

import prefuse.Visualization;
import prefuse.data.*;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.visual.NodeItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTree;
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

    TreeCursor m_cursor;
    Node m_savedCursor = null;

    private LinkedHashSet<Integer> m_foldedNodes = new LinkedHashSet<Integer>();

    public Stack<Integer> getNodePath(Node node)
    {
        Stack<Integer> path = new Stack<Integer>();

        Node climber = node;
        Node root = m_mindTree.m_displayTree.getRoot();

        assert (climber != null);

        while (climber != root)
        {
            path.add(0, m_mindTree.m_displayTree.getIndexInSiblings(climber));
            climber = climber.getParent();
            if (climber.getRow()==root.getRow() && climber != root) {
                m_logger.info("aaaaaaaaaaaa");
            }
        }

        return path;
    }

    public Node getNodeByPath(Stack<Integer> path)
    {
        Node node = m_mindTree.m_displayTree.getRoot();

        for (int pos : path) {

            if (node.getChildCount() == 0) {
                m_mindTree.attachChildren(node);
            }

            node = node.getChild(pos);

            if (node == null) {
                return null;
            }
        }

        return node;
    }


    public Node getRoot()
    {
        return m_mindTree.m_displayTree.getRoot();
    }

    public Node topSameDBNode(Node node)
    {
        Node topNode = node;
        Node root = getRoot();

        m_logger.info ("remove'd node's node : " + getNodePath(node.getParent()));
        for (Node n=node.getParent(); n!=root ;n=n.getParent())
        {
            m_logger.info ("remove clim path : " + getNodePath(n));
            if (m_mindTree.sameDBNode(n, node)) {
                topNode = n;
            }
        }

        return topNode;
    }

    public Node getFamiliarNode (Node node)
    {
        int start = node.getIndex();
        Node parent = node.getParent();
        Node familiar;
        //在topParent的子节点中，找到两个与被删除节点dbId不同的节点，和一个相同的节点
        for (int i=start+1; i<parent.getChildCount(); i++) {
            Node tmp = parent.getChild(i);
            if (!m_mindTree.sameDBNode(tmp, node)) {
                return tmp;
            }
        }

        for (int i=0; i<start; i++) {
            Node tmp = parent.getChild(i);
            if (!m_mindTree.sameDBNode(tmp, node)) {
                return tmp;
            }
        }
        return parent;
    }

    public Node setCursorByPath(Stack<Integer> path)
    {
        Node node = getNodeByPath(path);
        assert(node.getRow() != -1);
        m_cursor.setCursorNode(toVisual(node));
        return node;
    }

    public void moveCursorLeft()
    {
        m_cursor.moveLeft();
    }
    public void moveCursorRight()
    {
        m_cursor.moveRight();
    }

    public void moveCursorUp()
    {
        m_cursor.moveUp();
    }

    public void moveCursorDown()
    {
        m_cursor.moveDown();
    }

    public void setCursorNode(Node node)
    {
        assert(node.getRow() != -1);
        m_cursor.setCursorNode(toVisual(node));
    }

    public Node getCursorNode()
    {
        return toSource(m_cursor.getCursorNode());
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
                        m_foldedNodes.remove(i);
                    }
                }
            }
        });

        m_cursor = new TreeCursor((VisualTree)m_vis.getVisualGroup(m_treeGroupName));
    }

    public NodeItem toVisual (Node node)
    {
        if (node instanceof NodeItem) {
            return  (NodeItem) node;
        } else {
            return (NodeItem) m_vis.getVisualItem(m_treeNodesGroupName, node);
        }
    }

    public EdgeItem toVisual (Edge edge)
    {
        if (edge instanceof EdgeItem) {
            return (EdgeItem) edge;
        } else {
            return (EdgeItem) m_vis.getVisualItem(m_treeEdgesGroupName, edge);
        }
    }

    public Node toSource (NodeItem nodeItem)
    {
        return (Node) m_vis.getSourceTuple (nodeItem);
    }

    public Edge toSource (EdgeItem edgeItem)
    {
        return (Edge) m_vis.getSourceTuple (edgeItem);
    }

    public boolean isNode(VisualItem item)
    {
        return item.isInGroup(m_treeNodesGroupName);
    }

    private void trashNodeAndCursorNext(Node node)
    {
        Node root = getRoot();
        Node cursorNode = getCursorNode();

        if (m_mindTree.sameDBNode(root, node)) {
            return;
        }

        //从要删除的节点，向根节点找，找到最接近根的相同dbID的parent节点
        Node parent = node.getParent();
        Node topParent = topSameDBNode(parent);

        Node newCursor = getFamiliarNode(topParent.getChild(node.getIndex()));

        assert(newCursor.isValid());

        //using node path, compute the removed node in highest level;
        m_mindTree.trashNode(m_mindTree.getDBId(cursorNode.getParent()), cursorNode.getIndex());

        assert(newCursor.isValid());

        setCursorNode(newCursor);
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
            trashNodeAndCursorNext(getNodeByPath(m_nodePath));
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
        setCursorNode(newNode);

        return new AddingChildUndoer(getNodePath(newNode), m_mindTree.getDBId(newNode));
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
            Node cursorNode = setCursorByPath(m_nodePath);
            m_mindTree.addReference(m_mindTree.getDBId(cursorNode), m_pos, m_refereeDBId);
        }

        Object m_refereeDBId;
        int m_pos;
    }

    public AbstractUndoableEdit addReferenceUndoable(Node referer, int pos, Object refereeDBId)
    {
        m_mindTree.addReference(m_mindTree.getDBId(referer), pos, refereeDBId);

        // add a reference from m_cursor to other node, the m_cursor does not move the referered node

        return new AddingReferenceUndoer(getNodePath(referer), pos, refereeDBId);
    }

    public AbstractUndoableEdit addReferenceUndoable(Node refereeNode)
    {
        Node cursorNode = getCursorNode();
        return addReferenceUndoable(cursorNode, cursorNode.getChildCount(), m_mindTree.getDBId(refereeNode));
    }

    public void addPlaceholder(boolean asChild)
    {
        Node cursorNode = getCursorNode();
        m_savedCursor = cursorNode;
        if (asChild) {
            //FIXME: prefuse function using dbTree's argument
            cursorNode = m_mindTree.m_displayTree.addChild(cursorNode, m_mindTree.getChildCount(cursorNode));
        } else {
            Node parent = cursorNode.getParent();
            int pos = cursorNode.getIndex() + 1;
            cursorNode = m_mindTree.m_displayTree.addChild(parent, pos);
        }
        cursorNode.set(m_mindTree.sm_textPropName, "");
    }

    public void removePlaceholder()
    {
        Node cursorNode = getCursorNode();
        assert(isPlaceholer(cursorNode));
        assert(cursorNode != m_mindTree.m_displayTree.getRoot());

        Node placeholder = cursorNode;
        cursorNode = m_savedCursor;

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
        Node cursorNode = getCursorNode();
        assert(cursorNode != m_mindTree.m_displayTree.getRoot());
        assert(isPlaceholer(cursorNode));

        Node parent = cursorNode.getParent();
        int pos = cursorNode.getIndex();
        String text = cursorNode.getString(m_mindTree.sm_textPropName);

        m_mindTree.m_displayTree.removeChild(cursorNode);
        AbstractUndoableEdit undor =  addChildUndoable(parent, pos, text);
        cursorNode = parent.getChild(pos);
        return  undor;
    }

    //node has only dbId
    public AbstractUndoableEdit placeRefereeUndoable(Object refereeDBId)
    {
        Node cursorNode = getCursorNode();

        assert(cursorNode != m_mindTree.m_displayTree.getRoot());
        assert(isPlaceholer(cursorNode));

        Node sourceNode = cursorNode.getParent();
        int pos = cursorNode.getIndex();
        m_mindTree.m_displayTree.removeChild(cursorNode);

        AbstractUndoableEdit undor = addReferenceUndoable(sourceNode, pos, refereeDBId);
        cursorNode = sourceNode.getChild(pos);
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
            trashNodeAndCursorNext(getNodeByPath(m_nodePath));
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
        public void undo()
        {
            Node cursorNode = getCursorNode();
            setCursorByPath(m_nodePath);
            m_mindTree.addReference(m_mindTree.getDBId(cursorNode), m_pos, m_refereeDBId);

        }
        public void redo()
        {
            setCursorByPath(m_nodePath);
            m_mindTree.removeReference(m_refereeDBId, m_pos);
        }

        Object m_refereeDBId;
        int m_pos;
    }

    public AbstractUndoableEdit removeCursorUndoable()
    {
        Node cursorNode = getCursorNode();

        Node parent = cursorNode.getParent();
        Edge edge = m_mindTree.m_displayTree.getEdge(parent, cursorNode);
        if (m_mindTree.isRefEdge(edge)) {
            Object refereeDBId = m_mindTree.getDBId(cursorNode);
            int pos = cursorNode.getIndex();

            RemovingReferenceUndoer undoer = new RemovingReferenceUndoer(getNodePath(parent), refereeDBId, pos);

            m_mindTree.removeReference(m_mindTree.getDBId(parent), cursorNode.getIndex());
            setCursorNode(parent);

            return undoer;
        }
        else {
            RemovingChildUndoer undor = new RemovingChildUndoer(getNodePath(cursorNode),
                    m_mindTree.getDBId(cursorNode));
            trashNodeAndCursorNext(cursorNode);
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
        setCursorNode(newParent.getChild(newPos));
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
            Node oldParent = getNodeByPath(oldParentPath);
            Node newParent = getNodeByPath(newParentPath);
            moveChildImpl(oldParent, oldPos, newParent, newPos);
        }

        Stack<Integer> m_oldParentPath;
        int m_oldPos;
        Stack<Integer> m_newParentPath;
        int m_newPos;
    }

    public AbstractUndoableEdit resetParentUndoable(NodeItem newParentItem)
    {
        Node cursorNode = getCursorNode();

        if (cursorNode == m_mindTree.m_displayTree.getRoot()) {
            return null;
        }

        Node newParent = toSource(newParentItem);
        Node oldParent = cursorNode.getParent();

        if (m_mindTree.sameDBNode(newParent, oldParent)) {
            return null;
        }

        DBTree.InheritDirection inheritDirection = m_mindTree.getInheritDirection(cursorNode, newParent);
        if (inheritDirection == DBTree.InheritDirection.LINEAL_DESCENDANT) {
            return null;
        }

        Stack<Integer> oldParentPath = getNodePath(oldParent);
        Stack<Integer> newParentPath = getNodePath(newParent);
        int oldPos = cursorNode.getIndex();
        int newPos = newParent.getChildCount();

        MovingChildUndoer undor = new MovingChildUndoer(oldParentPath, oldPos, newParentPath, newPos);

        moveChildImpl(oldParent, oldPos, newParent, newPos);

        return undor;
    }

    public boolean cursorIsFolded()
    {
        Node cursorNode = getCursorNode();

        if (cursorNode.getChildCount() > 0) {
            NodeItem item = toVisual(cursorNode);
            return ! item.isExpanded();
        } else {
            return m_mindTree.getChildCount(cursorNode) > 0;
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

                    //m_logger.info ( "visiableNode " + m_mindTree.getText(node));
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

        m_foldedNodes.add(node.getRow());

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

                //m_logger.info ( "invisiableNode " + text);
                if (m_foldedNodes.contains(node.getRow())) {
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
            Node cursorNode = getCursorNode();

            if (m_foldIt)
                unfoldNode(toVisual(cursorNode));
            else
                foldNode(toVisual(cursorNode));
        }

        public void redo () {
            setCursorByPath(m_nodePath);
            Node cursorNode = getCursorNode();

            if (m_foldIt)
                foldNode(toVisual(cursorNode));
            else
                unfoldNode(toVisual(cursorNode));
        }

        boolean m_foldIt;
    }

    public AbstractUndoableEdit toggleFoldCursorUndoable()
    {
        Node cursorNode = getCursorNode();
        String text = m_mindTree.getText(cursorNode);
        VisualItem visualItem = toVisual(cursorNode);
        if (cursorNode.getChildCount() == 0)
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

        return new TogglingFoldUndoer (getNodePath(cursorNode), false);
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
            m_logger.info("nodePath="+ m_nodePath + ",   DBId="+m_mindTree.getDBId(getCursorNode()) + ",   oldValue="+m_oldValue);
        }

        public void redo ()
        {
            setCursorByPath(m_nodePath);
            setCursorProperty(m_property, m_newValue);
            System.out.print("nodePath="+ m_nodePath + ",   DBId="+m_mindTree.getDBId(getCursorNode()) + ",   newValue="+m_newValue);
        }

        final String m_property;
        final Object m_oldValue;
        final Object m_newValue;
    }

    private Object setCursorProperty (String property, Object value)
    {
        Node cursorNode = getCursorNode();
        Object oldValue = cursorNode.get(property);
        m_mindTree.setNodeProperty(m_mindTree.getDBId(cursorNode), property, value);
        return oldValue;
    }

    private AbstractUndoableEdit setCursorPropertyUndoable(String property, Object value)
    {
        Node cursorNode = getCursorNode();
        Object oldValue = setCursorProperty(property, value);
        return new SetPropertyUndoer(getNodePath(cursorNode), property, value, oldValue);
    }

    public AbstractUndoableEdit setCursorText(String text)
    {
        return setCursorPropertyUndoable(m_mindTree.sm_textPropName, text);
    }

    public boolean cursorIsPlaceholder()
    {
        return isPlaceholer(getCursorNode());
    }

    public void setPlaceholderCursorText(String text)
    {
        Node cursorNode = getCursorNode();
        assert(isPlaceholer(cursorNode));
        cursorNode.set(m_mindTree.sm_textPropName, text);
    }

    //TODO setFontFamliy setSize setColor
}
