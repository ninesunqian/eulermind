package infinitmindmap.operator;

import infinitmindmap.MindModel;
import infinitmindmap.MindOperator;
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

    ArrayList<Integer> m_parentPath;
    int pos;
    String m_text;
    Object m_childDbId;

    ArrayList<Integer> m_parentPathAfterDoing;

    public AddingChild(MindModel mindModel, Node formerCursor, int pos, String text) {
        super(mindModel, formerCursor);
        this.pos = pos;
        m_text = text;
    }

    public void does() {
        Node parent = getNodeByPath(m_formerCursorPath);
        m_parentPath = (ArrayList)m_formerCursorPath.clone();

        m_childDbId = m_mindModel.addChild(parent, pos, m_text);

        //重新取parent的路径
        m_parentPathAfterDoing = getNodePath(parent);
        m_laterCursorPath = (ArrayList)m_parentPathAfterDoing.clone();
        m_laterCursorPath.add(pos);
    }

    public void undo() {
        Node parent = getNodeByPath(m_parentPathAfterDoing);
        m_mindModel.trashNode(MindModel.getDBId(parent), pos);
    }

    public void redo() {
        Node parent = getNodeByPath(m_parentPath);
        m_mindModel.restoreNodeFromTrash(parent, m_childDbId);
    }
}
