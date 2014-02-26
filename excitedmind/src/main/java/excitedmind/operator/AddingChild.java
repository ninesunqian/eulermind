package excitedmind.operator;

import excitedmind.MindModel;
import excitedmind.MindOperator;
import prefuse.data.Node;
import prefuse.data.Tree;

import java.util.Stack;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-2-26
 * Time: 上午7:07
 * To change this template use File | Settings | File Templates.
 */
public class AddingChild extends MindOperator{
    int m_childPos;
    String m_text;
    Object m_childDbId

    AddingChild(MindModel mindModel, Node formerCursor, int pos, String text) {
        super(mindModel, formerCursor);
        m_childPos = pos;
        m_text = text;
    }

    public void doing() {
        Object parentDBId = getDBIdByPath(m_formerCursorPath);
        m_childDbId = m_mindModel.addChild(parentDBId, m_childPos, m_text);

        m_laterCursorPath = (Stack<Integer>) m_formerCursorPath.clone();
        m_laterCursorPath.add(m_childPos);
    }

    public void undo() {
        m_mindModel.trashNode(getDBIdByPath(m_formerCursorPath), m_childPos);
    }

    public void redo() {
        m_mindModel.restoreNodeFromTrash(m_childDbId);
    }
}
