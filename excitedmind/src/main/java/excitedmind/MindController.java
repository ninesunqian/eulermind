package excitedmind;

import excitedmind.operator.Removing;
import prefuse.data.Node;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-3-2
 * Time: 下午9:57
 * To change this template use File | Settings | File Templates.
 */
public class MindController extends UndoManager {
    Logger m_logger = Logger.getLogger(this.getClass().getName());

    Hashtable<Object, MindView> m_mindViews = new Hashtable<Object, MindView>();

    MindModel m_mindModel;
    JTabbedPane m_tabbedPane;

    ArrayList<NodeDndControl> m_externalMouseContollers =
            new ArrayList<NodeDndControl>();

    MindController(MindModel mindModel, JTabbedPane tabbedPane) {
        super();
        m_mindModel = mindModel;
        m_tabbedPane = tabbedPane;
    }

    void addExternalMouseController(NodeDndControl robustNodeItemController)
    {
        if (m_externalMouseContollers.contains(robustNodeItemController)) {
            return;
        }

        for (Object rootDBId : m_mindViews.keySet()) {
            m_mindViews.get(rootDBId).addControlListener(robustNodeItemController);
        }
    }

    public MindView findOrAddMindView(Object rootDBId) {
        MindView mindView = m_mindViews.get(rootDBId);
        if (mindView != null) {
            return mindView;
        }

        mindView = new MindView(m_mindModel, this, rootDBId);

        m_mindViews.put(rootDBId, mindView);
        Node root = mindView.m_tree.getRoot();
        m_tabbedPane.addTab(m_mindModel.getText(root), mindView);

        for(NodeDndControl controller : m_externalMouseContollers) {
            mindView.addControlListener(controller);
        }

        return  mindView;
    }

    public void removeMindView(Object rootDBId) {
        MindView mindView = m_mindViews.get(rootDBId);
        m_tabbedPane.remove(mindView);
        m_mindViews.remove(rootDBId);
    }

    public MindView exposeMindView(Object rootDBId) {
        MindView mindView = m_mindViews.get(rootDBId);
        if (mindView == null) {
            findOrAddMindView(rootDBId);
        }

        if (m_tabbedPane.getSelectedComponent() != mindView) {
            m_tabbedPane.setSelectedComponent(mindView);
        }
        return mindView;
    }

    public MindView getCurrentView() {
        return (MindView)m_tabbedPane.getSelectedComponent();
    }

    public Object getCurrentVertexId() {
        MindView currentView = getCurrentView();
        if (currentView == null) {
            return null;
        }

        Node node = currentView.getCursorSourceNode();
        return m_mindModel.getDBId(node);
    }

    private void updateMindViews(MindOperator operator, boolean isUndo)
    {
        //remove no needed mindview
        if (operator instanceof Removing) {
            Removing removing = (Removing)operator;
            Object trashedDBId = removing.m_removedDBId;

            for (Object rootDBId : m_mindViews.keySet()) {
                if (m_mindModel.getInheritDirection(trashedDBId, rootDBId) ==
                        MindDB.InheritDirection.LINEAL_DESCENDANT) {
                    removeMindView(rootDBId);
                }
            }
        }

        if (operator == null) {
            int i = 0;
        }

        MindView mindView = exposeMindView(operator.m_rootDBId);
        mindView.setCursorNodeByPath(isUndo ? operator.m_formerCursorPath : operator.m_laterCursorPath);

        //repaint remain mindviews
        for (Object rootDBId : m_mindViews.keySet()) {
            //FIXME: current cursor node is removed ? how ?
            m_mindViews.get(rootDBId).renderTree();
        }
    }

    public boolean addEdit(UndoableEdit edit) {
        MindOperator operator = (MindOperator)edit;
        operator.does();

        updateMindViews(operator, false);

        m_logger.info("m_formerCursorPath: " + operator.m_formerCursorPath.toString());
        m_logger.info("m_laterCursorPath: " + operator.m_laterCursorPath.toString());

        return super.addEdit(edit);
    }


    public void redo()
    {
        if (! canRedo())
            return;

        MindOperator operator = (MindOperator)editToBeRedone();
        super.redo();

        updateMindViews(operator, false);
    }

    public void undo()
    {
        if (! canUndo())
            return;

        MindOperator operator = (MindOperator)editToBeRedone();
        super.undo();

        updateMindViews(operator, true);
    }
}
