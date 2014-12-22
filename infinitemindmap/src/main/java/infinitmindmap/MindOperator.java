package infinitmindmap;

import prefuse.data.Node;
import prefuse.data.Tree;

import javax.swing.undo.AbstractUndoableEdit;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-2-26
 * Time: 上午7:17
 * To change this template use File | Settings | File Templates.
 */
public abstract class MindOperator extends AbstractUndoableEdit {
    protected MindModel m_mindModel;
    protected Object m_rootDBId;
    protected ArrayList<Integer> m_formerCursorPath;
    protected ArrayList<Integer> m_laterCursorPath;

    protected Logger m_logger;

    public MindOperator(MindModel mindModel, Node formerCursor) {
        m_logger = LoggerFactory.getLogger(this.getClass());

        Tree tree = (Tree)formerCursor.getGraph();
        m_mindModel = mindModel;
        m_rootDBId = m_mindModel.getDBId(tree.getRoot());
        m_formerCursorPath = m_mindModel.getNodePath(formerCursor);
    }

    protected Node getNodeByPath(ArrayList<Integer> path) {
        Tree tree = m_mindModel.findTree(m_rootDBId);
        Node node = m_mindModel.getNodeByPath(tree, path);
        return node;
    }

    protected ArrayList<Integer> getNodePath(Node node) {
        return m_mindModel.getNodePath(node);
    }

    public boolean canRedo() {
        return true;
    }

    public boolean canUndo() {
        return true;
    }

    abstract public void does() throws Exception;

}
