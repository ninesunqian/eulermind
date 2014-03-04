package excitedmind.operator;

import excitedmind.MindModel;
import excitedmind.MindOperator;
import prefuse.data.Node;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-3-3
 * Time: 下午9:27
 * To change this template use File | Settings | File Templates.
 */
public class SettingProptery extends MindOperator {
    Object m_nodeDBId;
    String m_property;
    Object m_oldValue;
    Object m_newValue;

    public SettingProptery(MindModel mindModel, Node formerCursor, String property, Object newValue, Object oldValue) {
        super(mindModel, formerCursor);
        m_nodeDBId = m_mindModel.getDBId(formerCursor);
        m_property = property;
        m_newValue = newValue;
        m_oldValue = m_mindModel.getNodeP
    }

    public void doing()
    {
        m_mindModel.setProperty(m_nodeDBId, m_property, m_newValue);
    }

    public void undo ()
    {
        setCursorByPath(m_nodePath);
        setCursorProperty(m_property, m_oldValue);
        m_logger.info("nodePath="+ m_nodePath + ",   DBId="+m_mindTree.getDBId(getCursorNode()) + ",   oldValue="+m_oldValue);
    }

    public void redo ()
    {
        setCursorByPath(m_nodePath);
        setCursorProperty(m_property, m_newValue);
        System.out.print("nodePath="+ m_nodePath + ",   DBId="+m_mindTree.getDBId(getCursorNode()) + ",   newValue="+m_newValue);
    }

    final String m_property;
    final Object m_oldValue;
    final Object m_newValue;
}

    private Object setCursorProperty (String property, Object value)
    {
        Node cursorNode = getCursorNode();
        Object oldValue = cursorNode.get(property);
        m_mindTree.setNodeProperty(m_mindTree.getDBId(cursorNode), property, value);
        return oldValue;
    }

    private AbstractUndoableEdit setCursorPropertyUndoable(String property, Object value)
    {
        Node cursorNode = getCursorNode();
        Object oldValue = setCursorProperty(property, value);
        return new SetPropertyUndoer(getNodePath(cursorNode), property, value, oldValue);
    }
}
