package excitedmind;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import prefuse.Constants;
import prefuse.Display;
import prefuse.action.layout.graph.TreeLayout;
import prefuse.data.Graph;
import prefuse.data.Schema;
import prefuse.data.tuple.TupleSet;
import prefuse.util.ArrayLib;
import prefuse.visual.NodeItem;

/**
 * <p>TreeLayout that computes a tidy layout of a node-link tree
 * diagram. This algorithm lays out a rooted tree such that each
 * depth level of the tree is on a shared line. The orientation of the
 * tree can be set such that the tree goes left-to-right (default),
 * right-to-left, top-to-bottom, or bottom-to-top.</p>
 * 
 * <p>The algorithm used is that of Christoph Buchheim, Michael J�nger,
 * and Sebastian Leipert from their research paper
 * <a href="http://citeseer.ist.psu.edu/buchheim02improving.html">
 * Improving Walker's Algorithm to Run in Linear Time</a>, Graph Drawing 2002.
 * This algorithm corrects performance issues in Walker's algorithm, which
 * generalizes Reingold and Tilford's method for tidy drawings of trees to
 * support trees with an arbitrary number of children at any given node.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MindTreeLayout extends TreeLayout {
    
    private int    m_orientation;  // the orientation of the tree
    private double m_bspace = 20;   // the spacing between sibling nodes
    private double m_tspace = 20;  // the spacing between subtrees
    private double m_dspace = 20;  // the spacing between depth levels
    private double m_offset = 20;  // pixel offset for root node position
    
    private double m_ax, m_ay; // for holding anchor co-ordinates
    
    
    /**
     * Create a new NodeLinkTreeLayout. A left-to-right orientation is assumed.
     * @param group the data group to layout. Must resolve to a Graph instance.
     */
    public MindTreeLayout(String group) {
        super(group);
        m_orientation = Constants.ORIENT_LEFT_RIGHT;
    }
    
    /**
     * Create a new NodeLinkTreeLayout.
     * @param group the data group to layout. Must resolve to a Graph instance.
     * @param orientation the orientation of the tree layout. One of
     * {@link prefuse.Constants#ORIENT_LEFT_RIGHT},
     * {@link prefuse.Constants#ORIENT_RIGHT_LEFT},
     * {@link prefuse.Constants#ORIENT_TOP_BOTTOM}, or
     * {@link prefuse.Constants#ORIENT_BOTTOM_TOP}.
     * @param dspace the spacing to maintain between depth levels of the tree
     * @param bspace the spacing to maintain between sibling nodes
     * @param tspace the spacing to maintain between neighboring subtrees
     */
    public MindTreeLayout(String group, int orientation,
            double dspace, double bspace, double tspace)
    {
        super(group);
        m_orientation = orientation;
        
        m_dspace = dspace;
        m_bspace = bspace;
        m_tspace = tspace;
        
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Set the orientation of the tree layout.
     * @param orientation the orientation value. One of
     * {@link prefuse.Constants#ORIENT_LEFT_RIGHT},
     * {@link prefuse.Constants#ORIENT_RIGHT_LEFT},
     * {@link prefuse.Constants#ORIENT_TOP_BOTTOM}, or
     * {@link prefuse.Constants#ORIENT_BOTTOM_TOP}.
     */
    public void setOrientation(int orientation) {
        if ( orientation < 0 || 
             orientation >= Constants.ORIENTATION_COUNT ||
             orientation == Constants.ORIENT_CENTER )
        {
            throw new IllegalArgumentException(
                "Unsupported orientation value: "+orientation);
        }
        m_orientation = orientation;
    }
    
    /**
     * Get the orientation of the tree layout.
     * @return the orientation value. One of
     * {@link prefuse.Constants#ORIENT_LEFT_RIGHT},
     * {@link prefuse.Constants#ORIENT_RIGHT_LEFT},
     * {@link prefuse.Constants#ORIENT_TOP_BOTTOM}, or
     * {@link prefuse.Constants#ORIENT_BOTTOM_TOP}.
     */
    public int getOrientation() {
        return m_orientation;
    }
    
    /**
     * Set the spacing between depth levels.
     * @param d the depth spacing to use
     */
    public void setDepthSpacing(double d) {
        m_dspace = d;
    }
    
    /**
     * Get the spacing between depth levels.
     * @return the depth spacing
     */
    public double getDepthSpacing() {
        return m_dspace;
    }
    
    /**
     * Set the spacing between neighbor nodes.
     * @param b the breadth spacing to use
     */
    public void setBreadthSpacing(double b) {
        m_bspace = b;
    }
    
    /**
     * Get the spacing between neighbor nodes.
     * @return the breadth spacing
     */
    public double getBreadthSpacing() {
        return m_bspace;
    }
    
    /**
     * Set the spacing between neighboring subtrees.
     * @param s the subtree spacing to use
     */
    public void setSubtreeSpacing(double s) {
        m_tspace = s;
    }
    
    /**
     * Get the spacing between neighboring subtrees.
     * @return the subtree spacing
     */
    public double getSubtreeSpacing() {
        return m_tspace;
    }
    
    /**
     * Set the offset value for placing the root node of the tree. The
     * dimension in which this offset is applied is dependent upon the
     * orientation of the tree. For example, in a left-to-right orientation,
     * the offset will a horizontal offset from the left edge of the layout
     * bounds.
     * @param o the value by which to offset the root node of the tree
     */
    public void setRootNodeOffset(double o) {
        m_offset = o;
    }
    
    /**
     * Get the offset value for placing the root node of the tree.
     * @return the value by which the root node of the tree is offset
     */
    public double getRootNodeOffset() {
        return m_offset;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.layout.Layout#getLayoutAnchor()
     */
    public Point2D getLayoutAnchor() {
        if ( m_anchor != null )
            return m_anchor;
        
        m_tmpa.setLocation(0,0);
        if ( m_vis != null ) {
            Display d = m_vis.getDisplay(0);
            Rectangle2D b = this.getLayoutBounds();
            switch ( m_orientation ) {
            case Constants.ORIENT_LEFT_RIGHT:
                m_tmpa.setLocation(m_offset, d.getHeight()/2.0);
                break;
            case Constants.ORIENT_RIGHT_LEFT:
                m_tmpa.setLocation(b.getMaxX()-m_offset, d.getHeight()/2.0);
                break;
            case Constants.ORIENT_TOP_BOTTOM:
                m_tmpa.setLocation(d.getWidth()/2.0, m_offset);
                break;
            case Constants.ORIENT_BOTTOM_TOP:
                m_tmpa.setLocation(d.getWidth()/2.0, b.getMaxY()-m_offset);
                break;
            }
            d.getInverseTransform().transform(m_tmpa, m_tmpa);
        }
        return m_tmpa;
    }
    
    private double spacing(NodeItem l, NodeItem r, boolean siblings) {
        boolean w = ( m_orientation == Constants.ORIENT_TOP_BOTTOM ||
                      m_orientation == Constants.ORIENT_BOTTOM_TOP );
        return (siblings ? m_bspace : m_tspace);
    }
    
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        Graph g = (Graph)m_vis.getGroup(m_group);
        initSchema(g.getNodes());
        
        Point2D a = getLayoutAnchor();
        m_ax = a.getX();
        m_ay = a.getY();
        
        NodeItem root = getLayoutRoot();
        Params rp = getParams(root);

		g.getSpanningTree(root);
        
        // do first pass - compute breadth information, collect depth info
        firstWalk(root, 0, 1);
        
        // do second pass - assign layout positions
        secondWalk(root, null, -rp.prelim, 0);
    }

    private void firstWalk(NodeItem n, int num, int depth) {
        Params np = getParams(n);
        
        boolean v = ( m_orientation == Constants.ORIENT_TOP_BOTTOM ||
                      m_orientation == Constants.ORIENT_BOTTOM_TOP );
        
        Rectangle2D nBounds = n.getBounds();
        np.deepSize = v ? nBounds.getHeight() : nBounds.getWidth();
        
        if (depth == 1) {
        	np.depth = 0;
        }
        else {
        	Params pp = getParams((NodeItem)n.getParent());
	        np.depth = pp.depth + pp.deepSize + m_dspace;
        }
        
        boolean expanded = n.isExpanded();
        if ( n.getChildCount() == 0 || !expanded ) // is leaf
        { 
            NodeItem l = (NodeItem)n.getPreviousSibling();
            if ( l == null ) {
                np.prelim = 0;
                np.mod = 0;
            } else {
            	Params lp = getParams(l);
        		np.mod = lp.mod + lp.breadth;
        		np.prelim = np.mod;
            }
            
        	np.breadth = v ? nBounds.getWidth() : nBounds.getHeight() + m_bspace;
        }
        else if ( expanded )
        {
        	NodeItem leftMost = (NodeItem)n.getFirstChild();
        	NodeItem rightMost = (NodeItem)n.getLastChild();
        	//NodeItem defaultAncestor = leftMost;
        	NodeItem c = leftMost;
        	for ( int i=0; c != null; ++i, c = (NodeItem)c.getNextSibling() )
        	{
        		firstWalk(c, i, depth+1);
        		//defaultAncestor = apportion(c, defaultAncestor);
        	}

        	double midpoint = 0.5 *
        	(getParams(leftMost).prelim + getParams(rightMost).prelim);

        	NodeItem left = (NodeItem)n.getPreviousSibling();

        	if ( left != null ) {
        		Params lp = getParams(left);
    			np.mod = lp.mod + lp.breadth + spacing(left, n, true);
    			np.prelim = np.mod + midpoint;
        	} else {
        		np.prelim = midpoint;
        	}

        	Params lastChildParams = getParams(rightMost);
        	np.breadth = lastChildParams.mod + lastChildParams.breadth;
        }
    }
    
    private void secondWalk(NodeItem n, NodeItem p, double m, int depth) {
        Params np = getParams(n);
        setBreadth(n, p, np.prelim + m);
        
    	setDepth(n, p, np.depth);
        
        if ( n.isExpanded() ) {
            depth += 1;
            for ( NodeItem c = (NodeItem)n.getFirstChild();
                  c != null; c = (NodeItem)c.getNextSibling() )
            {
                secondWalk(c, n, m + np.mod, depth);
            }
        }
        
        //np.clear();
    }
    
    private void setBreadth(NodeItem n, NodeItem p, double b) {
        switch ( m_orientation ) {
        case Constants.ORIENT_LEFT_RIGHT:
        case Constants.ORIENT_RIGHT_LEFT:
            setY(n, p, m_ay + b);
            break;
        case Constants.ORIENT_TOP_BOTTOM:
        case Constants.ORIENT_BOTTOM_TOP:
            setX(n, p, m_ax + b);
            break;
        default:
            throw new IllegalStateException();
        }
    }
    
    private void setDepth(NodeItem n, NodeItem p, double d) {
        switch ( m_orientation ) {
        case Constants.ORIENT_LEFT_RIGHT:
            setX(n, p, m_ax + d);
            break;
        case Constants.ORIENT_RIGHT_LEFT:
            setX(n, p, m_ax - d);
            break;
        case Constants.ORIENT_TOP_BOTTOM:
            setY(n, p, m_ay + d);
            break;
        case Constants.ORIENT_BOTTOM_TOP:
            setY(n, p, m_ay - d);
            break;
        default:
            throw new IllegalStateException();
        }
    }
    
    // ------------------------------------------------------------------------
    // Params Schema
    
    /**
     * The data field in which the parameters used by this layout are stored.
     */
    public static final String PARAMS = "_reingoldTilfordParams";
    /**
     * The schema for the parameters used by this layout.
     */
    public static final Schema PARAMS_SCHEMA = new Schema();
    static {
        PARAMS_SCHEMA.addColumn(PARAMS, Params.class);
    }
    
    protected void initSchema(TupleSet ts) {
        ts.addColumns(PARAMS_SCHEMA);
    }
    
    public static Params getParams(NodeItem item) {
        Params rp = (Params)item.get(PARAMS);
        if ( rp == null ) {
            rp = new Params();
            item.set(PARAMS, rp);
        }
        
        return rp;
    }
    
    /**
     * Wrapper class holding parameters used for each node in this layout.
     */
    public static class Params implements Cloneable {
    	//the root node position (left top point of node) of subtree in siblings forest area
        public double prelim;
    	//the top position of subtree in siblings forest area. suppose m_orientation is left-to-right
        public double mod;
        //the breadth of subtree 
        public double breadth;
        
        public double shift;
        public double change;
        
        // for not aligned tree, the position in deep direction
        public double deepSize;
        // for not aligned tree, the size in deep direction
        double depth;
        
        public void clear() {
            prelim = mod = shift = change = 0;
        }
    }
    
} // end of class NodeLinkTreeLayout
