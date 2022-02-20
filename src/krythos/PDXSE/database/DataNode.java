package krythos.PDXSE.database;

import java.util.ArrayList;
import java.util.List;

public class DataNode {
	private String m_key;
	private List<DataNode> m_nodes;
	private boolean m_isList;


	public DataNode(String key) {
		this(key, false);
	}


	public DataNode(String key, boolean isList) {
		setKey(key);
		m_nodes = new ArrayList<DataNode>(0);
		m_isList = isList;
	}


	public DataNode(String key, DataNode data, boolean isList) {
		this(key, isList);
		m_nodes.add(data);
	}


	public DataNode(String key, List<DataNode> data, boolean isList) {
		this(key, isList);
		m_nodes.addAll(data);
	}


	public void addNode(DataNode n) {
		this.m_nodes.add(n);
	}


	/**
	 * Retrieves the total bytes of the key of this {@link DataNode} and
	 * of all DataNodes nested within this DataNode.
	 * 
	 * @return Total bytes of this DataNode and nested DataNodes.
	 */
	public long byteLength() {
		long length = 0;
		length += this.getKey().length();

		for (DataNode n : this.getNodes())
			length += n.byteLength();
		return length;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DataNode) {
			DataNode n2 = (DataNode) obj;
			return n2.getKey().equals(this.getKey()) && n2.getNodes().equals(this.getNodes())
					&& n2.isList() == this.isList();
		}
		return false;
	}


	/**
	 * Finds the {@link DataNode} from the <code>path</code>, if it
	 * exists. If the size of <code>path</code> is 0, this DataNode is
	 * returned.
	 * 
	 * @param path
	 * @return DataNode specified, or <code>null</code> if the DataNode
	 *         doesn't exist.
	 */
	public DataNode find(List<Object> path) {
		if (path.size() > 0) {
			for (DataNode n : m_nodes)
				if (n.getKey().equals(path.get(0).toString())) {
					List<Object> new_path = new ArrayList<Object>(path);
					new_path.remove(0);
					return n.find(new_path);
				}
			return null;
		} else
			return this;
	}


	/**
	 * Finds the first {@link DataNode} inside of this {@code DataNode}
	 * with this key.
	 * 
	 * @param key Key to find.
	 * @return {@code DataNode} with the key or null if there is no
	 *         {@code DataNode} with the key.
	 */
	public DataNode find(String key) {
		for (DataNode n : m_nodes)
			if (n.getKey().equals(key))
				return n;
		return null;
	}


	/**
	 * Finds all the {@link DataNode DataNodes} inside of this
	 * {@code DataNode} with the given key.
	 * 
	 * @param key Key to find.
	 * @return {@code List<DataNode} of all {@code DataNodes} with the key
	 *         or {@code null} if there are no
	 *         {@code DataNodes} with the key.
	 */
	public List<DataNode> findAll(String key) {
		List<DataNode> r = new ArrayList<DataNode>();
		for (DataNode n : m_nodes)
			if (n.getKey().equals(key))
				r.add(n);
		return r.size() <= 0 ? null : r;
	}


	public String getKey() {
		return m_key;
	}


	public DataNode getNode(int index) {
		return this.m_nodes.get(index);
	}


	public List<DataNode> getNodes() {
		return this.m_nodes;
	}


	public boolean isKeyValuePair() {
		return m_nodes.size() == 1 && m_nodes.get(0).getNodes().size() == 0;
	}


	public boolean isList() {
		return m_isList;
	}


	/**
	 * Retrieves the total number of {@link DataNode}s nested within this
	 * DataNode.
	 * 
	 * @return Total number of nested DataNodes.
	 */
	public int length() {
		int length = this.getNodes().size();
		for (DataNode n : this.getNodes())
			length += n.length();
		return length;
	}


	/**
	 * Returns how many DataNodes inside this DataNode have the same key.
	 * 
	 * @param key
	 * 
	 * @return {@code Integer} of how many DataNodes share the specified
	 *         key.
	 */
	public int queryCount(String key) {
		int c = 0;
		for (DataNode n : getNodes())
			if (n.getKey().equals(key))
				c++;
		return c;
	}


	public void setKey(String key) {
		m_key = key;
	}


	@Override
	public String toString() {
		return this.getKey();
	}


	public String toString(int depth) {
		String str = this.getKey();

		if ((depth > 0 || depth < 0) && this.getNodes().size() > 0) {
			str += "[";
			for (DataNode n : this.getNodes())
				str += (n.equals(this.getNode(0)) ? "" : ", ") + n.toString(depth - 1);
			str += "]";
		}

		return str;
	}
}
