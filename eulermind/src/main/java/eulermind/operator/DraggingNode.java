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

    public DraggingNode(MindModel mindModel, Node formerCursor, Node droppedNode, NodeControl.HitPosition hitPosition)
    {
        super(mindModel, formerCursor);
        m_droppedNode = droppedNode;
        m_hitPosition = hitPosition;
        m_fromSameView = true;
    }

    //由其他view拖动至此
    public DraggingNode(MindModel mindModel, Node droppedNode, Object edgeDbId, NodeControl.HitPosition hitPosition)
    {
        super(mindModel, droppedNode);
        m_droppedNode = droppedNode;
        m_hitPosition = hitPosition;
        m_fromSameView = false;

        MindDB mindDB = m_mindModel.m_mindDb;

        com.tinkerpop.blueprints.Edge edge = mindDB.getEdge(edgeDbId);
        m_isRefNode = mindDB.getEdgeType(edge) == MindDB.EdgeType.REFERENCE;

        Vertex oldSource = mindDB.getEdgeSource(edge);
        m_oldSourceDbId = oldSource.getId();

        Vertex target = mindDB.getEdgeTarget(edge);
        m_targetDbId = target.getId();

        m_oldPos = mindDB.getEdgeIndex(mindDB.getOutEdgeVertexIds(oldSource), edgeDbId);
        m_oldEdgeDbId = edgeDbId;
    }

    //跨view拖动多个节点时，拖至上一个操作的节点下方。
    //FIXME: 当droppedNode有多个指向previousVertexDbId引用的时候，总是找到第一个。但不会造成节点关系错误。
    public DraggingNode(MindModel mindModel, Node droppedNode, Object edgeDbId, Object previousVertexDbId)
    {
        super(mindModel, droppedNode);

        for (int i=0; i<droppedNode.getChildCount(); i++) {
            Node childNode = droppedNode.getChild(i);
            if (MindModel.getDbId(childNode).equals(previousVertexDbId)) {
                m_droppedNode = childNode;
                m_hitPosition = NodeControl.HitPosition.BOTTOM;
                break;
            }
        }
        assert m_droppedNode != null;

        m_fromSameView = false;

        MindDB mindDB = m_mindModel.m_mindDb;

        com.tinkerpop.blueprints.Edge edge = mindDB.getEdge(edgeDbId);
        m_isRefNode = mindDB.getEdgeType(edge) == MindDB.EdgeType.REFERENCE;

        Vertex oldSource = mindDB.getEdgeSource(edge);
        m_oldSourceDbId = oldSource.getId();

        Vertex target = mindDB.getEdgeTarget(edge);
        m_targetDbId = target.getId();

        m_oldPos = mindDB.getEdgeIndex(mindDB.getOutEdgeVertexIds(oldSource), edgeDbId);
        m_oldEdgeDbId = edgeDbId;
    }

    public boolean does() {
        if (! prepareCursorInfo()) {
            return false;
        }

        if (m_fromSameView) {
            m_oldSourceDbId = m_formerCursorParentId;
            m_targetDbId = m_formerCursorId;
            m_oldPos = m_formerCursorPos;

            Edge oldEdge = m_formerCursor.getParentEdge();
            m_oldEdgeDbId = m_mindModel.getDbId(oldEdge);
        }

        if (!m_droppedNode.isValid()) {
            return false;
        }

        //不能拖动子树的根节点
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

        if (m_hitPosition == NodeControl.HitPosition.TOP) {
            m_newPos = m_droppedNode.getIndex();
        } else if (m_hitPosition == NodeControl.HitPosition.BOTTOM) {
            m_newPos = m_droppedNode.getIndex() + 1;
        } else {
            m_newPos = newParent.getChildCount();
        }

        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_oldSourceDbId, m_newSourceDbId)) {

            if (m_oldPos < m_newPos) {
                m_newPos--;
            }

            if (m_oldPos == m_newPos) {
                return false;
            }
        }

        m_oldParentOrReferrerPath = getNodePath(m_formerCursorParent);
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
        if (m_mindModel.m_mindDb.vertexIdIsSelf(m_oldSourceDbId, m_newSourceDbId)) {
            m_mindModel.changeChildPos(m_oldEdgeDbId, m_oldPos);
        } else {
            if (m_fromSameView) {
                handoverRelation(m_newSourceDbId, m_newPos, m_newParentOrReferrerPath,
                        m_oldSourceDbId, m_oldPos, m_oldParentOrReferrerPath);
            } else {
                handoverRelation(m_oldSourceDbId, m_oldPos, null,
                        m_newSourceDbId, m_newPos, null);
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
