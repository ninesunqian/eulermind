package eulermind.operator;

import eulermind.MindModel;
import eulermind.MindOperator;
import eulermind.view.NodeControl;
import prefuse.data.Edge;
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


public class DraggingNode extends MindOperator{

    Node m_droppedNode;
    NodeControl.HitPosition m_hitPosition;

    int m_newPos;

    Object m_newParentDbId;

    ArrayList<Integer> m_oldParentOrReferrerPath;
    ArrayList<Integer> m_newParentOrReferrerPath;

    ArrayList<Integer> m_oldParentOrReferrerPathAfterDoing;
    ArrayList<Integer> m_newParentOrReferrerPathAfterDoing;


    public DraggingNode(MindModel mindModel, Node formerCursor, Node droppedNode, NodeControl.HitPosition hitPosition)
    {
        super(mindModel, formerCursor);
        m_droppedNode = droppedNode;
        m_hitPosition = hitPosition;
    }

    public boolean does() {
        if (! prepareCursorInfo()) {
            return false;
        }

        if (!m_droppedNode.isValid()) {
            return false;
        }

        if (m_formerCursorParent == null) {
            return false;
        }

        Node newParent = null;
        if (m_hitPosition == NodeControl.HitPosition.BOTTOM || m_hitPosition == NodeControl.HitPosition.TOP) {
            newParent = m_droppedNode.getParent();
        } else {
            newParent = m_droppedNode;
        }

        if (newParent == null) {
            return false;
        }

        if (newParent.isDescendantOf(m_formerCursor)) {
            m_logger.info("canDo false: display Tree: new parent is descentant of cursor");
            return false;
        }

        m_newParentDbId = MindModel.getDbId(newParent);

        if (!m_isRefNode) {
            if (m_mindModel.m_mindDb.vertexIdIsDescendantOf(m_newParentDbId, m_formerCursorId)) {
                m_logger.info("canDo false: DB Tree: new parent is descentant of cursor");
                return false;
            }
        }

        if (m_hitPosition == NodeControl.HitPosition.TOP) {
            m_newPos = m_droppedNode.getIndex();
        } else if (m_hitPosition == NodeControl.HitPosition.BOTTOM) {
            m_newPos = m_droppedNode.getIndex() + 1;
        } else {
            m_newPos = newParent.getChildCount();
        }

        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_formerCursorParentId, m_newParentDbId)) {

            if (m_formerCursorPos < m_newPos) {
                m_newPos--;
            }

            if (m_formerCursorPos == m_newPos) {
                return false;
            }
        }

        m_oldParentOrReferrerPath = getNodePath(m_formerCursorParent);
        m_newParentOrReferrerPath = getNodePath(newParent);

        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_formerCursorParentId, m_newParentDbId)) {
            changePosition(m_oldParentOrReferrerPath, m_formerCursorPos, m_newPos);
        } else {
            handoverRelation(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerPath, m_newPos);
        }

        m_oldParentOrReferrerPathAfterDoing = getNodePath(m_formerCursorParent);
        m_newParentOrReferrerPathAfterDoing = getNodePath(newParent);
        m_laterCursorPath = (ArrayList)m_newParentOrReferrerPathAfterDoing.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");

        return true;
    }

    public void undo()
    {
        m_logger.info("arg:");
        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_formerCursorParentId, m_newParentDbId)) {
            changePosition(m_newParentOrReferrerPathAfterDoing, m_newPos, m_formerCursorPos);

        } else {
            handoverRelation(m_newParentOrReferrerPathAfterDoing, m_newPos,
                    m_oldParentOrReferrerPathAfterDoing, m_formerCursorPos);

        }

        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg:");
        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_formerCursorParentId, m_newParentDbId)) {
            changePosition(m_oldParentOrReferrerPath, m_formerCursorPos, m_newPos);

        } else {
            handoverRelation(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerPath, m_newPos);
        }
    }

    private void changePosition(ArrayList<Integer> parentPath, int oldPos, int newPos)
    {
        m_logger.info("arg: {}:{}", "parentPath", parentPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        if (oldPos == newPos) {
            return;
        }

        Node parentNode = getNodeByPath(parentPath);
        Edge childEdge = parentNode.getChildEdge(oldPos);
        m_mindModel.changeChildPos(m_mindModel.getDbId(childEdge), newPos);

        m_logger.info("ret:");
    }


    private void handoverRelation(ArrayList<Integer> oldSourcePath, int oldPos, ArrayList<Integer> newSourcePath, int newPos)
    {
        m_logger.info("arg: {}:{}", "oldSourcePath", oldSourcePath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newSourcePath", newSourcePath);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        Tree tree = m_mindModel.findTree(m_rootDbId);
        Node oldSourceNode = m_mindModel.getNodeByPath(tree, oldSourcePath);
        Node newSourceNode = m_mindModel.getNodeByPath(tree, newSourcePath);

        Node target = oldSourceNode.getChild(oldPos);

        Object oldSourceDbId = MindModel.getDbId(oldSourceNode);
        Object newSourceDbId = MindModel.getDbId(newSourceNode);
        Object targetDbId = MindModel.getDbId(target);

        assert m_isRefNode == m_mindModel.isRefNode(target);

        assert ! m_mindModel.m_mindDb.vertexIdIsSelf(oldSourceDbId, newSourceDbId);

        if (! m_isRefNode) {
            assert ! m_mindModel.m_mindDb.vertexIdIsInSubTreeOf(newSourceDbId, targetDbId);
        }

        m_mindModel.handoverRelation(oldSourceDbId, oldPos, oldSourceNode, newSourceDbId, newPos, newSourceNode);

        m_logger.info("ret:");
    }
}
