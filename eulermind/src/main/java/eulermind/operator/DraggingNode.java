package eulermind.operator;

import com.tinkerpop.blueprints.Vertex;
import eulermind.MindDB;
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

    ArrayList<Integer> m_oldParentOrReferrerPath;
    ArrayList<Integer> m_newParentOrReferrerPath;

    ArrayList<Integer> m_oldParentOrReferrerPathAfterDoing;
    ArrayList<Integer> m_newParentOrReferrerPathAfterDoing;

    boolean m_fromSameView;

    Object m_oldSourceDbId;
    Object m_newSourceDbId;
    Object m_targetDbId;
    int m_oldPos;
    Object m_oldEdgeDbId;

    //跨view拖动多个节点时，拖至上一个操作的节点下方。就是上一个被拖动的节点
    Object m_previousVertexDbId;

    public DraggingNode(MindModel mindModel, Node formerCursor, Node droppedNode, NodeControl.HitPosition hitPosition)
    {
        super(mindModel, formerCursor);
        m_droppedNode = droppedNode;
        m_hitPosition = hitPosition;
        m_fromSameView = true;
    }

    //由其他view拖动至此
    //previousVertexDbId是上一次drag操作设计的db节点，如果同时拖动多个节点，当前节点要放在previousVertexDbId的下方，作为它的弟弟
    public DraggingNode(MindModel mindModel, Node droppedNode, Object edgeDbId, NodeControl.HitPosition hitPosition,
                        Object previousVertexDbId)
    {
        super(mindModel, droppedNode);
        m_droppedNode = droppedNode;
        m_hitPosition = hitPosition;
        m_fromSameView = false;
        m_previousVertexDbId = previousVertexDbId;

        MindDB mindDB = m_mindModel.m_mindDb;

        m_oldEdgeDbId = edgeDbId;
        com.tinkerpop.blueprints.Edge edge = mindDB.getEdge(edgeDbId);
        m_isRefNode = mindDB.getEdgeType(edge) == MindDB.EdgeType.REFERENCE;

        Vertex oldSource = mindDB.getEdgeSource(edge);
        m_oldSourceDbId = oldSource.getId();

        Vertex target = mindDB.getEdgeTarget(edge);
        m_targetDbId = target.getId();

    }


    public boolean does() {
        if (! prepareCursorInfo()) {
            return false;
        }

        MindDB mindDB = m_mindModel.m_mindDb;

        if (m_fromSameView) {
            //不能拖动子树的根节点
            if (m_formerCursorParent == null) {
                return false;
            }

            m_oldSourceDbId = m_formerCursorParentId;
            m_targetDbId = m_formerCursorId;
            m_oldPos = m_formerCursorPos;

            Edge oldEdge = m_formerCursor.getParentEdge();
            m_oldEdgeDbId = m_mindModel.getDbId(oldEdge);

        } else {
            Vertex oldSource = mindDB.getVertex(m_oldSourceDbId);
            m_oldPos = m_mindModel.m_mindDb.getEdgeIndex(mindDB.getOutEdgeVertexIds(oldSource), m_oldEdgeDbId);
        }

        if (!m_droppedNode.isValid()) {
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

        if (m_fromSameView) {
            if (newParent.isDescendantOf(m_formerCursor)) {
                m_logger.info("canDo false: display Tree: new parent is desentant of cursor");
                return false;
            }
        }

        m_newSourceDbId = MindModel.getDbId(newParent);

        if (!m_isRefNode) {
            if (m_mindModel.m_mindDb.vertexIdIsInSubTreeOf(m_newSourceDbId, m_targetDbId)) {
                m_logger.info("canDo false: DB Tree: new parent is in subTree new child");
                return false;
            }
        }

        if (m_previousVertexDbId == null) {
            if (m_hitPosition == NodeControl.HitPosition.TOP) {
                m_newPos = m_droppedNode.getIndex();
            } else if (m_hitPosition == NodeControl.HitPosition.BOTTOM) {
                m_newPos = m_droppedNode.getIndex() + 1;
            } else {
                m_newPos = newParent.getChildCount();
            }

        } else {
            for (int i=newParent.getChildCount()-1; i>=0; i--) {
                Node childNode = newParent.getChild(i);
                if (MindModel.getDbId(childNode).equals(m_previousVertexDbId)) {
                    m_newPos = i + 1;
                    break;
                }
            }
        }

        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_oldSourceDbId, m_newSourceDbId)) {

            if (m_oldPos < m_newPos) {
                m_newPos--;
            }

            if (m_oldPos == m_newPos) {
                return false;
            }
        }

        if (m_fromSameView) {
            m_oldParentOrReferrerPath = getNodePath(m_formerCursorParent);
        }

        m_newParentOrReferrerPath = getNodePath(newParent);

        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_oldSourceDbId, m_newSourceDbId)) {
            m_mindModel.changeChildPos(m_oldEdgeDbId, m_newPos);
        } else {
            if (m_fromSameView) {
                handoverRelation(m_oldSourceDbId, m_oldPos, m_oldParentOrReferrerPath,
                        m_newSourceDbId, m_newPos, m_newParentOrReferrerPath);
            } else {
                handoverRelation(m_oldSourceDbId, m_oldPos, null,
                        m_newSourceDbId, m_newPos, null);

            }
        }

        if (m_fromSameView) {
            m_oldParentOrReferrerPathAfterDoing = getNodePath(m_formerCursorParent);
        }

        m_newParentOrReferrerPathAfterDoing = getNodePath(newParent);
        m_laterCursorPath = (ArrayList)m_newParentOrReferrerPathAfterDoing.clone();
        m_laterCursorPath.add(m_newPos);

        m_logger.info("ret:");

        return true;
    }

    public void undo()
    {
        m_logger.info("arg:");
        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_oldSourceDbId, m_newSourceDbId)) {
            m_mindModel.changeChildPos(m_oldEdgeDbId, m_oldPos);
        } else {
            if (m_fromSameView) {
                handoverRelation(m_newSourceDbId, m_newPos, m_newParentOrReferrerPathAfterDoing,
                        m_oldSourceDbId, m_oldPos, m_oldParentOrReferrerPathAfterDoing);
            } else {
                handoverRelation(m_newSourceDbId, m_newPos, null,
                        m_oldSourceDbId, m_oldPos, null);
            }
        }

        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg:");
        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_oldSourceDbId, m_newSourceDbId)) {
            m_mindModel.changeChildPos(m_oldEdgeDbId, m_newPos);
        } else {
            if (m_fromSameView) {
                handoverRelation(m_oldSourceDbId, m_oldPos, m_oldParentOrReferrerPath,
                        m_newSourceDbId, m_newPos, m_newParentOrReferrerPath);
            } else {
                handoverRelation(m_oldSourceDbId, m_oldPos, null,
                        m_newSourceDbId, m_newPos, null);

            }
        }
        m_logger.info("ret:");
    }

    private void handoverRelation(Object oldSourceDbId, int oldPos, ArrayList<Integer> oldSourcePath,
                                  Object newSourceDbId, int newPos, ArrayList<Integer> newSourcePath)
    {
        m_logger.info("arg: {}:{}", "oldSourceDbId", oldSourceDbId);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "oldSourcePath", oldSourcePath);
        m_logger.info("arg: {}:{}", "newSourceDbId", newSourceDbId);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newPos", newPos);
        m_logger.info("arg: {}:{}", "newSourcePath", newSourcePath);

        Node oldSourceNode = null;
        Node newSourceNode = null;

        if (m_fromSameView) {
            Tree tree = m_mindModel.findTree(m_rootDbId);
            oldSourceNode = m_mindModel.getNodeByPath(tree, oldSourcePath);
            newSourceNode = m_mindModel.getNodeByPath(tree, newSourcePath);
        }

        assert ! m_mindModel.m_mindDb.vertexIdIsSelf(oldSourceDbId, newSourceDbId);

        m_mindModel.handoverRelation(oldSourceDbId, oldPos, oldSourceNode, newSourceDbId, newPos, newSourceNode);

        m_logger.info("ret:");
    }
}
