package infinitmindmap.operator;

import infinitmindmap.MindModel;
import infinitmindmap.MindOperator;
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
    String m_importedFilePath;
    List m_newChildren;

    ArrayList<Integer> m_parentPath;
    ArrayList<Integer> m_parentPathAfterDoing;

    public ImportingFile(MindModel mindModel, Node formerCursor,  String path) {
        super(mindModel, formerCursor);
        m_importedFilePath = path;
        m_parentPath = getNodePath(formerCursor);
    }

    public void does() throws Exception
    {
        Node parent = getNodeByPath(m_parentPath);
        m_newChildren = m_mindModel.importFile(parent, m_importedFilePath);

        //importFIle之后，parent的路径可能会改变，所以要重新取一次路径
        m_parentPathAfterDoing = getNodePath(parent);

        m_laterCursorPath = (ArrayList)m_parentPathAfterDoing.clone();
        m_laterCursorPath.add(parent.getChildCount() - 1);
    }

    public void undo() {
        Node parent = getNodeByPath(m_parentPathAfterDoing);
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
