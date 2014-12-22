package infinitmindmap.operator;

import infinitmindmap.MindModel;
import infinitmindmap.MindOperator;
import prefuse.data.Node;

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
    int m_newPos;
    ArrayList<Integer> m_parentPath;

    ArrayList<Integer> m_parentPathAfterDoing;

    public ChangingPosition(MindModel mindModel, Node formerCursor, int newPos)
    {
        super(mindModel, formerCursor);

        m_logger.info("arg: {}: {}", "mindModel", mindModel);
        m_logger.info("arg: {}: {}", "formerCursor", formerCursor);
        m_logger.info("arg: {}: {}", "newPos", newPos);

        m_oldPos = formerCursor.getIndex();
        m_newPos = newPos;

        m_parentPath = getNodePath(formerCursor.getParent());

        m_logger.info("ret:");
    }

    public void does()
    {
        m_logger.info("arg:");

        Node parent = getNodeByPath(m_parentPath);
        changePosition(m_parentPath, m_oldPos, m_newPos);

        m_parentPathAfterDoing = getNodePath(parent);

        //在引用父节点的情况下， 在显示树中改变一个节点的位置，有可能改变父节点，以及父节点的父节点的位置..
        //所以要重新获取一下路径
        m_laterCursorPath = (ArrayList)m_parentPathAfterDoing.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void undo()
    {
        m_logger.info("arg:");

        changePosition(m_parentPathAfterDoing, m_newPos, m_oldPos);

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

        Node parentNode = getNodeByPath(parentPath);
        m_mindModel.changeChildPos(m_mindModel.getDBId(parentNode), oldPos, newPos);

        m_logger.info("ret:");
    }
}
