package excitedmind;

import java.util.ArrayList;

import prefuse.util.PrefuseLib;

import com.orientechnologies.orient.core.db.record.ORecordLazyList;
import com.sun.awt.AWTUtilities.Translucency;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class DBTree implements Graph {
	
	public final static String EDGE_TYPE_PROP_NAME = PrefuseLib.FIELD_PREFIX + "edgeType";
	private final static String CHILD_EDGES_PROP_NAME = PrefuseLib.FIELD_PREFIX + "childEdges";
	
	private final static int EDGE_TYPE_INCLUDE = 0;
	private final static int EDGE_TYPE_REFERENCE = 1;
	
    protected static final int ADDING_EDGE_END = 0x7FFFFFFF;
	
	private OrientGraph m_graph;
	
	private Index<Vertex> m_rootIndices; 
	private Index<Vertex> m_trashIndices; 
	
	String m_path;
	
	DBTree (String path)
	{
		m_graph = new OrientGraph (path);
		m_path = path;
		createIndices ();
	}
	
	@Override
	public Edge addEdge(Object arg0, Vertex arg1, Vertex arg2, String arg3) {
		//disable the method, to preserve a tree structure
		assert (false);
		return m_graph.addEdge(arg0, arg1, arg2, arg3);
	}
	
	@Override
	public Vertex addVertex(Object arg0) {
		return m_graph.addVertex(arg0);
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
		m_graph.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
	}
	
	private void createIndices ()
	{
		final String rootIndices = "rootIndices";
		m_rootIndices = m_graph.getIndex (rootIndices, Vertex.class);
		
		if (m_rootIndices == null)
		{
			m_rootIndices = m_graph.createIndex(rootIndices, Vertex.class);
		}
		
		final String trashIndices = "rootIndices";
		m_trashIndices = m_graph.getIndex (trashIndices, Vertex.class);
		
		if (m_rootIndices == null)
		{
			m_trashIndices = m_graph.createIndex(trashIndices, Vertex.class);
		}
	}
	
	public Vertex addRoot ()
	{
		Vertex root = m_graph.addVertex(null);
		m_rootIndices.put("root", "root", root);
		Object rootId = root.getId();
		Object oldId = rootId;
		System.out.println(root.getId());
		commit ();
		System.out.println(root.getId());
		m_graph = new OrientGraph (m_path);
		root = m_graph.getVertex(rootId);
		rootId = root.getId();
		System.out.println(root.getId());
		return root;
	}

	
	public class EdgeVertex {
		final public Vertex m_vertex;
		final public Edge m_edge;
		
		public EdgeVertex(Edge edge, Vertex vertex) {
			m_vertex = vertex;
			m_edge = edge;
		}
	};
	
	private Vertex getParentOnEdge (Edge edge)
	{
		return edge.getVertex(Direction.OUT);
	}
	
	private Vertex getChildOnEdge (Edge edge)
	{
		return edge.getVertex(Direction.IN);
	}
	
	private ArrayList<Object> getEdgeIDsToChildren (Vertex parent)
	{
		Object obj = parent.getProperty(CHILD_EDGES_PROP_NAME);
		if (obj == null)
			return null;
		
		ORecordLazyList implArray = (ORecordLazyList)obj;
		implArray.setAutoConvertToRecord(false);
		
		return (ArrayList<Object>)obj;
	}
	
	private int getEdgePos (ArrayList<Object> edgeIds, Vertex outVertex)
	{
		for (int i=0; i<edgeIds.size(); i++)
		{
			Object edgeId = edgeIds.get(i);
			Edge edge = getEdge (edgeId);
			if (edge.getVertex(Direction.OUT) == outVertex)
			{
				return i;
			}
		}
		return -1;
	}
	
	public Edge getEdge (Vertex start, Vertex end)
	{
		ArrayList<Object> childEdgeArray = getEdgeIDsToChildren(start);
		int pos = getEdgePos(childEdgeArray, end);
		
		if (pos == -1)
		{
			return null;
		}
		else
		{
			return getEdge(childEdgeArray.get(pos));
		}
	}
	
	public int getEdgeType (Edge edge)
	{
		return (Integer)edge.getProperty(EDGE_TYPE_PROP_NAME);
	}
	
	
	public EdgeVertex getBioParent(Vertex vertex)
	{
		Edge parentToVertex = vertex.getEdges(Direction.IN).iterator().next();
		
		if (parentToVertex == null) {
			return null;
			
		} else {
			Vertex parent = getParentOnEdge(parentToVertex);
			return new EdgeVertex(parentToVertex, parent);
		}
	}
	
	public EdgeVertex getChild(Vertex parent, int pos)
	{
		Edge childRelation = getEdgeToChild(parent, pos);
		if (childRelation == null)
		{
			return null;
		}
		else
		{
			Vertex child = getChildOnEdge(childRelation);
			return new EdgeVertex(childRelation, child);
		}
	}
	
	public EdgeVertex[] getChildren(Vertex parent)
	{
		ArrayList<Object> edgeIDsToChildren = getEdgeIDsToChildren(parent);
		if (edgeIDsToChildren == null)
		{
			return null;
		}
		else
		{
			EdgeVertex [] children = new EdgeVertex[edgeIDsToChildren.size()];
			System.out.println (edgeIDsToChildren);
			System.out.println (edgeIDsToChildren.get(0));
			System.out.println (edgeIDsToChildren);
			System.out.println (edgeIDsToChildren.get(1));
			for (int i=0; i<children.length; i++)
			{
				Edge edgeToChild = getEdge(edgeIDsToChildren.get(i));
				Vertex child = getChildOnEdge(edgeToChild);
				children [i] = new EdgeVertex(edgeToChild, child);
				System.out.println ("BBBPPP:  "+parent+"->"+child + "  : " + edgeToChild);
			}
			
			return children;
		}
	}
	
	public EdgeVertex addChild (Vertex parent, int pos)
	{
		Vertex child = addVertex(null);
		commit ();
		Edge edge = setParentChildEdge(parent, child, pos);
		return new EdgeVertex(edge, child);
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
	
	
	private Edge setParentChildEdge (Vertex parent, Vertex child, int pos)
	{
		Edge edge = m_graph.addEdge(null, parent, child, null);
		edge.setProperty(EDGE_TYPE_PROP_NAME, EDGE_TYPE_INCLUDE); 
		
		ArrayList<Object> childEdgeArray = (ArrayList<Object>)parent.getProperty(CHILD_EDGES_PROP_NAME);
		
		//FIXME: to make the edge'id is to local db
		commit ();
		
		if (childEdgeArray == null)
		{
			childEdgeArray = new ArrayList<Object> ();
			parent.setProperty(CHILD_EDGES_PROP_NAME, childEdgeArray);
		}
		
		if (pos == ADDING_EDGE_END || pos >= childEdgeArray.size())
		{
			childEdgeArray.add(edge.getId());
		}
		else
		{
			childEdgeArray.add(pos, edge.getId());
		}
		
		System.out.println (parent.getId().toString() + "-" + child.getId().toString() 
				+ "   : " + edge.getId().toString() +  "  pos:" + pos);
		commit ();
		System.out.println ("table = " + (ArrayList<Object>)parent.getProperty(CHILD_EDGES_PROP_NAME));
		return edge;
	}
	
	private void removeChildEdge (Vertex parent, int childPos)
	{
		ArrayList<Object> childEdgeArray = getEdgeIDsToChildren(parent);
		Object edgeId = childEdgeArray.get(childPos);
		m_graph.removeEdge(m_graph.getEdge(edgeId));
		childEdgeArray.remove(childPos);
		commit ();
	}
	
	
	public Edge setRefEdge (Vertex referer, Vertex referee, int pos)
	{
		if (referer.getId() == referee.getId())
		{
			return null;
		}
		
		Edge edge = setParentChildEdge(referer, referee, pos);
		edge.setProperty(EDGE_TYPE_PROP_NAME, EDGE_TYPE_REFERENCE);
		commit ();
		return edge;
	}
	
	private Object removeEdgeIDFromArray (Vertex startVertex, Vertex endVertex)
	{
		ArrayList<Object> childEdgeArray = getEdgeIDsToChildren(startVertex);
		
		for (Object edgeId : childEdgeArray)
		{
			Edge edge = getEdge(edgeId);
			if (edge.getVertex(Direction.OUT) == endVertex)
			{
				childEdgeArray.remove(edgeId);
				//FIXME: need to reset the property to vertex ?
				commit ();
				return edgeId;
			}
		}
		
		assert (false);
		return null;
	}
	
	public void removeRefEdge (Vertex referer, Vertex referee)
	{
		Object edgeId = removeEdgeIDFromArray(referer, referee);
		commit ();
		Edge edge = getEdge (edgeId);
		assert ((Integer)edge.getProperty(EDGE_TYPE_PROP_NAME) == EDGE_TYPE_REFERENCE);
	}
	
	
	private Edge getEdgeToChild (Vertex parent, int pos)
	{
		ArrayList<Object> childEdgeArray = (ArrayList<Object>)parent.getProperty(CHILD_EDGES_PROP_NAME);
		
		if (childEdgeArray == null)
		{
			return null;
		}
		else
		{
			return getEdge(childEdgeArray.get(pos));
		}
	}
	
	//remove vertex, the children append to 
	public void removeSubTree (Vertex vertex)
	{
		
	}
	
	public EdgeVertex restoreSubTree (Vertex vertex)
	{
		return null;
	}
	
	private void cleanTrash ()
	{
		
	}
	
	
	
	
}
