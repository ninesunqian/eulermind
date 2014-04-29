package excitedmind;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import prefuse.Constants;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.activity.Activity;
import prefuse.activity.ActivityAdapter;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.*;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

public class MindTreeRenderEngine {
    Logger m_logger = Logger.getLogger(this.getClass().getName());

	private final MindView m_mindView;
	private final Visualization m_vis;
	
	public final String m_treeGroupName;
    public final String m_treeNodesGroupName;
    public final String m_treeEdgesGroupName;
    
    private static final String sm_itemStyleActions = "itemStyleActions";
    private static final String sm_itemPositionActions = "itemPositionActions";
    
    public static final String sm_layoutAction = "layoutAction";
    
    
    private int m_orientation = Constants.ORIENT_LEFT_RIGHT;

    private RepaintAction m_repaintAction = new RepaintAction();

    private int m_cursorBackColor = ColorLib.rgb(210, 210, 210);
    private int m_shadowBackColor = ColorLib.rgb(240, 240, 240);
    private int m_normalBackColor = ColorLib.rgb(255, 255, 255);

	public MindTreeRenderEngine(MindView mindView, String treeGroupName) {
        m_logger.setLevel(Level.OFF);
		m_mindView = mindView;
		m_vis = m_mindView.getVisualization();
		
		m_treeGroupName = treeGroupName;
		m_treeNodesGroupName = PrefuseLib.getGroupName(treeGroupName, Graph.NODES); 
		m_treeEdgesGroupName = PrefuseLib.getGroupName(treeGroupName, Graph.EDGES); 

    	m_vis.setRendererFactory(makeItemRendererFactory());
    	
        m_vis.putAction(sm_itemStyleActions, makeItemStyleActions());
        m_vis.putAction(sm_itemPositionActions, makeItemPositionActions());
        
        ActionList layoutAction = new ActionList();

        //style action must be before layout action
        layoutAction.add(makeItemStyleActions());
        layoutAction.add(makeItemPositionActions());

        m_vis.putAction(sm_layoutAction, layoutAction);
        
        m_vis.putAction("repaint", m_repaintAction);
        
        m_vis.alwaysRunAfter(sm_layoutAction, "repaint");
        m_vis.alwaysRunAfter(sm_itemStyleActions, "repaint");
        m_vis.alwaysRunAfter(sm_itemPositionActions, "repaint");
	}
	
	public void run (final  Runnable runeAfterRePaint)
	{
        if (runeAfterRePaint != null) {
            m_repaintAction.addActivityListener(new ActivityAdapter() {
                @Override
                public void activityFinished(Activity a) {
                    runeAfterRePaint.run();
                    m_repaintAction.removeActivityListener(this);
                }
            });
        }

		m_vis.run(sm_layoutAction);

        //add this line, to make the edge invalid, so prefuse will recompute the position,
        m_vis.invalidateAll();
	}
	
    private Action makeItemStyleActions ()
    {
        ActionList actions = new ActionList();
        
    	actions.add(new NodeFontAction());
    	actions.add(new NodeColorAction());
    	actions.add(new NodeTextColorAction());
    	actions.add(new EdgeColorAction());
        actions.add(new EdgeStrokeAction());

        return actions;
    }
    
    private Action makeItemPositionActions ()
    {
        
        MindTreeLayout treeLayout =
        	new MindTreeLayout(m_treeGroupName, m_orientation, 50, 10);
        
    	treeLayout.setOrientation(Constants.ORIENT_LEFT_RIGHT);
        //must set the anchor, if not, the anchor will move to the center of display, every time.
        treeLayout.setLayoutAnchor(new Point2D.Double(25, 300));

        ActionList actions = new ActionList();
        
        actions.add(treeLayout);
        actions.add(new HoldFocusItemPanAction());
        
        return actions;
    }
    
    private DefaultRendererFactory makeItemRendererFactory ()
    {
	    NodeRenderer nodeRenderer = new NodeRenderer(MindModel.sm_textPropName, MindModel.sm_iconPropName);
    	nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
    	nodeRenderer.setHorizontalAlignment(Constants.LEFT);
    	nodeRenderer.setRoundedCorner(8, 8);
    	nodeRenderer.setHorizontalAlignment(Constants.LEFT);
        //nodeRenderer.setHorizontalPadding(10);

	    EdgeRenderer edgeRenderer = new EdgeRenderer(Constants.EDGE_TYPE_CURVE);
    	edgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
    	edgeRenderer.setHorizontalAlignment2(Constants.LEFT);
    	edgeRenderer.setVerticalAlignment1(Constants.CENTER);
    	edgeRenderer.setVerticalAlignment2(Constants.CENTER);
    	edgeRenderer.setDefaultLineWidth(1.5);

    	return new DefaultRendererFactory(nodeRenderer, edgeRenderer);
    }
    
    private	VisualItem m_holdedItem;
	private double m_holdedItemX;
	private double m_holdedItemY;
    
    public void holdItem (VisualItem item)
    {
    	m_holdedItem = item;
	    m_holdedItemX = item.getX();
	    m_holdedItemY = item.getY();
    }

	
    public class HoldFocusItemPanAction extends Action {

        public void run(double frac) {
	        if (m_holdedItem != null)
	        {
	        	double x = m_holdedItem.getX();
	        	double y = m_holdedItem.getY();
	        	m_mindView.pan(m_holdedItemX-x, m_holdedItemY-y);
	        	m_holdedItem = null;
	        }
        }
    }

    public class NodeColorAction extends ColorAction {

        public NodeColorAction() {
            super(m_treeNodesGroupName, VisualItem.FILLCOLOR);
        }

        public int getColor(VisualItem item) {

            Node node = m_mindView.toSource((NodeItem)item);
            Node cursorNode = m_mindView.getCursorSourceNode();

            if (node == cursorNode)
                return m_cursorBackColor;
            else if (!m_mindView.isPlaceholer(node) && m_mindView.m_mindModel.sameDBNode(node,cursorNode))
                return m_shadowBackColor;
            else
                return m_normalBackColor;
        }
    }
    
    public class NodeTextColorAction extends ColorAction {

        public NodeTextColorAction() {
            super(m_treeNodesGroupName, VisualItem.TEXTCOLOR);
        }

        public int getColor(VisualItem item) {
            Integer color = (Integer)item.get(MindModel.sm_textColorPropName);

            if (color == null)
                return ColorLib.rgb(0, 0, 0);
            else
                return color;
        }
    }
    
    public class NodeFontAction extends FontAction {

        public NodeFontAction() {
            super(m_treeNodesGroupName);
        }

        public Font getFont(VisualItem item) {
            String family = (String)item.get(MindModel.sm_fontFamilyPropName);
            Integer size = (Integer)item.get(MindModel.sm_fontSizePropName);

            if (family == null) {
                family = "SansSerif";
            }

            if (size == null) {
                size = 16;
            }
            //String family = (family)item.get(MindModel.sm_fontFamilyPropName);
            return FontLib.getFont(family, Font.PLAIN, size);
        }
    }
    
    public class EdgeColorAction extends ColorAction {
        public EdgeColorAction() {
            super(m_treeEdgesGroupName, VisualItem.STROKECOLOR);
        }

        public int getColor(VisualItem item) {
            return ColorLib.rgb(200, 200, 200);
        }
    }

    public class EdgeStrokeAction extends StrokeAction {
        public EdgeStrokeAction() {
            super(m_treeEdgesGroupName);
        }

        public BasicStroke getStroke(VisualItem item) {
            if (m_mindView.isPlaceholer(item)) {
                return StrokeLib.getStroke(1.0f);
            }

            if (m_mindView.m_mindModel.isRefEdge((EdgeItem) item)) {
                float dash [] = {10f, 5f};
                return StrokeLib.getStroke(1.0f, dash);
            } else {
                return StrokeLib.getStroke(1.0f);
            }
        }
    }

    public class NodeRenderer extends LabelRenderer {
        public NodeRenderer(String textField) {
            super(textField);
        }

        public NodeRenderer(String textField, String imageField) {
            super(textField, imageField);
        }

        protected String getText(VisualItem item) {
            //return m_mindView.m_mindModel.getText((NodeItem)item) + " " + MindTreeLayout.getLayoutInfo((NodeItem)item);
            return m_mindView.m_mindModel.getText((NodeItem)item);
        }

        protected String getImageLocation(VisualItem item) {
            return MindIcons.getIconPath(item.getString(MindModel.sm_iconPropName));
            //return MindIcons.getIconPath("icon");
            //return null;
        }

        public int getRenderType(VisualItem item) {
            //FIXME: add a color action
            item.setStrokeColor(ColorLib.rgb(150,150,150));

            if (((NodeItem) item).getChildCount() != 0) {
                if (item.isExpanded()) {
                    return RENDER_TYPE_FILL;
                } else {
                    return RENDER_TYPE_DRAW_AND_FILL;
                }

            } else {
                MindModel mindModel = m_mindView.m_mindModel;

                int childCount = mindModel.getChildCount((NodeItem)item);

                if (childCount == 0) {
                    return RENDER_TYPE_FILL;
                } else {
                    return RENDER_TYPE_DRAW_AND_FILL;
                }
            }
        }

        public void render(Graphics2D g, VisualItem item) {
            super.render(g, item);

            if (item == m_mindView.getDragHitNode()) {
                RectangularShape shape = (RectangularShape)getShape(item);

                float x = (float)shape.getX();
                float y = (float)shape.getY();
                float width = (float)shape.getWidth();
                float height = (float)shape.getHeight();

                float colorStartX = x;
                float colorStartY = y;
                float colorEndX = x + width;
                float colorEndY = y + width;

                switch (m_mindView.m_mouseControl.m_hitPosition){
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

                int red = ColorLib.red(m_cursorBackColor);
                int green = ColorLib.green(m_cursorBackColor);
                int blue = ColorLib.blue(m_cursorBackColor);

                red = green = blue = 128;

                GradientPaint gradientPaint =new GradientPaint(colorStartX, colorStartY,
                        ColorLib.getColor(red, green, blue, 0),
                        colorEndX, colorEndY,
                        ColorLib.getColor(red, green, blue, 128), false);

                g.setPaint(gradientPaint);
                g.fillRect((int)x, (int)y, (int)width, (int)height);
            }
        }
    }
}
