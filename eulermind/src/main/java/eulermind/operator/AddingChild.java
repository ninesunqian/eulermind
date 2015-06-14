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

public class AddingChild extends MindOperator{

    ArrayList<Integer> m_parentPath;
    int pos;
    String m_text;
    Object m_childDbId;

    ArrayList<Integer> m_parentPathAfterDoing;

    public AddingChild(MindModel mindModel, Node formerCursor, int pos, String text) {
        super(mindModel, formerCursor);
        this.pos = pos;
        m_text = text;
    }

    public boolean does() {
        prepareCursorInfo();

        Node parent = getNodeByPath(m_formerCursorPath);
        m_parentPath = (ArrayList)m_formerCursorPath.clone();

        Node child = m_mindModel.addChild(parent, pos, m_text);
        m_childDbId = m_mindModel.getDbId(child);

        //重新取parent的路径
        m_parentPathAfterDoing = getNodePath(parent);
        m_laterCursorPath = (ArrayList)m_parentPathAfterDoing.clone();
        m_laterCursorPath.add(pos);
        return true;
    }

    public void undo() {
        m_mindModel.trashNode(m_childDbId);
    }

    public void redo() {
        m_mindModel.restoreNodeFromTrash(m_childDbId);
    }
}
