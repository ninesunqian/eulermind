package eulermind;

import prefuse.data.Node;
import prefuse.data.Tree;

import javax.swing.undo.AbstractUndoableEdit;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public abstract class MindOperator extends AbstractUndoableEdit {
    protected MindModel m_mindModel;
    protected Object m_rootDbId;
    protected ArrayList<Integer> m_formerCursorPath;
    protected ArrayList<Integer> m_laterCursorPath;

    protected Logger m_logger;

    public MindOperator(MindModel mindModel, Node formerCursor) {
        m_logger = LoggerFactory.getLogger(this.getClass());

        Tree tree = (Tree)formerCursor.getGraph();
        m_mindModel = mindModel;
        m_rootDbId = m_mindModel.getDbId(tree.getRoot());
        m_formerCursorPath = m_mindModel.getNodePath(formerCursor);
    }

    protected Node getNodeByPath(ArrayList<Integer> path) {
        Tree tree = m_mindModel.findTree(m_rootDbId);
        Node node = m_mindModel.getNodeByPath(tree, path);
        return node;
    }

    protected ArrayList<Integer> getNodePath(Node node) {
        return m_mindModel.getNodePath(node);
    }

    public boolean canRedo() {
        return true;
    }

    public boolean canUndo() {
        return true;
    }

    abstract public void does() throws Exception;

}
