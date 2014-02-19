package excitedmind;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.util.GraphLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
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

    enum HittedPosition {OUTSIDE, TOP, BOTTOM, RIGHT};

    Display m_display;

    boolean m_ctrlDowned;
    long m_ctrlReleaseTime;

    NodeItem m_hittedNode;
    HittedPosition m_hittedPosition;

    Point m_mousePressPoint;

    RobustNodeItemController(Display display) {
        m_ctrlReleaseTime = 0;
        m_ctrlDowned = false;
        m_display = display;
        m_hittedNode = null;
        m_hittedPosition = HittedPosition.OUTSIDE;
    }

    HittedPosition getHittedPosition(NodeItem node, double x, double y)
    {
        Rectangle2D bounds = node.getBounds();
        if (bounds.contains(x, y))
        {
            if (x > bounds.getCenterX()) {
                return HittedPosition.RIGHT;
            }

            if (y > bounds.getCenterY()) {
                return HittedPosition.BOTTOM;

            } else {
                return HittedPosition.TOP;
            }
        } else {
            return HittedPosition.OUTSIDE;
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

    public void nodeItemHitted(NodeItem item, NodeItem hittedNode, HittedPosition hittedPosition, boolean m_ctrlDowned) {

    }

    public void nodeItemMissed(NodeItem item, NodeItem hittedNode, boolean m_ctrlDowned) {

    }

    public void nodeItemDragged(NodeItem item, MouseEvent e) {
    }


    public void itemDragged(VisualItem item, MouseEvent e) {
        if (!(item instanceof NodeItem)) {
            return;
        }

        //m_logger.info("itemDragged : " + item.getString(MindTree.sm_textPropName));

        Point point = e.getPoint();
        if (point.distance(m_mousePressPoint) < 10) {
            return;
        }

        NodeItem nodeItem = (NodeItem)item;
        nodeItemDragged(nodeItem, e);

        Point mousePoint = e.getPoint();
        VisualItem hittedTarget = m_display.findItem(mousePoint);

        if (hittedTarget == null || ! (hittedTarget instanceof NodeItem)) {
            if (m_hittedNode != null) {
                nodeItemMissed(nodeItem, m_hittedNode, m_ctrlDowned);
                m_hittedNode = null;
                m_hittedPosition = HittedPosition.OUTSIDE;
            }

            return;
        }

        NodeItem hittedNode = (NodeItem)hittedTarget;
        HittedPosition hittedPosition = getHittedPosition(hittedNode, mousePoint.getX(), mousePoint.getY());

        if (m_hittedNode != hittedNode) {
            if (m_hittedNode != null) {
                nodeItemMissed(nodeItem, m_hittedNode, m_ctrlDowned);
            }
        }

        if (m_hittedNode != hittedNode || m_hittedPosition != hittedPosition) {
            nodeItemHitted(nodeItem, hittedNode, hittedPosition, m_ctrlDowned);
        }

        m_hittedNode = hittedNode;
        m_hittedPosition = hittedPosition;
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

        //m_logger.info("itemReleased : " + item.getString(MindTree.sm_textPropName));

        NodeItem nodeItem = (NodeItem)item;
        nodeItemReleased(nodeItem, e);

        Point mousePoint = e.getPoint();
        VisualItem hittedTarget = m_display.findItem(mousePoint);

        if (hittedTarget != null && hittedTarget instanceof NodeItem) {
            HittedPosition hittedPosition = getHittedPosition((NodeItem)hittedTarget, mousePoint.getX(), mousePoint.getY());
            nodeItemDropped(nodeItem, (NodeItem)hittedTarget, hittedPosition, releaseWithCtrl());
        } else {
            nodeItemDropped(nodeItem, null, HittedPosition.OUTSIDE, releaseWithCtrl());
        }

        m_hittedNode = null;
        m_hittedPosition = HittedPosition.OUTSIDE;
    }

    public void nodeItemDropped(NodeItem item, NodeItem hittedNode, HittedPosition hittedPosition, boolean m_ctrlDowned) {

    }

    public void itemKeyPressed(VisualItem item, KeyEvent e) {
        if (item instanceof NodeItem) {
            nodeItemKeyPressed((NodeItem)item, e);
        }

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

        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            m_ctrlReleaseTime = System.currentTimeMillis();
            m_ctrlDowned = false;
        }
    }

    public void nodeItemKeyReleased(NodeItem item, KeyEvent e) {

    }
}
