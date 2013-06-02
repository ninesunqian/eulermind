package excitedmind;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;

import excitedmind.operators.EditAction;
import excitedmind.operators.RemoveAction;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.FontAnimator;
import prefuse.action.animate.LocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.filter.FisheyeTreeFilter;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.ControlAdapter;
import prefuse.controls.FocusControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Tree;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.search.PrefixSearchTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;
import prefuse.visual.tuple.TableNodeItem;


public class MindTreeRenderEngine {
	private final MindView m_mindView;
	private final Visualization m_vis;
	
	public final String m_treeGroupName;
    public final String m_treeNodesGroupName;
    public final String m_treeEdgesGroupName;
    
    private static final String sm_itemStyleActions = "itemStyleActions";
    private static final String sm_itemPositionActions = "itemPositionActions";
    
    public static final String sm_layoutAction = "layoutAction";
    
    private NodeLinkTreeLayout m_treeLayout;
    
    private int m_orientation = Constants.ORIENT_LEFT_RIGHT;
	
	
	public MindTreeRenderEngine(MindView mindView, String treeGroupName) {
		m_mindView = mindView;
		m_vis = m_mindView.getVisualization();
		
		m_treeGroupName = treeGroupName;
		m_treeNodesGroupName = PrefuseLib.getGroupName(treeGroupName, Graph.NODES); 
		m_treeEdgesGroupName = PrefuseLib.getGroupName(treeGroupName, Graph.EDGES); 

        addItemStyleActions();
        addItemPositionActions();
        
        setItemRenderer ();

        // quick repaint
        m_vis.putAction("repaint", new RepaintAction());

        // create the filtering and layout
        ActionList layoutAction = new ActionList();
        layoutAction.add(m_vis.getAction(sm_itemPositionActions));
        layoutAction.add(m_vis.getAction(sm_itemStyleActions));
        m_vis.putAction(sm_layoutAction, layoutAction);
        m_vis.alwaysRunAfter(sm_layoutAction, "repaint");
		// TODO Auto-generated constructor stub
	}
	
    private void addItemStyleActions ()
    {
        ActionList actions = new ActionList();
        
    	actions.add(new NodeFontAction());
    	actions.add(new NodeColorAction());
    	actions.add(new NodeTextColorAction());
    	
    	actions.add(new EdgeColorAction());
    	
        m_vis.putAction(sm_itemStyleActions, actions);
    }
    
    private void addItemPositionActions ()
    {
        
        m_treeLayout = new NodeLinkTreeLayout(m_treeGroupName,
                m_orientation, 50, 0, 8);
        
    	m_treeLayout.setOrientation(Constants.ORIENT_LEFT_RIGHT);
    	
        //must set the anchor, if not, the anchor will move to the center of display, every time.
        m_treeLayout.setLayoutAnchor(new Point2D.Double(25, 300));

        
        ActionList actions = new ActionList();
        actions.add(m_treeLayout);
        actions.add(new HoldFocusItemPanAction());
        
        m_vis.putAction(sm_itemPositionActions, actions);
    }
    
    private void setItemRenderer ()
    {
	    LabelRenderer nodeRenderer;
	    EdgeRenderer edgeRenderer;

    	nodeRenderer = new LabelRenderer(MindTree.sm_textPropName);
    	nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
    	nodeRenderer.setHorizontalAlignment(Constants.LEFT);
    	nodeRenderer.setRoundedCorner(8, 8);
    	nodeRenderer.setHorizontalAlignment(Constants.LEFT);

    	edgeRenderer = new EdgeRenderer(Constants.EDGE_TYPE_CURVE);
    	edgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
    	edgeRenderer.setHorizontalAlignment2(Constants.LEFT);
    	edgeRenderer.setVerticalAlignment1(Constants.CENTER);
    	edgeRenderer.setVerticalAlignment2(Constants.CENTER);

    	DefaultRendererFactory rf = new DefaultRendererFactory(nodeRenderer, edgeRenderer);
    	
    	//TODO remove it
    	//rf.add(new InGroupPredicate(m_treeEdgesGroupName), edgeRenderer);
    	
    	m_vis.setRendererFactory(rf);
    }

	
    public class HoldFocusItemPanAction extends Action {

        public void run(double frac) {
	        if (m_needPan)
	        {
	        	double x = m_curFocus.getX();
	        	double y = m_curFocus.getY();
	        	pan(m_clickedItemX-x, m_clickedItemY-y);
	        	m_needPan = false;
	        }
        }
    }

    public class NodeColorAction extends ColorAction {

        public NodeColorAction() {
            super(m_treeNodesGroupName, VisualItem.FILLCOLOR);
        }

        public int getColor(VisualItem item) {
        	if (item == m_mindView.getFocusNode())
                return ColorLib.rgb(255, 255, 0);
        	else if (m_vis.isInGroup(item, Visualization.SEARCH_ITEMS))
                return ColorLib.rgb(255, 0, 0);
            else if (m_vis.isInGroup(item, Visualization.FOCUS_ITEMS))
                return ColorLib.rgb(0, 255, 0);
            else
                return ColorLib.rgb(255, 255, 255);
        	
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
            return FontLib.getFont("SansSerif",Font.PLAIN,10);
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
}
