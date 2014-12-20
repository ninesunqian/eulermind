package mindworld.operator;

import mindworld.MindDB;
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
public class HandoveringChild extends MindOperator{

    ArrayList<Integer> m_oldParentPath;
    int m_oldPos;
    ArrayList<Integer> m_newParentPath;
    int m_newPos;

    ArrayList<Integer> m_oldParentPathAfterDoing;
    ArrayList<Integer> m_newParentPathAfterDoing;
    public HandoveringChild(MindModel mindModel, Node formerCursor, Node newParent, int newPos)
    {
        super(mindModel, formerCursor);

        m_logger.info("arg: {}: {}", "mindModel", mindModel);
        m_logger.info("arg: {}: {}", "formerCursor", formerCursor);
        m_logger.info("arg: {}: {}", "newParent", newParent);
        m_logger.info("arg: {}: {}", "newPos", newPos);


        m_oldParentPath = getNodePath(formerCursor.getParent());
        m_oldPos = formerCursor.getIndex();

        m_newParentPath = getNodePath(newParent);
        m_newPos = newPos;

        m_laterCursorPath = (ArrayList<Integer>)m_newParentPath.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void does()
    {
        m_logger.info("arg:");

        Node oldParentNode = getNodeByPath(m_oldParentPath);
        Node newParentNode = getNodeByPath(m_newParentPath);

        handoverChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);

        m_oldParentPathAfterDoing = getNodePath(oldParentNode);
        m_newParentPathAfterDoing = getNodePath(newParentNode);

        m_laterCursorPath = (ArrayList) m_newParentPathAfterDoing.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void undo()
    {
        m_logger.info("arg:");
        handoverChild(m_newParentPathAfterDoing, m_newPos,
                m_oldParentPathAfterDoing, m_oldPos);
        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg: ");
        handoverChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);
        m_logger.info("ret: ");
    }

    private void handoverChild(ArrayList<Integer> oldParentPath, int oldPos, ArrayList<Integer> newParentPath, int newPos)
    {
        m_logger.info("arg: {}:{}", "oldParentPath", oldParentPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newParentPath", newParentPath);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        Tree tree = m_mindModel.findTree(m_rootDBId);
        Node oldParentNode = m_mindModel.getNodeByPath(tree, oldParentPath);
        Node newParentNode = m_mindModel.getNodeByPath(tree, newParentPath);

        Node child = oldParentNode.getChild(oldPos);

        assert ! m_mindModel.isSelfInDB(child, newParentNode) && ! m_mindModel.isDescendantInDB(child, newParentNode);

        assert !MindModel.getDBId(oldParentNode).equals(MindModel.getDBId(newParentNode));
        assert !m_mindModel.isRefNode(child);

        m_mindModel.handoverChild(oldParentNode, oldPos, newParentNode, newPos);

        m_logger.info("ret:");
    }
}
