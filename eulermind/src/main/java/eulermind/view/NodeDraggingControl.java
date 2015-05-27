package eulermind.view;

import eulermind.MindModel;
import eulermind.MindOperator;
import eulermind.operator.AddingReference;
import eulermind.operator.ChangingPosition;
import eulermind.operator.HandoveringChild;
import eulermind.operator.HandoveringReference;
import eulermind.view.MindView;
import eulermind.view.NodeControl;
import prefuse.data.Node;
import prefuse.visual.NodeItem;

import java.awt.dnd.DragSource;

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

class NodeDraggingControl extends NodeControl {

    private MindView m_mindView;
    private boolean m_dragging = false;

    NodeDraggingControl(MindView mindView) {
        super(mindView);
        this.m_mindView = mindView;
    }

    @Override
    public void dragStart(NodeItem item, DragAction dragAction) {
        if (m_mindView.beginChanging()) {
            m_mindView.setCursor(dragAction == DragAction.LINK ? DragSource.DefaultLinkDrop : DragSource.DefaultMoveDrop);
            m_dragging = true;
        }
    }

    private Object[] getPossibleEdgeSource(Node droppedNode, NodeControl.HitPosition hitPosition)
    {
        Object ret[] = new Object[2];

        if (droppedNode == m_mindView.m_tree.getRoot()) {
            ret[0] = droppedNode;
            ret[1] = m_mindView.m_mindModel.getDBChildCount(droppedNode);
            return ret;
        }

        switch (hitPosition) {
            case TOP:
                ret[0] = droppedNode.getParent();
                ret[1] = droppedNode.getIndex();
                break;
            case BOTTOM:
                ret[0] = droppedNode.getParent();
                ret[1] = droppedNode.getIndex() + 1;
                break;
            case RIGHT:
                ret[0] = droppedNode;
                ret[1] = m_mindView.m_mindModel.getDBChildCount(droppedNode);
                break;
            default:
                ret[0] = null;
                ret[1] = -1;
                break;
        }

        return ret;
    }

    //TODO: 改成nodeItem 参数类型
    private boolean canDrop(NodeItem fromNodeItem, NodeItem hitNodeItem, HitPosition hitPosition, DragAction dragAction)
    {
        Node fromNode = m_mindView.toSource(fromNodeItem);
        Node hitNode = m_mindView.toSource(hitNodeItem);
        Object possibleEdgeSource[] = getPossibleEdgeSource(hitNode, hitPosition);

        Node parentNode = fromNode.getParent();
        Node newParentNode = (Node)possibleEdgeSource[0];

        if (possibleEdgeSource[0] == null) {
            return false;
        }

        //拖动到父节点的右半部，视为无效操作
        if (m_mindView.m_mindModel.isSelfInDB(parentNode, newParentNode)) {
            if (hitPosition == HitPosition.RIGHT) {
                return false;
            }
        }

        switch (dragAction) {
            case LINK:
                return true;
            case MOVE:
                if (!MindModel.getDbId(parentNode).equals(MindModel.getDbId(newParentNode))) {
                    if (! m_mindView.m_mindModel.isRefNode(fromNode)) {
                        return m_mindView.m_mindModel.canResetParent(fromNode, (Node)possibleEdgeSource[0]);
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private void setCursorShape(NodeItem sourceNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction)
    {
        boolean cursorEnabled = (hitNode == null) || canDrop(sourceNode, hitNode, hitPosition, dragAction);

        switch (dragAction) {
            case LINK:
                m_mindView.setCursor(cursorEnabled ? DragSource.DefaultLinkDrop : DragSource.DefaultLinkNoDrop);
                break;
            case MOVE:
                m_mindView.setCursor(cursorEnabled ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
                break;
            default:
                break;
        }
    }

    NodeItem m_dragHitted;

    //TODO: add dragHitted Node to MindView for render
    //if dropNode or dropPostion changed, give the event
    @Override
    public void dragHit(NodeItem item, NodeItem hitNode,
                        HitPosition hitPosition, DragAction dragAction)
    {
        if (!m_dragging) {
            return;
        }

        setCursorShape(item, hitNode, hitPosition, dragAction);
        m_dragHitted = hitNode;
        m_mindView.renderTree();
    }

    @Override
    public void dragMiss(NodeItem item, NodeItem dropNode, DragAction dragAction)
    {
        if (!m_dragging) {
            return;
        }

        setCursorShape(item, null, HitPosition.OUTSIDE, dragAction);
        m_dragHitted = null;
        m_mindView.renderTree();
    }

    MindOperator getDragOperator(Node draggedNode, Node droppedNode,
                                NodeControl.HitPosition hitPosition,
                                NodeControl.DragAction dragAction)
    {
        MindModel mindModel = m_mindView.m_mindModel;

        Object possibleEdgeSource[] = getPossibleEdgeSource(droppedNode, hitPosition);
        MindOperator operator = null;

        if (dragAction == NodeControl.DragAction.LINK) {
            Node referrer = (Node)possibleEdgeSource[0];
            int position = (Integer)possibleEdgeSource[1];

            operator = new AddingReference(mindModel, draggedNode, referrer, position);

        } else {

            Node newParent = (Node)possibleEdgeSource[0];
            int newPosition = (Integer)possibleEdgeSource[1];

            if (draggedNode == m_mindView.m_tree.getRoot()) {
                m_logger.info("forbid drag prefuse root to other as child");
                return null;
            }

            Node parent = draggedNode.getParent();

            if (MindModel.getDbId(newParent).equals(MindModel.getDbId(parent))) {
                //拖到现在的父节点的右半部，视为无效操作
                if (hitPosition == HitPosition.RIGHT) {
                    return null;
                }

                int oldPosition = draggedNode.getIndex();
                if (oldPosition < newPosition) {
                    newPosition--;
                }

                if (oldPosition == newPosition) {
                    return null;
                }

                operator = new ChangingPosition(mindModel, draggedNode, newPosition);

            } else {

                if (mindModel.isRefNode(draggedNode)) {
                    operator = new HandoveringReference(mindModel, draggedNode, newParent, newPosition);
                } else {

                    assert ! mindModel.isDescendantInDB(draggedNode, newParent);
                    assert(mindModel.canResetParent(draggedNode, newParent));

                    operator = new HandoveringChild(mindModel, draggedNode, newParent, newPosition);
                }
            }
        }

        return operator;
    }

    @Override
    public void dragEnd(NodeItem draggedNode, NodeItem droppedNode, HitPosition hitPosition, DragAction dragAction)
    {
        if (!m_dragging) {
            return;
        }

        m_logger.info("nodeItemDropped");
        MindOperator operator = null;

        if (droppedNode != null) {
            m_logger.info(String.format("--- dragAction %s", dragAction.toString()));
            if (canDrop(draggedNode, droppedNode, hitPosition, dragAction)) {
                operator = getDragOperator(draggedNode, droppedNode, hitPosition, dragAction);
            }
        }

        if (operator != null) {
            m_mindView.m_mindController.does(operator);
        } else {
            m_mindView.renderTreeToEndChanging();
        }

        m_dragging = false;
        m_mindView.setCursor(null);
    }

    @Override
    public void dragActionChanged(NodeItem draggedNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction)
    {
        if (!m_dragging) {
            return;
        }

        setCursorShape(draggedNode, hitNode, hitPosition, dragAction);
    }
}
