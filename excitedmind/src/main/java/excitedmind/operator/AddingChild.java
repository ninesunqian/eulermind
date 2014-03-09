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
public class AddingChild extends MindOperator{
    int pos;
    String m_text;
    Object m_childDbId;

    public AddingChild(MindModel mindModel, Node formerCursor, int pos, String text) {
        super(mindModel, formerCursor);
        this.pos = pos;
        m_text = text;

    }

    public void does() {
        Object parentDBId = getDBIdByPath(m_formerCursorPath);
        m_childDbId = m_mindModel.addChild(parentDBId, pos, m_text);

        m_laterCursorPath = (Stack<Integer>) m_formerCursorPath.clone();
        m_laterCursorPath.add(pos);
    }

    public void undo() {
        m_mindModel.trashNode(getDBIdByPath(m_formerCursorPath), pos);
    }

    public void redo() {
        m_mindModel.restoreNodeFromTrash(m_childDbId);
    }
}
