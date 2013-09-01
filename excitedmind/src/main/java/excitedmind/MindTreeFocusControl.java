package excitedmind;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.data.expression.Predicate;
import prefuse.data.tuple.TupleSet;
import prefuse.util.StringLib;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;


/**
 * <p>Updates the contents of a TupleSet of focus items in response to mouse
 * actions. For example, clicking a node or double-clicking a node could
 * update its focus status. This Control supports monitoring a specified
 * number of clicks to executing a focus change. By default a click pattern
 * will cause a VisualItem to become the sole member of the focus group.
 * Hold down the control key while clicking to add an item to a group
 * without removing the current members.</p>
 * 
 * <p>Updating a focus group does not necessarily cause
 * the display to change. For this functionality, either register an action
 * with this control, or register a TupleSetListener with the focus group.
 * </p>
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindTreeFocusControl extends ControlAdapter {

    private String group = Visualization.FOCUS_ITEMS;
    protected String activity;
    protected VisualItem curFocus;
    protected int button = Control.LEFT_MOUSE_BUTTON;
    protected InGroupPredicate filter = null;
    MindTree m_mindTree;
    
    
    /**
     * Creates a new FocusControl that changes the focus when an item is 
     * clicked the specified number of times. A click value of zero indicates
     * that the focus should be changed in response to mouse-over events.
     * @param act an action run to upon focus change 
     */
    public MindTreeFocusControl(MindTree mindTree, String nodeGroup, String act) {
    	m_mindTree = mindTree;
    	filter = new InGroupPredicate (nodeGroup);
        activity = act;
    }
    
    /**
     * Perform a filtering check on the input item.
     * @param item the item to check against the filter
     * @return true if the item should be considered, false otherwise
     */
    protected boolean filterCheck(VisualItem item) {
        if ( filter == null )
            return true;
        
        try {
            return filter.getBoolean(item);
        } catch ( Exception e ) {
            Logger.getLogger(getClass().getName()).warning(
                e.getMessage() + "\n" + StringLib.getStackTrace(e));
            return false;
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.controls.Control#itemEntered(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemEntered(VisualItem item, MouseEvent e) {
    	if ( !filterCheck(item) ) return;
    	
    	Display d = (Display)e.getSource();
    	d.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    	Visualization vis = item.getVisualization();
    	TupleSet ts = vis.getFocusGroup(group);
    	ts.setTuple(item);
    	curFocus = item;
    	runActivity(vis);
    }
    
    /**
     * @see prefuse.controls.Control#itemExited(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemExited(VisualItem item, MouseEvent e) {
    	if ( !filterCheck(item) ) return;
    	Display d = (Display)e.getSource();
    	d.setCursor(Cursor.getDefaultCursor());
    	curFocus = null;
    	Visualization vis = item.getVisualization();
    	TupleSet ts = vis.getFocusGroup(group);
    	ts.removeTuple(item);
    	runActivity(vis);
    }
    
    /**
     * @see prefuse.controls.Control#itemClicked(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemClicked(VisualItem item, MouseEvent e) {
        if ( !filterCheck(item) ) return;
        System.out.println ("mouse Clicked");
        
        /*
        if (item.isInGroup(MindView.sm_treeNodesGroupName))
        {
            m_mindTree.ToggleFoldNode(item);
        }
        */
    }
    
    private void runActivity(Visualization vis) {
        if ( activity != null ) {
            vis.run(activity);
        }
    }
    
} // end of class FocusControl
