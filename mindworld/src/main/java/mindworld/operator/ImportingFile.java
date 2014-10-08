package mindworld.operator;

import mindworld.MindModel;
import mindworld.MindOperator;
import prefuse.data.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-10-7
 * Time: 下午2:30
 * To change this template use File | Settings | File Templates.
 */
public class ImportingFile extends MindOperator{
    String m_path;
    List m_newChildren;

    public ImportingFile(MindModel mindModel, Node formerCursor,  String path) {
        super(mindModel, formerCursor);
        m_path = path;
    }

    public void does() throws Exception
    {
        Node parent = getNodeByPath(m_formerCursorPath);
        m_newChildren = m_mindModel.importFile(parent, m_path);

        m_laterCursorPath = (ArrayList<Integer>) m_formerCursorPath.clone();
        m_laterCursorPath.add(parent.getChildCount() - 1);
    }

    public void undo() {
        Node parent = getNodeByPath(m_formerCursorPath);
        for (int i=0; i<m_newChildren.size(); i++) {
            m_mindModel.trashNode(MindModel.getDBId(parent), parent.getChildCount() + i);
        }
    }

    public void redo() {
        for (int i=0; i<m_newChildren.size(); i++) {
            m_mindModel.restoreNodeFromTrash(getNodeByPath(m_formerCursorPath), m_newChildren.get(i));
        }
    }
}
