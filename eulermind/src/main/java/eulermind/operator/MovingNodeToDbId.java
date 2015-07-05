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


public class MovingNodeToDbId extends MindOperator{

    int m_newPos;

    Object m_newParentDbId;

    ArrayList<Integer> m_oldParentOrReferrerPath;
    ArrayList<Integer> m_oldParentOrReferrerPathAfterDoing;


    //当前节点移动移动到一个MindView没有显示的节点中
    public MovingNodeToDbId(MindModel mindModel, Node formerCursor, Object newParentDbId)
    {
        super(mindModel, formerCursor);
        m_newParentDbId = newParentDbId;
    }

    public boolean does() {
        /*
        if (! prepareCursorInfo()) {
            return false;
        }

        if (! m_isRefNode) {
            if (m_mindModel.m_mindDb.vertexIdIsDescendantOf(m_newParentDbId, m_formerCursorId)) {
                m_logger.info("canDo false: DB Tree: new parent is descentant of cursor");
                return false;
            }
        }

        m_newPos = m_mindModel.m_mindDb.getChildOrReferentCount(m_mindModel.m_mindDb.getVertex(m_newParentDbId));

        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_formerCursorParentId, m_newParentDbId)) {
            return false;
        }

        if (m_formerCursorParent != null) {
            m_oldParentOrReferrerPath = getNodePath(m_formerCursorParent);
        }

            if (m_isRefNode) {
                handoverReferent(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerDbId, m_newPos);
            } else {
                handoverChild(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerDbId, m_newPos);
            }
        }

        m_oldParentOrReferrerPathAfterDoing = getNodePath(m_formerCursorParent);
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
        */

        return true;
    }

    public void undo()
    {
        /*
        m_logger.info("arg:");
            if (m_isRefNode) {
                handoverReferent(m_newParentOrReferrerPathAfterDoing, m_newPos,
                        m_oldParentOrReferrerPathAfterDoing, m_formerCursorPos);

            } else {
                handoverChild(m_newParentOrReferrerPathAfterDoing, m_newPos,
                        m_oldParentOrReferrerPathAfterDoing, m_formerCursorPos);
            }

        m_logger.info("ret:");
        */
    }

    public void redo()
    {
        /*
        m_logger.info("arg:");
            if (m_isRefNode) {
                handoverReferent(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerPath, m_newPos);

            } else {
                handoverChild(m_oldParentOrReferrerPath, m_formerCursorPos, m_newParentOrReferrerPath, m_newPos);
            }
            */

    }

    private void handoverReferent(ArrayList<Integer> oldReferrerPath, int oldPos, Object newReferrerDbId)
    {
        /*
        m_logger.info("arg: {}:{}", "oldReferrerPath", oldReferrerPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newReferrerDbId", newReferrerDbId);

        Tree tree = m_mindModel.findTree(m_rootDbId);
        Node oldReferrerNode = m_mindModel.getNodeByPath(tree, oldReferrerPath);

        Node child = oldReferrerNode.getChild(oldPos);

        assert !MindModel.getDbId(oldReferrerNode).equals(newReferrerDbId);
        assert m_mindModel.isRefNode(child);

        int newPos = m_mindModel.m_mindDb.getChildOrReferentCount(m_mindModel.m_mindDb.getVertex(newReferrerDbId));

        m_mindModel.handoverReferent(oldReferrerNode, oldPos, newReferrerDbId, newPos, null);

        m_logger.info("ret:");
        */
    }

    private void handoverChild(ArrayList<Integer> oldParentPath, int oldPos, Object newParentDbId)
    {
        /*
        m_logger.info("arg: {}:{}", "oldParentPath", oldParentPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newParentDbId", newParentDbId);

        Tree tree = m_mindModel.findTree(m_rootDbId);
        Node oldParentNode = m_mindModel.getNodeByPath(tree, oldParentPath);

        Node child = oldParentNode.getChild(oldPos);

        assert ! m_mindModel.isSelfInDB(child, newParentNode) && ! m_mindModel.isAncestorOfInDB(child, newParentNode);

        assert !MindModel.getDbId(oldParentNode).equals(MindModel.getDbId(newParentNode));
        assert !m_mindModel.isRefNode(child);

        m_mindModel.handoverRelation(oldParentNode, oldPos, newParentNode, newPos);

        m_logger.info("ret:");
        */
    }
}
