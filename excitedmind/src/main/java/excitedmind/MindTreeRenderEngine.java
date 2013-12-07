package excitedmind;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.logging.Logger;

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
import prefuse.activity.ActivityListener;
import prefuse.data.Graph;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.util.StrokeLib;
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
	
	public MindTreeRenderEngine(MindView mindView, String treeGroupName) {
		m_mindView = mindView;
		m_vis = m_mindView.getVisualization();
		
		m_treeGroupName = treeGroupName;
		m_treeNodesGroupName = PrefuseLib.getGroupName(treeGroupName, Graph.NODES); 
		m_treeEdgesGroupName = PrefuseLib.getGroupName(treeGroupName, Graph.EDGES); 

    	m_vis.setRendererFactory(makeItemRendererFactory());
    	
        m_vis.putAction(sm_itemStyleActions, makeItemStyleActions());
        m_vis.putAction(sm_itemPositionActions, makeItemPositionActions());
        
        ActionList layoutAction = new ActionList();
        layoutAction.add(makeItemPositionActions());
        layoutAction.add(makeItemStyleActions());
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
        	new MindTreeLayout(m_treeGroupName, m_orientation, 50, 0, 0);
        
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
	    NodeRenderer nodeRenderer = new NodeRenderer(MindTree.sm_textPropName);
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
            return m_mindView.getVisMindTree().getNodeColor((NodeItem)item);
        }
    }
    
    public class NodeTextColorAction extends ColorAction {

        public NodeTextColorAction() {
            super(m_treeNodesGroupName, VisualItem.TEXTCOLOR);
        }

        public int getColor(VisualItem item) {
            return ColorLib.rgb(0, 0, 0);
        }

    }
    
    public class NodeFontAction extends FontAction {

        public NodeFontAction() {
            super(m_treeNodesGroupName);
        }

        public Font getFont(VisualItem item) {
            return FontLib.getFont("SansSerif",Font.PLAIN,15);
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
            if (m_mindView.getVisMindTree().isPlaceholer(item)) {
                return StrokeLib.getStroke(1.0f);
            }

            if (m_mindView.getVisMindTree().isRefEdge((EdgeItem)item)) {
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
            String s = item.getString(MindTree.sm_textPropName);
            return  (s==null || s.length() < 2 )? " " + s + " ": s;
        }

        public int getRenderType(VisualItem item) {
            //FIXME: add a color action
            item.setStrokeColor(ColorLib.rgb(150,150,150));

            VisualMindTree visualMindTree = m_mindView.getVisMindTree();

            int childCount = visualMindTree.getChildCount((NodeItem)item);

            if (((NodeItem) item).getChildCount() != 0) {
                if (item.isExpanded()) {
                    return RENDER_TYPE_FILL;
                } else {
                    return RENDER_TYPE_DRAW_AND_FILL;
                }

            } else {
                if (visualMindTree.getChildCount((NodeItem)item) == 0) {
                    return RENDER_TYPE_FILL;
                } else {
                    return RENDER_TYPE_DRAW_AND_FILL;
                }
            }
        }
    }
}
