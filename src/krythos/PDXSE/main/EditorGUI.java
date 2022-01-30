
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import krythos.PDXSE.database.DataNode;
import krythos.util.abstract_interfaces.FunctionalAction;
import krythos.util.logger.Log;
import krythos.util.swing.SimpleProgressBar;
import krythos.util.swing.SwingMisc;

public class EditorGUI extends JFrame {
	private static final long serialVersionUID = 2369002556725993873L;

	private Controller m_controller;
	private DataNode m_data;
	private JLabel m_lblEditorKey;
	private DataNode m_selectedNode;
	private JTree m_tree;
	private JTextField m_txtEditorValue;


	public EditorGUI() {
		this(new DataNode("Root", false), null);
	}


	public EditorGUI(DataNode data, Controller controller) {
		m_controller = controller;
		m_data = data;
		initGUI();
	}


	public void constructTreeFromData(DefaultMutableTreeNode tree_node, DataNode data_node, JProgressBar bar) {
		DefaultMutableTreeNode new_node = new DefaultMutableTreeNode(data_node);
		tree_node.add(new_node);

		if (bar != null)
			bar.setValue(bar.getValue() + 1);

		// Add Nested Nodes
		for (DataNode dn : data_node.getNodes())
			constructTreeFromData(new_node, dn, bar);

	}


	public void initGUI() {
		SimpleProgressBar progress_bar = new SimpleProgressBar(null, 0, m_data.length() + 7);
		progress_bar.setTitle("Initiating GUI...");
		progress_bar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progress_bar.setValue(0);
		progress_bar.setVisible(true);

		//// Create JTree ////
		// Create Data Tree from Data
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(m_data);
		Log.println(root.getUserObject().toString());
		for (DataNode dn : m_data.getNodes())
			constructTreeFromData(root, dn, progress_bar.bar());

		// Progress Bar
		progress_bar.increment();

		// Create JTree itself.
		m_tree = new JTree(root);
		m_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		m_tree.addTreeSelectionListener(new EditorTreeSelectionListener());
		m_tree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"),
				"select");
		m_tree.getActionMap().put("select", new FunctionalAction(e -> m_txtEditorValue.grabFocus()));

		// Progress Bar
		progress_bar.increment();

		//// Create GUI ////
		// Init Content Pane
		JComponent contentPane = (JComponent) this.getContentPane();
		SpringLayout layout = new SpringLayout();
		contentPane.setLayout(layout);

		// Progress Bar
		progress_bar.increment();

		// Tree JScrollPane
		JScrollPane s_pane_jtree = new JScrollPane(m_tree);
		s_pane_jtree.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		s_pane_jtree.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		// Progress Bar
		progress_bar.increment();

		// Editor JPanel
		JPanel pane_editor = new JPanel();
		SpringLayout editor_layout = new SpringLayout();
		pane_editor.setLayout(editor_layout);

		m_lblEditorKey = new JLabel("[No Selection]: ");
		m_txtEditorValue = new JTextField();
		m_txtEditorValue.setEditable(true);
		m_txtEditorValue.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "assign_value");
		m_txtEditorValue.getActionMap().put("assign_value",
				new FunctionalAction(e -> updateValue(m_selectedNode, m_txtEditorValue.getText())));

		JButton btnPopulate = new JButton("Populate");
		btnPopulate.addActionListener(e -> m_controller.generatePopsCheat());

		JButton btnConvert = new JButton("Convert Pops");
		btnConvert.addActionListener(e -> m_controller.convertPopsCheat());

		pane_editor.add(m_lblEditorKey);
		pane_editor.add(m_txtEditorValue);
		pane_editor.add(btnPopulate);
		pane_editor.add(btnConvert);

		// Progress Bar
		progress_bar.increment();

		// Editor JPanel Spring Constraints
		editor_layout.putConstraint(SpringLayout.NORTH, m_lblEditorKey, 5, SpringLayout.NORTH, pane_editor);
		editor_layout.putConstraint(SpringLayout.WEST, m_lblEditorKey, 5, SpringLayout.WEST, pane_editor);

		editor_layout.putConstraint(SpringLayout.NORTH, m_txtEditorValue, 5, SpringLayout.NORTH, pane_editor);
		editor_layout.putConstraint(SpringLayout.WEST, m_txtEditorValue, 5, SpringLayout.EAST, m_lblEditorKey);
		editor_layout.putConstraint(SpringLayout.EAST, m_txtEditorValue, -5, SpringLayout.EAST, pane_editor);

		editor_layout.putConstraint(SpringLayout.NORTH, btnPopulate, 5, SpringLayout.SOUTH, m_txtEditorValue);
		editor_layout.putConstraint(SpringLayout.WEST, btnPopulate, 5, SpringLayout.WEST, pane_editor);

		editor_layout.putConstraint(SpringLayout.NORTH, btnConvert, 5, SpringLayout.SOUTH, m_txtEditorValue);
		editor_layout.putConstraint(SpringLayout.WEST, btnConvert, 5, SpringLayout.EAST, btnPopulate);

		// Progress Bar
		progress_bar.increment();

		// Content Pane Spring Constraints
		layout.putConstraint(SpringLayout.NORTH, s_pane_jtree, 5, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.WEST, s_pane_jtree, 5, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.SOUTH, s_pane_jtree, -5, SpringLayout.SOUTH, contentPane);
		layout.putConstraint(SpringLayout.EAST, s_pane_jtree, 0, SpringLayout.HORIZONTAL_CENTER, contentPane);

		layout.putConstraint(SpringLayout.NORTH, pane_editor, 5, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.WEST, pane_editor, 5, SpringLayout.HORIZONTAL_CENTER, contentPane);
		layout.putConstraint(SpringLayout.SOUTH, pane_editor, -5, SpringLayout.SOUTH, contentPane);
		layout.putConstraint(SpringLayout.EAST, pane_editor, 0, SpringLayout.EAST, contentPane);

		// Progress Bar
		progress_bar.increment();

		contentPane.add(s_pane_jtree);
		contentPane.add(pane_editor);
		contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
		contentPane.getActionMap().put("save", new FunctionalAction(e -> m_controller.save()));

		// Progress Bar
		progress_bar.dispose();

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.pack();
		this.setSize(500, 500);
		SwingMisc.centerWindow(this);
		this.setVisible(true);
	}


	public void updateValue(DataNode node, String value) {
		if (node == null) {
			Log.debug(this, "updateValue: Node is Null.");
			Log.printDialog("Please Select a Node.");
			return;
		}
		Log.debug(this, "UpdateValue: " + node.toString(0) + ", " + value);
		node.setKey(m_txtEditorValue.getText());
		((DefaultTreeModel) m_tree.getModel()).reload();
	}


	private class EditorTreeSelectionListener implements TreeSelectionListener {

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) m_tree.getLastSelectedPathComponent();
			if (selectedNode == null)
				return;
			Log.debug(this, "Selected: " + selectedNode.toString());
			m_selectedNode = (DataNode) ((DefaultMutableTreeNode) selectedNode).getUserObject();
			DefaultMutableTreeNode parentNode = ((DefaultMutableTreeNode) selectedNode.getParent());

			if (parentNode != null) {
				DataNode parent = (DataNode) ((DefaultMutableTreeNode) selectedNode.getParent()).getUserObject();
				m_lblEditorKey.setText(parent.getKey());
			}
			m_txtEditorValue.setText(((DataNode) selectedNode.getUserObject()).getKey());

		}

	}
}
