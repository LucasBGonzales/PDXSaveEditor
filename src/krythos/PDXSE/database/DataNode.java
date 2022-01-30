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


	public DataNode(String key, DataNode data, boolean isList) {
		this(key, isList);
		m_nodes.add(data);
	}


	public DataNode(String key, List<DataNode> data, boolean isList) {
		this(key, isList);
		m_nodes.addAll(data);
	}


	public DataNode(String key, boolean isList) {
		setKey(key);
		m_nodes = new ArrayList<DataNode>(0);
		m_isList = isList;
	}


	/**
	 * Find's the first DataNode inside of this DataNode with this key.
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


	public boolean isList() {
		return m_isList;
	}


	public void addNode(DataNode n) {
		this.m_nodes.add(n);
	}


	public List<DataNode> getNodes() {
		return this.m_nodes;
	}


	public DataNode getNode(int index) {
		return this.m_nodes.get(index);
	}


	public boolean isKeyValuePair() {
		return m_nodes.size() == 1 && m_nodes.get(0).getNodes().size() == 0;
	}


	public void setKey(String key) {
		m_key = key;
	}


	public String getKey() {
		return m_key;
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
