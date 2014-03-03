package excitedmind.operator;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-3-3
 * Time: 下午9:27
 * To change this template use File | Settings | File Templates.
 */
public class SettingProptery {
    {
        SetPropertyUndoer (Stack<Integer> nodePath, String property, Object newValue, Object oldValue)
        {
            super(nodePath);
            m_property = property;
            m_oldValue = oldValue;
            m_newValue = newValue;
            m_logger.info("nodePath="+ nodePath + ",   newValue="+m_newValue + ",   oldValue="+m_oldValue);
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
