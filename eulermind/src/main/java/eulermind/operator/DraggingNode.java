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

        if (m_mindModel.m_mindDb.vertexIdIsDescendantOf(m_newParentDbId, m_formerCursorId)) {
            m_logger.info("canDo false: DB Tree: new parent is descentant of cursor");
            return false;
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
            if (m_isRefNode) {
                handoverReferent(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerPath, m_newPos);
            } else {
                handoverChild(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerPath, m_newPos);
            }
        }

        m_oldParentOrReferrerPathAfterDoing = getNodePath(m_formerCursorParent);
        m_newParentOrReferrerPathAfterDoing = getNodePath(newParent);
        m_laterCursorPath = (ArrayList)m_newParentOrReferrerPath.clone();
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
            if (m_isRefNode) {
                handoverReferent(m_newParentOrReferrerPathAfterDoing, m_newPos,
                        m_oldParentOrReferrerPathAfterDoing, m_formerCursorPos);

            } else {
                handoverChild(m_newParentOrReferrerPathAfterDoing, m_newPos,
                        m_oldParentOrReferrerPathAfterDoing, m_formerCursorPos);
            }
        }

        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg:");
        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_formerCursorParentId, m_newParentDbId)) {
            changePosition(m_oldParentOrReferrerPath, m_formerCursorPos, m_newPos);

        } else {
            if (m_isRefNode) {
                handoverReferent(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerPath, m_newPos);

            } else {
                handoverChild(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerPath, m_newPos);
            }
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


    private void handoverReferent(ArrayList<Integer> oldReferrerPath, int oldPos, ArrayList<Integer> newReferrerPath, int newPos)
    {
        m_logger.info("arg: {}:{}", "oldReferrerPath", oldReferrerPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newReferrerPath", newReferrerPath);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        Tree tree = m_mindModel.findTree(m_rootDbId);
        Node oldReferrerNode = m_mindModel.getNodeByPath(tree, oldReferrerPath);
        Node newReferrerNode = m_mindModel.getNodeByPath(tree, newReferrerPath);

        Node child = oldReferrerNode.getChild(oldPos);

        assert !MindModel.getDbId(oldReferrerNode).equals(MindModel.getDbId(newReferrerNode));
        assert m_mindModel.isRefNode(child);

        m_mindModel.handoverReferent(oldReferrerNode, oldPos, newReferrerNode, newPos);

        m_logger.info("ret:");
    }

    private void handoverChild(ArrayList<Integer> oldParentPath, int oldPos, ArrayList<Integer> newParentPath, int newPos)
    {
        m_logger.info("arg: {}:{}", "oldParentPath", oldParentPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newParentPath", newParentPath);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        Tree tree = m_mindModel.findTree(m_rootDbId);
        Node oldParentNode = m_mindModel.getNodeByPath(tree, oldParentPath);
        Node newParentNode = m_mindModel.getNodeByPath(tree, newParentPath);

        Node child = oldParentNode.getChild(oldPos);

        assert ! m_mindModel.isSelfInDB(child, newParentNode) && ! m_mindModel.isAncestorOfInDB(child, newParentNode);

        assert !MindModel.getDbId(oldParentNode).equals(MindModel.getDbId(newParentNode));
        assert !m_mindModel.isRefNode(child);

        m_mindModel.handoverChild(oldParentNode, oldPos, newParentNode, newPos);

        m_logger.info("ret:");
    }
}
