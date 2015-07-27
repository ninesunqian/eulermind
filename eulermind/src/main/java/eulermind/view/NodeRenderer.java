package eulermind.view;

import eulermind.MindModel;
import eulermind.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.util.StrokeLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;


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

public class NodeRenderer extends MyLabelRenderer {
    MindView m_mindView;
    Logger m_logger = LoggerFactory.getLogger(this.getClass());
    private boolean m_showBigBorderOfCursor = false;
    double m_cursorBorderExpand = 0;

    void setCursorBorderExpand(double cursorBorderExpand)
    {
        m_cursorBorderExpand = cursorBorderExpand;
    }

    void setShowBigBorderOfCursor(boolean showBigBorderOfCursor)
    {
        m_showBigBorderOfCursor = showBigBorderOfCursor;
    }

    public NodeRenderer(MindView mindView, String textField) {
        super(textField);
        m_mindView = mindView;
    }

    public NodeRenderer(MindView mindView, String textField, String imageField) {
        super(textField, imageField);
        m_mindView = mindView;
    }

    protected String getText(VisualItem item) {
        //return m_mindView.m_mindModel.getText((NodeItem)item) + " " + MindTreeLayout.getLayoutInfo((NodeItem)item);
        return m_mindView.m_mindModel.getText((NodeItem)item);
        //return m_mindView.m_mindModel.getNodeDebugInfo((NodeItem)item);
    }

    protected Image getImage(VisualItem item) {
        String iconName = MindModel.getNodeIcon(item);
        if (iconName == null) {
            return null;
        }
        return Style.getImageIcon(iconName).getImage();
    }

    public int getRenderType(VisualItem item) {
        //FIXME: add a color action
        item.setStrokeColor(ColorLib.rgb(150, 150, 150));

        if (((NodeItem) item).getChildCount() != 0) {
            if (item.isExpanded()) {
                return RENDER_TYPE_FILL;
            } else {
                return RENDER_TYPE_DRAW_AND_FILL;
            }

        } else {
            if (m_mindView.isPlaceholder(item)) {
                return RENDER_TYPE_FILL;

            } else {
                MindModel mindModel = m_mindView.m_mindModel;

                int childCount = mindModel.getDBChildCount((NodeItem) item);

                if (childCount == 0) {
                    return RENDER_TYPE_FILL;
                } else {
                    return RENDER_TYPE_DRAW_AND_FILL;
                }
            }
        }
    }

    public void render(Graphics2D g, VisualItem item) {
        super.render(g, item);

        if (! (item instanceof NodeItem)) {
            return;
        }

        NodeItem nodeItem = (NodeItem)item;

        if (nodeItem == m_mindView.m_dndDargOverNode && m_mindView.m_dndHitPosition != NodeControl.HitPosition.OUTSIDE) {
            RectangularShape shape = (RectangularShape)getShape(nodeItem);

            float x = (float)shape.getX();
            float y = (float)shape.getY();
            float width = (float)shape.getWidth();
            float height = (float)shape.getHeight();

            float colorStartX = x;
            float colorStartY = y;
            float colorEndX = x + width;
            float colorEndY = y + width;

            //switch (m_mindView.m_dragControl.m_hitPosition){
            switch (m_mindView.m_dndHitPosition){
                case TOP:
                    m_logger.info("TOP gradient");
                    height /= 2;

                    colorStartX = colorEndX = x;
                    colorStartY = y + height;
                    colorEndY = y;
                    break;

                case BOTTOM:
                    m_logger.info("bottom gradient");
                    y += height / 2;
                    height /= 2;

                    colorStartX = colorEndX = x;
                    colorStartY = y;
                    colorEndY = y + height;
                    break;

                case RIGHT:
                    m_logger.info("right gradient");
                    x += width / 2;
                    width /= 2;

                    colorStartY = colorEndY = y;
                    colorStartX = x;
                    colorEndX = x + width;
                    break;

                case OUTSIDE:
                    return;
            }

            int red = ColorLib.red(Style.sm_cursorBackColor);
            int green = ColorLib.green(Style.sm_cursorBackColor);
            int blue = ColorLib.blue(Style.sm_cursorBackColor);

            red = green = blue = 128;

            GradientPaint gradientPaint =new GradientPaint(colorStartX, colorStartY,
                    ColorLib.getColor(red, green, blue, 0),
                    colorEndX, colorEndY,
                    ColorLib.getColor(red, green, blue, 128), false);

            g.setPaint(gradientPaint);
            g.fillRect((int)x, (int)y, (int)width, (int)height);
        }

        if (! m_showBigBorderOfCursor) {
            return;
        }

        /*
        if (m_mindView != m_mindView.m_mindController.getCurrentView()) {
            return;
        }
        */

        java.util.List<NodeItem> selectedNodeItems = m_mindView.m_mindController.getCurrentView().getSelectedNodeItems();
        //m_logger.info("selected NodeItem size {}", selectedNodeItems.size());

        if (selectedNodeItems.contains(nodeItem)) {
            Color color = ColorLib.getColor(0, 0, 255, 255);
            paintCursorBoundary(g, nodeItem, color);

        } else  {
            for (NodeItem selectedNode : selectedNodeItems) {
                if (!selectedNode.isValid()) {
                    continue;
                }

                if (m_mindView.m_mindModel.isSelfInDB(nodeItem, selectedNode)) {
                    Color color = ColorLib.getColor(0, 0, 255, 80);
                    paintCursorBoundary(g, nodeItem, color);
                    break;
                }
            }
        }
    }

    private void paintCursorBoundary(Graphics2D g, VisualItem item, Color color)
    {
        Rectangle2D bounds = (Rectangle2D)item.getBounds().clone();

        GraphicsLib.expand(bounds, m_cursorBorderExpand);

        RoundRectangle2D borderShape = new RoundRectangle2D.Double(
                bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), m_cursorBorderExpand, m_cursorBorderExpand);

        BasicStroke stroke = StrokeLib.getStroke(3.0f);
        GraphicsLib.paint(g, borderShape, stroke, color, null, RENDER_TYPE_DRAW);
    }
}
