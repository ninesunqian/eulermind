package mindworld;

import mindworld.operator.AddingReference;
import mindworld.operator.ChangingPosition;
import mindworld.operator.HandoveringChild;
import mindworld.operator.HandoveringReference;
import prefuse.data.Node;
import prefuse.visual.NodeItem;

import java.awt.dnd.DragSource;

/**
* Created with IntelliJ IDEA.
* User: wangxuguang
* Date: 14-11-19
* Time: 下午10:21
* To change this template use File | Settings | File Templates.
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
            ret[1] = MindModel.getDBChildCount(droppedNode);
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
                ret[1] = MindModel.getDBChildCount(droppedNode);
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

        switch (dragAction) {
            case LINK:
                return true;
            case MOVE:
                if (!MindModel.getDBId(parentNode).equals(MindModel.getDBId(newParentNode))) {
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

            if (MindModel.getDBId(newParent).equals(MindModel.getDBId(parent))) {
                int oldPosition = draggedNode.getIndex();
                if (oldPosition < newPosition) {
                    newPosition--;
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
