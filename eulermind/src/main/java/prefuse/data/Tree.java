package prefuse.data;

import java.util.Iterator;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import prefuse.data.event.TreeRootChangeListener;
import prefuse.data.expression.Predicate;
import prefuse.util.PrefuseConfig;
import prefuse.util.collections.CopyOnWriteArrayList;
import prefuse.util.collections.IntIterator;

/**
 * <p>Graph subclass that models a tree structure of hierarchical
 * parent-child relationships. For each edge, the source node is considered
 * the parent, and the target node is considered the child. For the tree
 * structure to be valid, each node can have at most one parent, and hence
 * only one edge for which that node is the target. In addition to the methods
 * of the Graph class, the tree also supports methods for navigating the tree
 * structure, such as accessing parent or children nodes and next or previous
 * sibling nodes (siblings are children nodes with a shared parent). Unlike the
 * graph class, the default source and target key field names are renamed to
 * {@link #DEFAULT_SOURCE_KEY} and {@link #DEFAULT_TARGET_KEY}.
 * Like the {@link Graph} class, Trees are backed by node and edge
 * tables, and use {@link prefuse.data.Node} and
 * {@link prefuse.data.Edge} instances to provide object-oriented access
 * to nodes and edges.</p>
 * 
 * <p>The Tree class does not currently enforce that the graph structure remain
 * a valid tree. This is to allow a chain of editing operations that may break
 * the tree structure at some point before repairing it. Use the
 * {@link #isValidTree()} method to test the validity of a tree.</p>
 * 
 * <p>By default, the {@link #getSpanningTree()} method simply returns a
 * reference to this Tree instance. However, if a spanning tree is created at a
 * new root u sing the {@link #getSpanningTree(Node)} method, a new
 * {@link SpanningTree} instance is generated.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Tree extends Graph {

    private static final Logger s_logger
        = LoggerFactory.getLogger(Tree.class);
    
    /** Default data field used to denote the source node in an edge table */
    public static final String DEFAULT_SOURCE_KEY 
        = PrefuseConfig.get("data.tree.sourceKey");
    /** Default data field used to denote the target node in an edge table */
    public static final String DEFAULT_TARGET_KEY
        = PrefuseConfig.get("data.tree.targetKey");
    
    // implement as graph with limitations on edge settings
    // catch external modification events and throw exceptions as necessary
    
    /** The node table row number for the root node of the tree. */
    protected int m_root = -1;
    protected int m_really_root = -1;
    
    // ------------------------------------------------------------------------
    // Constructors
    
    private CopyOnWriteArrayList m_rootChangeListeners = new CopyOnWriteArrayList();
    
    /**
     * Create a new, empty Tree.
     */
    public Tree() {
        super(new Table(), false);
    }
    
    /**
     * Create a new Tree.
     * @param nodes the backing table to use for node data.
     * Node instances of this graph will get their data from this table.
     * @param edges the backing table to use for edge data.
     * Edge instances of this graph will get their data from this table.
     */
    public Tree(Table nodes, Table edges) {
        this(nodes, edges, DEFAULT_SOURCE_KEY, DEFAULT_TARGET_KEY);
    }

    /**
     * Create a new Tree.
     * @param nodes the backing table to use for node data.
     * Node instances of this graph will get their data from this table.
     * @param edges the backing table to use for edge data.
     * Edge instances of this graph will get their data from this table.
     * @param sourceKey data field used to denote the source node in an edge
     * table
     * @param targetKey data field used to denote the target node in an edge
     * table
     */
    public Tree(Table nodes, Table edges, String sourceKey, String targetKey) {
        this(nodes, edges, DEFAULT_NODE_KEY, sourceKey, targetKey);
    }
    
    /**
     * Create a new Tree.
     * @param nodes the backing table to use for node data.
     * Node instances of this graph will get their data from this table.
     * @param edges the backing table to use for edge data.
     * Edge instances of this graph will get their data from this table.
     * @param nodeKey data field used to uniquely identify a node. If this
     * field is null, the node table row numbers will be used
     * @param sourceKey data field used to denote the source node in an edge
     * table
     * @param targetKey data field used to denote the target node in an edge
     * table
     */
    public Tree(Table nodes, Table edges, String nodeKey,
            String sourceKey, String targetKey)
    {
        super(nodes, edges, false, nodeKey, sourceKey, targetKey);
        
        for ( IntIterator rows = nodes.rows(); rows.hasNext(); ) {
            int n = rows.nextInt();
            if ( getParent(n) < 0 ) {
                m_root = n;
                break;
            }
        }
        m_really_root = m_root;
    }
    
    /**
     * Internal method for setting the root node.
     * @param root the root node to set
     */
    public void setRoot(Node root) {
    	int oldRoot = m_root;
        m_root = root.getRow();
        
        fireRootChangeEvent (m_root, oldRoot);
    }
        
    /**
     * Internal method for setting the root node.
     * @param root the root node to set
     */
    public void setRoot(int root) {
    	int oldRoot = m_root;
        m_root = root;
        fireRootChangeEvent (m_root, oldRoot);
    }

    /**
     * set the really root of tree
     */
    public void restoreReallyRoot() {
        if (m_really_root != m_root) {
            setRoot(m_really_root);
        }
    }

    /**
     *
     */



    // ------------------------------------------------------------------------
    // Tree Mutators
    
    /**
     * Add a new root node to an empty Tree.
     * @return the node id (node table row number) of the new root node.
     */
    public int addRootRow() {
        if ( getNodeCount() != 0 ) {
            throw new IllegalStateException(
                    "Can only add a root node to an empty tree");
        }
        
        m_root = addNodeRow();
        m_really_root = m_root;
        fireRootChangeEvent (m_root, -1);
        return m_root;
    }

    /**
     * Add a new root node to an empty Tree.
     * @return the newly added root Node
     */
    public Node addRoot() {
        return getNode(addRootRow());
    }
    
    public int addChild(int parent, int pos) {
        int child = super.addNodeRow();
        addChildEdge(parent, child, pos);
        return child;
    }

    public int addParent(int child) {

        if (child == m_really_root) {
            int newParent = super.addNodeRow();
            addChildEdge(newParent, child, 0);

            m_root = newParent;
            m_really_root = m_root;
            fireRootChangeEvent (m_root, child);

            return newParent;

        } else {
            int oldParent = getParent(child);
            int oldParentEdge = getParentEdge(child);
            int oldIndex = getChildIndex(oldParent, child);

            removeEdge(oldParentEdge);

            int newParent = super.addNodeRow();

            addChildEdge(newParent, child, 0);
            addChildEdge(oldParent, newParent, oldIndex);
            return newParent;
        }
    }
    
    /**
     * Add a child node to the given parent node. An edge between the two
     * will also be created.
     * @param parent the parent node id (node table row number)
     * @return the added child node id
     */
    public int addChild(int parent) {
        return addChild(parent, ADDING_EDGE_END);
    }
    
    
    public Node addChild(Node parent, int pos) {
        nodeCheck(parent, true);
        return getNode(addChild(parent.getRow(), pos));
    }
    /**
     * Add a child node to the given parent node. An edge between the two
     * will also be created.
     * @param parent the parent node
     * @return the added child node
     */
    public Node addChild(Node parent) {
        return addChild(parent, ADDING_EDGE_END);
    }
    
    public int addChildEdge(int parent, int child, int edgePosInParent) {
        return super.addEdge(parent, child, edgePosInParent, ADDING_EDGE_END);
    }
    
    /**
     * Add a child edge between the given nodes.
     * @param parent the parent node id (node table row number)
     * @param child the child node id (node table row number)
     * @return the added child edge id
     */
    public int addChildEdge(int parent, int child) {
        return addChildEdge(parent, child, ADDING_EDGE_END);
    }

    public Edge addChildEdge(Node parent, Node child, int edgePosInParent) {
        nodeCheck(parent, true);
        nodeCheck(child, true);
        return getEdge(addChildEdge(parent.getRow(), child.getRow(), edgePosInParent));
    }
    
    /**
     * Add a child edge between the given nodes.
     * @param parent the parent node
     * @param child the child node
     * @return the added child edge
     */
    public Edge addChildEdge(Node parent, Node child) {
        return addChildEdge(parent, child, ADDING_EDGE_END);
    }
    
    /**
     * Remove a child edge from the Tree. The child node and its subtree
     * will also be removed from the Tree.
     * @param edge the edge id (edge table row number) of the edge to remove
     * @return true if the edge and attached subtree is successfully removed,
     * false otherwise
     */
    public boolean removeChildEdge(int edge) {
        return removeChild(getTargetNode(edge));
    }

    /**
     * Remove a child edge from the Tree. The child node and its subtree
     * will also be removed from the Tree.
     * @param e the edge to remove
     * @return true if the edge and attached subtree is successfully removed,
     * false otherwise
     */
    public boolean removeChildEdge(Edge e) {
        edgeCheck(e, true);
        return removeChild(getTargetNode(e.getRow()));
    }
    
    /**
     * Remove a node and its entire subtree rooted at the node from the tree.
     * @param node the node id (node table row number) to remove
     * @return true if the node and its subtree is successfully removed,
     * false otherwise
     */
    public boolean removeChild(int node) {
        while ( getChildCount(node) > 0 ) {
            removeChild(getLastChildRow(node));
        }
        return removeNode(node);
    }
    
    /**
     * Remove the descendants of a node from the tree, except the node.
     * @param n the node to remove
     * @return true if the node and its subtree is successfully removed,
     * false otherwise
     */
    public boolean removeDescendants (Node n) {
        nodeCheck(n, true);
        return removeDescendants (n.getRow());
    }
    
    /**
     * Remove the descendants of a node from the tree, except the node.
     * @param node the node to remove
     * @return true if the node and its subtree is successfully removed,
     * false otherwise
     */
    public boolean removeDescendants (int node) {
    	boolean ret = true;
        while ( getChildCount(node) > 0 ) {
            if (!removeChild(getLastChildRow(node))) {
	        	ret = false;
            }
        }
        return ret;
    }
    
    /**
     * Remove a node and its entire subtree rooted at the node from the tree.
     * @param n the node to remove
     * @return true if the node and its subtree is successfully removed,
     * false otherwise
     */
    public boolean removeChild(Node n) {
        nodeCheck(n, true);
        return removeChild(n.getRow());
    }
    
    
    // ------------------------------------------------------------------------
    // Tree Accessors
    
    /**
     * Get the root node id (node table row number).
     * @return the root node id
     */
    public int getRootRow() {
        return m_root;
    }
    
    /**
     * Get the root node.
     * @return the root Node
     */
    public Node getRoot() {
        return (Node)m_nodeTuples.getTuple(m_root);
    }

    /**
     * Get the child node id at the given index.
     * @param node the parent node id (node table row number)
     * @param idx the child index
     * @return the child node id (node table row number)
     */
    public int getChild(int node, int idx) {
        int outEdge = getChildEdge(node, idx);
        if (outEdge == -1) {
            return -1;
        }
        return getTargetNode(outEdge);
    }

    public int getChildEdge(int node, int idx) {
        return getOutEdge(node, idx);
    }

    /**
     * Get the child node at the given index.
     * @param node the parent Node
     * @param idx the child index
     * @return the child Node
     */
    public Node getChild(Node node, int idx) {
        int c = getChild(node.getRow(), idx);
        return ( c<0 ? null : getNode(c) );
    }

    public Edge getChildEdge(Node node, int idx) {
        int e = getChildEdge(node.getRow(), idx);
        return ( e<0 ? null : getEdge(e) );
    }

    public boolean isParentOf(Node thiz, Node that) {
        return getParent(that) == thiz;
    }

    public boolean isChildOf(Node thiz, Node that) {
        return getParent(thiz) == that;
    }

    public boolean isSiblingOf(Node thiz, Node that) {
        return thiz != that && getParent(thiz) == getParent(that);
    }


    public boolean isDescendantOf(Node thiz, Node that) {
        Node parent = getParent(thiz);
        while (parent != null) {
            if (parent == that) {
                return true;
            }

            parent = getParent(parent);
        }
        return false;
    }

    public boolean isAncestorOf(Node thiz, Node that) {
        Node parent = getParent(that);
        while (parent != null) {
            if (parent == thiz) {
                return true;
            }

            parent = getParent(parent);
        }
        return false;
    }

    public boolean subTreeContains(Node subTreeRoot, Node node) {
        return isAncestorOf(subTreeRoot, node) || node == subTreeRoot;
    }

    public int getIndexInSiblings(int child)
    {
        int parent = getParent(child);
        if (parent == -1)
            return -1;

        int[] outlinks = (int[])getNodeTable().get(parent, OUTLINKS);
        for (int i=0; i<getChildCount(parent); i++)
        {
            if (getTargetNode(outlinks[i]) == child)
            {
                return i;
            }
        }

        assert (false);
        return -1;
    }

    public int getIndexInSiblings(Node child)
    {
        return getIndexInSiblings(child.getRow());
    }

    /**
     * Get the child index (order number of the child) for the given parent
     * node id and child node id.
     * @param parent the parent node id (node table row number)
     * @param child the child node id (node table row number)
     * @return the index of the child, or -1 if the given child node is not
     * actually a child of the given parent node, or either node is
     * invalud.
     */
    public int getChildIndex(int parent, int child) {
        assert ( getParent(child) == parent );
        return getIndexInSiblings(child);
    }

    /**
     * Get the child index (order number of the child) for the given parent
     * and child nodes.
     * @param p the parent Node
     * @param c the child Node
     * @return the index of the child, or -1 if the given child node is not
     * actually a child of the given parent node, or either node is
     * invalud.
     */
    public int getChildIndex(Node p, Node c) {
        return getChildIndex(p.getRow(), c.getRow());
    }


    /**
     * Get the child index (order number of the child) for the given parent
     * node id and child node id.
     * @param parent the parent node id (node table row number)
     * @param oldIndex the old child index
     * @return the old index of the child, or -1 if the given child node is not
     * actually a child of the given parent node, or either node is
     * invalud.
     */
    public int changeChildIndex(int parent, int oldIndex, int newIndex) {
        int[] outlinks = (int[])getNodeTable().get(parent, OUTLINKS);

        if (newIndex == -1)
            newIndex = outlinks.length - 1;

        if (oldIndex == -1)
            oldIndex = outlinks.length - 1;

        if (newIndex < oldIndex)
        {
            int tmp = outlinks [oldIndex];
            System.arraycopy(outlinks, newIndex, outlinks, newIndex+1, oldIndex-newIndex);
            outlinks[newIndex] = tmp;
        }
        else if (oldIndex < newIndex)
        {
            int tmp = outlinks [oldIndex];
            System.arraycopy(outlinks, oldIndex+1, outlinks, oldIndex, newIndex-oldIndex);
            outlinks[newIndex] = tmp;
        }

        return oldIndex;
    }

    /**
     * Get the child index (order number of the child) for the given parent
     * node id and child node id.
     * @param parent the parent node id (node table row number)
     * @param oldIndex the old child index
     * @return the old index of the child, or -1 if the given child node is not
     * actually a child of the given parent node, or either node is
     * invalud.
     */
    public int changeChildIndex(Node parent, int oldIndex, int newIndex) {
        return changeChildIndex(parent.getRow(), oldIndex, newIndex);
    }


    /**
     * Get the node id of the first child of the given parent node id.
     * @param node the parent node id (node table row number)
     * @return the node id of the first child
     */
    public int getFirstChildRow(int node) {
        return getChild(node, 0);
    }

    /**
     * Get the first child node of the given parent node.
     * @param node the parent Node
     * @return the first child Node
     */
    public Node getFirstChild(Node node) {
        return getChild(node, 0);
    }
    
    /**
     * Get the node id of the last child of the given parent node id.
     * @param node the parent node id (node table row number)
     * @return the node id of the last child
     */
    public int getLastChildRow(int node) {
        return getChild(node, getChildCount(node) - 1);
    }
    
    /**
     * Get the last child node of the given parent node.
     * @param node the parent Node
     * @return the last child Node
     */
    public Node getLastChild(Node node) {
        return getChild(node, node.getChildCount()-1);
    }

    public  boolean hasPreviousSibling (Node node)
    {
        return getIndexInSiblings(node) > 0;
    }

    public boolean hasNextSibling (Node node)
    {
        return getIndexInSiblings(node) > getParent(node).getChildCount();
    }

    /**
     * Get the node id of the previous sibling of the given node id.
     * @param node a node id (node table row number)
     * @return the node id of the previous sibling, or -1 if there
     * is no previous sibling.
     */
    public int getPreviousSiblingRow(int node) {
    	Table nodeTable = getNodeTable();
        int p = getParent(node);
        if ( p < 0 )
            return -1;
        int[] outlinks = (int[])nodeTable.get(p, OUTLINKS);
        
        int idx = getChildIndex(p, node);;
        return ( idx<=0 ? -1 : getTargetNode(outlinks[idx - 1]));
    }
    
    /**
     * Get the previous sibling of the given node.
     * @param node a node
     * @return the previous sibling, or null if there is no previous sibling
     */
    public Node getPreviousSibling(Node node) {
        int n = getPreviousSiblingRow(node.getRow());
        return ( n<0 ? null : getNode(n) );
    }
    
    /**
     * Get the node id of the next sibling of the given node id.
     * @param node a node id (node table row number)
     * @return the node id of the next sibling, or -1 if there
     * is no next sibling.
     */
    public int getNextSiblingRow(int node) {
    	Table nodeTable = getNodeTable();
        int p = getParent(node);
        if ( p < 0 )
            return -1;
        int[] outlinks = (int[])nodeTable.get(p, OUTLINKS);
        int idx = getChildIndex(p, node);
        int max = getChildCount(p)-1;
        int ret = ( idx<0 || idx>=max ? -1 : getTargetNode(outlinks[idx+1]));
        //System.out.println( node + "'s next sibling is " + ret);
        return ret;
    }
    
    /**
     * Get the next sibling of the given node.
     * @param node a node
     * @return the next sibling, or null if there is no next sibling
     */
    public Node getNextSibling(Node node) {
        int n = getNextSiblingRow(node.getRow());
        return ( n<0 ? null : getNode(n) );
    }
    
    /**
     * Get the depth of the given node id in the tree.
     * @param node a node id (node table row number)
     * @return the depth of the node in tree. The root node
     * is at a depth level of 0, with each child at a greater
     * depth level. -1 is returned if the input node id is not
     * in the tree.
     */
    public int getDepth(int node) {
        if ( !getNodeTable().isValidRow(node) )
            return -1;
        
        int depth = 0;
        if ( node!=m_root && getParent(node) < 0 ) return -1;
        for ( int i=node; i!=m_root && i>=0; ++depth, i=getParent(i) );
        return depth;
    }
    
    /**
     * Get the number of children of the given node id.
     * @param node a node id (node table row number)
     * @return the number of child nodes for the given node
     */
    public int getChildCount(int node) {
        return getOutDegree(node);
    }

    /**
     * Get the edge id of the edge to the given node's parent.
     * @param node the node id (node table row number)
     * @return the edge id (edge table row number) of the parent edge
     */
    public int getParentEdge(int node) {
        if ( getInDegree(node) > 0 ) {
            int[] inlinks = (int[])getNodeTable().get(node, INLINKS);
            return inlinks[0];
        } else {
            return -1;
        }
    }
    
    /**
     * Get the edge to the given node's parent.
     * @param n a Node instance
     * @return the parent Edge connecting the given node to its parent
     */
    public Edge getParentEdge(Node n) {
        nodeCheck(n, true);
        int pe = getParentEdge(n.getRow());
        return ( pe < 0 ? null : getEdge(pe) );
    }
    
    /**
     * Get a node's parent node id
     * @param node the child node id (node table row number)
     * @return the parent node id, or -1 if there is no parent
     */
    public int getParent(int node) {
        int pe = getParentEdge(node);
        return ( pe < 0 ? -1 : getSourceNode(pe) );
    }

    /**
     * Get a node's parent node
     * @param n the child node
     * @return the parent node, or null if there is no parent
     */
    public Node getParent(Node n) {
        int p = getParent(n.getRow());
        return ( p < 0 ? null : getNode(p) );
    }
    
    // ------------------------------------------------------------------------
    // Iterators
    
    /**
     * Get an iterator over the edge ids for edges connecting child nodes to
     * a given parent
     * @param node the parent node id (node table row number)
     * @return an iterator over the edge ids for edges conencting child nodes
     * to a given parent
     */
    public IntIterator childEdgeRows(int node) {
        return super.outEdgeRows(node);
    }
    
    /**
     * Get an iterator over the edges connecting child nodes to a given parent 
     * @param n the parent node
     * @return an iterator over the edges connecting child nodes to a given
     * parent
     */
    public Iterator childEdges(Node n) {
        return super.outEdges(n);
    }
    
    /**
     * Get an iterator over the child nodes of a parent node.
     * @param n the parent node
     * @return an iterator over the child nodes of a parent node
     */
    public Iterator children(Node n) {
        return super.outNeighbors(n);
    }

    public static interface DepthFirstTraverseProcessor
    {
        //return: true: continue deeper, false stop
        abstract public boolean run(Node parent, Node node, int level);
    }

    private void depthFirstTraverse(Node parent, Node node, int level, DepthFirstTraverseProcessor proc)
    {
        if (proc.run(parent, node, level))
        {
            Iterator children_iter = children(node);
            while (children_iter.hasNext())
            {
                depthFirstTraverse(node, (Node) children_iter.next(), level + 1, proc);
            }
        }
    }

    public void depthFirstTraverse(Node node, DepthFirstTraverseProcessor proc)
    {
        depthFirstTraverse(null, node, 0, proc);
    }

    public static abstract class DepthFirstReverseTraverseProcessor
    {
        public boolean needDown() {
            return true;
        }
        //return: true: continue deeper, false stop
        abstract public void run(Node parent, Node node, int level);
    }

    public enum ChildTraverseOrder {OLDER_FIRST, YOUNGER_FIRST}

    private void depthFirstReverseTraverse(Node parent, Node node, int level,
                                           DepthFirstReverseTraverseProcessor proc, ChildTraverseOrder childTraverseOrder)
    {
        if (proc.needDown()) {
            if (childTraverseOrder == ChildTraverseOrder.OLDER_FIRST) {
                for (int i=0; i<node.getChildCount(); i++)
                {
                    depthFirstReverseTraverse(node, node.getChild(i), level + 1, proc, childTraverseOrder);
                }

            } else {
                for (int i=node.getChildCount()-1; i>=0; i--)
                {
                    depthFirstReverseTraverse(node, node.getChild(i), level + 1, proc, childTraverseOrder);
                }

            }
        }

        proc.run(parent, node, level);
    }

    public void depthFirstReverseTraverse(Node node, DepthFirstReverseTraverseProcessor proc, ChildTraverseOrder childTraverseOrder)
    {
        depthFirstReverseTraverse(null, node, 0, proc, childTraverseOrder);
    }

    public static interface BreadthFristTraverseProcessor
    {
        abstract public void run(Node node);
    }

    public void breadthFirstTraverse(Node subTreeRoot, BreadthFristTraverseProcessor proc)
    {
        LinkedList<Node> traverseNodes = new LinkedList<>();
        traverseNodes.add(subTreeRoot);

        while (! traverseNodes.isEmpty()) {
            Node node = traverseNodes.pop();
            proc.run(node);

            for (int i=0; i<node.getChildCount(); i++) {
                traverseNodes.add(node.getChild(i));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Sanity Test
    
    /**
     * Check that the underlying graph structure forms a valid tree.
     * @return true if this is a valid tree, false otherwise
     */
    public boolean isValidTree() {
        // TODO: write a visitor interface and use that instead?
        int nnodes = getNodeCount();
        int nedges = getEdgeCount();
        
        // first make sure there are n nodes and n-1 edges
        if ( nnodes != nedges+1 ) {
            s_logger.warn("Node/edge counts incorrect.");
            return false;
        }
        
        // iterate through nodes, make sure each one has the right
        // number of parents
        int root = getRootRow();
        IntIterator nodes = getNodeTable().rows();
        while ( nodes.hasNext() ) {
            int n = nodes.nextInt();
            int id = getInDegree(n);
            if ( n==root && id > 0 ) {
                s_logger.warn("Root node has a parent.");
                return false;
            } else if ( id > 1 ) {
                s_logger.warn("Node "+n+" has multiple parents.");
                return false;
            }
        }
        
        // now do a traversal and make sure we visit everything
        int[] counts = new int[] { 0, nedges };
        isValidHelper(getRootRow(), counts);
        if ( counts[0] > nedges ) {
            s_logger.warn("The tree has non-tree edges in it.");
            return false;
        }
        if ( counts[0] < nedges ) {
            s_logger.warn("Not all of the tree was visited. " +
                "Only "+counts[0]+"/"+nedges+" edges encountered");
            return false;
        }
        return true;
    }
    
    /**
     * isValidTree's recursive helper method.
     */
    private void isValidHelper(int node, int[] counts) {
        IntIterator edges = childEdgeRows(node);
        int ncount = 0;
        while ( edges.hasNext() ) {
            // get next edge, increment count
            int edge = edges.nextInt();
            ++ncount; ++counts[0];
            // visit the next edge
            int c = getAdjacentNode(edge, node);
            isValidHelper(c, counts);
            // check the counts
            if ( counts[0] > counts[1] )
                return;
        }
    }

    // ------------------------------------------------------------------------
    // Spanning Tree Methods
    
    /**
     * Returns a spanning tree over this tree. If no spanning tree
     * has been constructed at an alternative root, this method simply returns
     * a pointer to this Tree instance. If a spanning tree rooted at an
     * alternative node has been created, that tree is returned.
     * 
     * @return a spanning tree over this tree
     * @see #getSpanningTree(Node)
     * @see Graph#clearSpanningTree()
     */
    public Tree getSpanningTree() {
        return this;
        //disable spanning tree;
        //return m_spanning==null ? this : m_spanning;
    }
    
    /**
     * Returns a spanning tree over this tree, rooted at the given root. If
     * the given root is not the same as that of this Tree, a new spanning
     * tree instance will be constructed, made the current spanning tree
     * for this Tree instance, and returned.
     * 
     * To clear out any generated spanning trees use the clearSpanningTree()
     * method of the Graph class. After calling clearSpanningTree(), the
     * getSpanningTree() method (with no arguments) will return a pointer
     * to this Tree instance instead of any generated spanning trees.
     * 
     * @param root the node at which to root the spanning tree.
     * @return a spanning tree over this tree, rooted at the given root
     * @see #getSpanningTree()
     * @see Graph#clearSpanningTree()
     */
    public Tree getSpanningTree(Node root) {
        //disable spanning tree;
        assert (false);
        nodeCheck(root, true);
        if ( m_spanning == null ) {
            if ( m_root == root.getRow() ) {
                return this;
            } else {
                m_spanning = new SpanningTree(this, root);
            }
        } else if ( m_spanning.getRoot() != root ) {
            m_spanning.buildSpanningTree(root);
        }
        return m_spanning;
    }

    private static void copyNodeEdgeRecursively(Node sourceNode, Node targetNode, Predicate filter,
                                                String[] nodeFields, String[] edgeFields)
    {
        Tree sourceTree = (Tree)sourceNode.getGraph();
        Tree targetTree = (Tree)targetNode.getGraph();

        if (nodeFields == null) {
            nodeFields = sourceTree.getNodeCustomFields();
        }

        if (edgeFields == null) {
            edgeFields = sourceTree.getEdgeCustomFields();
        }

        Table.copyTuple(sourceNode, targetNode, nodeFields);

        for(int i=0; i<sourceNode.getChildCount(); i++) {
            Node sourceChild = sourceNode.getChild(i);

            if (filter != null && !filter.getBoolean(sourceChild)) {
                continue;
            }

            Edge sourceEdge = sourceChild.getParentEdge();

            Node targetChild = targetTree.addChild(targetNode);
            Edge targetEdge = targetChild.getParentEdge();

            Table.copyTuple(sourceEdge, targetEdge, edgeFields);

            copyNodeEdgeRecursively(sourceChild, targetChild, filter, nodeFields, edgeFields);
        }
    }

    public Tree copySubTree(Node subTreeRoot, Predicate filter, String[] nodeFields, String[] edgeFields) {

        if (filter != null && !filter.getBoolean(subTreeRoot)) {
            return null;
        }

        Table nodeTable = getNodeTable();
        Table edgeTable = getEdgeTable();

        Table newNodeTable = nodeTable.getSchema().instantiate();
        Table newEdgeTable = edgeTable.getSchema().instantiate();

        //需要去掉这4列，tree才会正确初始化
        newNodeTable.removeColumn(OUTDEGREE);
        newNodeTable.removeColumn(INDEGREE);
        newNodeTable.removeColumn(OUTLINKS);
        newNodeTable.removeColumn(INLINKS);

        Tree newTree = new Tree(newNodeTable, newEdgeTable, m_nkey, m_skey, m_tkey);

        Node newTreeRoot = newTree.addRoot();

        copyNodeEdgeRecursively(subTreeRoot, newTreeRoot, filter, nodeFields, edgeFields);
        return newTree;
    }

    public Tree copySubTree(Node subTreeRoot, String[] nodeFields, String[] edgeFields) {
        return copySubTree(subTreeRoot, null, nodeFields, edgeFields);
    }

    public void pasteTree(Node parent, int pos, Tree subTree, Predicate filter,
                          String[] nodeFields, String[] edgeFields) {
        if (filter != null && filter.getBoolean(subTree.getRoot())) {
            return;
        }

        Node subTreeMountPoint = addChild(parent, pos);
        copyNodeEdgeRecursively(subTree.getRoot(), subTreeMountPoint, filter, nodeFields, edgeFields);
    }

    public void pasteTree(Node parent, int pos, Tree subTree) {
        pasteTree(parent, pos, subTree, null, null, null);
    }

    public void addRootChangeListener(TreeRootChangeListener listnr) {
        if ( !m_rootChangeListeners.contains(listnr) )
            m_rootChangeListeners.add(listnr);
    }

    public void removeRootChangeListener(TreeRootChangeListener listnr) {
        m_rootChangeListeners.remove(listnr);
    }
    
    public void removeAllRootChangeListeners() {
    	m_rootChangeListeners.clear();
    }
    
    private void fireRootChangeEvent (int newRoot, int oldRoot)
    {
        if ( !m_rootChangeListeners.isEmpty() ) {            
            Object[] lstnrs = m_rootChangeListeners.getArray();
            for ( int i=0; i<lstnrs.length; ++i ) {
                ((TreeRootChangeListener)lstnrs[i]).rootChanged(this, newRoot, oldRoot);
            }
        }
    }

} // end of class Tree
