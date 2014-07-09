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
public class ChangingPosition extends MindOperator{

    int m_oldPos;
    ArrayList<Integer> m_parentPath;
    int m_newPos;

    public ChangingPosition(MindModel mindModel, Node formerCursor, int newPos)
    {
        super(mindModel, formerCursor);

        m_logger.info("arg: {}: {}", "mindModel", mindModel);
        m_logger.info("arg: {}: {}", "formerCursor", formerCursor);
        m_logger.info("arg: {}: {}", "newPos", newPos);

        m_parentPath = m_mindModel.getNodePath(formerCursor.getParent());
        m_oldPos = formerCursor.getIndex();
        m_newPos = newPos;

        m_laterCursorPath = (ArrayList<Integer>) m_parentPath.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void does()
    {
        m_logger.info("arg:");
        changePosition(m_parentPath, m_oldPos, m_newPos);
        m_logger.info("ret:");
    }

    public void undo()
    {
        m_logger.info("arg:");
        changePosition(m_parentPath, m_newPos, m_oldPos);
        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg: ");
        changePosition(m_parentPath, m_oldPos, m_newPos);
        m_logger.info("ret: ");
    }

    private void changePosition(ArrayList<Integer> parentPath, int oldPos, int newPos)
    {
        m_logger.info("arg: {}:{}", "parentPath", parentPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        if (oldPos == newPos) {
            return;
        }

        Tree tree = m_mindModel.findTree(m_rootDBId);
        Node parentNode = m_mindModel.getNodeByPath(tree, parentPath);

        m_mindModel.changeChildPos(m_mindModel.getDBId(parentNode), oldPos, newPos);

        m_logger.info("ret:");
    }
}
