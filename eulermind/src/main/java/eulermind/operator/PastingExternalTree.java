package eulermind.operator;

import eulermind.MindModel;
import eulermind.MindOperator;
import prefuse.data.Node;
import prefuse.data.Tree;

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

public class PastingExternalTree extends MindOperator {
    ArrayList<Integer> m_parentPath;
    int m_pos;
    Tree m_externalTree;
    Object m_newSubTreeRootDbId;

    ArrayList<Integer> m_parentPathAfterDoing;
    public PastingExternalTree(MindModel mindModel, Node formerCursor, Tree externalTree)
    {
        super(mindModel, formerCursor);
        this.m_pos = formerCursor.getChildCount();
        m_externalTree = externalTree;
    }

    public boolean does() {
        if (! prepareCursorInfo()) {
            return false;
        }
        Node parent = getNodeByPath(m_formerCursorPath);
        m_parentPath = (ArrayList)m_formerCursorPath.clone();

        Node newSubTreeRoot = m_mindModel.pasteTree(parent, m_pos, m_externalTree);
        m_newSubTreeRootDbId = m_mindModel.getDbId(newSubTreeRoot);

        //重新取parent的路径
        m_parentPathAfterDoing = getNodePath(parent);
        m_laterCursorPath = (ArrayList)m_parentPathAfterDoing.clone();
        m_laterCursorPath.add(m_pos);
        return true;
    }

    public void undo() {
        m_mindModel.trashNode(m_newSubTreeRootDbId);
    }

    public void redo() {
        m_mindModel.restoreNodeFromTrash(m_newSubTreeRootDbId);
    }
}
