package eulermind.view;

import eulermind.MindModel;
import eulermind.MindOperator;
import eulermind.operator.*;
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

    MindOperator getDragOperator(NodeItem draggedNode, NodeItem droppedNode,
                                NodeControl.HitPosition hitPosition,
                                NodeControl.DragAction dragAction)
    {
        MindModel mindModel = m_mindView.m_mindModel;

        MindOperator operator = null;

        if (dragAction == NodeControl.DragAction.LINK) {
            /* TODO:
            Node referrer = (Node)possibleEdgeSource[0];
            int position = (Integer)possibleEdgeSource[1];

            operator = new AddingReference(mindModel, draggedNode, referrer, position);
            */

        } else {
            operator = new DraggingNode(mindModel, m_mindView.toSource(draggedNode),
                    m_mindView.toSource(droppedNode), hitPosition);

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
            operator = getDragOperator(draggedNode, droppedNode, hitPosition, dragAction);
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
