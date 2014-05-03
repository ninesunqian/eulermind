package excitedmind.operator;

import excitedmind.MindModel;
import excitedmind.MindOperator;
import prefuse.data.Node;
import prefuse.data.Tree;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-3-3
 * Time: 下午9:26
 * To change this template use File | Settings | File Templates.
 */
public class MovingChild extends MindOperator{

    ArrayList<Integer> m_oldParentPath;
    int m_oldPos;
    ArrayList<Integer> m_newParentPath;
    int m_newPos;

    public MovingChild(MindModel mindModel, Node formerCursor, Node newParent, int newPos)
    {
        super(mindModel, formerCursor);

        m_oldParentPath = m_mindModel.getNodePath(formerCursor.getParent());
        m_oldPos = formerCursor.getIndex();

        m_newParentPath = m_mindModel.getNodePath(newParent);
        m_newPos = newPos;

        m_laterCursorPath = (ArrayList<Integer>)m_newParentPath.clone();
        m_laterCursorPath.add(m_newPos);
    }

    public void does()
    {
        moveChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);
    }

    public void undo()
    {
        moveChild(m_newParentPath, m_newPos, m_oldParentPath, m_oldPos);
    }

    public void redo()
    {
        moveChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);
    }

    private void moveChild(ArrayList<Integer> oldParentPath, int oldPos, ArrayList<Integer> newParentPath, int newPos)
    {
        Tree tree = m_mindModel.findTree(m_rootDBId);
        Node oldParentNode = m_mindModel.getNodeByPath(tree, oldParentPath);
        Node newParentNode = m_mindModel.getNodeByPath(tree, newParentPath);

        Object oldParentDBId = m_mindModel.getDBId(oldParentNode);
        Object newParentDBId = m_mindModel.getDBId(newParentNode);

        m_mindModel.moveChild(oldParentDBId, oldPos, newParentDBId, newPos);
    }
}
