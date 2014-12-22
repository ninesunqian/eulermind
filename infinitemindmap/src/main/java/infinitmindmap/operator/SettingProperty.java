package infinitmindmap.operator;

import infinitmindmap.MindModel;
import infinitmindmap.MindOperator;
import prefuse.data.Node;

import java.util.ArrayList;

public class SettingProperty extends MindOperator {
    Object m_nodeDBId;
    String m_property;
    Object m_oldValue;
    Object m_newValue;

    public SettingProperty(MindModel mindModel, Node formerCursor, String property, Object newValue) {
        super(mindModel, formerCursor);
        m_nodeDBId = m_mindModel.getDBId(formerCursor);
        m_property = property;
        m_newValue = newValue;
        m_oldValue = m_mindModel.getProperty(m_nodeDBId, m_property);
        m_laterCursorPath = (ArrayList<Integer>) m_formerCursorPath.clone();
    }

    public void does()
    {
        m_mindModel.setProperty(m_nodeDBId, m_property, m_newValue);
    }

    public void undo ()
    {
        m_mindModel.setProperty(m_nodeDBId, m_property, m_oldValue);
    }

    public void redo ()
    {
        does();
    }
}
