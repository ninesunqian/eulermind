package excitedmind.operator;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-3-3
 * Time: 下午9:25
 * To change this template use File | Settings | File Templates.
 */
public class RemovingReference {
    //node path is the referer node
    RemovingReference(Stack<Integer> nodePath, Object refereeDBId, int pos)
    {
        super(nodePath);
        m_refereeDBId = refereeDBId;
        m_pos = pos;
    }
    public void undo()
    {
        Node cursorNode = getCursorNode();
        setCursorByPath(m_nodePath);
        m_mindTree.addReference(m_mindTree.getDBId(cursorNode), m_pos, m_refereeDBId);

    }
    public void redo()
    {
        setCursorByPath(m_nodePath);
        m_mindTree.removeReference(m_refereeDBId, m_pos);
    }

    Object m_refereeDBId;
    int m_pos;
}
