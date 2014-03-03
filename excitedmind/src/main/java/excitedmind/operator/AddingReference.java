package excitedmind.operator;

import excitedmind.MindModel;
import excitedmind.MindOperator;
import prefuse.data.Node;

import java.util.Stack;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-2-26
 * Time: 上午7:07
 * To change this template use File | Settings | File Templates.
 */
public class AddingReference extends MindOperator {
    Object m_refererDBId;
    Object m_refereeDBId;
    int m_pos;
    Stack<Integer> m_refererNodePath;

    //formerCursor is referer: using add referee node by edit prompter
    public AddingReference(MindModel mindModel, Node formerCursor, Node refererNode, int pos) {
        super(mindModel, formerCursor);
        m_refererDBId = m_mindModel.getDBId(refererNode);
        m_refereeDBId = m_mindModel.getDBId(formerCursor);
        m_pos = pos;
        m_refererNodePath = m_mindModel.getNodePath(refererNode);
    }

    //formerCursor is referer: using for drag referee node to referer node by mouse
    public AddingReference(MindModel mindModel, Node formerCursor, Object refereeDBId, int pos) {
        super(mindModel, formerCursor);
        m_refererDBId = m_mindModel.getDBId(formerCursor);
        m_refereeDBId = refereeDBId;
        m_pos = pos;
        m_refererNodePath = m_mindModel.getNodePath(formerCursor);
    }

    public void doing() {
        m_mindModel.addReference(m_refererDBId, m_pos, m_refereeDBId);

        m_laterCursorPath = (Stack<Integer>) m_refererNodePath.clone();
        m_laterCursorPath.add(m_pos);
    }

    public void undo() {
        m_mindModel.removeReference(m_refererDBId, m_pos);
    }

    public void redo() {
        m_mindModel.addReference(m_refererDBId, m_pos, m_refereeDBId);
    }
}
