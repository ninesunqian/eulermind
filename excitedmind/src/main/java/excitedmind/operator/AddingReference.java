package excitedmind.operator;

import excitedmind.MindModel;
import excitedmind.MindOperator;
import prefuse.data.Node;

import java.util.ArrayList;
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
    ArrayList<Integer> m_referrerNodePath;

    private void init(Node referrerNode, Object referrerDBId, Object referentDBId, int pos) {
        m_referrerDBId = referrerDBId;
        m_referentDBId = referentDBId;
        m_pos = pos;

        m_referrerNodePath = m_mindModel.getNodePath(referrerNode);

        m_laterCursorPath = (ArrayList<Integer>) m_referrerNodePath.clone();
        m_laterCursorPath.add(m_pos);
    }

    //formerCursor is referent: using for drag referent node to referrer node by mouse
    public AddingReference(MindModel mindModel, Node formerCursor, Node referrerNode, int pos) {
        super(mindModel, formerCursor);
        init(referrerNode, m_mindModel.getDBId(referrerNode), m_mindModel.getDBId(formerCursor), pos);
    }

    //formerCursor is referrer: using add referent node by edit prompter
    public AddingReference(MindModel mindModel, Node formerCursor, Object referentDBId, int pos) {
        super(mindModel, formerCursor);
        init(formerCursor, m_mindModel.getDBId(formerCursor), referentDBId, pos);
    }

    public void does() {
        m_mindModel.addReference(m_referrerDBId, m_pos, m_referentDBId);
    }

    public void undo() {
        m_mindModel.removeReference(m_referrerDBId, m_pos);
    }

    public void redo() {
        m_mindModel.addReference(m_referrerDBId, m_pos, m_referentDBId);
    }
}
