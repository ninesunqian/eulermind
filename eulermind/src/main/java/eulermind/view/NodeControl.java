package eulermind.view;

import eulermind.MindModel;
import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.util.ui.UILib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
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

public abstract class NodeControl extends ControlAdapter {

    Logger m_logger = LoggerFactory.getLogger(this.getClass());
    static Logger s_logger = LoggerFactory.getLogger(NodeControl.class);

    public enum HitPosition {OUTSIDE, TOP, BOTTOM, RIGHT};

    Display m_display;

    boolean m_dragging;

    NodeItem m_hitNode;
    HitPosition m_hitPosition;

    Point m_mousePressPoint;

    enum DragAction {
        MOVE,
        LINK,
    };
    DragAction m_dragAction;

    NodeControl(Display display) {
        m_display = display;
        m_hitNode = null;
        m_hitPosition = HitPosition.OUTSIDE;
    }

    private void clearHitNode()
    {
        m_hitNode = null;
        m_hitPosition = HitPosition.OUTSIDE;
    }

    static HitPosition getHitPosition(Display display, NodeItem node, Point point)
    {
        Point absPoint = new Point();
        display.getAbsoluteCoordinate(point, absPoint);

        Rectangle2D bounds = node.getBounds();

        if (bounds.contains(absPoint.getX(), absPoint.getY()))
        {
            if (absPoint.getX() > bounds.getCenterX()) {
                return HitPosition.RIGHT;
            }

            if (absPoint.getY() > bounds.getCenterY()) {
                return HitPosition.BOTTOM;

            } else {
                return HitPosition.TOP;
            }
        } else {
            return HitPosition.OUTSIDE;
        }
    }


    static public Object[] hitTest(Display display, Point point) {
        Object ret[] = new Object[2];

        VisualItem item = display.findItem(point);
        if (item != null && item instanceof NodeItem) {
            ret[0] = item;
            ret[1] = getHitPosition(display, (NodeItem)item, point);
            s_logger.info("item: {}, hit {},  point,{},{}", item, ret[1], point.getX(), point.getY());
        }
        return ret;
    }


    final public void itemEntered(VisualItem item, MouseEvent e) {
        if (item instanceof NodeItem) {
            nodeItemEntered((NodeItem) item, e);
        }
    }

    public void nodeItemEntered(NodeItem item, MouseEvent e) {

    }

    final public void itemExited(VisualItem item, MouseEvent e) {
        if (item instanceof NodeItem) {
            nodeItemExited((NodeItem)item, e);
        }
    }

    public void nodeItemExited(NodeItem item, MouseEvent e) {

    }

    final public void itemPressed(VisualItem item, MouseEvent e) {
        if (item instanceof NodeItem) {
            nodeItemPressed((NodeItem) item, e);
            m_mousePressPoint = e.getPoint();
        }
    }

    public void nodeItemPressed(NodeItem item, MouseEvent e) {

    }

    final public void itemClicked(VisualItem item, MouseEvent e) {
        if (item instanceof NodeItem) {
            nodeItemClicked((NodeItem) item, e);
            m_mousePressPoint = null;
        }
    }

    public void nodeItemClicked(NodeItem item, MouseEvent e) {

    }

    public void dragHit(NodeItem draggedNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction) {

    }

    public void dragMiss(NodeItem draggedNode, NodeItem hitNode, DragAction dragAction) {

    }

    public void dragStart(NodeItem draggedNode, DragAction dragAction) {
    }

    public void dragActionChanged(NodeItem draggedNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction) {

    }

    public void dragEnd(NodeItem draggedNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction) {

    }

    private NodeItem getHitNode(Point point) {
        VisualItem hitTarget = m_display.findItem(point);

        if (hitTarget != null && hitTarget instanceof NodeItem) {
            return (NodeItem)hitTarget;
        }
        else {
            return null;
        }
    }

    private DragAction getDragAction(MouseEvent e) {
        return e.isControlDown() ? DragAction.LINK : DragAction.MOVE;
    }

    public void itemDragged(VisualItem item, MouseEvent e) {
        if (!(item instanceof NodeItem)) {
            return;
        }

        m_logger.info("drag item{}", item);

        Point point = e.getPoint();
        if (m_mousePressPoint == null || point.distance(m_mousePressPoint) < 10 || ! UILib.isButtonPressed(e, LEFT_MOUSE_BUTTON)) {
            return;
        }

        NodeItem draggedNode = (NodeItem)item;
        DragAction dragAction = getDragAction(e);

        Point mousePoint = e.getPoint();
        NodeItem curHitNode = getHitNode(mousePoint);


        if (! m_dragging) {
            m_logger.info("dragStart");
            dragStart(draggedNode, dragAction);
            m_dragging = true;
            m_dragAction = dragAction;
        }

        if (curHitNode == null || curHitNode != m_hitNode || curHitNode == draggedNode) {
            assert (m_hitNode != draggedNode);
            if (m_hitNode != null) {
                m_logger.info("itemMissed : " + m_hitNode.getString(MindModel.TEXT_PROP_NAME));
                dragMiss(draggedNode, m_hitNode, dragAction);
            }
            clearHitNode();

        }

        if (curHitNode != null && curHitNode != draggedNode) {

            HitPosition hitPosition = getHitPosition(m_display, curHitNode, mousePoint);

            if (curHitNode != m_hitNode || hitPosition != m_hitPosition) {
                m_hitNode = curHitNode;
                m_hitPosition = hitPosition;

                m_logger.info("itemHit : " + curHitNode.getString(MindModel.TEXT_PROP_NAME) + " - " + m_hitPosition.toString());
                dragHit(draggedNode, curHitNode, hitPosition, dragAction);
            }
        }

        if (m_dragAction != dragAction) {
            dragActionChanged(draggedNode, m_hitNode, m_hitPosition, dragAction);
            m_dragAction = dragAction;
        }
    }

    public void nodeItemReleased(NodeItem item, MouseEvent e) {
    }


    public void itemReleased(VisualItem item, MouseEvent e) {
        if (!(item instanceof NodeItem)) {
            return;
        }

        m_mousePressPoint = null;

        nodeItemReleased((NodeItem)item, e);

        if (! UILib.isButtonPressed(e, LEFT_MOUSE_BUTTON) ) {
            return;
        }

        NodeItem draggedNode = (NodeItem)item;

        m_dragging = false;

        Point mousePoint = e.getPoint();
        NodeItem curHitNode = getHitNode(mousePoint);

        DragAction dragAction = getDragAction(e);

        if (curHitNode != null && curHitNode != draggedNode) {
            HitPosition hitPosition = getHitPosition(m_display, curHitNode, mousePoint);
            dragEnd(draggedNode, curHitNode, hitPosition, dragAction);
            m_logger.info("itemDropped : " + curHitNode.getString(MindModel.TEXT_PROP_NAME));
        } else {
            dragEnd(draggedNode, null, HitPosition.OUTSIDE, dragAction);
        }

        clearHitNode();
    }


    final public void itemKeyPressed(VisualItem item, KeyEvent e) {
        if (item instanceof NodeItem) {
            nodeItemKeyPressed((NodeItem)item, e);
        }
    }

    public void nodeItemKeyPressed(NodeItem item, KeyEvent e) {

    }

    final public void itemKeyReleased(VisualItem item, KeyEvent e) {
        if (item instanceof NodeItem) {
            nodeItemKeyReleased((NodeItem)item, e);
        }
    }

    public void nodeItemKeyReleased(NodeItem item, KeyEvent e) {

    }

}
