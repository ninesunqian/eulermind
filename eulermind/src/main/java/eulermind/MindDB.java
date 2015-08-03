package eulermind;

import java.nio.ByteBuffer;
import java.util.*;

import com.orientechnologies.common.util.OCallable;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.serialization.OSerializableStream;
import com.tinkerpop.blueprints.*;
import prefuse.util.PrefuseLib;

import com.orientechnologies.orient.core.db.record.ORecordLazyList;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/*
The MIT License (MIT)
Copyright (c) 2012-2014 wangxuguang ninesunqian@163.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

public class MindDB {
    Logger m_logger = LoggerFactory.getLogger(this.getClass());

	public final static String EDGE_TYPE_PROP_NAME = "t"; //type
    public final static String EDGE_INNER_ID_PROP_NAME = "i"; //EdgeInnerId

    public final static String CREATE_TIME_PROP_NAME = "c"; //创建时间

	private final static String ROOT_INDEX_NAME = "rootIndex";
	private final static String ROOT_KEY_NAME = "root";

	private final static String TRASH_INDEX_NAME = "trashIndex";
	private final static String TRASH_KEY_NAME = "trash";

    //这几个属性使用频率不高，属性名可以长点
	private final static String SAVED_PARENT_ID_PROP_NAME = "th_parent";
	private final static String SAVED_POS_PROP_NAME = "th_pos";
	public final static String SAVED_REFERRER_INFO_PROP_NAME = "th_referrers";
    public final static String IS_TRASHED_PROP_NAME = PrefuseLib.FIELD_PREFIX + "isTrashed";

    public final static String VERTEX_CLASS = "mind_node";

    public final static int MAX_OUT_EDGES = Short.MAX_VALUE - 1;

	public enum EdgeType {INCLUDE, REFERENCE};

	public OrientGraph m_graph;

	private Index<Vertex> m_rootIndex;
	private Index<Vertex> m_trashIndex;

    Object m_rootId;

	String m_path;

    EdgeVertexIdCache m_edgeVertexIdCache = new EdgeVertexIdCache();


    MindDB(String path)
	{
        //FIXME: 没有试用OrientGraph(final String url, final boolean iAutoStartTx)
        //FIXME: 需要再那里commit，并测试效率 ?

		m_graph = new OrientGraph (path, true);
		m_path = path;

        m_rootIndex = getOrCreateIndex(ROOT_INDEX_NAME);
        m_trashIndex = getOrCreateIndex(TRASH_INDEX_NAME);

        createFullTextVertexKeyIndex(MindModel.TEXT_PROP_NAME);

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

	private Vertex addVertex(Object arg0) {
        Vertex vertex =  m_graph.addVertex(null, MindModel.TEXT_PROP_NAME, "a");
        vertex.setProperty(CREATE_TIME_PROP_NAME, System.currentTimeMillis());
        return m_graph.getVertex(vertex.getId());
	}

	public Edge getEdge(Object arg0) {
		return m_graph.getEdge(arg0);
	}

	public Vertex getVertex(Object arg0) {
		return m_graph.getVertex(arg0);
	}

	public void shutdown() {
		cleanTrash ();
		m_graph.shutdown();
	}
	
	public void commit() {
		m_graph.commit();
	}

    public Index<Vertex> getOrCreateIndex(final String indexName)
	{
        Index<Vertex> index = m_graph.getIndex(indexName, Vertex.class);
        if (index == null) {
            m_graph.executeOutsideTx(new OCallable<Object, OrientBaseGraph>() {
                @Override
                public Object call(OrientBaseGraph iArgument) {
                    m_graph.createIndex(indexName, Vertex.class);
                    return null;
                }
            });
        }
        index = m_graph.getIndex(indexName, Vertex.class);

        return index;
	}

    public void dropIndex(String indexName)
    {
        //FIXME: dropIndex后马上 createIndex, 会有bug，提示该index已经存在
        m_graph.dropIndex(indexName);
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
	
	public <T> ArrayList<T> getContainerProperty (Element source, String propName)
	{
		//Because outEdgeArray must be convert to ORecordLazyList, so its type is not ArrayList.
		Object container = source.getProperty(propName);
		if (container == null)
		{
            return new ArrayList<T> ();
		}
		
		if (container instanceof ORecordLazyList)
		{
			ORecordLazyList implArray = (ORecordLazyList)container;
			implArray.setAutoConvertToRecord(false);
            return new ArrayList((implArray));

		} else {
            //return a copy list, to avoid being clear by Graph.commit
            return new ArrayList((ArrayList<Object>)container);
        }
	}

    public void setContainerProperty(Vertex source, String propName, ArrayList container)
    {
        if (container.isEmpty()) {
            source.removeProperty(propName);
        } else {
            source.setProperty(propName, container);
        }
    }

    public static String getOutEdgeInnerId(Edge edge)
    {
        return edge.getProperty(EDGE_INNER_ID_PROP_NAME);
    }

    public EdgeVertexId getEdgeVertexId(Edge edge) {
        EdgeVertexId edgeVertexId = m_edgeVertexIdCache.getEdge(edge.getId());
        if (edgeVertexId == null) {
            edgeVertexId = new EdgeVertexId(edge);
            m_edgeVertexIdCache.cacheEdge(edgeVertexId);
        }
        return edgeVertexId;
    }

    //因为要存至cache，所以不能返回null
    private List<EdgeVertexId> getOutEdgeVertexIdsFromBackDb(Vertex source)
    {
        ArrayList<EdgeVertexId> outEdgeIdPair = new ArrayList<>();
        ArrayList<Edge> noInnerIdEdges = new ArrayList<>();

        Iterator<Edge> outEdgeIterator = source.getEdges(Direction.OUT).iterator();
        while (outEdgeIterator.hasNext())
        {
            Edge outEdge = outEdgeIterator.next();
            String innerId = getOutEdgeInnerId(outEdge);
            if (innerId != null && innerId.length() > 0) {
                outEdgeIdPair.add(new EdgeVertexId(outEdge));
            } else {
                noInnerIdEdges.add(outEdge);
            }
        }

        Collections.sort(outEdgeIdPair);

        for (Edge noInnerIdEdge : noInnerIdEdges) {
            insertToOrderedOutEdges(outEdgeIdPair, -1, noInnerIdEdge);
        }

        return outEdgeIdPair;
    }

    /*
	private List<EdgeVertexId> getOutEdgeVertexIds(Vertex source, boolean updateCache)
	{
        //TODO mindModel用来判断Node是不是更新过了，应该不需要
        List<EdgeVertexId> outEdgeIdPairs = m_outEdgeInnerIdCache.get(source.getId());
        if (outEdgeIdPairs == null) {
            outEdgeIdPairs = getOutEdgeVertexIdsFromBackDb(source);
            m_outEdgeInnerIdCache.put(source.getId(), outEdgeIdPairs);
        }
        return outEdgeIdPairs;
	}
	*/

    //返回的是一份拷贝，用户可以自由修改
    public List<EdgeVertexId> getOutEdgeVertexIds(Vertex source)
    {
        List<EdgeVertexId> outEdgeVertexIds = m_edgeVertexIdCache.getOutEdgesOfOneVertex(source.getId());
        if (outEdgeVertexIds == null) {
            outEdgeVertexIds = getOutEdgeVertexIdsFromBackDb(source);
            m_edgeVertexIdCache.cacheOutEdgesOfOneVertex(source.getId(), outEdgeVertexIds);
        }

        return outEdgeVertexIds;
    }

    private EdgeVertex getParentFromBackDb(Vertex vertex)
    {
        if (vertex.getId().toString().equals("#9:2")) {
            int debug = 1;
        }
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
            return new EdgeVertex(parent, vertex, parentToVertex);
        }
    }

    public List getInheritPath(Object dbId)
    {
        assert !(dbId instanceof Vertex);

        LinkedList inheritPath = new LinkedList();

        Object parentDbId = getParentDbId(dbId);
        while (parentDbId != null) {
            inheritPath.addFirst(parentDbId);
            parentDbId = getParentDbId(parentDbId);
        }

        return inheritPath;
    }

    public boolean vertexIdIsSelf(Object thiz, Object that) {
        return thiz.equals(that);
    }

    public boolean vertexIdIsParentOf(Object thiz, Object that) {
        return vertexIdIsSelf(thiz, getParentDbId(that));
    }

    public boolean vertexIdIsChildOf(Object thiz, Object that) {
        return vertexIdIsSelf(getParentDbId(thiz), that);
    }

    public boolean vertexIdIsSiblingOf(Object thiz, Object that) {
        return vertexIdIsSelf(getParentDbId(thiz), getParentDbId(that));
    }


    public boolean vertexIdIsDescendantOf(Object thiz, Object that) {
        List thizInheritPath = getInheritPath(thiz);
        return thizInheritPath.contains(that);
    }

    public boolean vertexIdIsInSubTreeOf(Object thiz, Object that) {
        return vertexIdIsDescendantOf(thiz, that) || vertexIdIsSelf(thiz, that);
    }

    public boolean vertexIdIsAncestorOf(Object thiz, Object that) {
        List thatInheritPath = getInheritPath(that);
        return thatInheritPath.contains(thiz);
    }

    public boolean subTreeContainsVertexId(Object subTreeId, Object vertexId) {
        return vertexIdIsAncestorOf(subTreeId, vertexId) || vertexIdIsSelf(subTreeId, vertexId);
    }

	public EdgeType getEdgeType(Edge edge)
	{
        int edgeTypeValue = edge.getProperty(EDGE_TYPE_PROP_NAME);
		return EdgeType.values()[edgeTypeValue];
	}

    private char[] stringIdToCharArray(String id, int arraySize, char padding) {
        assert id.length() < arraySize;

        char chars[] = new char[arraySize];

        int i, j;

        for (i=0; i<id.length(); i++) {
            chars[i] = id.charAt(i);
        }

        for (j=i; j<arraySize; j++) {
            chars[j] = padding;
        }

        return chars;
    }

    private String getMiddleString(String lower, String upper)
    {
        //字符串比较类似纯小数的比较 "ab" < "ac" 类似  0.01 < 0.03
        //  a看作0，  z看作9

        assert lower != null;
        assert upper != null;

        if (lower.length() == 0 && upper.length() == 0) {
            return  "h"; // "h" = 0.2, 根据二八定律，用户很有可能在后面追加节点
        }

        assert lower.compareTo(upper) < 0 || upper.length() == 0;

        int charCount = lower.length() > upper.length() ? lower.length() : upper.length();
        charCount += 2; //补上两位用户扩展

        char lowerChars[] = stringIdToCharArray(lower, charCount, 'a');
        char upperChars[] =  upper.length() == 0 ?
                stringIdToCharArray("", charCount, 'z') :  stringIdToCharArray(upper, charCount, 'a');

        int i = 0;

        while(lowerChars[i] == upperChars[i]) {
            i++;
        }

        if (lowerChars[i] + 1 == upperChars[i]) {
            //0.19994 , 0.2 中间有一个 0.19995

            i++;

            //跳过中间的999
            while (lowerChars[i] == 'z') {
                i++;
            }

            if (lowerChars[i] == 'a') {
                //如果是 0.19990, 返回 0.19992
                lowerChars[i] = 'h';
            } else {
                //如果是 0.19993, 返回 0.19994
                lowerChars[i]++;
            }

        } else {
            //0.19999,  0.4123 之间有一个0.2
            lowerChars[i]++;
        }

        return new String(lowerChars, 0, i+1);
    }


    //TODO: 需要整理
    private EdgeVertexId insertToOrderedOutEdges(List<EdgeVertexId> outEdgeIdPairs, int pos, Edge edge)
    {
        String newInnerId;
        String upper;
        String lower;

        if (pos < 0) {
            pos = outEdgeIdPairs.size();
        }

        assert pos <= outEdgeIdPairs.size();

        if (pos == 0) {
            lower = "";
        } else  {
            lower = outEdgeIdPairs.get(pos - 1).m_edgeInnerId;
        }

        if (pos == outEdgeIdPairs.size()) {
            upper = "";
        } else {
            upper = outEdgeIdPairs.get(pos).m_edgeInnerId;
        }

        newInnerId = getMiddleString(lower, upper);

        edge.setProperty(EDGE_INNER_ID_PROP_NAME, newInnerId);

        EdgeVertexId edgeVertexId = new EdgeVertexId(edge);

        outEdgeIdPairs.add(pos, edgeVertexId);

        return edgeVertexId;
    }

	private Edge addEdge(Vertex source, Vertex target, int pos, EdgeType edgeType)
	{
        List<EdgeVertexId> outEdgeVertexIds = getOutEdgeVertexIds(source);

		Edge edge = m_graph.addEdge(null, source, target, "E");
        edge.setProperty(EDGE_TYPE_PROP_NAME, edgeType.ordinal());

        EdgeVertexId newEdgeVertexId = insertToOrderedOutEdges(outEdgeVertexIds, pos, edge);

        m_edgeVertexIdCache.addNewEdge(newEdgeVertexId);

		return edge;
	}

    public EdgeVertex addRefEdge(Vertex referrer, Vertex referent)
    {
        return addRefEdge(referrer, referent, -1);
    }

    public EdgeVertex addRefEdge(Vertex referrer, Vertex referent, int pos)
    {
        Edge edge = addEdge(referrer, referent, pos, EdgeType.REFERENCE);
        verifyVertex(referrer);
        verifyVertex(referent);

        return new EdgeVertex(referrer, referent, edge);
    }

    private void removeEdge (Edge edge)
    {
        Vertex source = edge.getVertex(Direction.OUT);
        m_edgeVertexIdCache.removeInvalidEdge(new EdgeVertexId(edge));
        m_graph.removeEdge(edge);
    }

    public void removeRefEdge(Edge edge)
    {
        assert getEdgeType(edge) == EdgeType.REFERENCE;
        removeEdge(edge);
    }

    public Edge getEdge(Vertex source, int pos)
	{
		List<EdgeVertexId> outEdgeIds = getOutEdgeVertexIds(source);

        if (outEdgeIds == null || outEdgeIds.isEmpty()) {
            assert source.getEdges(Direction.OUT).iterator().hasNext() == false;
            return null;
        }

        return getEdge(outEdgeIds.get(pos).m_edgeId);
	}

    public EdgeVertex addChild (Vertex parent)
    {
        return addChild(parent, -1);
    }

	public EdgeVertex addChild (Vertex parent, int pos)
	{
        m_logger.debug("MindDB insert at {}", pos);

		Vertex child = addVertex(null);
        ORecordId childId = (ORecordId) child.getId();

        //文档上说新建的节点id都是临时，经过测试id不是临时的，所以加入了判断
        if (childId.isTemporary()) {
            commit();
            child = getVertex(child.getId());
            parent = getVertex(parent.getId());
        }

        Edge edge = addEdge(parent, child, pos, EdgeType.INCLUDE);
        m_edgeVertexIdCache.addNewEdge(new EdgeVertexId(edge));

        verifyVertex(parent);
        verifyVertex(child);

        return new EdgeVertex(parent, child, edge);
	}

    public int getChildOrReferentCount(Vertex vertex)
    {
        return getOutEdgeVertexIds(vertex).size();
    }

	public EdgeVertex getChildOrReferent(Vertex parent, int pos)
	{
        List<EdgeVertexId> outEdgeIds = getOutEdgeVertexIds(parent);

        if (outEdgeIds.size() > pos) {
            return outEdgeIds.get(pos).getEdgeVertex();
        } else {
            return null;
        }
	}
	
	public ArrayList<EdgeVertex> getChildrenAndReferents(Vertex parent)
	{
		List<EdgeVertexId> outEdgeIdPairs = getOutEdgeVertexIds(parent);

		ArrayList<EdgeVertex> children = new ArrayList<EdgeVertex>();

		children.ensureCapacity(outEdgeIdPairs.size());

		for (EdgeVertexId edgeVertexId : outEdgeIdPairs)
		{
			children.add(edgeVertexId.getEdgeVertex());
		}
		return children;
	}
	
    public EdgeVertexId getParentEdgeId(Object dbId)
    {
        if (dbId.equals(m_rootId) || isInTrashIndex(dbId)) {
            if (isInTrashIndex(dbId)) {
                assert isVertexTrashed(getVertex(dbId));
            }
            return null;
        }

        EdgeVertexId edgeParentId = m_edgeVertexIdCache.getParentEdge(dbId);

        if (edgeParentId == null) {
            EdgeVertex toParent = getParentFromBackDb(getVertex(dbId));
            if (toParent == null) {
                assert false;
                /*若要修复，只能关闭所有MindView才能修复
                m_logger.warn("meet orphan vertex, move it to root");
                addEdge(getVertex(m_rootId), getVertex(dbId), -1, EdgeType.INCLUDE);
                toParent = getParentFromBackDb(getVertex(dbId));
                */
            }
            edgeParentId = toParent.getEdgeVertexId();
            m_edgeVertexIdCache.cacheEdge(edgeParentId);
        }

        return edgeParentId;
    }

    public Object getParentDbId(Object dbId)
    {
        EdgeVertexId edgeParentId = getParentEdgeId(dbId);
        if (edgeParentId == null) {
            return null;
        } else {
            return edgeParentId.m_sourceId;
        }
    }


    public EdgeVertex getParentEge(Vertex vertex)
    {
        if (vertex.getId().equals(m_rootId)) {
            return null;
        }

        EdgeVertexId edgeParentId = m_edgeVertexIdCache.getParentEdge(vertex.getId());

        if (edgeParentId == null) {
            EdgeVertex toParent = getParentFromBackDb(vertex);
            edgeParentId = new EdgeVertexId(toParent.m_edge);
            m_edgeVertexIdCache.cacheEdge(edgeParentId);
            return toParent;
        }

        return edgeParentId.getEdgeVertex();
    }


    public EdgeVertex handoverChild(Vertex child, Vertex newParent, int toPos)
    {
        assert ! vertexIdIsDescendantOf(newParent.getId(), child.getId());

        EdgeVertex oldParent = getParentEge(child);
        removeEdge(oldParent.m_edge);

        Edge edge = addEdge(newParent, child, toPos, EdgeType.INCLUDE);

        verifyVertex(oldParent.m_source);
        verifyVertex(newParent);
        verifyVertex(child);

        return new EdgeVertex(newParent, child, edge);
    }

    //由于两个节点可能加入多个引用边，只有边能确定是哪个引用
    public EdgeVertex handoverReferent(Edge oldEdge, Vertex toReferrer, int toPos)
    {
        assert getEdgeType(oldEdge) == EdgeType.REFERENCE;
        Vertex referent = getEdgeTarget(oldEdge);
        removeEdge(oldEdge);

        Edge newEdge = addEdge(toReferrer, referent, toPos, EdgeType.REFERENCE);

        verifyVertex(toReferrer);
        verifyVertex(referent);

        return new EdgeVertex(toReferrer, referent, newEdge);
    }

    public EdgeVertexId changeEdgePos(Edge edge, int newPos)
    {
        Vertex source;
        Object sourceId;

        EdgeVertexId edgeVertexId = m_edgeVertexIdCache.getEdge(edge.getId());
        if (edgeVertexId != null) {
            sourceId = edgeVertexId.m_sourceId;
            source = m_graph.getVertex(sourceId);
        } else {
            source = getEdgeSource(edge);
            sourceId = source.getId();
        }

        List<EdgeVertexId> outEdgeVertexIds = getOutEdgeVertexIds(source);

        int oldPos = getEdgeIndex(outEdgeVertexIds, edge.getId());

        if (oldPos == newPos) {
            return edgeVertexId;
        }

        outEdgeVertexIds.remove(new EdgeVertexId(edge));

        EdgeVertexId newEdgeVertexId = insertToOrderedOutEdges(outEdgeVertexIds, newPos, edge);
        m_edgeVertexIdCache.updateEdgeInnerId(newEdgeVertexId);

        return newEdgeVertexId;
    }

    public ArrayList<EdgeVertex> getReferrers(Vertex referent)
	{
		Iterator<Edge> edgeIterator = referent.getEdges(Direction.IN).iterator();
		Edge refEdge;
		ArrayList<EdgeVertex> referrerArray = new ArrayList<> ();
		
		while (edgeIterator.hasNext())
		{
			refEdge = edgeIterator.next();
			
			if (getEdgeType(refEdge) == EdgeType.REFERENCE)
			{
				Vertex referrer = getEdgeSource(refEdge);
				referrerArray.add(new EdgeVertex(referrer, referent, refEdge));
			}
		}

        Collections.sort(referrerArray);
		
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
						deepTraverse(child.m_target, proc, level+1);
					}
				}
			}
		}
	}

	private void deepTraverse (Vertex vertex, Processor proc)
	{
		deepTraverse(vertex, proc, 0);
	}

    //删除子树时用，保存子树之外的节点到子树之内的节点的引用关系
	public static class RefLinkInfo implements OSerializableStream, Comparable<RefLinkInfo> {
		public final Object m_referrer;
		public final Object m_referent;
        public final Object m_edge;
		public final int m_pos;
		
		RefLinkInfo (Object referrer, Object referent, Object edge, int pos)
		{
			m_referrer = referrer;
			m_referent = referent;
            m_edge = edge;
			m_pos = pos;

            assert referent instanceof ORecordId;
            assert referrer instanceof ORecordId;
            assert edge instanceof ORecordId;
		}

        public byte[] toStream() throws OSerializationException
        {
            byte referrerBytes[] = ((ORecordId)m_referrer).toStream();
            byte referentBytes[] = ((ORecordId)m_referent).toStream();
            byte edgeBytes[] = ((ORecordId)m_edge).toStream();

            ByteBuffer byteBuffer = ByteBuffer.allocate(16 + referrerBytes.length + referentBytes.length + edgeBytes.length);

            byteBuffer.putInt(m_pos);
            byteBuffer.putInt(referrerBytes.length);
            byteBuffer.putInt(referentBytes.length);
            byteBuffer.putInt(edgeBytes.length);

            byteBuffer.put(referrerBytes);
            byteBuffer.put(referentBytes);
            byteBuffer.put(edgeBytes);

            //orientdb内部不是直接存byte[], 而是先把它当成utf-8字符串先解析一遍，再存储。不知道为什么这样
            //所以这里用base64 转换一下
            return Base64.encodeBase64(byteBuffer.array());
        }

        public RefLinkInfo fromStream(byte[] iStream) throws OSerializationException
        {
            iStream = Base64.decodeBase64(iStream);
            ByteBuffer byteBuffer = ByteBuffer.wrap(iStream);
            int pos = byteBuffer.getInt();
            int referrerByteLength = byteBuffer.getInt();
            int referentByteLength = byteBuffer.getInt();
            int edgeByteLength = byteBuffer.getInt();

            int from = 16;
            int to = from + referrerByteLength;
            byte referrerByte[] = Arrays.copyOfRange(iStream, from, to);

            from = to;
            to += referentByteLength;
            byte referentByte[] = Arrays.copyOfRange(iStream, from, to);

            from = to;
            to += edgeByteLength;
            byte edgeByte[] = Arrays.copyOfRange(iStream, from, to);

            assert to == iStream.length;

            ORecordId referrer = (new ORecordId()).fromStream(referrerByte);
            ORecordId referent = (new ORecordId()).fromStream(referentByte);
            ORecordId edge = (new ORecordId()).fromStream(edgeByte);
            return new RefLinkInfo(referrer, referent, edge, pos);
        }

        public int compareTo(RefLinkInfo other) {
            if (! m_referrer.equals(other.m_referrer)) {
                return m_referrer.hashCode() - other.m_referrer.hashCode();
            } else {
                return m_pos - other.m_pos;
            }
        }
    }

    public int getEdgeIndex(List<EdgeVertexId> edges, Object edgeId)
    {
        for (int i=0; i<edges.size(); i++) {
            if (edges.get(i).m_edgeId.equals(edgeId)) {
                return i;
            }
        }
        return -1;
    }

	//return the removed vertex
	public void trashSubTree(final Vertex removedVertex)
	{
        //collect the refer info, to help update display tree
        final ArrayList<RefLinkInfo> refLinkInfos = new ArrayList<> ();

		deepTraverse(removedVertex, new Processor() {
			
			public boolean run(Vertex vertex, int level) {
				ArrayList<EdgeVertex> referrers = getReferrers(vertex);
				
				if (referrers != null)
				{
					for (EdgeVertex referrer : referrers)
					{
                        //跳过被删除子树内部的引用关系
                        if (subTreeContainsVertexId(removedVertex.getId(), referrer.m_source.getId())) {
                            continue;
                        }

                        //因为重复利用率不高，所以此处不必再加入m_outEdgeInnerIdCache
                        List<EdgeVertexId> outEdgeIdPairsOfReferrer = m_edgeVertexIdCache.getOutEdgesOfOneVertex(referrer.m_source.getId());
                        if (outEdgeIdPairsOfReferrer == null) {
                            outEdgeIdPairsOfReferrer = getOutEdgeVertexIdsFromBackDb(referrer.m_source);
                        }

                        int edgeIndex = getEdgeIndex(outEdgeIdPairsOfReferrer, referrer.m_edge.getId());

                        refLinkInfos.add(new RefLinkInfo(referrer.m_source.getId(), referrer.m_target.getId(),
                                referrer.m_edge.getId(), edgeIndex));

                        removeRefEdge(referrer.m_edge);
					}
				}

                vertex.setProperty(IS_TRASHED_PROP_NAME, System.currentTimeMillis());

				return true;
			}
		});


        //由于被删除节点的父节点可能引用了子树的其他节点。 所以: 删除时，先删除引用边，再删除子树, 恢复时，先恢复子树，再恢复引用边
        //而且恢复时，引用边要根据边的位置，防止位置错乱

        EdgeVertex edgeParent = getParentEge(removedVertex);
        List<EdgeVertexId> outEdgeIdPairsOfRemovedVertex = getOutEdgeVertexIds(edgeParent.m_source);
        int removedVertexIndex = getEdgeIndex(outEdgeIdPairsOfRemovedVertex, edgeParent.m_edge.getId());

		
		removedVertex.setProperty(SAVED_PARENT_ID_PROP_NAME, edgeParent.m_source.getId());
		removedVertex.setProperty(SAVED_POS_PROP_NAME, removedVertexIndex);

        //需要先按照边的位置排序，保证恢复的时候，按照位置从小到大的顺序添加引用边
        Collections.sort(refLinkInfos);
        setContainerProperty(removedVertex, SAVED_REFERRER_INFO_PROP_NAME, refLinkInfos);

        removeEdge(edgeParent.m_edge);

		m_trashIndex.put(TRASH_KEY_NAME, TRASH_KEY_NAME, removedVertex);

        verifyTrashedTree(removedVertex);
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
        if (! isInTrashIndex(vertex.getId())) {
            return null;
        }

        Object parentId = vertex.getProperty(SAVED_PARENT_ID_PROP_NAME);
        if (parentId instanceof Vertex) {
            parentId = ((Vertex)parentId).getId();
        }

        int pos = (Integer)vertex.getProperty(SAVED_POS_PROP_NAME);
        ArrayList<Object> refLinkInfoes = getContainerProperty(vertex, SAVED_REFERRER_INFO_PROP_NAME);

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
        assert isInTrashIndex(vertex.getId());

		Object parentId = vertex.getProperty(SAVED_PARENT_ID_PROP_NAME);
        if (parentId instanceof Vertex) {
            parentId = ((Vertex)parentId).getId();
        }

		int pos = vertex.getProperty(SAVED_POS_PROP_NAME);
		ArrayList<Object> refLinkInfoes = getContainerProperty(vertex, SAVED_REFERRER_INFO_PROP_NAME);
		
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
		return new EdgeVertex(parent, vertex, edge);
	}

    private void removeSubTree(Vertex root)
    {
        for (EdgeVertex edgeVertex : getChildrenAndReferents(root)) {
            if (getEdgeType(edgeVertex.m_edge) == EdgeType.INCLUDE) {
                removeSubTree(edgeVertex.m_target);
            }
        }

        try {
            Long trashedTime =  root.getProperty(IS_TRASHED_PROP_NAME);
            if (trashedTime == null) {
                return;
            }

            assert trashedTime != null;
            Date date = new Date(trashedTime);

            m_logger.info("remove vertex trashed at {}",  date);
            m_graph.removeVertex(root);

        } catch (ORecordNotFoundException e) {
            m_logger.warn("remove Vertex: not found: {}, exception {}", root.getId(), e);
        }
    }

    private boolean isInTrashIndex(Object dbId) {

        for (Vertex trashedRoot : m_trashIndex.get(TRASH_KEY_NAME, TRASH_KEY_NAME)) {
            //FIXME: 会不会null做为遍历的结尾，有待验证
            if (trashedRoot == null) {
                break;
            }
            if (dbId.equals(trashedRoot.getId())) {
                return true;
            }
        }
        return false;
    }

	private void cleanTrash ()
	{
        for (Vertex vertex: m_trashIndex.get(TRASH_KEY_NAME, TRASH_KEY_NAME)) {
            if (vertex == null) {
                break;
            }

            try {
                removeSubTree(vertex);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                m_logger.error("RemoveSubTree: {}: {}", vertex.getId(), vertex.getProperty(MindModel.TEXT_PROP_NAME));
            }
        }
	}
	
	public void copyProperty(Element from, Element to)
	{
        assert from.getClass() == to.getClass();
		for (String key : from.getPropertyKeys())
		{
            to.setProperty(key, from.getProperty(key));
		}
	}

	public GraphQuery query() {
		return m_graph.query();
	}

    public void createFullTextVertexKeyIndex(final String key)
    {
        Set<String> indexedKeys = m_graph.getIndexedKeys(Vertex.class);

        for (String indexedKey : indexedKeys) {
            if (indexedKey.equals(key)) {
                return;
            }
        }

        m_graph.executeOutsideTx(new OCallable<Object, OrientBaseGraph>() {
            @Override
            public Object call(OrientBaseGraph iArgument) {
                OClass type = m_graph.getVertexBaseType();
                if (!type.existsProperty(key)) {
                    type.createProperty(key, OType.STRING);
                }

                ODocument metadata = new ODocument();
                metadata.fromJSON("{\"analyzer\":\"org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer\"}");

                type.createIndex("V."+key, "FULLTEXT", null, metadata, "LUCENE", new String[]{key});
                return null;
            }
        });
    }

    public Iterable<Vertex> getVertices(String key, String value)
    {
        return m_graph.getVertices(key, value);
    }

    public Iterable<Vertex> getVertices(final String iKey, Object iValue) {
        return m_graph.getVertices(iKey, iValue);
    }

    public Iterable<Vertex> getVertices() {
        return m_graph.getVertices();
    }

    private void verifyCachedInheritPathValid(Object parentDbId, Object childDbId)
    {
        List childInheritPath = getInheritPath(childDbId);
        List parentInheritPath = getInheritPath(parentDbId);
        assert childInheritPath.size() > 0;
        assert childInheritPath.get(childInheritPath.size()-1).equals(parentDbId) &&
                childInheritPath.subList(0, childInheritPath.size()-1).equals(parentInheritPath);
    }

    private void verifyOutEdges(Vertex vertex)
    {
        List<EdgeVertexId> outEdgeVertexIds = getOutEdgeVertexIds(vertex);
        Map<String, Integer> outEdgeIdPairExists = new HashMap<>();

        for(EdgeVertexId edgeVertexId : outEdgeVertexIds) {
            outEdgeIdPairExists.put(edgeVertexId.m_edgeInnerId, 0);
        }

        int edgeNumber = 0;
        for (Edge outEdge : vertex.getEdges(Direction.OUT)) {
            edgeNumber++;

            EdgeVertexId outEdgeIdPair = getEdgeVertexId(outEdge);
            assert outEdgeVertexIds.contains(outEdgeIdPair);
            outEdgeIdPairExists.put(outEdgeIdPair.m_edgeInnerId, outEdgeIdPairExists.get(outEdgeIdPair.m_edgeInnerId) + 1);

            if (getEdgeType(outEdge) == EdgeType.INCLUDE) {
                Vertex childVertex = getEdgeTarget(outEdge);
                verifyCachedInheritPathValid(vertex.getId(), childVertex.getId());

            } else {
                assert getEdgeType(outEdge) == EdgeType.REFERENCE;
            }
        }

        Iterator iter = outEdgeIdPairExists.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<EdgeVertexId, Integer> entry = (Map.Entry) iter.next();
            assert entry.getValue() == 1;
        }

        assert (edgeNumber == outEdgeVertexIds.size());
    }

    private void verifyInEdges(Vertex vertex)
    {
        int metParent = 0;

        for (Edge inEdge : vertex.getEdges(Direction.IN)) {
            Vertex parentOrReferrerVertex = getEdgeSource(inEdge);

            List<EdgeVertexId> parentOrReferrerOutEdgeIdPairs = getOutEdgeVertexIds(parentOrReferrerVertex);
            assert parentOrReferrerOutEdgeIdPairs.contains(getEdgeVertexId(inEdge));

            if (getEdgeType(inEdge) == EdgeType.INCLUDE) {
                verifyCachedInheritPathValid(parentOrReferrerVertex.getId(), vertex.getId());
                metParent ++;
            } else {
                assert(getEdgeType(inEdge) == EdgeType.REFERENCE);
            }
        }

        if (!vertex.getId().equals(m_rootId)) {
            assert metParent == 1;
        } else {
            assert metParent == 0;
        }
    }

    public void verifyVertex(Vertex vertex)
    {
        //after commit, must add it
        vertex = m_graph.getVertex(vertex.getId());

        verifyInEdges(vertex);
        verifyOutEdges(vertex);
    }

    public boolean isVertexTrashed(Vertex vertex)
    {
        return vertex.getProperty(IS_TRASHED_PROP_NAME) != null;
    }

    private void setVertexTrashed(Vertex vertex, boolean trashed)
    {
        if (trashed == true)
            vertex.setProperty(IS_TRASHED_PROP_NAME, System.currentTimeMillis());
        else
            vertex.removeProperty(IS_TRASHED_PROP_NAME);
    }

    public void verifyTrashedTree(final Vertex root)
    {
        assert isInTrashIndex(root.getId());

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
        assert refLinkInfos == null || !refLinkInfos.isEmpty();

        if (refLinkInfos != null) {
            for (RefLinkInfo refLinkInfo : refLinkInfos) {
                assert root.getId().equals(refLinkInfo.m_referent) ||
                        vertexIdIsAncestorOf(root.getId(), refLinkInfo.m_referent);
            }
        }

        verifyOutEdges(root);

        deepTraverse(root, new Processor() {

            public boolean run(Vertex vertex, int level) {
                if (vertex == root) {
                    return true;
                }

                verifyVertex(vertex);

                return true;
            }
        });
    }

    //这是一个保存临时信息的类
    public class EdgeVertex implements Comparable <EdgeVertex> {
        final public Vertex m_source;
        final public Vertex m_target;
        final public Edge m_edge;
        final public String m_edgeInnerId;

        public EdgeVertex(Vertex source, Vertex target, Edge edge) {
            m_source = source;
            m_edge = edge;
            m_target = target;
            m_edgeInnerId = getOutEdgeInnerId(edge);

            assert  m_edge.getVertex(Direction.OUT).getId().equals(m_source);
            assert  m_edge.getVertex(Direction.IN).getId().equals(m_target);
        }

        public int compareTo(EdgeVertex other) {
            if (! m_source.getId().equals(other.m_source.getId())) {
                return m_source.getId().hashCode() - other.m_source.getId().hashCode();
            } else {
                return getOutEdgeInnerId(m_edge).compareTo(getOutEdgeInnerId(other.m_edge));
            }
        }

        public EdgeVertexId getEdgeVertexId ()
        {
            return new EdgeVertexId(m_source.getId(), m_target.getId(), m_edge.getId(), m_edgeInnerId);
        }
    };


    public class EdgeVertexId  implements Comparable <EdgeVertexId> {
        public final Object m_sourceId;
        public final Object m_edgeId;
        public final Object m_targetId;
        public final String m_edgeInnerId; //用于排序，无其他用途

        EdgeVertexId(Edge edge) {

            m_sourceId = edge.getVertex(Direction.OUT).getId();
            m_targetId = edge.getVertex(Direction.IN).getId();
            m_edgeId = edge.getId();
            m_edgeInnerId = getOutEdgeInnerId(edge);

            assert m_targetId.equals(m_rootId) || m_sourceId != null;
        }

        //用于通过一个EdgeVertexId，修改 edgeInnerId后，重新生成一个EdgeVertexId
        EdgeVertexId(Object sourceId, Object targetId, Object edgeId, String edgeInnerId) {
            m_sourceId = sourceId;
            m_targetId = targetId;
            m_edgeId = edgeId;
            m_edgeInnerId = edgeInnerId;

            assert m_targetId.equals(m_rootId) || m_sourceId != null;
        }

        public int compareTo(EdgeVertexId other) {
            if (! m_sourceId.equals(other.m_sourceId)) {
                return m_sourceId.hashCode() - other.m_sourceId.hashCode();
            } else {
                return m_edgeInnerId.compareTo(other.m_edgeInnerId);
            }
        }

        @Override
        public boolean equals(Object other)
        {
            if(this == other){
                return true;
            }

            if(other == null){
                return false;
            }

            if(this.getClass() != other.getClass()){
                return false;
            }

            EdgeVertexId otherEdgeVertexid = (EdgeVertexId)other;

            if (m_edgeId.equals(((EdgeVertexId) other).m_edgeId)) {
                assert m_sourceId.equals(((EdgeVertexId) other).m_sourceId);
                assert m_targetId.equals(((EdgeVertexId) other).m_targetId);
                assert m_edgeInnerId.equals(((EdgeVertexId) other).m_edgeInnerId);
                return true;
            } else {
                assert (! m_sourceId.equals(((EdgeVertexId) other).m_sourceId)) ||
                        (! m_targetId.equals(((EdgeVertexId) other).m_targetId)) ||
                        (! m_edgeInnerId.equals(((EdgeVertexId) other).m_edgeInnerId));
                return false;
            }
        }

        EdgeVertex getEdgeVertex()
        {
            return new EdgeVertex(m_graph.getVertex(m_sourceId), m_graph.getVertex(m_targetId), m_graph.getEdge(m_edgeId));
        }
    }

    class EdgeVertexIdCache {

        final int CACHE_CAPACITY = 2048;

        //vertex到父节点的映射
        LinkedHashMap<Object, EdgeVertexId> m_parentEdgeMap =
                new LinkedHashMap<Object, EdgeVertexId>(512, 0.75f, true) {

                    protected boolean removeEldestEntry (Map.Entry<Object, EdgeVertexId> eldest) {
                        return size() > CACHE_CAPACITY;
                    }
                };

        //vertex到子节点的映射
        LinkedHashMap<Object, TreeMap<String, EdgeVertexId>> m_outEdgesMap =
                new LinkedHashMap<Object, TreeMap<String, EdgeVertexId>>(512, 0.75f, true) {

                    protected boolean removeEldestEntry (Map.Entry<Object, TreeMap<String, EdgeVertexId>> eldest) {
                        return size() > CACHE_CAPACITY;
                    }
                };

        //edgeId到EdgeVertexId的映射
        LinkedHashMap<Object, EdgeVertexId> m_edges =
            new LinkedHashMap<Object, EdgeVertexId>(512, 0.75f, true) {

                protected boolean removeEldestEntry (Map.Entry<Object, EdgeVertexId> eldest) {
                    return size() > CACHE_CAPACITY;
                }
            };

        void cacheEdge(EdgeVertexId edge)
        {
            m_edges.put(edge.m_edgeId, edge);

            if (getEdgeType(m_graph.getEdge(edge.m_edgeId)) == EdgeType.INCLUDE) {
                m_parentEdgeMap.put(edge.m_targetId, edge);
            }
        }

        EdgeVertexId getEdge(Object edgeId) {
            return m_edges.get(edgeId);
        }

        EdgeVertexId getParentEdge(Object childId) {
            return m_parentEdgeMap.get(childId);
        }

        void cacheOutEdgesOfOneVertex(Object sourceVertexId, List<EdgeVertexId> outEdgeVertexIds)
        {
            if (m_outEdgesMap.get(sourceVertexId) != null) {
                m_outEdgesMap.remove(sourceVertexId);
            }

            TreeMap<String, EdgeVertexId> outEdgeMap = new TreeMap<>();

            for (EdgeVertexId edge : outEdgeVertexIds) {
                outEdgeMap.put(edge.m_edgeInnerId, edge);
                cacheEdge(edge);
            }

            m_outEdgesMap.put(sourceVertexId, outEdgeMap);
        }

        List<EdgeVertexId> getOutEdgesOfOneVertex(Object sourceVertexId)
        {
            TreeMap<String, EdgeVertexId> outEdgeVertexIds = m_outEdgesMap.get(sourceVertexId);
            if (outEdgeVertexIds == null) {
                return null;
            }

            return new ArrayList<EdgeVertexId>(outEdgeVertexIds.values());
        }

        void addNewEdge(EdgeVertexId edge) {
            m_edges.put(edge.m_edgeId, edge);

            if (getEdgeType(m_graph.getEdge(edge.m_edgeId)) == EdgeType.INCLUDE) {
                m_parentEdgeMap.put(edge.m_targetId, edge);
            }

            TreeMap<String, EdgeVertexId> outEdgeVertexIds = m_outEdgesMap.get(edge.m_sourceId);
            if (outEdgeVertexIds != null) {
                outEdgeVertexIds.put(edge.m_edgeInnerId, edge);
            }
        }

        //边已经被删除了，所以就不能简单传一个edgeId了
        void removeInvalidEdge(EdgeVertexId edge) {
            m_edges.remove(edge.m_edgeId);

            //因为不知道是否是父子边，所以直接删除，缓存的父子映射
            //TODO: EdgeVertexId中加入边的类型，以便提高性能
            m_parentEdgeMap.remove(edge.m_targetId);

            TreeMap<String, EdgeVertexId> outEdgeVertexIds = m_outEdgesMap.get(edge.m_sourceId);
            if (outEdgeVertexIds != null) {
                EdgeVertexId removedEdge = outEdgeVertexIds.remove(edge.m_edgeInnerId);
                assert removedEdge.m_edgeId.equals(edge.m_edgeId);
                assert removedEdge.m_edgeInnerId.equals(edge.m_edgeInnerId);
            }
        }

        void updateEdgeInnerId(EdgeVertexId newEdge)
        {
            m_edges.remove(newEdge.m_edgeId);
            m_edges.put(newEdge.m_edgeId, newEdge);

            if (getEdgeType(m_graph.getEdge(newEdge.m_edgeId)) == EdgeType.INCLUDE) {
                m_parentEdgeMap.remove(newEdge.m_targetId);
                m_parentEdgeMap.put(newEdge.m_targetId, newEdge);
            }

            TreeMap<String, EdgeVertexId> outEdgeVertexIds = m_outEdgesMap.get(newEdge.m_sourceId);
            if (outEdgeVertexIds != null) {
                for (Map.Entry<String, EdgeVertexId> entry : outEdgeVertexIds.entrySet()) {
                    if (entry.getValue().m_edgeId.equals((newEdge.m_edgeId))) {
                            outEdgeVertexIds.remove(entry.getKey());
                            outEdgeVertexIds.put(newEdge.m_edgeInnerId, newEdge);
                            return;
                    }
                }
            }

            assert false;
        }
    }
}
