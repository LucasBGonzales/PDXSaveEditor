package kytheros.PDXSE.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataNode {
	private String m_key;
	private List<DataNode> m_nodes;
	private boolean m_isList;
	private DataNode m_parent;


	/**
	 * Initialize with only a key. Will establish this {@link DataNode} as
	 * not being
	 * a list.
	 * 
	 * @param key {@link String} key for this node.
	 */
	public DataNode(String key) {
		this(key, false);
	}


	/**
	 * Initialize with a key and define whether this {@link DataNode}
	 * should be
	 * treated as a list.
	 * 
	 * @param key    {@link String} key for this node.
	 * @param isList {@link Boolean} whether the children of this node
	 *               should be
	 *               treated as a list.
	 */
	public DataNode(String key, boolean isList) {
		setKey(key);
		m_nodes = new ArrayList<DataNode>(0);
		setList(isList);
	}


	/**
	 * Initialize with a key and a single initial child node, and define
	 * whether
	 * this {@link DataNode} should be treated as a list.
	 * 
	 * @param key    {@link String} key for this node.
	 * @param data   Initial {@link DataNode} to be assigned as a child of
	 *               this
	 *               node.
	 * @param isList {@link Boolean} whether the children of this node
	 *               should be
	 *               treated as a list.
	 */
	public DataNode(String key, DataNode data, boolean isList) {
		this(key, isList);
		addNode(data);
	}


	/**
	 * Initialize with a key and a {@link List} of child nodes. Will
	 * automatically
	 * establish this {@link DataNode} to be treated as a list.
	 * 
	 * @param key  {@link String} key for this node.
	 * @param data Initial {@link List} of {@link DataNode DataNodes} to
	 *             be assigned
	 *             as children of this node.
	 */
	public DataNode(String key, List<DataNode> data) {
		this(key, true);
		m_nodes.addAll(data);
	}


	/**
	 * Set whether this {@link DataNode node} should be treated as a list
	 * for purposes of saving to file.
	 * 
	 * @param isList
	 */
	public void setList(boolean isList) {
		this.m_isList = isList;
	}


	/**
	 * Adds a node {@link DataNode n} as a child of this node. Will also
	 * set {@code this} as parent of }link DataNode n}.
	 * 
	 * @param n Child to add to this {@link DataNode}
	 */
	public void addNode(DataNode n) {
		n.setParent(this);
		this.m_nodes.add(n);
	}


	/**
	 * Automatically assigns {@link DataNode this} as the parent of each
	 * of it's
	 * children. If {@code recursive} is {@code true} then this function
	 * will also
	 * be called on each of the children to assign parentage to the entire
	 * tree.
	 * 
	 * @param recursive If <code>true</code>, then this function is called
	 *                  recursively for all {@link DataNode DataNodes}
	 *                  nested in
	 *                  this <code>DataNode</code>.
	 */
	public void autoAssignParent(boolean recursive) {
		for (DataNode n : getNodes()) {
			n.setParent(this);
			if (recursive)
				n.autoAssignParent(recursive);
		}
	}


	/**
	 * Retrieves the total bytes of the key of this {@link DataNode} and
	 * of all
	 * DataNodes nested within this DataNode.
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


	/**
	 * Finds the {@link DataNode} from the <code>path</code>, if it
	 * exists. If the
	 * size of <code>path</code> is 0, this DataNode is returned.
	 * 
	 * @param path Path of DataNode to find.
	 * @return DataNode specified, or <code>null</code> if the DataNode
	 *         doesn't
	 *         exist.
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
	 * Convenience function. Converts the arguments to a list and calls
	 * {@link DataNode#find(List) find(List)}.
	 * 
	 * @param path Path of DataNode to find.
	 * @return DataNode specified, or <code>null</code> if the DataNode
	 *         doesn't
	 *         exist.
	 */
	public DataNode find(Object... path) {
		return find(Arrays.asList(path));
	}


	/**
	 * Finds the first {@link DataNode} inside of this {@code DataNode}
	 * with this
	 * key.
	 * 
	 * @param key Key to find.
	 * @return {@code DataNode} with the key or null if there is no
	 *         {@code DataNode}
	 *         with the key.
	 */
	public DataNode find(String key) {
		for (DataNode n : m_nodes)
			if (n.getKey().equals(key))
				return n;
		return null;
	}


	/**
	 * Finds all the {@link DataNode DataNodes} inside of this
	 * {@code DataNode} with
	 * the given key.
	 * 
	 * @param key Key to find.
	 * @return {@code List<DataNode} of all {@code DataNodes} with the key
	 *         or
	 *         {@code null} if there are no {@code DataNodes} with the
	 *         key.
	 */
	public List<DataNode> findAll(String key) {
		List<DataNode> r = new ArrayList<DataNode>();
		for (DataNode n : m_nodes)
			if (n.getKey().equals(key))
				r.add(n);
		return r.size() <= 0 ? null : r;
	}


	/**
	 * Retrieves the depth of this node (how many parents it has).
	 * 
	 * @return Depth of this node.
	 */
	public int getDepth() {
		return getDepth(0);
	}


	/**
	 * Gets the key of the node.
	 * 
	 * @return this node's key.
	 */
	public String getKey() {
		return m_key;
	}


	/**
	 * Gets the key of the node represented as
	 * <p>
	 * <code>Integer.valueOf(getKey());</code>
	 * 
	 * @return The node's key as an integer.
	 */
	public int getKeyAsInt() {
		return Integer.valueOf(m_key);
	}


	/**
	 * Returns the key of the first child node. Useful when the
	 * {@link DataNode} is
	 * a known key-value pair.
	 * 
	 * @return {@link String} Key of the first child of the node, or
	 *         <code>null</code> if there are no nodes.
	 */
	public String getKeyValue() {
		if (this.m_nodes.size() > 0)
			return this.m_nodes.get(0).getKey();
		return null;
	}


	/**
	 * Gets the child node at the specified index. Equivalent to
	 * {@code getNodes().get(index)}.
	 * 
	 * @param index
	 * @return The node at <code>index</code>
	 */
	public DataNode getNode(int index) {
		return this.m_nodes.get(index);
	}


	/**
	 * Gets the child nodes of the node.
	 * 
	 * @return this node's child nodes.
	 */
	public List<DataNode> getNodes() {
		return this.m_nodes;
	}


	/**
	 * Returns the parent of this DataNode
	 * 
	 * @return Parent {@link DataNode} of this node, or <code>null</code>
	 *         if there
	 *         is no parent.
	 */
	public DataNode getParent() {
		return m_parent;
	}


	/**
	 * Returns the index of a child {@link DataNode node} in this
	 * {@link DataNode}. If the child doesn't exist, {@code -1} is
	 * returned.
	 * 
	 * @param node
	 * @return Index of child node, or -1 if the child doesn't exist in
	 *         this node.
	 */
	public int indexOf(DataNode node) {
		for (int i = 0; i < m_nodes.size(); i++)
			if (m_nodes.get(i).equals(node))
				return i;
		return -1;
	}


	/**
	 * Whether this {@link DataNode} has a parent node.
	 * 
	 * @return <code>true</code> if this node has a parent,
	 *         <code>false</code> if
	 *         {@link #getParent() getParent()} is <code>null</code>.
	 */
	public boolean hasParent() {
		return getParent() != null;
	}


	/**
	 * Returns <code>true</code> if this node has one child, and that
	 * child has no
	 * children, and this node is not a list, then this node is a
	 * key-value pair.
	 * <p>
	 * {@code getNodes().size() == 1 && getNodes().get(0).getNodes().size() == 0 && !isList()}
	 * 
	 * @return whether this node and its child represent a key-value pair.
	 */
	public boolean isKeyValuePair() {
		return m_nodes.size() == 1 && m_nodes.get(0).getNodes().size() == 0 && !isList();
	}


	/**
	 * <code>true</code> if the node is flagged to be treated as a list.
	 * 
	 * @return Whether this node is a list.
	 */
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


	/**
	 * Removes the specified node.
	 * 
	 * @param node Node to be removed, determined using
	 *             <code>{DataNode}.equals(Object});</code>
	 * @return <code>true</code> if a node is removed, <code>false</code>
	 *         if not.
	 */
	public boolean removeNode(DataNode node) {
		for (int i = 0; i < m_nodes.size(); i++)
			if (getNode(i).equals(node)) {
				removeNode(i);
				return true;
			}
		return false;
	}


	/**
	 * Removes the node at the specified index. If the index provided is
	 * outside the
	 * range of the nodes list, nothing happens.
	 * 
	 * @param index Index of the node to be removed.
	 * @return <code>true</code> if a node is removed, <code>false</code>
	 *         if not.
	 */
	public boolean removeNode(int index) {
		if (this.m_nodes.size() >= index) {
			this.m_nodes.remove(index);
			return true;
		}
		return false;
	}


	/**
	 * Removes the node with the specified key. If <code>true</code> is
	 * passed as a
	 * second argument, then all nodes with the specified key will be
	 * removed.
	 * 
	 * @param key        Key to search for.
	 * @param remove_all <code>true</code> to remove all instances of
	 *                   nodes with the
	 *                   key. <code>false</code> to remove only the first
	 *                   instance.
	 * @return Number of nodes removed.
	 */
	public int removeNode(String key, boolean... remove_all) {
		int removed = 0;
		for (int i = 0; i < m_nodes.size(); i++)
			if (m_nodes.get(i).getKey().equals(key)) {
				removeNode(i);
				i--;
				removed++;
				if (!remove_all[0])
					break;
			}
		return removed;
	}


	/**
	 * Sets the key for the node.
	 * 
	 * @param key The new key for this node.
	 */
	public void setKey(String key) {
		m_key = key;
	}


	/**
	 * Sets the key of the first child node. Useful when the
	 * {@link DataNode} is a
	 * known key-value pair.
	 * 
	 * @param value Value to set. Does nothing if there are no nodes.
	 */
	public void setKeyValue(String value) {
		if (this.m_nodes.size() > 0)
			this.m_nodes.get(0).setKey(value);
	}


	/**
	 * Completely replaces the {@link DataNode}'s nested nodes with a new
	 * list.
	 * 
	 * @param newNodes DataNodes to replace existing nodes.
	 */
	public void setNodes(List<DataNode> newNodes) {
		this.m_nodes = newNodes;
	}


	/**
	 * Sets the parent of this DataNode.
	 * 
	 * @param parent
	 */
	public void setParent(DataNode parent) {
		m_parent = parent;
	}


	/**
	 * Simply returns {@link #getKey()} as a {@link String} representation
	 * of this
	 * node. Use {@link #toString(int) toString(int depth)} to view nested
	 * children
	 * as well.
	 * 
	 * @return {@link String} representation of this object.
	 */
	@Override
	public String toString() {
		return this.getKey();
	}


	/**
	 * Returns a {@link String} representation of this node and it's
	 * children up to
	 * the specified depth of recursion. if the specified depth is less
	 * than zero,
	 * than the recursion will continue to include all children.
	 * 
	 * @param depth Depth of recursion. Use -1 for infinite depth.
	 * @return {@link String} representation of this node and it's
	 *         children up to
	 *         {@code depth}.
	 */
	public String toString(int depth) {
		String str = this.getKey();

		if (depth != 0 && this.getNodes().size() > 0) {
			str += "[";
			for (DataNode n : this.getNodes())
				str += (n.equals(this.getNode(0)) ? "" : ", ") + n.toString(depth < 0 ? -1 : depth - 1);
			str += "]";
		}

		return str;
	}


	/**
	 * Retrieves the depth of this node (how many parents it has).
	 * 
	 * @param count Count of depth so far.
	 * @return Depth of this node.
	 */
	private int getDepth(int count) {
		return m_parent != null ? m_parent.getDepth(count + 1) : count;
	}


	/**
	 * Counts the number of children in this node.
	 * 
	 * @return
	 */
	public int countChildren() {
		return m_nodes.size();
	}
}
