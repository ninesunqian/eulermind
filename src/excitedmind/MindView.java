package excitedmind;

import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;

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
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindView extends Display {

	public static final String sm_treeGroupName = "tree";
    public static final String sm_treeNodesGroupName = "tree.nodes";
    public static final String sm_treeEdgesGroupName = "tree.edges";
    
    private static final String sm_itemStyleActions = "itemStyleActions";
    private static final String sm_itemPositionActions = "itemPositionActions";
    
    private static final String sm_searchAnimator = "searchAnimator";
    private static final String sm_orientAnimator = "orientAnimator";

    private static final String sm_layoutAction = "layoutAction";
    private static final String sm_layoutAnimator = "layoutAnimator";
    
    
    private NodeLinkTreeLayout m_treeLayout;
    private CollapsedSubtreeLayout m_subTreeLayout;
    
    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;

    private int m_orientation = Constants.ORIENT_LEFT_RIGHT;
    
    private MindTree m_mindTree;
    private VisualItem m_curFocus;
    
    public MindView(String path, Object rootId) {
        super(new Visualization());
        
        m_mindTree = new MindTree(path, rootId);

        m_vis.add(sm_treeGroupName, m_mindTree.m_tree);

        setPefuseAction ();

        setSearchAnimation ();
        
        // initialize the display
        setSize(700, 600);
        setItemSorter(new TreeDepthItemSorter());
        
        addControlListener(new ZoomToFitControl());
        addControlListener(new ZoomControl());
        addControlListener(new WheelZoomControl());
        addControlListener(new PanControl());
        addControlListener(new ControlAdapter() {
        	
        	public void itemEntered(VisualItem item, MouseEvent e) {
        		if (item.isInGroup(sm_treeNodesGroupName))
        		{
        			m_vis.cancel(sm_layoutAnimator);
	    			System.out.println ("mouse entered");
        			m_curFocus = item;
	        		m_vis.run(sm_layoutAction);
        		}
        		
        	}
        	
        	public void itemClicked(VisualItem item, MouseEvent e) {
    			System.out.println ("mouse Clicked");
    			
        		if (item.isInGroup(sm_treeNodesGroupName))
        		{
        			m_vis.cancel(sm_layoutAnimator);
	        		m_mindTree.ToggleFoldNode(item);
	        		m_vis.run(sm_layoutAction);
        		}
        	} 
        }
        );
//        addControlListener(new FocusControl(1, sm_layoutAction));
        
        
        setKey ();

        // filter graph and perform layout
        setOrientation(m_orientation);
        m_vis.run(sm_layoutAction);
    }
    
    private void setPefuseAction ()
    {
        setItemRenderer ();

        addItemStyleActions();
        addItemPositionActions();

        // quick repaint
        ActionList repaint = new ActionList();
        repaint.add(m_vis.getAction(sm_itemPositionActions));
        addRepaintActionsTo(repaint);
        m_vis.putAction("repaint", repaint);

        //search animate
        ActionList searchAnimate = new ActionList(400);
        addItemStyleAnimatorsTo(searchAnimate);
        addRepaintAnimatorsTo(searchAnimate);
        m_vis.putAction(sm_searchAnimator, searchAnimate);

        // create animator for orientation changes
        ActionList orientAnimator = new ActionList(2000);
        addItemPositionAnimatorsTo(orientAnimator);
        addRepaintAnimatorsTo(orientAnimator);
        m_vis.putAction(sm_orientAnimator, orientAnimator);

        // create the filtering and layout
        ActionList layoutAction = new ActionList();
        layoutAction.add(m_vis.getAction(sm_itemPositionActions));
        layoutAction.add(m_vis.getAction(sm_itemStyleActions));
        m_vis.putAction(sm_layoutAction, layoutAction);

        ActionList layoutAnimator = new ActionList(500);
        layoutAnimator.setPacingFunction(new SlowInSlowOutPacer());
        addItemStyleAnimatorsTo(layoutAnimator);
        addItemPositionAnimatorsTo(layoutAnimator);
        addRepaintAnimatorsTo(layoutAnimator);
        m_vis.putAction(sm_layoutAnimator, layoutAnimator);
        
        m_vis.alwaysRunAfter(sm_layoutAction, sm_layoutAnimator);
    	
    }
    
    private void setItemRenderer ()
    {
        m_nodeRenderer = new LabelRenderer(MindTree.sm_textPropName);
        m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
        m_nodeRenderer.setRoundedCorner(8, 8);
        
        m_edgeRenderer = new EdgeRenderer(Constants.EDGE_TYPE_CURVE);

        DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
        rf.add(new InGroupPredicate(sm_treeEdgesGroupName), m_edgeRenderer);
        m_vis.setRendererFactory(rf);
    }

    
    private void setSearchAnimation ()
    {
        TupleSet search = new PrefixSearchTupleSet();
        m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);
        search.addTupleSetListener(new TupleSetListener() {
            public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
                m_vis.cancel(sm_searchAnimator);
                m_vis.run(sm_itemStyleActions);
                m_vis.run(sm_searchAnimator);
            }
        });
    	
    }
    
    public void setKey ()
    {
        registerKeyboardAction(new OrientAction(Constants.ORIENT_LEFT_RIGHT),
                "left-to-right", KeyStroke.getKeyStroke("ctrl 1"), WHEN_FOCUSED);
        registerKeyboardAction(new OrientAction(Constants.ORIENT_TOP_BOTTOM),
                "top-to-bottom", KeyStroke.getKeyStroke("ctrl 2"), WHEN_FOCUSED);
        registerKeyboardAction(new OrientAction(Constants.ORIENT_RIGHT_LEFT),
                "right-to-left", KeyStroke.getKeyStroke("ctrl 3"), WHEN_FOCUSED);
        registerKeyboardAction(new OrientAction(Constants.ORIENT_BOTTOM_TOP),
                "bottom-to-top", KeyStroke.getKeyStroke("ctrl 4"), WHEN_FOCUSED);
    }

    public void setOrientation(int orientation) {
        switch (orientation) {
        case Constants.ORIENT_LEFT_RIGHT:
            m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
            m_edgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
            m_edgeRenderer.setHorizontalAlignment2(Constants.LEFT);
            m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
            m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
            break;
        case Constants.ORIENT_RIGHT_LEFT:
            m_nodeRenderer.setHorizontalAlignment(Constants.RIGHT);
            m_edgeRenderer.setHorizontalAlignment1(Constants.LEFT);
            m_edgeRenderer.setHorizontalAlignment2(Constants.RIGHT);
            m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
            m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
            break;
        case Constants.ORIENT_TOP_BOTTOM:
            m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
            m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
            m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
            m_edgeRenderer.setVerticalAlignment1(Constants.BOTTOM);
            m_edgeRenderer.setVerticalAlignment2(Constants.TOP);
            break;
        case Constants.ORIENT_BOTTOM_TOP:
            m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
            m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
            m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
            m_edgeRenderer.setVerticalAlignment1(Constants.TOP);
            m_edgeRenderer.setVerticalAlignment2(Constants.BOTTOM);
            break;
        default:
            throw new IllegalArgumentException(
                    "Unrecognized orientation value: " + orientation);
        }
        m_orientation = orientation;
        m_treeLayout.setOrientation(orientation);
        m_subTreeLayout.setOrientation(orientation);
    }

    public int getOrientation() {
        return m_orientation;
    }

    // ------------------------------------------------------------------------

    public class OrientAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		
		private int orientation;

        public OrientAction(int orientation) {
            this.orientation = orientation;
        }

        public void actionPerformed(ActionEvent evt) {
            setOrientation(orientation); 
            getVisualization().cancel(sm_orientAnimator);
            getVisualization().run(sm_itemPositionActions);
            getVisualization().run(sm_orientAnimator);
        }
    }

    public class AutoPanAction extends Action {
        private Point2D m_start = new Point2D.Double();
        private Point2D m_end = new Point2D.Double();
        private Point2D m_cur = new Point2D.Double();
        private int m_bias = 150;

        public void run(double frac) {
            TupleSet ts = m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
            if (ts.getTupleCount() == 0)
                return;

            if (frac == 0.0) {
                int xbias = 0, ybias = 0;
                switch (m_orientation) {
                case Constants.ORIENT_LEFT_RIGHT:
                    xbias = m_bias;
                    break;
                case Constants.ORIENT_RIGHT_LEFT:
                    xbias = -m_bias;
                    break;
                case Constants.ORIENT_TOP_BOTTOM:
                    ybias = m_bias;
                    break;
                case Constants.ORIENT_BOTTOM_TOP:
                    ybias = -m_bias;
                    break;
                }

                VisualItem vi = (VisualItem) ts.tuples().next();
                m_cur.setLocation(getWidth() / 2, getHeight() / 2);
                getAbsoluteCoordinate(m_cur, m_start);
                m_end.setLocation(vi.getX() + xbias, vi.getY() + ybias);
            } else {
                m_cur.setLocation(m_start.getX() + frac
                        * (m_end.getX() - m_start.getX()), m_start.getY()
                        + frac * (m_end.getY() - m_start.getY()));
                panToAbs(m_cur);
            }
        }
    }
    
    private void addItemPositionActions ()
    {
        m_subTreeLayout = new CollapsedSubtreeLayout(sm_treeGroupName, m_orientation);
        
        m_treeLayout = new NodeLinkTreeLayout(sm_treeGroupName,
                m_orientation, 50, 0, 8);
        m_treeLayout.setLayoutAnchor(new Point2D.Double(25, 300));

        
        ActionList actions = new ActionList();
        actions.add(m_treeLayout);
        actions.add(m_subTreeLayout);
        
        m_vis.putAction(sm_itemPositionActions, actions);
    }
    
    private void addItemPositionAnimatorsTo (ActionList animators)
    {
        animators.add(new VisibilityAnimator(sm_treeGroupName));
        //animators.add(new AutoPanAction());
        animators.add(new LocationAnimator(sm_treeNodesGroupName));
    }
    
    private void addItemStyleActions ()
    {
    	ItemAction nodeFont = new FontAction(sm_treeNodesGroupName, FontLib.getFont("Tahoma", 16));
    	ItemAction nodeColor = new NodeColorAction(sm_treeNodesGroupName);
    	ItemAction textColor = new ColorAction(sm_treeNodesGroupName, VisualItem.TEXTCOLOR, ColorLib.rgb(0, 0, 0));

    	ItemAction edgeColor = new ColorAction(sm_treeEdgesGroupName, VisualItem.STROKECOLOR, 
    			ColorLib.rgb(200, 200, 200));

        ActionList actions = new ActionList();
        
    	actions.add(nodeFont);
    	actions.add(nodeColor);
    	actions.add(textColor);
    	
    	actions.add(edgeColor);
    	
        m_vis.putAction(sm_itemStyleActions, actions);
    }
    
    private void addItemStyleAnimatorsTo (ActionList animators)
    {
    	ItemAction nodeFontAnimator = new FontAnimator(sm_treeNodesGroupName);
    	ItemAction nodeColorAnimator = new ColorAnimator(sm_treeNodesGroupName);

    	animators.add(nodeFontAnimator);
    	animators.add(nodeColorAnimator);
    }
    
    private void addRepaintActionsTo (ActionList animators)
    {
    	animators.add(new RepaintAction());
    }
    
    private void addRepaintAnimatorsTo (ActionList animators)
    {
        animators.add(new QualityControlAnimator());
    	animators.add(new RepaintAction());
    }

    public class NodeColorAction extends ColorAction {

        public NodeColorAction(String group) {
            super(group, VisualItem.FILLCOLOR);
        }

        public int getColor(VisualItem item) {
        	if (item == m_curFocus)
                return ColorLib.rgb(255, 255, 0);
        	else if (m_vis.isInGroup(item, Visualization.SEARCH_ITEMS))
                return ColorLib.rgb(255, 0, 0);
            else if (m_vis.isInGroup(item, Visualization.FOCUS_ITEMS))
                return ColorLib.rgb(0, 255, 0);
            else
                return ColorLib.rgb(255, 255, 255);
        }

    } // end of inner class TreeMapColorAction
    
    public UndoManager m_undoManager = new UndoManager();

} // end of class TreeMap
