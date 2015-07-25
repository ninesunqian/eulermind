package eulermind;

import prefuse.data.Edge;
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
    protected Logger m_logger;
    protected MindModel m_mindModel;

    //用于定位在哪个Tree上触发的操作
    protected Object m_rootDbId;

    //用户操作相关的节点，在多选情况下。生成Operator的时候，与执行Operator的时候树的结构不同。所以要记下Node
    protected Tree m_tree;
    protected Node m_formerCursor;
    protected Node m_formerCursorParent;
    protected boolean m_isRefNode;

    //当undo, redo的时候，m_formerCursorParent, m_formerCursor 可能已经失效。保留下dbId
    protected Object m_formerCursorId;
    protected Object m_formerCursorParentId;

    //当前光标路径, 执行does的时候才能确定
    //用于redo时,定位光标
    protected ArrayList<Integer> m_formerCursorPath;

    //执行操作之后的路径，does执行之后的光标路径。需要先计算下一个光标node，然后就算出路径
    //用于undo时,定位光标
    protected ArrayList<Integer> m_laterCursorPath;

    protected int m_formerCursorPos;

    //同时操作多个节点的时候，表示第一个操作符。undo的时候也是回退到这里
    public boolean m_firstInGroup = false;

    public MindOperator(MindModel mindModel, Node formerCursor) {
        m_logger = LoggerFactory.getLogger(this.getClass());

        m_tree = (Tree)formerCursor.getGraph();
        m_mindModel = mindModel;
        m_rootDbId = m_mindModel.getDbId(m_tree.getRoot());

        m_formerCursor = formerCursor;
        m_formerCursorId = MindModel.getDbId(m_formerCursor);

        //多选操作的。当轮到某个选集中的节点时，它的父节点或许早已变化。所以把获取父节点的操作放在preDoes里。
        Edge parentEdge = m_formerCursor.getParentEdge();
        m_isRefNode = parentEdge == null ? false : m_mindModel.isRefEdge(parentEdge);
    }

    //子类的does方法中，先调用这个函数，记录下光标节点的信息
    protected boolean prepareCursorInfo() {
        if (!m_formerCursor.isValid()) {
            return false;
        }

        m_formerCursorParent = m_formerCursor.getParent();

        if (m_formerCursorParent != null) {
            m_formerCursorParentId = MindModel.getDbId(m_formerCursorParent);
            m_formerCursorPos = m_formerCursor.getIndex();
            m_isRefNode = m_mindModel.isRefEdge(m_formerCursor.getParentEdge());
        } else {
            m_formerCursorParentId = null;
            m_formerCursorPos = -1;
            m_isRefNode = false;
        }

        m_formerCursorPath = m_mindModel.getNodePath(m_formerCursor);
        m_formerCursorPos = m_formerCursor.getIndex();
        return true;
    }

    protected void saveLaterCursorInfo(Node laterCursor) {
        m_laterCursorPath = m_mindModel.getNodePath(laterCursor);
    }

    protected Node getNodeByPath(ArrayList<Integer> path) {
        Tree tree = m_mindModel.findTree(m_rootDbId);
        Node node = m_mindModel.getNodeByPath(tree, path);
        return node;
    }

    protected ArrayList<Integer> getNodePath(Node node) {
        return m_mindModel.getNodePath(node);
    }

    //如果返回true，表示成功执行
    //如果返回false，表示条件不足，没有执行
    abstract public boolean does() throws Exception;

    public boolean canRedo() {
        return true;
    }

    public boolean canUndo() {
        return true;
    }
}
