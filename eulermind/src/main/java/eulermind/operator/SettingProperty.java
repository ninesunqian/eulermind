package eulermind.operator;

import eulermind.MindModel;
import eulermind.MindOperator;
import prefuse.data.Node;

import java.util.ArrayList;

/*
The MIT License (MIT)
Copyright (c) 2012-2014 wangxuguang ninesunqian@163.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

public class SettingProperty extends MindOperator {
    Object m_nodeDBId;
    String m_property;
    Object m_oldValue;
    Object m_newValue;

    public SettingProperty(MindModel mindModel, Node formerCursor, String property, Object newValue) {
        super(mindModel, formerCursor);
        m_nodeDBId = m_mindModel.getDbId(formerCursor);
        m_property = property;
        m_newValue = newValue;
        m_oldValue = m_mindModel.getProperty(m_nodeDBId, m_property);
    }

    public boolean does()
    {
        if (! prepareCursorInfo()) {
            return false;
        }

        m_mindModel.setProperty(m_nodeDBId, m_property, m_newValue);
        m_laterCursorPath = (ArrayList<Integer>) m_formerCursorPath.clone();
        return true;
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
