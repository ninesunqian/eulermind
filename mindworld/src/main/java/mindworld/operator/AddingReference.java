package mindworld.operator;

import mindworld.MindModel;
import mindworld.MindOperator;
import prefuse.data.Node;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-2-26
 * Time: 上午7:07
 * To change this template use File | Settings | File Templates.
 */
public class AddingReference extends MindOperator {
    Object m_referrerDBId;
    Object m_referentDBId;
    int m_pos;
    ArrayList<Integer> m_referrerNodePath;

    static Logger s_logger = LoggerFactory.getLogger(AddingReference.class);

    private void init(Node referrerNode, Object referrerDBId, Object referentDBId, int pos) {
        m_referrerDBId = referrerDBId;
        m_referentDBId = referentDBId;
        m_pos = pos;

        m_referrerNodePath = m_mindModel.getNodePath(referrerNode);
        s_logger.info(String.format("referrerDBId %s -- referentDBId %s, referrerNodePath %s",
                referrerDBId.toString(), referentDBId.toString(), m_referrerNodePath.toString()));
    }

    //formerCursor is referent: using for drag referent node to referrer node by mouse
    public AddingReference(MindModel mindModel, Node formerCursor, Node referrerNode, int pos) {
        super(mindModel, formerCursor);
        s_logger.info(String.format("formerCursor %s -- refererNode %s", m_mindModel.getNodeDebugInfo(formerCursor), m_mindModel.getNodeDebugInfo(referrerNode)));
        init(referrerNode, m_mindModel.getDBId(referrerNode), m_mindModel.getDBId(formerCursor), pos);
    }

    //formerCursor is referrer: using add referent node by edit prompter
    public AddingReference(MindModel mindModel, Node formerCursor, Object referentDBId, int pos) {
        super(mindModel, formerCursor);
        init(formerCursor, m_mindModel.getDBId(formerCursor), referentDBId, pos);
    }

    public void does() {
        m_mindModel.addReference(getNodeByPath(m_referrerNodePath), m_pos, m_referentDBId);

        m_laterCursorPath = (ArrayList<Integer>) m_referrerNodePath.clone();
        m_laterCursorPath.add(m_pos);
    }

    public void undo() {
        m_mindModel.removeReference(m_referrerDBId, m_pos);
    }

    public void redo() {
        m_mindModel.addReference(getNodeByPath(m_formerCursorPath), m_pos, m_referentDBId);
    }
}
