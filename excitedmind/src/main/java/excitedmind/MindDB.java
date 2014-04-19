package excitedmind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import com.tinkerpop.blueprints.*;
import prefuse.util.PrefuseLib;

import com.orientechnologies.orient.core.db.record.ORecordLazyList;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class MindDB implements Graph {
    Logger m_logger = Logger.getLogger(this.getClass().getName());

	public final static String EDGE_TYPE_PROP_NAME = PrefuseLib.FIELD_PREFIX + "edgeType";
	public final static String CHILD_EDGES_PROP_NAME = PrefuseLib.FIELD_PREFIX + "childEdges";
    public final static String INHERIT_PATH_PROP_NAME = PrefuseLib.FIELD_PREFIX + "inheritPath";

	private final static String ROOT_INDEX_NAME = PrefuseLib.FIELD_PREFIX + "rootIndex";
	private final static String ROOT_KEY_NAME = PrefuseLib.FIELD_PREFIX + "root";

	private final static String TRASH_INDEX_NAME = PrefuseLib.FIELD_PREFIX + "trashIndex";
	private final static String TRASH_KEY_NAME = PrefuseLib.FIELD_PREFIX + "trash";

	private final static String SAVED_PARENT_ID_PROP_NAME = PrefuseLib.FIELD_PREFIX + "parent";
	private final static String SAVED_POS_PROP_NAME = PrefuseLib.FIELD_PREFIX + "pos";
	public final static String SAVED_REFERRER_INFO_PROP_NAME = PrefuseLib.FIELD_PREFIX + "referrers";

	enum EdgeType {INCLUDE, REFERENCE};

    enum InheritDirection {SELF, LINEAL_SIBLING,  COLLATERAL_SIBLING,
                          LINEAL_ANCESTOR, COLLATERAL_ANCESTOR,
                          LINEAL_DESCENDANT, COLLATERAL_DESCENDANT};

    protected static final int ADDING_EDGE_END = 0x7FFFFFFF;

	public OrientGraph m_graph;

	private Index<Vertex> m_rootIndex;
	private Index<Vertex> m_trashIndex;

    Object m_rootId;

	String m_path;

    MindDB(String path)
	{
		m_graph = new OrientGraph (path);
		m_path = path;

        m_rootIndex = getOrCreateIndex(ROOT_INDEX_NAME);
        m_trashIndex = getOrCreateIndex(TRASH_INDEX_NAME);

        Vertex root = null;
        if (m_rootIndex.get(ROOT_KEY_NAME, ROOT_KEY_NAME).iterator().hasNext()) {
            root = m_rootIndex.get(ROOT_KEY_NAME, ROOT_KEY_NAME).iterator().next();

        } else {
            root = addVertex(null);
            m_rootIndex.put(ROOT_KEY_NAME, ROOT_KEY_NAME, root);

            //translate the root id from temporary to permanence
            m_graph.commit();
            ArrayList inheritPath = new ArrayList();
            root.setProperty(INHERIT_PATH_PROP_NAME, inheritPath);
            m_graph.commit();
        }

        m_rootId = root.getId();
	}


    @Override
    protected void finalize() throws Throwable
    {
        m_graph.commit();
    }

    @Override
	public Edge addEdge(Object arg0, Vertex arg1, Vertex arg2, String arg3) {
		//disable the method, to preserve a tree structure
		assert (false);
		return m_graph.addEdge(arg0, arg1, arg2, arg3);
	}
	
	@Override
	public Vertex addVertex(Object arg0) {
		Vertex vertex = m_graph.addVertex(arg0);
        m_graph.commit();
        return m_graph.getVertex(vertex.getId());
	}
	@Override
	public Edge getEdge(Object arg0) {
		return m_graph.getEdge(arg0);
	}
	@Override
	public Iterable<Edge> getEdges() {
		return m_graph.getEdges();
	}
	@Override
	public Iterable<Edge> getEdges(String arg0, Object arg1) {
		return m_graph.getEdges(arg0, arg1);
	}
	@Override
	public Features getFeatures() {
		return m_graph.getFeatures();
	}
	@Override
	public Vertex getVertex(Object arg0) {
		return m_graph.getVertex(arg0);
	}
	@Override
	public Iterable<Vertex> getVertices() {
		return m_graph.getVertices();
	}
	@Override
	public Iterable<Vertex> getVertices(String arg0, Object arg1) {
		return m_graph.getVertices(arg0, arg1);
	}
	@Override
	public void removeEdge(Edge arg0) {
		//disable the method, to preserve a tree structure
		assert (false);
		m_graph.removeEdge(arg0);
	}
	@Override
	public void removeVertex(Vertex arg0) {
		//disable the method, to preserve a tree structure
		assert (false);
		m_graph.removeVertex(arg0);
	}
	@Override
	public void shutdown() {
		cleanTrash ();
		m_graph.shutdown();
	}
	
	public void commit() {
		m_graph.commit();
	}

    public Index<Vertex> getOrCreateIndex(String indexName)
	{
        Index<Vertex> index = m_graph.getIndex(indexName, Vertex.class);
        if (index == null) {
            index = m_graph.createIndex(indexName, Vertex.class);
        }

        return index;
	}
	
	public Object getRootId ()
	{
		return m_rootId;
	}
	
	public Vertex getEdgeSource (Edge edge)
	{
		return edge.getVertex(Direction.OUT);
	}
	
	public Vertex getEdgeTarget (Edge edge)
	{
		return edge.getVertex(Direction.IN);
	}
	
	public ArrayList<Object> getContainerProperty (Vertex source, String propName, boolean ifNullCreate)
	{
		//Because outEdgeArray must be convert to ORecordLazyList, so its type is not ArrayList.
		Object container = source.getProperty(propName);
		if (container == null)
		{
			if (ifNullCreate)
			{
				container = new ArrayList<Object> ();
				source.setProperty(propName, container);
			}
			else
			{
				return null;
			}
		}
		
		if (container instanceof ORecordLazyList)
		{
			ORecordLazyList implArray = (ORecordLazyList)container;
			implArray.setAutoConvertToRecord(false);
		}

        //return a copy list, to avoid being clear by Graph.commit
        return new ArrayList((ArrayList<Object>)container);
	}
	
	private ArrayList<Object> getEdgeIDsToChildren (Vertex source, boolean ifNullCreate)
	{
		return getContainerProperty (source, CHILD_EDGES_PROP_NAME, ifNullCreate);
	}

    public ArrayList<Object> getInheritPath (Vertex source)
    {
        return getContainerProperty (source, INHERIT_PATH_PROP_NAME, true);
    }

    public InheritDirection getInheritDirection(ArrayList fromInheritPath, Object fromVetexDBId,
                                                ArrayList toInheritPath, Object toVetexDBId)
    {
        fromInheritPath = (ArrayList)fromInheritPath.clone();
        toInheritPath = (ArrayList)toInheritPath.clone();

        fromInheritPath.add(fromVetexDBId);
        toInheritPath.add(toVetexDBId);

        int fromGeneration = fromInheritPath.size();
        int toGeneration = toInheritPath.size();

        int i;

        for (i=0; i<fromGeneration && i<toGeneration; i++)
        {
            if (! fromInheritPath.get(i).equals(toInheritPath.get(i)))
            {
                break;
            }
        }

        if (fromGeneration == toGeneration) {
            if (i == fromGeneration)
                return InheritDirection.LINEAL_SIBLING;
            else
                return InheritDirection.COLLATERAL_SIBLING;
        } else if (fromGeneration < toGeneration) {
            if (i == fromGeneration)
                return InheritDirection.LINEAL_DESCENDANT;
            else
                return InheritDirection.COLLATERAL_DESCENDANT;

        } else {
            if (i == toGeneration)
                return InheritDirection.LINEAL_ANCESTOR;
            else
                return InheritDirection.COLLATERAL_ANCESTOR;
        }

    }

    public InheritDirection getInheritRelation (Vertex from, Vertex to)
    {
        if (from.getId() == to.getId()) {
            return InheritDirection.SELF;
        }

        ArrayList fromInheritPath = getInheritPath(from);
        ArrayList toInheritPath = getInheritPath(to);

        return getInheritDirection(fromInheritPath, from.getId(), toInheritPath, to.getId());
    }
	
	public EdgeType getEdgeType (Edge edge)
	{
        int edgeTypeValue = (Integer)edge.getProperty(EDGE_TYPE_PROP_NAME);
		return EdgeType.values()[edgeTypeValue];
	}
	
	private Edge addEdge (Vertex source, Vertex target, int pos, EdgeType edgeType)
	{
		//the label should not be null or "", 
		//it is related to the implement of blueprints
		Edge edge = m_graph.addEdge(null, source, target, "a");

		edge.setProperty(EDGE_TYPE_PROP_NAME, edgeType.ordinal()); 
		
		//to make the edge'id is to local db
		commit ();

        edge = m_graph.getEdge(edge.getId());
		
		ArrayList<Object> outEdgeArray = getEdgeIDsToChildren(source, true);
		
		if (pos == ADDING_EDGE_END || pos >= outEdgeArray.size())
		{
			outEdgeArray.add(edge.getId());
		}
		else
		{
			outEdgeArray.add(pos, edge.getId());
		}
		
		//NOTICE: the container property must be reset to Vertex.
		//If not, the last item will not be save to db.
		//it is the bug of blueprints or orientdb
		
		source.setProperty(CHILD_EDGES_PROP_NAME, outEdgeArray);
		commit ();
		return edge;
	}
	
	//only addRefEdge and removeRefEdge is public
	
	public Edge addRefEdge (Vertex referrer, Vertex referent, int pos)
	{
		assert (referrer.getId() != referent.getId());
		return addEdge (referrer, referent, pos, EdgeType.REFERENCE);
	}

    private void removeEdge (Vertex source, int pos, EdgeType assert_type)
    {
        ArrayList<Object> outEdgeArray = getEdgeIDsToChildren(source, false);
        Object edgeId = outEdgeArray.get(pos);

        Edge edge = m_graph.getEdge(edgeId);
        assert (getEdgeType(edge) == assert_type);

        m_graph.removeEdge(m_graph.getEdge(edgeId));

        outEdgeArray.remove(pos);
        //NOTICE: the container property must be reset to Vertex.
        //If not, the last item will not be save to db.
        //it is the bug of blueprints or orientdb
        source.setProperty(CHILD_EDGES_PROP_NAME, outEdgeArray);
    }

	public void removeRefEdge (Vertex source, int pos)
	{
        removeEdge(source, pos, EdgeType.REFERENCE);
	}

    public Edge getEdge (Vertex source, int pos)
	{
		ArrayList<Object> childEdgeArray = getEdgeIDsToChildren(source, false);
		
		if (childEdgeArray == null)
		{
			return null;
		}
		else
		{
			return getEdge(childEdgeArray.get(pos));
		}
	}

	public class EdgeVertex {
		final public Vertex m_vertex;
		final public Edge m_edge;
		
		public EdgeVertex(Edge edge, Vertex vertex) {
			m_vertex = vertex;
			m_edge = edge;
		}
	};

    private Edge buildParentage(Vertex parent, Vertex child, int pos)
    {
        Edge edge = addEdge(parent, child, pos, EdgeType.INCLUDE);

        ArrayList<Object> inheritPath = new ArrayList<Object> ();
        ArrayList<Object> parentInheritPath = getInheritPath(parent);

        inheritPath.addAll(parentInheritPath);
        inheritPath.add(parent.getId());

        child.setProperty(INHERIT_PATH_PROP_NAME, inheritPath);

        return edge;
    }
	
	public EdgeVertex addChild (Vertex parent, int pos)
	{
		Vertex child = addVertex(null);
		Edge edge =  buildParentage(parent, child, pos);
        return new EdgeVertex(edge, child);
	}

	public EdgeVertex getChildOrReferent(Vertex parent, int pos)
	{
		Edge edge = getEdge(parent, pos);
		if (edge == null)
		{
			return null;
		}
		else
		{
			Vertex child = getEdgeTarget(edge);
			return new EdgeVertex(edge, child);
		}
	}
	
	public ArrayList<EdgeVertex> getChildrenAndReferents(Vertex parent)
	{
		ArrayList<Object> edgeIDsToChildren = getEdgeIDsToChildren(parent, false);
		
		if (edgeIDsToChildren == null)
			return null;
		
		ArrayList<EdgeVertex> children = new ArrayList<EdgeVertex>();

		children.ensureCapacity(edgeIDsToChildren.size());

		for (Object edgeId : edgeIDsToChildren)
		{
			Edge edgeToChild = getEdge(edgeId);
			Vertex child = getEdgeTarget(edgeToChild);
			children.add(new EdgeVertex(edgeToChild, child));
		}

		return children;
	}
	
	public EdgeVertex getParent(Vertex vertex)
	{
		Iterator<Edge> edgeIterator = vertex.getEdges(Direction.IN).iterator();
		Edge parentToVertex = null;
		
		while (edgeIterator.hasNext())
		{
            Edge edge = edgeIterator.next();

			if (getEdgeType(edge) == EdgeType.INCLUDE) {
                parentToVertex = edge;
				break;
            }
		}

		if (parentToVertex == null) {
			return null;
			
		} else {
			Vertex parent = getEdgeSource(parentToVertex);
			return new EdgeVertex(parentToVertex, parent);
		}
	}

    public EdgeVertex handoverChild(Vertex fromParent, int fromPos, Vertex toParent, int toPos)
    {
        Vertex child = getChildOrReferent(fromParent, fromPos).m_vertex;
        removeEdge (fromParent, fromPos, EdgeType.INCLUDE);
        Edge edge = buildParentage(toParent, child, toPos);
        return new EdgeVertex(edge, child);
    }

    public void changeChildPos (Vertex parent, int oldPos, int newPos)
    {
        if (oldPos == newPos)
            return;


        ArrayList<Object> outEdgeArray = getEdgeIDsToChildren(parent, true);
        Object edgeId = outEdgeArray.remove(oldPos);

        if (oldPos < newPos) {
            outEdgeArray.add(newPos-1, edgeId);
        } else {
            outEdgeArray.add(newPos, edgeId);
        }

        parent.setProperty(CHILD_EDGES_PROP_NAME, outEdgeArray);
    }

    public ArrayList<EdgeVertex> getReferrers(Vertex referent)
	{
		Iterator<Edge> edgeIterator = referent.getEdges(Direction.IN).iterator();
		Edge refEdge;
		ArrayList<EdgeVertex> referrerArray = new ArrayList<EdgeVertex> ();
		
		
		while (edgeIterator.hasNext())
		{
			refEdge = edgeIterator.next();
			
			if (getEdgeType(refEdge) == EdgeType.REFERENCE)
			{
				Vertex referrer = getEdgeSource(refEdge);
				referrerArray.add(new EdgeVertex(refEdge, referrer));
			}
		}
		
		return referrerArray.size() == 0 ? null : referrerArray;
	}
	
	private interface Processor 
	{
		//return: true: continue deeper, false stop
	 	abstract public boolean run (Vertex vertex, int level);
	}
	
	private void deepTraverse (Vertex vertex, Processor proc, int level)
	{
		if (proc.run(vertex, level))
		{
			ArrayList<EdgeVertex> children = getChildrenAndReferents(vertex);
			
			if (children != null)
			{

				for (EdgeVertex child : children)
				{
					if (getEdgeType(child.m_edge) == EdgeType.INCLUDE)
					{
						deepTraverse(child.m_vertex, proc, level+1);
					}
				}
			}
		}
	}
	
	private void deepTraverse (Vertex vertex, Processor proc)
	{
		deepTraverse (vertex, proc, 0);
	}
	
	//remove vertex, the children append to 
	public static class RefLinkInfo {
		final Object m_referrer;
		final Object m_referent;
		final int m_pos;
		
		RefLinkInfo (Object referrer, Object referent, int pos)
		{
			m_referrer = referrer;
			m_referent = referent;
			m_pos = pos;
		}
	}
	
	//return the removed vertex
	public Vertex trashSubTree(Vertex parent, int pos)
	{
		EdgeVertex edgeVertex = getChildOrReferent(parent, pos);
		
		assert (getEdgeType(edgeVertex.m_edge) == EdgeType.INCLUDE);
		
		Vertex removedVertex = edgeVertex.m_vertex;

        //collect the nodes and edges which refer the removed treee
        final ArrayList<RefLinkInfo> refLinkInfos = new ArrayList<RefLinkInfo> ();
		deepTraverse(removedVertex, new Processor() {
			
			public boolean run(Vertex vertex, int level) {
				ArrayList<EdgeVertex> referrers = getReferrers(vertex);
				
				if (referrers != null)
				{
					for (EdgeVertex referrer : referrers)
					{
						ArrayList<Object> edgeArray = getEdgeIDsToChildren(referrer.m_vertex, false);
						int edgeIndex = edgeArray.indexOf(referrer.m_edge.getId());

						refLinkInfos.add(new RefLinkInfo(referrer.m_vertex.getId(), vertex.getId(), edgeIndex));

						removeRefEdge(referrer.m_vertex, edgeIndex);
					}
				}
				
				return true;
			}
		});
		
		removedVertex.setProperty(SAVED_PARENT_ID_PROP_NAME, parent.getId());
		removedVertex.setProperty(SAVED_POS_PROP_NAME, pos);
		removedVertex.setProperty(SAVED_REFERRER_INFO_PROP_NAME, refLinkInfos);

        removeEdge(parent, pos, EdgeType.INCLUDE);

		m_trashIndex.put(TRASH_KEY_NAME, TRASH_KEY_NAME, removedVertex);
		
		commit ();
		
		return removedVertex;
	}

    public static class TrashedTreeContext {
        Object m_parentId;
        int m_pos;
        ArrayList<RefLinkInfo> m_refLinkInfos;

        TrashedTreeContext (Object parentId, int pos, ArrayList<RefLinkInfo> refLinkInfos)
        {
            m_parentId = parentId;
            m_pos = pos;
            m_refLinkInfos = refLinkInfos;
        }
    }

    public TrashedTreeContext getTrashedTreeContext (Vertex vertex)
    {
        Object parentId = vertex.getProperty(SAVED_PARENT_ID_PROP_NAME);
        int pos = (Integer)vertex.getProperty(SAVED_POS_PROP_NAME);
        ArrayList<Object> refLinkInfos = getContainerProperty (vertex, SAVED_REFERRER_INFO_PROP_NAME, false);

        assert (parentId != null);

        if (refLinkInfos == null) {
            return new TrashedTreeContext (parentId, pos, null);

        } else {
            ArrayList<RefLinkInfo> new_array = new ArrayList<RefLinkInfo>();
            for (Object obj : refLinkInfos)
            {
                RefLinkInfo refLinkInfo = (RefLinkInfo) obj;
                new_array.add(refLinkInfo);
            }

            return new TrashedTreeContext (parentId, pos, new_array);
        }

    }
	
	//return parent vertex, and 
	public EdgeVertex restoreTrashedSubTree(Vertex vertex)
	{
		Object parentId = vertex.getProperty(SAVED_PARENT_ID_PROP_NAME);
		int pos = (Integer)vertex.getProperty(SAVED_POS_PROP_NAME);
		ArrayList<Object> refLinkInfos = getContainerProperty (vertex, SAVED_REFERRER_INFO_PROP_NAME, false);
		
		Vertex parent = getVertex(parentId);
		Edge edge = addEdge(parent, vertex, pos, EdgeType.INCLUDE);
		
		if (refLinkInfos != null)
		{
			for (Object obj : refLinkInfos)
			{
				RefLinkInfo refLinkInfo = (RefLinkInfo) obj;
				addRefEdge(getVertex(refLinkInfo.m_referrer),
						getVertex(refLinkInfo.m_referent),
						refLinkInfo.m_pos);
			}
		}
		
		vertex.removeProperty(SAVED_PARENT_ID_PROP_NAME);
		vertex.removeProperty(SAVED_POS_PROP_NAME);
		vertex.removeProperty(SAVED_REFERRER_INFO_PROP_NAME);

        m_trashIndex.remove(TRASH_KEY_NAME, TRASH_KEY_NAME, vertex);
        commit ();
		return new EdgeVertex(edge, parent);
	}

    private void removeSubTree(Vertex root)
    {
        for (EdgeVertex edgeVertex : getChildrenAndReferents(root)) {
            if (getEdgeType(edgeVertex.m_edge) == EdgeType.INCLUDE) {
                removeSubTree(edgeVertex.m_vertex);
            }
        }

        m_graph.removeVertex(root);
    }

	private void cleanTrash ()
	{
        for (Vertex vertex: m_trashIndex.get(TRASH_KEY_NAME, TRASH_KEY_NAME)) {
            removeSubTree(vertex);
        }
	}
	
	
	private void copyProperty(Element from, Element to)
	{
		for (String key : from.getPropertyKeys())
		{
			if (key != CHILD_EDGES_PROP_NAME)
			{
				to.setProperty(key, from.getProperty(key));
			}
		}
		
	}

	public GraphQuery query() {
		return m_graph.query();
	}

    public void createFullTextVertexKeyIndex(String key)
    {
        Set<String> indexedKeys = m_graph.getIndexedKeys(Vertex.class);

        for (String indexedKey : indexedKeys) {
            if (indexedKey.equals(key)) {
                return;
            }
        }

        m_graph.createKeyIndex(key, Vertex.class, new Parameter("type", "FULLTEXT_HASH_INDEX"));
    }

    public Iterable<Vertex> getVertices(String key, String value)
    {
        return m_graph.getVertices(key, value);
    }
}
