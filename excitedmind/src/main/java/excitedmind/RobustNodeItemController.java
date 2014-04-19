package excitedmind;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 14-2-3
 * Time: 下午6:52
 * To change this template use File | Settings | File Templates.
 */
public abstract class RobustNodeItemController extends ControlAdapter {

    Logger m_logger = Logger.getLogger(this.getClass().getName());

    enum HitPosition {OUTSIDE, TOP, BOTTOM, RIGHT};

    Display m_display;

    boolean m_ctrlDowned;
    long m_ctrlReleaseTime;

    NodeItem m_hitNode;
    HitPosition m_hitPosition;

    Point m_mousePressPoint;

    RobustNodeItemController(Display display) {
        m_ctrlReleaseTime = 0;
        m_ctrlDowned = false;
        m_display = display;
        m_hitNode = null;
        m_hitPosition = HitPosition.OUTSIDE;
    }

    private void clearHitNode()
    {
        m_hitNode = null;
        m_hitPosition = HitPosition.OUTSIDE;
    }

    HitPosition getHitPosition(NodeItem node, Point point)
    {
        Point absPoint = new Point();
        m_display.getAbsoluteCoordinate(point, absPoint);

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


    public void itemEntered(VisualItem item, MouseEvent e) {
        if (item instanceof NodeItem) {
            nodeItemEntered((NodeItem) item, e);
        }
    }

    public void nodeItemEntered(NodeItem item, MouseEvent e) {

    }

    public void itemExited(VisualItem item, MouseEvent e) {
        if (item instanceof NodeItem) {
            nodeItemExited((NodeItem)item, e);
        }
    }

    public void nodeItemExited(NodeItem item, MouseEvent e) {

    }

    public void itemPressed(VisualItem item, MouseEvent e) {
        if (item instanceof NodeItem) {
            nodeItemPressed((NodeItem) item, e);
        }

        m_mousePressPoint = e.getPoint();
    }

    public void nodeItemPressed(NodeItem item, MouseEvent e) {

    }

    public void itemClicked(VisualItem item, MouseEvent e) {
        if (item instanceof NodeItem) {
            nodeItemClicked((NodeItem) item, e);
        }
    }

    public void nodeItemClicked(NodeItem item, MouseEvent e) {

    }

    public void nodeItemHit(NodeItem item, NodeItem hitNode, HitPosition hitPosition, boolean ctrlDowned) {

    }

    public void nodeItemMissed(NodeItem item, NodeItem hitNode, boolean ctrlDowned) {

    }

    public void nodeItemDragged(NodeItem item, MouseEvent e) {
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
    public void itemDragged(VisualItem item, MouseEvent e) {
        if (!(item instanceof NodeItem)) {
            return;
        }

        Point point = e.getPoint();
        if (point.distance(m_mousePressPoint) < 10) {
            return;
        }

        NodeItem fromNode = (NodeItem)item;
        nodeItemDragged(fromNode, e);

        Point mousePoint = e.getPoint();

        NodeItem curHitNode = getHitNode(mousePoint);

        if (curHitNode == null || curHitNode != m_hitNode || curHitNode == fromNode) {
            assert (m_hitNode != fromNode);
            if (m_hitNode != null) {
                m_logger.info("itemMissed : " + m_hitNode.getString(MindModel.sm_textPropName));
                nodeItemMissed(fromNode, m_hitNode, m_ctrlDowned);
            }
            clearHitNode();
        }

        if (curHitNode != null && curHitNode != fromNode) {

            HitPosition hitPosition = getHitPosition(curHitNode, mousePoint);

            if (curHitNode != m_hitNode || hitPosition != m_hitPosition) {
                m_hitNode = curHitNode;
                m_hitPosition = hitPosition;

                m_logger.info("itemHit : " + curHitNode.getString(MindModel.sm_textPropName) + " - " + m_hitPosition.toString());
                nodeItemHit(fromNode, curHitNode, hitPosition, m_ctrlDowned);
            }
        }
    }

    public void nodeItemReleased(NodeItem item, MouseEvent e) {
    }

    private boolean releaseWithCtrl() {
        return m_ctrlDowned || System.currentTimeMillis() - m_ctrlReleaseTime < 500;
    }

    public void itemReleased(VisualItem item, MouseEvent e) {
        if (!(item instanceof NodeItem)) {
            return;
        }

        m_logger.info("itemReleased : " + item.getString(MindModel.sm_textPropName));

        NodeItem fromNode = (NodeItem)item;
        nodeItemReleased(fromNode, e);

        Point mousePoint = e.getPoint();
        NodeItem curHitNode = getHitNode(mousePoint);

        if (curHitNode != fromNode) {
            if (curHitNode != null) {
                HitPosition hitPosition = getHitPosition(curHitNode, mousePoint);
                nodeItemDropped(fromNode, curHitNode, hitPosition, releaseWithCtrl());
                m_logger.info("itemDropped : " + curHitNode.getString(MindModel.sm_textPropName));
            } else {
                m_logger.info("itemDropped : null");
                nodeItemDropped(fromNode, null, HitPosition.OUTSIDE, releaseWithCtrl());
            }
        }

        clearHitNode();
    }

    //if hitNode == null, no dropped to a node
    public void nodeItemDropped(NodeItem item, NodeItem hitNode, HitPosition hitPosition, boolean m_ctrlDowned) {

    }

    public void itemKeyPressed(VisualItem item, KeyEvent e) {
        if (item instanceof NodeItem) {
            nodeItemKeyPressed((NodeItem)item, e);
        }

        m_logger.info("ctrl pressed");

        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            m_ctrlDowned = true;
        }
    }

    public void nodeItemKeyPressed(NodeItem item, KeyEvent e) {

    }

    public void itemKeyReleased(VisualItem item, KeyEvent e) {
        if (item instanceof NodeItem) {
            nodeItemKeyReleased((NodeItem)item, e);
        }

        m_logger.info("ctrl released");

        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            m_ctrlReleaseTime = System.currentTimeMillis();
            m_ctrlDowned = false;
        }
    }

    public void nodeItemKeyReleased(NodeItem item, KeyEvent e) {

    }
}
