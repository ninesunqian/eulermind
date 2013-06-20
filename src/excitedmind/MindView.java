package excitedmind;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;

import excitedmind.operators.EditAction;
import excitedmind.operators.RemoveAction;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.distortion.Distortion;
import prefuse.action.distortion.FisheyeDistortion;
import prefuse.action.layout.Layout;
import prefuse.controls.AnchorUpdateControl;
import prefuse.controls.ControlAdapter;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.util.ColorLib;
import prefuse.util.PrefuseLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.TreeDepthItemSorter;
import prefuse.visual.tuple.TableNodeItem;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindView extends Display {

	public final String m_treeGroupName = "tree";
    public final String m_treeNodesGroupName;
    public final String m_treeEdgesGroupName;
    
    private MindTree m_mindTree;
    private TableNodeItem m_curFocus;
    
    private double m_clickedItemX;
    private double m_clickedItemY;
    
    MindTreeRenderEngine m_renderEngine;
    
    
    /** The data group name of menu items. */
    public static final String ITEMS = "items";
    /** The label data field for menu items. */
    public static final String LABEL = "label";
    /** The action data field for menu items. */
    public static final String ACTION = "action";
    /**
     * This schema holds the data representation for internal storage of
     * menu items.
     */
    protected static final Schema ITEM_SCHEMA = new Schema();
    static {
        ITEM_SCHEMA.addColumn(MindTree.sm_textPropName, String.class);
    }
    
    private Table m_items = ITEM_SCHEMA.instantiate(); // table of menu items
    
    private double m_maxHeight = 500; // maximum menu height in pixels
    private double m_scale = 7;       // scale parameter for fisheye distortion
    
    
     
    
    public MindView(String path, Object rootId) {
        super(new Visualization());
        setSize(700, 600);
        
        setHighQuality(true);
        
        m_mindTree = new MindTree(path, rootId);
		m_treeNodesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.NODES); 
		m_treeEdgesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.EDGES); 
        
        m_vis.add(m_treeGroupName, m_mindTree.m_tree);
        setItemSorter(new TreeDepthItemSorter());
        
        //history
        m_vis.addTable(ITEMS, m_items);

        m_renderEngine = new MindTreeRenderEngine(this, m_treeGroupName);
        
        setMouseControlListener ();
        setKeyControlListener ();
        
        renderTree ();
        
        {
        	addHistoryItem();
        	// text color function
        	// items with the mouse over printed in red, otherwise black
        	
        	ColorAction colors = new ColorAction(ITEMS, VisualItem.TEXTCOLOR);
        	colors.setDefaultColor(ColorLib.gray(0));
        	colors.add("hover()", ColorLib.rgb(255,0,0));

        	// initial layout and coloring
        	ActionList init = new ActionList();
        	init.add(new VerticalLineLayout(ITEMS, m_maxHeight));
        	init.add(colors);
        	init.add(new RepaintAction());
        	m_vis.putAction("init", init);

        	// fisheye distortion based on the current anchor location
        	ActionList distort = new ActionList();
        	Distortion feye = new FisheyeDistortion(0,m_scale);
        	distort.add(feye);
        	distort.add(colors);
        	distort.add(new RepaintAction());
        	m_vis.putAction("distort", distort);

        	// update the distortion anchor position to be the current
        	// location of the mouse pointer
        	addControlListener(new AnchorUpdateControl(feye, "distort"));	
        	
        	m_vis.run("init");
        }
    }
    
    
    
    //add history node
    public void addHistoryItem() {
    	for (int i=0; i<20; i++)
    	{
    		int row = m_items.addRow();
    		m_items.set(row, MindTree.sm_textPropName, Integer.toString(i));
    	}
    }
    
    public boolean isNode (VisualItem item)
    {
    	return item.isInGroup(m_treeNodesGroupName);
    }
    
    public boolean isEdge (VisualItem item)
    {
    	return item.isInGroup(m_treeEdgesGroupName);
    }
    
    public void renderTree ()
    {
        m_renderEngine.run ();
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
        		if (isNode(item))
        		{
	    			System.out.println ("mouse entered");
        			m_curFocus = (TableNodeItem)item;
        			renderTree();
        			
        			Rectangle2D bounds = item.getBounds();
        			MindTreeLayout.Params params = MindTreeLayout.getParams((NodeItem)item);
        			System.out.println ("    mod = " + params.mod);
        			System.out.println ("    prelim = " + params.prelim);
        			System.out.println ("    breadth = " + params.breadth);
        			System.out.println ("    bounds.y = " + bounds.getY());
        			
        			
        		}
        		
        	}
        	
        	public void itemClicked(VisualItem item, MouseEvent e) {
    			System.out.println ("mouse Clicked");
    			m_curFocus = (TableNodeItem)item;
    			
        		if (isNode(item))
        		{
        			m_renderEngine.holdItem(item);
    			
	        		m_mindTree.ToggleFoldNode(item);
        			renderTree();
	        		
        		}
        	} 
        }
        );
    	
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
    
    
    
    /** history
     * Lines up all VisualItems vertically. Also scales the size such that
     * all items fit within the maximum layout size, and updates the
     * Display to the final computed size.
     */
    public class VerticalLineLayout extends Layout {
        private double m_maxHeight = 600;
        
        public VerticalLineLayout(String group, double maxHeight) {
        	super (group);
            m_maxHeight = maxHeight;
        }
        
        public void run(double frac) {
            // first pass
            double w = 0, h = 0;
            Iterator iter = m_vis.items(m_group);
            while ( iter.hasNext() ) {
                VisualItem item = (VisualItem)iter.next();
                item.setSize(1.0);
                h += item.getBounds().getHeight();
            }
            double scale = h > m_maxHeight ? m_maxHeight/h : 1.0;
            
            Display d = m_vis.getDisplay(0);
            Insets ins = d.getInsets();
            
            // second pass
            h = ins.top;
            double ih, y=0, x=ins.left;
            iter = m_vis.items(m_group);
            while ( iter.hasNext() ) {
                VisualItem item = (VisualItem)iter.next();
                item.setSize(scale); item.setEndSize(scale);
                Rectangle2D b = item.getBounds();
                
                w = Math.max(w, b.getWidth());
                ih = b.getHeight();
                y = h+(ih/2);
                setX(item, null, x);
                setY(item, null, y);
                h += ih;
            }
            
            /*
            // set the display size to fit text
            setSize(d, (int)Math.round(2*m_scale*w + ins.left + ins.right),
                       (int)Math.round(h + ins.bottom));
                       */
        }
        
        private void setSize(final Display d, final int width, final int height)
        {
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
        			d.setSize(width, height);
        		}
        	});
        }
    } // end of inner class VerticalLineLayout

} // end of class TreeMap
