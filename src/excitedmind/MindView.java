package excitedmind;

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
import prefuse.visual.tuple.TableNodeItem;

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
    
    public static final String sm_layoutAction = "layoutAction";
    
    
    private NodeLinkTreeLayout m_treeLayout;
    
    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;

    private int m_orientation = Constants.ORIENT_LEFT_RIGHT;
    
    private MindTree m_mindTree;
    private TableNodeItem m_curFocus;
    
    private double m_clickedItemX;
    private double m_clickedItemY;
    
    public MindView(String path, Object rootId) {
        super(new Visualization());
        setSize(700, 600);
        
        m_mindTree = new MindTree(path, rootId);
        m_vis.add(sm_treeGroupName, m_mindTree.m_tree);
        setItemSorter(new TreeDepthItemSorter());

        setPefuseAction ();
        
        setMouseControlListener ();
        setKeyControlListener ();

        m_vis.run(sm_layoutAction);
    }
    
    boolean m_needPan;
    private void setMouseControlListener ()
    {
        addControlListener(new ZoomToFitControl());
        addControlListener(new ZoomControl());
        addControlListener(new WheelZoomControl());
        addControlListener(new PanControl());
        addControlListener(new ControlAdapter() {
        	
        	//TODO
//        addControlListener(new FocusControl(1, sm_layoutAction));
        	public void itemEntered(VisualItem item, MouseEvent e) {
        		if (item.isInGroup(sm_treeNodesGroupName))
        		{
	    			System.out.println ("mouse entered");
        			m_curFocus = (TableNodeItem)item;
	        		m_vis.run(sm_layoutAction);
        		}
        		
        	}
        	
        	public void itemClicked(VisualItem item, MouseEvent e) {
    			System.out.println ("mouse Clicked");
    			m_curFocus = (TableNodeItem)item;
    			
        		if (item.isInGroup(sm_treeNodesGroupName))
        		{
	    			m_clickedItemX = item.getX();
	    			m_clickedItemY = item.getY();
    			
	        		m_mindTree.ToggleFoldNode(item);
	        		m_vis.run(sm_layoutAction);
	        		
	        		m_vis.invalidateAll();
	        		m_needPan = true;
        		}
        	} 
        }
        );
    	
    }
    
    
    private void setPefuseAction ()
    {
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

    	m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);

    	m_edgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
    	m_edgeRenderer.setHorizontalAlignment2(Constants.LEFT);

    	m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
    	m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);

    	m_treeLayout.setOrientation(Constants.ORIENT_LEFT_RIGHT);
    }

    
    
    public void setKeyControlListener ()
    {
        registerKeyboardAction(new EditAction(this), "edit", KeyStroke.getKeyStroke("F2"), WHEN_FOCUSED);
        registerKeyboardAction(new RemoveAction(this), "remove", KeyStroke.getKeyStroke("DELETE"), WHEN_FOCUSED);
        
        registerKeyboardAction(new ActionListener() {

        	@Override
        	public void actionPerformed(ActionEvent e) {
        		if (m_undoManager.canUndo())
	        		m_undoManager.undo();

        	}
        }, 
        "back", 
        KeyStroke.getKeyStroke("F3"), 
        WHEN_FOCUSED);
        
        registerKeyboardAction(new ActionListener() {

        	@Override
        	public void actionPerformed(ActionEvent e) {
        		if (m_undoManager.canRedo())
	        		m_undoManager.redo();

        	}
        }, 
        "redo", 
        KeyStroke.getKeyStroke("F4"), 
        WHEN_FOCUSED);
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
    
    private void addItemPositionActions ()
    {
        
        m_treeLayout = new NodeLinkTreeLayout(sm_treeGroupName,
                m_orientation, 50, 0, 8);
        //must set the anchor, if not, the anchor will move to the center of display, every time.
        m_treeLayout.setLayoutAnchor(new Point2D.Double(25, 300));

        
        ActionList actions = new ActionList();
        actions.add(m_treeLayout);
        actions.add(new HoldFocusItemPanAction());
        
        m_vis.putAction(sm_itemPositionActions, actions);
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
    
    private UndoManager m_undoManager = new UndoManager();
    
    public UndoManager getUndoManager ()
    {
    	return m_undoManager;
    }
    
    public MindTree getMindTree ()
    {
    	return m_mindTree;
    }
    
    public TableNodeItem getFocusNode ()
    {
    	return m_curFocus;
    }

} // end of class TreeMap
