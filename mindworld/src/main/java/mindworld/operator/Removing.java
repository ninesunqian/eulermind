package mindworld.operator;

import mindworld.MindModel;
import mindworld.MindOperator;
import prefuse.data.Node;
import prefuse.data.Tree;

import java.util.ArrayList;

public class Removing extends MindOperator {
    public Object m_parentDBId;
    public int m_siblingPos;
    public Object m_removedDBId;

    public boolean m_isRefRelation;

    public Removing(MindModel mindModel, Node formerCursor)
    {
        super(mindModel, formerCursor);
        Node parent = formerCursor.getParent();
        m_parentDBId = m_mindModel.getDBId(parent);

        m_siblingPos = formerCursor.getIndex();
        m_removedDBId = mindModel.getDBId(formerCursor);

        computeLaterCursor(formerCursor);

        m_isRefRelation = m_mindModel.isRefNode(formerCursor);
    }

    public void does()
    {
        if (m_isRefRelation) {
            m_mindModel.removeReference(m_parentDBId, m_siblingPos);
        } else {
            m_mindModel.trashNode(m_parentDBId, m_siblingPos);
        }
    }

    public void undo()
    {
        ArrayList parentPath = (ArrayList)m_formerCursorPath.clone();
        parentPath.remove(parentPath.size() - 1);

        Node parentNode = getNodeByPath(parentPath);
        if (m_isRefRelation) {
            m_mindModel.addReference(parentNode, m_siblingPos, m_removedDBId);
        } else {
            m_mindModel.restoreNodeFromTrash(parentNode, m_removedDBId);
        }
    }

    public void redo()
    {
        does();
    }

    public static boolean canDo(MindModel mindModel, Tree tree, Node node)
    {
        Node root = tree.getRoot();

        if (node == root) {
            return false;
        }

        if (mindModel.isRefNode(node)) {
            return true;
        } else {
            if (mindModel.isDescendantInDB(node, root) || mindModel.isSelfInDB(node, root)) {
                return false;
            } else {
                return true;
            }
        }
    }

    private Node getNearestSiblingWithDiffDBId(Node node)
    {
        int start = node.getIndex();
        Node parent = node.getParent();

        //firstly to right
        for (int i=start+1; i<parent.getChildCount(); i++) {
            Node tmp = parent.getChild(i);
            if (!m_mindModel.isSelfInDB(tmp, node)) {
                return tmp;
            }
        }

        //then to left
        for (int i=start-1; i>=0; i--) {
            Node tmp = parent.getChild(i);
            if (!m_mindModel.isSelfInDB(tmp, node)) {
                return tmp;
            }
        }

        return null;
    }

    private Node topSameDBNode(Tree tree, Node node)
    {
        Node topNode = node;

        Node tmpNode = node;
        Node root = tree.getRoot();

        while (tmpNode != null)
        {
            if (m_mindModel.isSelfInDB(tmpNode, node)) {
                topNode = tmpNode;
            }
            tmpNode = tmpNode.getParent();
        }

        return topNode;
    }

    private void computeLaterCursor(Node formerCursor)
    {
        Tree tree = (Tree)formerCursor.getGraph();
        Node parent = formerCursor.getParent();

        //change formerCursor to its avatar nearest to root
        parent = topSameDBNode(tree, parent);
        formerCursor = parent.getChild(m_siblingPos);

        m_formerCursorPath = getNodePath(formerCursor);

        if (m_isRefRelation) {
            if (parent.getChildCount()  == 1) {
                m_laterCursorPath = getNodePath(parent);

            } else {
                if (formerCursor.getIndex() == parent.getChildCount() - 1) {
                    m_laterCursorPath = getNodePath(formerCursor);
                    m_laterCursorPath.set(0, formerCursor.getIndex() - 1);
                } else {
                    //formerCursor's older sibling move into this position
                    m_laterCursorPath = getNodePath(formerCursor);
                }
            }

        } else {

            Node nearestSiblingWithDiffDBId =  getNearestSiblingWithDiffDBId(formerCursor);

            if (nearestSiblingWithDiffDBId == null) {
                m_laterCursorPath = getNodePath(parent);

            } else {
                ArrayList<Node> siblingsWithDiffDBId = new ArrayList<Node>();

                for (int i=0; i<parent.getChildCount(); i++) {
                    Node sibling = parent.getChild(i);
                    if (!m_mindModel.isSelfInDB(sibling, formerCursor)) {
                        siblingsWithDiffDBId.add(parent.getChild(i));
                    }
                }

                m_laterCursorPath = getNodePath(formerCursor);
                m_laterCursorPath.set(m_laterCursorPath.size()-1, siblingsWithDiffDBId.indexOf(nearestSiblingWithDiffDBId));
            }
        }
    }
}
