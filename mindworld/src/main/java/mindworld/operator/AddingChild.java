package mindworld.operator;

import mindworld.MindModel;
import mindworld.MindOperator;
import prefuse.data.Node;

import java.util.ArrayList;

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
        Node parent = getNodeByPath(m_formerCursorPath);
        m_childDbId = m_mindModel.addChild(parent, pos, m_text);

        m_laterCursorPath = (ArrayList<Integer>) m_formerCursorPath.clone();
        m_laterCursorPath.add(pos);
    }

    public void undo() {
        Node parent = getNodeByPath(m_formerCursorPath);
        m_mindModel.trashNode(MindModel.getDBId(parent), pos);
    }

    public void redo() {
        m_mindModel.restoreNodeFromTrash(getNodeByPath(m_formerCursorPath), m_childDbId);
    }
}
