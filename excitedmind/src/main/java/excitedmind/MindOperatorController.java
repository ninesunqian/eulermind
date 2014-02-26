package excitedmind;

import java.util.*;

import prefuse.Visualization;
import prefuse.data.*;

import prefuse.util.PrefuseLib;

import javax.swing.undo.AbstractUndoableEdit;
import java.util.logging.Logger;

public class MindOperatorController {
    final Logger m_logger = Logger.getLogger(this.getClass().getName());
    public final String m_treeGroupName;
    public final String m_treeNodesGroupName;
    public final String m_treeEdgesGroupName;

    //MindTree
    final MindTree m_mindTree;

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
        for (Node n=node.getParent(); n!=root; n=n.getParent())
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

    public MindOperatorController(MindTree mindTree, Visualization vis, String treeGroupName)
    {
        m_mindTree = mindTree;

        m_treeGroupName = treeGroupName;
        m_treeEdgesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.EDGES);
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
        return new AddingReferenceUndoer(getNodePath(referer), pos, refereeDBId);
    }


    boolean canAddReference(Node refererNode)
    {
        Node refereeNode = getCursorNode();
        return refererNode != refereeNode;
        /*
        return !m_mindTree.sameDBNode(refererNode, refereeNode)
                && !m_mindTree.sameDBNode(refereeNode.getParent(), refererNode);
                */
    }

    public AbstractUndoableEdit addReferenceUndoable(Node refererNode)
    {
        assert(canAddReference(refererNode));
        Node refereeNode = getCursorNode();
        AbstractUndoableEdit undoer =  addReferenceUndoable(refererNode, refererNode.getChildCount(), m_mindTree.getDBId(refereeNode));
        setCursorNode(refererNode);
        return undoer;
    }

    public void addPlaceholder(boolean asChild)
    {
        Node cursorNode = getCursorNode();
        Node newNode;
        m_savedCursor = cursorNode;
        if (asChild) {
            //FIXME: prefuse function using dbTree's argument
            newNode = m_mindTree.m_displayTree.addChild(cursorNode, m_mindTree.getChildCount(cursorNode));
        } else {
            Node parent = cursorNode.getParent();
            int pos = cursorNode.getIndex() + 1;
            newNode = m_mindTree.m_displayTree.addChild(parent, pos);
        }
        newNode.set(m_mindTree.sm_textPropName, "");
        setCursorNode(newNode);
    }

    public void removePlaceholder()
    {
        Node placeholderNode = getCursorNode();
        assert(isPlaceholer(placeholderNode));
        assert(placeholderNode != m_mindTree.m_displayTree.getRoot());

        m_mindTree.m_displayTree.removeChild(placeholderNode);
        setCursorNode(m_savedCursor);
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
        return  undor;
    }

    //node has only dbId
    public AbstractUndoableEdit placeRefereeUndoable(Object refereeDBId)
    {
        Node cursorNode = getCursorNode();

        assert(cursorNode != m_mindTree.m_displayTree.getRoot());
        assert(isPlaceholer(cursorNode));

        Node referer = cursorNode.getParent();
        int pos = cursorNode.getIndex();
        m_mindTree.m_displayTree.removeChild(cursorNode);

        AbstractUndoableEdit undor = addReferenceUndoable(referer, pos, refereeDBId);
        setCursorNode(referer.getChild(pos));
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
        if (newPos == MindDB.ADDING_EDGE_END) {
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

    public boolean canResetParent(Node newParent)
    {
        Node cursorNode =getCursorNode();
        return (! m_mindTree.sameDBNode(newParent, cursorNode))
                && (! m_mindTree.sameDBNode(cursorNode.getParent(), newParent))
                && (!m_mindTree.isInDBSubTree(newParent, cursorNode));
    }

    public AbstractUndoableEdit resetParentUndoable(Node newParent)
    {
        Node cursorNode = getCursorNode();

        assert(canResetParent(newParent));

        if (cursorNode == m_mindTree.m_displayTree.getRoot()) {
            return null;
        }

        Node oldParent = cursorNode.getParent();

        if (m_mindTree.sameDBNode(newParent, oldParent)) {
            return null;
        }

        MindDB.InheritDirection inheritDirection = m_mindTree.getInheritDirection(cursorNode, newParent);
        if (inheritDirection == MindDB.InheritDirection.LINEAL_DESCENDANT) {
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
