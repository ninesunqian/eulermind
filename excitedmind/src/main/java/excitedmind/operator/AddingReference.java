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
    Object m_referrerDBId;
    Object m_referentDBId;
    int m_pos;
    Stack<Integer> m_referrerNodePath;

    //formerCursor is referrer: using add referent node by edit prompter
    public AddingReference(MindModel mindModel, Node formerCursor, Node referrerNode, int pos) {
        super(mindModel, formerCursor);
        m_referrerDBId = m_mindModel.getDBId(referrerNode);
        m_referentDBId = m_mindModel.getDBId(formerCursor);
        m_pos = pos;
        m_referrerNodePath = m_mindModel.getNodePath(referrerNode);
    }

    //formerCursor is referrer: using for drag referent node to referrer node by mouse
    public AddingReference(MindModel mindModel, Node formerCursor, Object referentDBId, int pos) {
        super(mindModel, formerCursor);
        m_referrerDBId = m_mindModel.getDBId(formerCursor);
        m_referentDBId = referentDBId;
        m_pos = pos;
        m_referrerNodePath = m_mindModel.getNodePath(formerCursor);
    }

    public void doing() {
        m_mindModel.addReference(m_referrerDBId, m_pos, m_referentDBId);

        m_laterCursorPath = (Stack<Integer>) m_referrerNodePath.clone();
        m_laterCursorPath.add(m_pos);
    }

    public void undo() {
        m_mindModel.removeReference(m_referrerDBId, m_pos);
    }

    public void redo() {
        m_mindModel.addReference(m_referrerDBId, m_pos, m_referentDBId);
    }
}
