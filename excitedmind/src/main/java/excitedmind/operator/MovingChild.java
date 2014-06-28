package excitedmind.operator;

import excitedmind.MindDB;
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

        m_logger.info("arg: {}: {}", "mindModel", mindModel);
        m_logger.info("arg: {}: {}", "formerCursor", formerCursor);
        m_logger.info("arg: {}: {}", "newParent", newParent);
        m_logger.info("arg: {}: {}", "newPos", newPos);


        m_oldParentPath = m_mindModel.getNodePath(formerCursor.getParent());
        m_oldPos = formerCursor.getIndex();

        m_newParentPath = m_mindModel.getNodePath(newParent);
        m_newPos = newPos;

        m_laterCursorPath = (ArrayList<Integer>)m_newParentPath.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void does()
    {
        m_logger.info("arg:");
        moveChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);
        m_logger.info("ret:");
    }

    public void undo()
    {
        m_logger.info("arg:");
        moveChild(m_newParentPath, m_newPos, m_oldParentPath, m_oldPos);
        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg: ");
        moveChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);
        m_logger.info("ret: ");
    }

    private void moveChild(ArrayList<Integer> oldParentPath, int oldPos, ArrayList<Integer> newParentPath, int newPos)
    {
        m_logger.info("arg: {}:{}", "oldParentPath", oldParentPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newParentPath", newParentPath);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        Tree tree = m_mindModel.findTree(m_rootDBId);
        Node oldParentNode = m_mindModel.getNodeByPath(tree, oldParentPath);
        Node newParentNode = m_mindModel.getNodeByPath(tree, newParentPath);

        Node child = oldParentNode.getChild(oldPos);
        MindDB.InheritDirection inheritDirectionToNewParent =
                m_mindModel.getInheritDirection(child, newParentNode);

        assert inheritDirectionToNewParent != MindDB.InheritDirection.SELF
                && inheritDirectionToNewParent != MindDB.InheritDirection.LINEAL_DESCENDANT;

        if (oldParentPath == newParentPath) {
            m_mindModel.changeChildPos(m_mindModel.getDBId(oldParentNode), oldPos, newPos);
        } else {
            m_mindModel.moveChild(oldParentNode, oldPos, newParentNode, newPos);
        }

        m_logger.info("ret:");
    }
}
