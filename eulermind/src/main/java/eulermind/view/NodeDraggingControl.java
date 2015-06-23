package eulermind.view;

import eulermind.MindModel;
import eulermind.MindOperator;
import eulermind.operator.*;
import prefuse.data.Node;
import prefuse.visual.NodeItem;

import java.awt.dnd.DragSource;
import java.util.ArrayList;
import java.util.List;

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

    private void setCursorShape(NodeItem sourceNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction)
    {
        switch (dragAction) {
            case LINK:
                m_mindView.setCursor(DragSource.DefaultLinkDrop);
                break;
            case MOVE:
                m_mindView.setCursor(DragSource.DefaultMoveDrop);
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

    List<MindOperator> getDragOperator(NodeItem droppedNodeItem,
                                NodeControl.HitPosition hitPosition,
                                NodeControl.DragAction dragAction)
    {
        MindModel mindModel = m_mindView.m_mindModel;

        List<MindOperator> operators = new ArrayList<>();

        List<Node> selectedNodes = m_mindView.getSelectedSourceNodes();

        selectedNodes = m_mindView.breadthFirstSort(selectedNodes);

        if (dragAction == NodeControl.DragAction.LINK) {

            int edgePosition;
            Node referrerNode;

            if (hitPosition == HitPosition.TOP || hitPosition == HitPosition.BOTTOM) {
                referrerNode = droppedNodeItem.getParent();
                if (referrerNode == null) {
                    return null;
                }

                edgePosition = (hitPosition == HitPosition.TOP) ? droppedNodeItem.getIndex() : droppedNodeItem.getIndex() + 1;

            } else {
                referrerNode = droppedNodeItem;
                edgePosition = droppedNodeItem.getChildCount();
            }

            //由于添加引用操作，是新建Node。所以多选的时候，选集中的后续节点不能作为前驱节点的兄弟
            for (Node selectedNode : selectedNodes) {
                operators.add(new AddingReference(mindModel, selectedNode, referrerNode, edgePosition));
                edgePosition++;
            }

        } else {

            selectedNodes = m_mindView.removeNodesWithSameInEdgeDbId(selectedNodes);

            operators.add(new DraggingNode(mindModel, selectedNodes.get(0), droppedNodeItem, hitPosition));

            for(int i=1; i<selectedNodes.size(); i++) {
                operators.add(new DraggingNode(mindModel, selectedNodes.get(i), selectedNodes.get(i-1), HitPosition.BOTTOM));
            }
        }

        return operators;
    }

    @Override
    public void dragEnd(NodeItem draggedNode, NodeItem droppedNode, HitPosition hitPosition, DragAction dragAction)
    {
        if (!m_dragging) {
            return;
        }

        m_logger.info("nodeItemDropped");
        List<MindOperator> operators = null;

        if (droppedNode != null && hitPosition != HitPosition.OUTSIDE) {
            m_logger.info(String.format("--- dragAction %s", dragAction.toString()));
            operators = getDragOperator(droppedNode, hitPosition, dragAction);
        }

        if (operators != null && operators.size() > 0) {
            m_mindView.m_mindController.does(operators);
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
