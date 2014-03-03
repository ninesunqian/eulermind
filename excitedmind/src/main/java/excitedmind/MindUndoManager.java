package excitedmind;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-3-2
 * Time: 下午9:57
 * To change this template use File | Settings | File Templates.
 */
public abstract class MindUndoManager extends UndoManager {
    Hashtable<Object, MindView> m_mindViews = new Hashtable<Object, MindView>();
    Object m_currentRoot;

    MindModel m_mindModel;
    private UndoableEdit edit;

    MindUndoManager(MindModel mindModel) {
        super();
        m_mindModel = mindModel;
    }

    public boolean addMindView(Object rootDBId, MindView mindView) {
        if (m_mindViews.get(rootDBId) != null) {
            return false;
        } else {
            m_mindViews.put(rootDBId, mindView);
            return true;
        }
    }

    abstract public void exposeMindView(MindView mindView);

    public void ascendOneRoot(Object oldRootDBId, Object newRootDBId, int pos)
    {
        MindView mindView = m_mindViews.get(oldRootDBId);
        m_mindViews.remove(oldRootDBId);
        m_mindViews.put(newRootDBId, mindView);
        for(UndoableEdit edit : edits) {
            MindOperator operator = (MindOperator)edit;
            if (operator.m_rootDBId.equals(oldRootDBId)) {
                operator.m_rootDBId = newRootDBId;
                operator.m_formerCursorPath.insertElementAt(pos, 0);
            }
        }
    }

    public synchronized void redo()
    {
        if (! canRedo())
            return;

        MindOperator operator = (MindOperator)editToBeRedone();
        if (operator.m_rootDBId != m_currentRoot) {
            exposeMindView(m_mindViews.get(operator.m_rootDBId));
            m_currentRoot = operator.m_rootDBId;
        }
        super.redo();
    }

    public synchronized void undo()
    {
        if (! canUndo())
            return;

        MindOperator operator = (MindOperator)editToBeRedone();
        if (operator.m_rootDBId != m_currentRoot) {
            exposeMindView(m_mindViews.get(operator.m_rootDBId));
            m_currentRoot = operator.m_rootDBId;
        }
        super.undo();
    }
}
