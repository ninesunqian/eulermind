package mindworld.operator;

import mindworld.MindModel;
import mindworld.MindOperator;
import prefuse.data.Node;
import prefuse.data.Tree;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-3-3
 * Time: 下午9:26
 * To change this template use File | Settings | File Templates.
 */
public class HandoveringReference extends MindOperator{

    ArrayList<Integer> m_oldReferrerPath;
    int m_oldPos;
    ArrayList<Integer> m_newReferrerPath;
    int m_newPos;

    public HandoveringReference(MindModel mindModel, Node formerCursor, Node newReferrer, int newPos)
    {
        super(mindModel, formerCursor);

        m_logger.info("arg: {}: {}", "mindModel", mindModel);
        m_logger.info("arg: {}: {}", "formerCursor", formerCursor);
        m_logger.info("arg: {}: {}", "newReferrer", newReferrer);
        m_logger.info("arg: {}: {}", "newPos", newPos);


        m_oldReferrerPath = m_mindModel.getNodePath(formerCursor.getParent());
        m_oldPos = formerCursor.getIndex();

        m_newReferrerPath = m_mindModel.getNodePath(newReferrer);
        m_newPos = newPos;

        m_laterCursorPath = (ArrayList<Integer>)m_newReferrerPath.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void does()
    {
        m_logger.info("arg:");
        handoverReferent(m_oldReferrerPath, m_oldPos, m_newReferrerPath, m_newPos);
        m_logger.info("ret:");
    }

    public void undo()
    {
        m_logger.info("arg:");
        handoverReferent(m_newReferrerPath, m_newPos, m_oldReferrerPath, m_oldPos);
        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg: ");
        handoverReferent(m_oldReferrerPath, m_oldPos, m_newReferrerPath, m_newPos);
        m_logger.info("ret: ");
    }

    static public boolean canDo()
    {
        return false;
    }

    private void handoverReferent(ArrayList<Integer> oldReferrerPath, int oldPos, ArrayList<Integer> newReferrerPath, int newPos)
    {
        m_logger.info("arg: {}:{}", "oldReferrerPath", oldReferrerPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newReferrerPath", newReferrerPath);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        Tree tree = m_mindModel.findTree(m_rootDBId);
        Node oldReferrerNode = m_mindModel.getNodeByPath(tree, oldReferrerPath);
        Node newReferrerNode = m_mindModel.getNodeByPath(tree, newReferrerPath);

        Node child = oldReferrerNode.getChild(oldPos);

        assert !MindModel.getDBId(oldReferrerNode).equals(MindModel.getDBId(newReferrerNode));
        assert m_mindModel.isRefNode(child);

        m_mindModel.handoverReferent(oldReferrerNode, oldPos, newReferrerNode, newPos);

        m_logger.info("ret:");
    }
}
