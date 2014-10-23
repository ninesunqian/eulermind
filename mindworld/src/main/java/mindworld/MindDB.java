package mindworld;

import java.nio.ByteBuffer;
import java.util.*;

import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.serialization.OSerializableStream;
import com.tinkerpop.blueprints.*;
import prefuse.util.PrefuseLib;

import com.orientechnologies.orient.core.db.record.ORecordLazyList;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class MindDB implements Graph {
    Logger m_logger = LoggerFactory.getLogger(this.getClass());

	public final static String EDGE_TYPE_PROP_NAME = PrefuseLib.FIELD_PREFIX + "edgeType";
    public final static String OUT_EDGE_INNER_ID_PROP_NAME = PrefuseLib.FIELD_PREFIX + "outEdgeInnerId";
	public final static String OUT_EDGES_PROP_NAME = PrefuseLib.FIELD_PREFIX + "childEdges";

	private final static String ROOT_INDEX_NAME = PrefuseLib.FIELD_PREFIX + "rootIndex";
	private final static String ROOT_KEY_NAME = PrefuseLib.FIELD_PREFIX + "root";

	private final static String TRASH_INDEX_NAME = PrefuseLib.FIELD_PREFIX + "trashIndex";
	private final static String TRASH_KEY_NAME = PrefuseLib.FIELD_PREFIX + "trash";

	private final static String SAVED_PARENT_ID_PROP_NAME = PrefuseLib.FIELD_PREFIX + "parent";
	private final static String SAVED_POS_PROP_NAME = PrefuseLib.FIELD_PREFIX + "pos";
	public final static String SAVED_REFERRER_INFO_PROP_NAME = PrefuseLib.FIELD_PREFIX + "referrers";

    public final static String IS_TRASHED_PROP_NAME = PrefuseLib.FIELD_PREFIX + "isTrashed";

    public final static String VERTEX_CLASS = "mind_node";

    public final static int MAX_OUT_EDGES = Short.MAX_VALUE - 1;

	enum EdgeType {INCLUDE, REFERENCE};

    public enum InheritDirection {SELF, LINEAL_SIBLING,  COLLATERAL_SIBLING,
                          LINEAL_ANCESTOR, COLLATERAL_ANCESTOR,
                          LINEAL_DESCENDANT, COLLATERAL_DESCENDANT};

    public static final int ADDING_EDGE_END = 0x7FFFFFFF;

	public OrientGraph m_graph;

	private Index<Vertex> m_rootIndex;
	private Index<Vertex> m_trashIndex;

    Object m_rootId;

	String m_path;

    final int sm_parentDBIdCapacity = 2048;

    LinkedHashMap<Object, Object> m_parentDBIdCache =
            new LinkedHashMap<Object, Object>(256, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry (Map.Entry<Object, Object> eldest) {
             return size() > sm_parentDBIdCapacity;
          }
     };

    MindDB(String path)
	{
		m_graph = new OrientGraph (path, false);
		m_path = path;

        m_rootIndex = getOrCreateIndex(ROOT_INDEX_NAME);
        m_trashIndex = getOrCreateIndex(TRASH_INDEX_NAME);

        createFullTextVertexKeyIndex(MindModel.sm_textPropName);

        Vertex root = null;
        if (m_rootIndex.get(ROOT_KEY_NAME, ROOT_KEY_NAME).iterator().hasNext()) {
            root = m_rootIndex.get(ROOT_KEY_NAME, ROOT_KEY_NAME).iterator().next();

        } else {
            root = addVertex(null);
            m_rootIndex.put(ROOT_KEY_NAME, ROOT_KEY_NAME, root);

            //translate the root id from temporary to permanence
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
        Vertex vertex =  m_graph.addVertex(null, MindModel.sm_textPropName, "a");
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
	
	public <T> ArrayList<T> getContainerProperty (Vertex source, String propName, boolean ifNullCreate)
	{
		//Because outEdgeArray must be convert to ORecordLazyList, so its type is not ArrayList.
		Object container = source.getProperty(propName);
		if (container == null)
		{
			if (ifNullCreate)
			{
				container = new ArrayList<T> ();
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
	
	public ArrayList<Short> getOutEdgeInnerIds(Vertex source, boolean ifNullCreate)
	{
		return getContainerProperty (source, OUT_EDGES_PROP_NAME, ifNullCreate);
	}

    public Short getOutEdgeInnerId(Edge edge)
    {
        return edge.getProperty(OUT_EDGE_INNER_ID_PROP_NAME);
    }

    private EdgeVertex getParentSkipCache(Vertex vertex)
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

    public List getInheritPath(Object dbId)
    {
        assert !(dbId instanceof Vertex);

        LinkedList inheritPath = new LinkedList();

        Object parentDBId = getParentDBId(dbId);
        while (parentDBId != null) {
            inheritPath.addFirst(parentDBId);
            parentDBId = getParentDBId(parentDBId);
        }

        return inheritPath;
    }

    static public InheritDirection getInheritDirection(List fromInheritPath, Object fromVetexDBId,
                                                List toInheritPath, Object toVetexDBId)
    {
        fromInheritPath = (List)((LinkedList)fromInheritPath).clone();
        toInheritPath = (List)((LinkedList)toInheritPath).clone();

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

        List fromInheritPath = getInheritPath(from.getId());
        List toInheritPath = getInheritPath(to.getId());

        return getInheritDirection(fromInheritPath, from.getId(), toInheritPath, to.getId());
    }
	
	public EdgeType getEdgeType(Edge edge)
	{
        int edgeTypeValue = (Integer)edge.getProperty(EDGE_TYPE_PROP_NAME);
		return EdgeType.values()[edgeTypeValue];
	}

    private List<Short> getNumberHoles(ArrayList<Short> numbers)
    {
        short max = 0;
        ArrayList<Short> holes = new ArrayList<Short>();

        for (short number : numbers) {
            if (max < number) {
                max = number;
            }
        }
        boolean existArray[] = new boolean[max + 1];
        for (int i=0; i<existArray.length; i++) {
            existArray[i] = false;
        }

        for (short number : numbers) {
            existArray[number] = true;
        }

        for (short i = 0; i < existArray.length; i++) {
            if (existArray[i] == false) {
                holes.add(i);
            }
        }

        return holes;
    }

    private Short allocateOutEdgeInnerId(Vertex vertex, int pos)
    {
        ArrayList<Short> outEdgeInnerIds = getOutEdgeInnerIds(vertex, false);

        if (outEdgeInnerIds == null) {
            outEdgeInnerIds = new ArrayList<Short>();
        }

        List<Short> innerIdHoles = getNumberHoles(outEdgeInnerIds);

        Short newEdgeInnerId = innerIdHoles.size() > 0 ? innerIdHoles.get(0) : (short)outEdgeInnerIds.size();

        if (pos == ADDING_EDGE_END) {
            outEdgeInnerIds.add(newEdgeInnerId);
        } else {
            assert 0 <= pos && pos <= outEdgeInnerIds.size();
            outEdgeInnerIds.add(pos, newEdgeInnerId);
        }

        //NOTICE: the container property must be reset to Vertex.
        //If not, the last item will not be save to db.
        //it is the bug of blueprints or orientdb
        vertex.setProperty(OUT_EDGES_PROP_NAME, outEdgeInnerIds);

        return newEdgeInnerId;
    }

	private Edge addEdge(Vertex source, Vertex target, int pos, EdgeType edgeType)
	{
		Edge edge = m_graph.addEdge(null, source, target, "E");
        Short outEdgeInnerId = allocateOutEdgeInnerId(source, pos);

		edge.setProperty(EDGE_TYPE_PROP_NAME, edgeType.ordinal());
        edge.setProperty(OUT_EDGE_INNER_ID_PROP_NAME, outEdgeInnerId);
		
		return edge;
	}
	
    public Edge addRefEdge(Vertex referrer, Vertex referent, int pos)
    {
        Edge edge = addEdge(referrer, referent, pos, EdgeType.REFERENCE);
        verifyVertex(referrer);
        verifyVertex(referent);

        return edge;
    }

    private void removeEdge (Vertex source, int pos, EdgeType assert_type)
    {
        ArrayList<Short> outEdgeInnerIds = getOutEdgeInnerIds(source, false);
        short outEdgeInnerId = outEdgeInnerIds.get(pos);

        Edge edgeBeingRemoved = getEdge(source, pos);
        assert edgeBeingRemoved != null;
        assert getEdgeType(edgeBeingRemoved) == assert_type;

        m_graph.removeEdge(edgeBeingRemoved);

        outEdgeInnerIds.remove(pos);

        if (outEdgeInnerIds.isEmpty()) {
            source.removeProperty(OUT_EDGES_PROP_NAME);
        } else {
            source.setProperty(OUT_EDGES_PROP_NAME, outEdgeInnerIds);
        }
    }

    private void removeRefEdgeImpl(Vertex source, int pos)
    {
        EdgeVertex referent = getChildOrReferent(source, pos);
        removeEdge(source, pos, EdgeType.REFERENCE);

        verifyVertex(source);
        verifyVertex(referent.m_vertex);
    }

	public void removeRefEdge (Vertex source, int pos)
	{
        removeRefEdgeImpl(source, pos);
	}

    public Edge getEdge(Vertex source, int pos)
	{
		ArrayList<Short> outEdgeInnerIds = getOutEdgeInnerIds(source, false);

        if (outEdgeInnerIds == null) {
            assert source.getEdges(Direction.OUT).iterator().hasNext() == false;
            return null;
        }

        Short outEdgeInnerId = outEdgeInnerIds.get(pos);
        Iterator<Edge> outEdgeIterator = source.getEdges(Direction.OUT).iterator();
        while (outEdgeIterator.hasNext())
        {
            Edge outEdge = outEdgeIterator.next();
            if (outEdge.getProperty(OUT_EDGE_INNER_ID_PROP_NAME).equals(outEdgeInnerId)) {
                return outEdge;
            }
        }

        return null;
	}

	static public class EdgeVertex {
		final public Vertex m_vertex;
		final public Edge m_edge;
		
		public EdgeVertex(Edge edge, Vertex vertex) {
			m_vertex = vertex;
			m_edge = edge;
		}
	};

	public EdgeVertex addChild (Vertex parent, int pos)
	{
		Vertex child = addVertex(null);
        parent = getVertex(parent.getId());
        Edge edge = addEdge(parent, child, pos, EdgeType.INCLUDE);

        //must commit to get path
        commit();

        child = getVertex(child.getId());
        edge = getEdge(edge.getId());

        m_parentDBIdCache.put(child.getId(), parent.getId());

        verifyVertex(parent);
        verifyVertex(child);

        return new EdgeVertex(edge, child);
	}

    public int getChildOrReferentCount(Vertex vertex)
    {
        ArrayList<Short> outEdgeInnerIds = getOutEdgeInnerIds(vertex, false);
        return outEdgeInnerIds.size();
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
		ArrayList<Short> outEdgeInnerIds = getOutEdgeInnerIds(parent, false);

		if (outEdgeInnerIds == null)
			return null;
		
		ArrayList<EdgeVertex> children = new ArrayList<EdgeVertex>();

		children.ensureCapacity(outEdgeInnerIds.size());

		for (int i=0; i<outEdgeInnerIds.size(); i++)
		{
			Edge edgeToChild = getEdge(parent, i);
			Vertex child = getEdgeTarget(edgeToChild);
			children.add(new EdgeVertex(edgeToChild, child));
		}
		return children;
	}
	
    public Object getParentDBId(Object dbId)
    {
        if (dbId.equals(m_rootId)) {
            return null;
        }

        Object cachedParentDBId = m_parentDBIdCache.get(dbId);

        if (cachedParentDBId != null) {
            return cachedParentDBId;
        } else {
            EdgeVertex toParent = getParentSkipCache(getVertex(dbId));
            if (toParent == null || toParent.m_vertex == null) {
                int i= 1;
            }
            Object parentDBId = toParent.m_vertex.getId();
            assert(!(dbId instanceof Vertex));
            assert(!(parentDBId instanceof Vertex));
            m_parentDBIdCache.put(dbId, parentDBId);
            return parentDBId;
        }
    }

    public Vertex getParent(Vertex vertex)
    {
        if (vertex.getId().equals(m_rootId)) {
            return null;
        }

        Object cachedParentDBId = m_parentDBIdCache.get(vertex.getId());
        if (cachedParentDBId == null) {
            EdgeVertex toParent = getParentSkipCache(vertex);
            m_parentDBIdCache.put(vertex.getId(), toParent.m_vertex.getId());
            return toParent.m_vertex;
        } else {
            return getVertex(cachedParentDBId);
        }
    }

    public EdgeVertex handoverChild(Vertex fromParent, int fromPos, Vertex toParent, int toPos)
    {
        Vertex child = getChildOrReferent(fromParent, fromPos).m_vertex;
        removeEdge (fromParent, fromPos, EdgeType.INCLUDE);
        Edge edge = addEdge(toParent, child, toPos, EdgeType.INCLUDE);

        m_parentDBIdCache.put(child.getId(), toParent.getId());

        verifyVertex(fromParent);
        verifyVertex(toParent);
        verifyVertex(child);

        return new EdgeVertex(edge, child);
    }

    public EdgeVertex handoverReferent(Vertex fromReferrer, int fromPos, Vertex toReferrer, int toPos)
    {
        Vertex referent = getChildOrReferent(fromReferrer, fromPos).m_vertex;
        removeEdge (fromReferrer, fromPos, EdgeType.REFERENCE);
        Edge edge = addEdge(toReferrer, referent, toPos, EdgeType.REFERENCE);

        verifyVertex(fromReferrer);
        verifyVertex(toReferrer);
        verifyVertex(referent);

        return new EdgeVertex(edge, referent);
    }

    public void changeChildOrReferentPos(Vertex parent, int oldPos, int newPos)
    {
        if (oldPos == newPos)
            return;

        ArrayList<Short> outEdgeInnerIds = getOutEdgeInnerIds(parent, true);

        Short edgeInnerId = outEdgeInnerIds.remove(oldPos);
        outEdgeInnerIds.add(newPos, edgeInnerId);

        parent.setProperty(OUT_EDGES_PROP_NAME, outEdgeInnerIds);

        parent = m_graph.getVertex(parent.getId());
        verifyVertex(parent);
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
	public static class RefLinkInfo implements OSerializableStream {
		final Object m_referrer;
		final Object m_referent;
		final int m_pos;
		
		RefLinkInfo (Object referrer, Object referent, int pos)
		{
			m_referrer = referrer;
			m_referent = referent;
			m_pos = pos;
		}

        @Override
        public byte[] toStream() throws OSerializationException
        {
            byte referrerBytes[] = ((ORecordId)m_referrer).toStream();
            byte referentBytes[] = ((ORecordId)m_referent).toStream();

            ByteBuffer byteBuffer = ByteBuffer.allocate(12 + referrerBytes.length + referentBytes.length);

            byteBuffer.putInt(m_pos);
            byteBuffer.putInt(referrerBytes.length);
            byteBuffer.putInt(referentBytes.length);

            byteBuffer.put(referrerBytes);
            byteBuffer.put(referentBytes);

            return byteBuffer.array();
        }

        @Override
        public RefLinkInfo fromStream(byte[] iStream) throws OSerializationException
        {
            ByteBuffer byteBuffer = ByteBuffer.wrap(iStream);
            int pos = byteBuffer.getInt();
            int referrerByteLength = byteBuffer.getInt();
            int referentByteLength = byteBuffer.getInt();

            byte referrerByte[] = Arrays.copyOfRange(iStream, 12, 12 + referrerByteLength);
            byte referentByte[] = Arrays.copyOfRange(iStream, 12 + referrerByteLength, iStream.length);

            ORecordId referrer = (new ORecordId()).fromStream(referrerByte);
            ORecordId referent = (new ORecordId()).fromStream(referentByte);
            return new RefLinkInfo(referrer, referent, pos);
        }
    }
	
	//return the removed vertex
	public Vertex trashSubTree(Vertex parent, int pos)
	{
		EdgeVertex edgeVertex = getChildOrReferent(parent, pos);
		
		assert (getEdgeType(edgeVertex.m_edge) == EdgeType.INCLUDE);
		
		Vertex removedVertex = edgeVertex.m_vertex;

        //collect the refer info, to help update display tree
        final ArrayList<RefLinkInfo> refLinkInfos = new ArrayList<RefLinkInfo> ();

		deepTraverse(removedVertex, new Processor() {
			
			public boolean run(Vertex vertex, int level) {
				ArrayList<EdgeVertex> referrers = getReferrers(vertex);
				
				if (referrers != null)
				{
					for (EdgeVertex referrer : referrers)
					{
						ArrayList<Short> outEdgeInnerIds = getOutEdgeInnerIds(referrer.m_vertex, false);
						int edgeIndex = outEdgeInnerIds.indexOf(getOutEdgeInnerId(referrer.m_edge));

                        refLinkInfos.add(new RefLinkInfo(referrer.m_vertex.getId(), vertex.getId(), edgeIndex));
						removeRefEdgeImpl(referrer.m_vertex, edgeIndex);
					}
				}

                vertex.setProperty(IS_TRASHED_PROP_NAME, true);

				return true;
			}
		});
		
		removedVertex.setProperty(SAVED_PARENT_ID_PROP_NAME, parent.getId());
		removedVertex.setProperty(SAVED_POS_PROP_NAME, pos);
        removedVertex.setProperty(SAVED_REFERRER_INFO_PROP_NAME, refLinkInfos);

        removeEdge(parent, pos, EdgeType.INCLUDE);

		m_trashIndex.put(TRASH_KEY_NAME, TRASH_KEY_NAME, removedVertex);

        verifyTrashedTree(removedVertex);

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
        if (parentId instanceof Vertex) {
            parentId = ((Vertex)parentId).getId();
        }

        int pos = (Integer)vertex.getProperty(SAVED_POS_PROP_NAME);
        ArrayList<Object> refLinkInfoes = getContainerProperty (vertex, SAVED_REFERRER_INFO_PROP_NAME, false);

        assert (parentId != null);

        if (refLinkInfoes == null) {
            return new TrashedTreeContext (parentId, pos, null);

        } else {
            ArrayList<RefLinkInfo> new_array = new ArrayList<RefLinkInfo>();
            for (Object obj : refLinkInfoes)
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
        if (parentId instanceof Vertex) {
            parentId = ((Vertex)parentId).getId();
        }

		int pos = (Integer)vertex.getProperty(SAVED_POS_PROP_NAME);
		ArrayList<Object> refLinkInfoes = getContainerProperty (vertex, SAVED_REFERRER_INFO_PROP_NAME, false);
		
		Vertex parent = getVertex(parentId);
		Edge edge = addEdge(parent, vertex, pos, EdgeType.INCLUDE);
		
		if (refLinkInfoes != null)
		{
			for (Object obj : refLinkInfoes)
			{
				RefLinkInfo refLinkInfo = (RefLinkInfo) obj;
				addEdge(getVertex(refLinkInfo.m_referrer),
                        getVertex(refLinkInfo.m_referent),
                        refLinkInfo.m_pos,
                        EdgeType.REFERENCE);
                verifyVertex(getVertex(refLinkInfo.m_referrer));
                verifyVertex(getVertex(refLinkInfo.m_referent));
			}
		}

        deepTraverse(vertex, new Processor() {
            public boolean run(Vertex vertex, int level) {
                vertex.removeProperty(IS_TRASHED_PROP_NAME);
                return true;
            }
        });

		vertex.removeProperty(SAVED_PARENT_ID_PROP_NAME);
		vertex.removeProperty(SAVED_POS_PROP_NAME);
		vertex.removeProperty(SAVED_REFERRER_INFO_PROP_NAME);

        m_trashIndex.remove(TRASH_KEY_NAME, TRASH_KEY_NAME, vertex);

        assert(!(parentId instanceof Vertex));
        m_parentDBIdCache.put(vertex.getId(), parentId);
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
			if (key != OUT_EDGES_PROP_NAME)
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

        OrientVertexType type = m_graph.getVertexBaseType();
        type.createProperty(key, OType.STRING);

        type.createIndex("V."+key, "FULLTEXT", null, null, "LUCENE", new String[]{key});
    }

    public Iterable<Vertex> getVertices(String key, String value)
    {
        return m_graph.getVertices(key, value);
    }

    public Iterable<Vertex> getVertices(final String label, final String[] iKey, Object[] iValue) {
        return m_graph.getVertices(label, iKey, iValue);
    }

    private void verifyCachedInheritPathValid(Object parentDBId, Object childDBId)
    {
        List childInheritPath = getInheritPath(childDBId);
        List parentInheritPath = getInheritPath(parentDBId);
        assert childInheritPath.get(childInheritPath.size()-1).equals(parentDBId) &&
                childInheritPath.subList(0, childInheritPath.size()-1).equals(parentInheritPath);
    }

    public boolean isParentChildRelation(Object parentDBId, Object childDBId)
    {
        List childInheritPath = getInheritPath(childDBId);
        List parentInheritPath = getInheritPath(parentDBId);
        return childInheritPath.get(childInheritPath.size()-1).equals(parentDBId) &&
                childInheritPath.subList(0, childInheritPath.size()-1).equals(parentInheritPath);
    }

    private void verifyOutEdges(Vertex vertex)
    {
        ArrayList<Short> outEdgeInnerIds = getOutEdgeInnerIds(vertex, true);
        Map<Short, Integer> innerIdExists = new HashMap<Short, Integer>();

        for(Short innerId : outEdgeInnerIds) {
            innerIdExists.put(innerId, 0);
        }

        int edgeNumber = 0;
        for (Edge outEdge : vertex.getEdges(Direction.OUT)) {
            edgeNumber++;

            Short innerId = getOutEdgeInnerId(outEdge);
            assert outEdgeInnerIds.contains(innerId);
            innerIdExists.put(innerId, innerIdExists.get(innerId) + 1);

            if (getEdgeType(outEdge) == EdgeType.INCLUDE) {
                Vertex childVertex = getEdgeTarget(outEdge);
                verifyCachedInheritPathValid(vertex.getId(), childVertex.getId());

            } else {
                assert getEdgeType(outEdge) == EdgeType.REFERENCE;
            }
        }

        Iterator iter = innerIdExists.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Short, Integer> entry = (Map.Entry) iter.next();
            assert entry.getValue() == 1;
        }

        assert (edgeNumber == outEdgeInnerIds.size());
    }

    private void verifyInEdges(Vertex vertex)
    {
        boolean metParent = false;

        for (Edge inEdge : vertex.getEdges(Direction.IN)) {
            Vertex parentOrReferrerVertex = getEdgeSource(inEdge);

            ArrayList<Short> parentOrReferrerOutEdgeInnerIds = getOutEdgeInnerIds(parentOrReferrerVertex, true);
            //TODO
            assert parentOrReferrerOutEdgeInnerIds.contains(getOutEdgeInnerId(inEdge));

            if (getEdgeType(inEdge) == EdgeType.INCLUDE) {
                assert metParent == false;
                verifyCachedInheritPathValid(parentOrReferrerVertex.getId(), vertex.getId());
                metParent = true;
            } else {
                assert(getEdgeType(inEdge) == EdgeType.REFERENCE);
            }
        }

        if (!vertex.getId().equals(m_rootId)) {
            assert(metParent == true);
        }
    }

    public void verifyVertex(Vertex vertex)
    {
        /*
        //after commit, must add it
        vertex = m_graph.getVertex(vertex.getId());

        verifyInEdges(vertex);
        verifyOutEdges(vertex);
        */
    }

    public boolean isVertexTrashed(Vertex vertex)
    {
        return vertex.getProperty(IS_TRASHED_PROP_NAME) != null;
    }

    private void setVertexTrashed(Vertex vertex, boolean trashed)
    {
        if (trashed == true)
            vertex.setProperty(IS_TRASHED_PROP_NAME, true);
        else
            vertex.removeProperty(IS_TRASHED_PROP_NAME);
    }

    public void verifyTrashedTree(final Vertex root)
    {
        boolean trashIndexContained = false;
        for (Vertex trashedRoot : m_trashIndex.get(TRASH_KEY_NAME, TRASH_KEY_NAME)) {
            if (root.getId().equals(trashedRoot.getId())) {
                trashIndexContained = true;
            }
        }
        assert trashIndexContained == true;

        Iterable<Edge> inEdges = root.getEdges(Direction.IN);
        assert inEdges.iterator().hasNext() == false;

        Object parentId = root.getProperty(SAVED_PARENT_ID_PROP_NAME);

        //FIXME: in orientdb, if a property is a vertexId, getProperty translate it to Vertex
        if (parentId instanceof Vertex){
            parentId = ((Vertex) parentId).getId();
        }
        Integer pos = root.getProperty(SAVED_POS_PROP_NAME);
        ArrayList<RefLinkInfo> refLinkInfos = (ArrayList<RefLinkInfo>)root.getProperty(SAVED_REFERRER_INFO_PROP_NAME);

        assert parentId != null;
        assert pos != null;
        assert refLinkInfos != null;

        verifyCachedInheritPathValid(parentId, root.getId());

        for (RefLinkInfo refLinkInfo : refLinkInfos) {
            assert root.getId().equals(refLinkInfo.m_referent) ||
                    getInheritRelation(root, getVertex(refLinkInfo.m_referent)) == InheritDirection.LINEAL_DESCENDANT;
        }

        verifyOutEdges(root);

        deepTraverse(root, new Processor() {

            public boolean run(Vertex vertex, int level) {
                if (vertex == root) {
                    return true;
                }

                verifyVertex(vertex);

                //only has one inEdge
                Iterable<Edge> inEdges = vertex.getEdges(Direction.IN);

                Iterator iterator = inEdges.iterator();
                assert iterator.hasNext() == true;
                iterator.next();
                assert iterator.hasNext() == false;

                return true;
            }
        });
    }
}
