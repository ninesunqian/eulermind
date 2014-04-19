package excitedmind.operator;

import excitedmind.MindModel;
import excitedmind.MindOperator;
import prefuse.data.Node;
import prefuse.data.Tree;

public class RemovingSubTree extends MindOperator {
    public Object m_trashDBId;
    public int m_siblingPos;

    public RemovingSubTree(MindModel mindModel, Node formerCursor)
    {
        super(mindModel, formerCursor);
        m_siblingPos = formerCursor.getIndex();
        m_trashDBId = mindModel.getDBId(formerCursor);
    }

    public void does()
    {
        Tree tree = m_mindModel.findTree(m_rootDBId);
        Node node = m_mindModel.getNodeByPath(tree, m_formerCursorPath);

        //TODO: move to mindview
        /*
        if (m_mindTree.sameDBNode(root, node)) {
            return;
        }
        */

        //从要删除的节点，向根节点找，找到最接近根的相同dbID的parent节点
        Node topParent = topSameDBNode(tree, node.getParent());

        Node newCursor = getFamiliarNode(topParent.getChild(node.getIndex()));

        assert(newCursor.isValid());

        //using node path, compute the removed node in highest level;
        m_mindModel.trashNode(m_mindModel.getDBId(topParent), m_siblingPos);

        m_laterCursorPath = m_mindModel.getNodePath(newCursor);
    }

    public void undo ()
    {
        m_mindModel.restoreNodeFromTrash(m_trashDBId);
    }

    public void redo ()
    {
        does();
    }

    private Node getFamiliarNode (Node node)
    {
        int start = node.getIndex();
        Node parent = node.getParent();
        Node familiar;
        //在topParent的子节点中，找到两个与被删除节点dbId不同的节点，和一个相同的节点
        for (int i=start+1; i<parent.getChildCount(); i++) {
            Node tmp = parent.getChild(i);
            if (!m_mindModel.sameDBNode(tmp, node)) {
                return tmp;
            }
        }

        for (int i=0; i<start; i++) {
            Node tmp = parent.getChild(i);
            if (!m_mindModel.sameDBNode(tmp, node)) {
                return tmp;
            }
        }
        return parent;
    }

    private Node topSameDBNode(Tree tree, Node node)
    {
        Node topNode = node;
        Node root = tree.getRoot();

        while (node != root)
        {
            if (m_mindModel.sameDBNode(node, node)) {
                topNode = node;
            }
            node = node.getParent();
        }

        return topNode;
    }
}
