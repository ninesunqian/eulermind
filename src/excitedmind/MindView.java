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

        //TODO  setPefuseAction ();
        
        setMouseControlListener ();
        setKeyControlListener ();

        //TODO  m_vis.run(sm_layoutAction);
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

} // end of class TreeMap
