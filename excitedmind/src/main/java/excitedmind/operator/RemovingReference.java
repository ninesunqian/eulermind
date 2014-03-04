package excitedmind.operator;

import excitedmind.MindModel;
import excitedmind.MindOperator;
import prefuse.data.Node;

import java.util.Stack;

public class RemovingReference extends MindOperator {
    Object m_referrerDBId;
    Object m_referentDBId ;
    int m_pos;

    RemovingReference(MindModel mindModel, Node formerCursor)
    {
        super(mindModel, formerCursor);
        m_referentDBId = mindModel.getDBId(formerCursor);
        m_referrerDBId = mindModel.getDBId(formerCursor.getParent());
        m_pos = formerCursor.getIndex();
    }

    public void doing ()
    {
        m_mindModel.removeReference(m_referrerDBId, m_pos);
        m_laterCursorPath = (Stack<Integer>)m_formerCursorPath.clone();
        m_laterCursorPath.add(m_pos);
    }

    public void undo()
    {
        m_mindModel.addReference(m_referrerDBId, m_pos, m_referentDBId);
    }

    public void redo()
    {
        m_mindModel.removeReference(m_referrerDBId, m_pos);
    }
}
