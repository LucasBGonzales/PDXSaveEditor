package krythos.PDXSE.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

import krythos.PDXSE.database.DataNode;
import krythos.PDXSE.gui.EditorGUI;
import krythos.PDXSE.gui.PopsRatioDialog;
import krythos.PDXSE.gui.ProvinceEditorDialog;
import krythos.util.logger.Log;
import krythos.util.misc.KArrays;
import krythos.util.misc.SystemUtils;
import krythos.util.swing.KDialogs;
import krythos.util.swing.SimpleProgressBar;
import krythos.util.swing.dialogs.InputListDialog;
import krythos.util.swing.dialogs.InputListDialog.ListSelection;

public class Controller {
	private DataNode m_data;


	private EditorGUI m_editor;

	private int m_saveVersion;
	private boolean f_save_readable;


	/**
	 * Will load the data from user-provided save file, then initialize the Editor
	 * GUI. If the user doesn't select a file, then the GUI will be loaded with no
	 * data tree.
	 */
	public Controller(boolean load_files, int load_data_version, int save_data_version, boolean save_readable) {
		m_data = null;
		Log.info("Load Data Version: " + load_data_version);
		Log.info("Save Data Version: " + load_data_version);
		Log.info("Save Readable: " + save_readable);
		m_saveVersion = save_data_version;
		f_save_readable = save_readable;
		if (load_files) {
			try {
				File f = getFile();
				if (f == null)
					load_files = false;
				else if (load_data_version == 0)
					m_data = loadData(f);
				else if (load_data_version == 1)
					m_data = loadData_old(f);
			} catch (IOException e1) {
				Log.error(this, e1.getMessage());
				e1.printStackTrace();
			}
		}
		if (!load_files)
			m_data = new DataNode("Empty");
		m_editor = new EditorGUI(m_data, this);
	}


	/**
	 * Sorts a Map in ascending or descending order based on the values of the
	 * key-value pairs.
	 * 
	 * @param unsortMap The unsorted map to sort.
	 * @param order     Ascending (<code>true</code>) or Descending
	 *                  (<code>false</code>).
	 * @return A sorted Map<DataNode, Integer>.
	 */
	private static Map<DataNode, Float> sortByValue(Map<DataNode, Float> unsortMap, final boolean order) {
		List<Entry<DataNode, Float>> list = new LinkedList<>(unsortMap.entrySet());

		// Sorting the list based on values
		list.sort((o1, o2) -> order
				? o1.getValue().compareTo(o2.getValue()) == 0 ? o1.getKey().getKey().compareTo(o2.getKey().getKey())
						: o1.getValue().compareTo(o2.getValue())
				: o2.getValue().compareTo(o1.getValue()) == 0 ? o2.getKey().getKey().compareTo(o1.getKey().getKey())
						: o2.getValue().compareTo(o1.getValue()));
		return list.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));
	}


	/**
	 * Partner function for {@link #saveData_old(DataNode, File) saveData} that
	 * primarily retrieves and writes data to a PrintWriter stream. It is recursive,
	 * calling itself for each nested within the provided {@link DataNode}.
	 * 
	 * @param node   {@link DataNode} to write data from.
	 * @param stream {@link PrintWriter} to write data to.
	 */
	private void getData(DataNode node, PrintWriter stream) {
		String tab = (new String(new char[node.getDepth()])).replace("\0", "\t");
		String output = tab + node.getKey();

		// Determine Operator
		if (node.isList()) {
			if (!node.getKey().equals(""))
				output += "=";
			output += "{\n";
		} else if (node.getNodes().size() == 1)
			output += "=";

		stream.print(output);
		output = "";

		// Build Output
		for (DataNode n : node.getNodes()) {
			// Is Value
			if (n.getNodes().size() <= 0 && !n.isList()) {
				output = n.getKey() + (node.getNodes().size() == 1 ? "\n" : " ");

				stream.print(output);
				output = "";
			} else // Is Key-List
				getData(n, stream);

		}
		if (node.isList()) {
			if (tab.length() > 0)
				tab = tab.substring(0, tab.length() - 0);
			else
				tab = "";
			stream.print(tab + output + " } \n");
		}
	}


	/**
	 * Prompts the user for a file location. It will attempt to default the view to
	 * the save-file location for Imperator: Rome save files.
	 * 
	 * @return {@link File} pointing to a file that may or may not exist.
	 */
	private File getFile() {
		File def_file = null;
		if (SystemUtils.isWindows())
			def_file = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()
					+ "\\Paradox Interactive\\Imperator\\save games");
		else if (SystemUtils.isLinux())
			def_file = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()
					+ "/.local/share/Paradox Interactive/Imperator/save games/");
		Log.info("Filename: " + def_file.getPath());
		File[] files = KDialogs.fileChooser(false, null, def_file);
		return files != null && files.length > 0 ? files[0] : null;
	}


	/**
	 * Retrieves all the cultures represented by the pops in the given nation.
	 * 
	 * @param nationID Nation from which to search for cultures.
	 * @return Cultures of pops in the nation.
	 */
	private List<String> getNationCultures(Object nationID) {
		List<String> cultures = new LinkedList<String>();
		List<DataNode> pops = getPopObjectsFromIDs(getOwnedPops(nationID));
		for (DataNode pop : pops) {
			String culture = pop.find("culture").getNode(0).getKey();
			if (!cultures.contains(culture))
				cultures.add(culture);
		}

		return cultures;
	}


	private String getNationID() {
		String nation_id = JOptionPane.showInputDialog("Enter Nation ID: ");
		if (nation_id == null || nation_id.trim().equals("")) {
			return null;
		} else
			return nation_id.trim();
	}


	/**
	 * Gets the Pop IDs of every pop in a province owned by the specified nation.
	 * 
	 * @param nation_id ID of the nation we want to get pops from.
	 * @return {@link List} of {@link DataNode}s representing the IDs of the owned
	 *         pops.
	 */
	private List<DataNode> getOwnedPops(Object nation_id) {
		List<DataNode> pop_ids = new LinkedList<DataNode>();

		DataNode provinces = m_data.find("provinces");
		for (DataNode province : provinces.getNodes()) {
			DataNode owner = province.find("owner");
			if (owner != null && owner.getNode(0).getKey().equals(nation_id.toString()))
				pop_ids.addAll(getPopsFromProvince(province));
			;
		}
		return pop_ids;
	}


	private List<DataNode> getPopObjectsFromIDs(List<DataNode> lstIDs) {
		List<DataNode> population = new LinkedList<DataNode>(m_data.find("population").find("population").getNodes());
		List<DataNode> pops_return = new LinkedList<DataNode>();

		for (DataNode pop : population)
			for (DataNode id : lstIDs)
				if (pop.getKey().equals(id.getKey())) {
					pops_return.add(pop);
					break;
				}
		return pops_return;
	}


	/**
	 * 
	 * @param province {@link DataNode} of the province to retrieve the pops from.
	 * @return {@link List List<DataNode>} of all pops in the given province.
	 */
	private List<DataNode> getPops(DataNode province) {
		List<DataNode> pop_ids = new LinkedList<DataNode>();
		for (DataNode node : province.findAll("pop"))
			pop_ids.add(node.getNode(0));

		return pop_ids;
	}


	/**
	 * 
	 * @param province_id
	 * @return
	 */
	private List<DataNode> getPopsFromProvince(Object province_id) {
		List<DataNode> pop_ids = new LinkedList<DataNode>();

		DataNode province = m_data.find("provinces", province_id.toString());
		pop_ids.addAll(getPops(province));

		return pop_ids;
	}


	/**
	 * Gets a population ratio from the user.
	 * 
	 * @return {@link Integer} array representing the desired pop ratio.
	 */
	private int[] getPopTypeRatio() {
		return (new PopsRatioDialog()).runDialog();
	}


	/**
	 * Returns the Primary Culture of the provided nation ID.
	 * 
	 * @param nation_id ID of the nation to get the primary culture from.
	 * @return {@link String} of the primary culture, or <code>null</code> if
	 *         retrieve failed.
	 */
	private String getPrimaryCulture(String nation_id) {
		try {
			return m_data.find((Object[]) new String[] { "country", "country_database", nation_id, "primary_culture" })
					.getNode(0).getKey();
		} catch (NullPointerException e) {
			Log.error(null, "Failed Retrieval.", m_editor);
			return null;
		}
	}


	/**
	 * Returns the Primary Religion of the provided nation ID.
	 * 
	 * @param nation_id ID of the nation to get the primary religion from.
	 * @return {@link String} of the primary religion, or <code>null</code> if
	 *         retrieve failed.
	 */
	private String getPrimaryReligion(String nation_id) {
		try {
			return m_data.find((Object[]) new String[] { "country", "country_database", nation_id, "religion" })
					.getNode(0).getKey();
		} catch (NullPointerException e) {
			Log.error(null, "Failed Retrieval.", m_editor);
			return null;
		}
	}


	private String getProvinceID() {
		String province_id = JOptionPane.showInputDialog("Enter Province ID: ");
		if (province_id == null || province_id.trim().equals("")) {
			return null;
		} else
			return province_id.trim();
	}


	private DataNode loadData(File save_game) throws IOException {
		Log.info("Loading File: " + save_game.getAbsolutePath());

		// ProgressBar
		int size_kb = (int) (save_game.length() / 1000);
		SimpleProgressBar progress_bar = new SimpleProgressBar(null, 0, size_kb);
		progress_bar.setTitle("Loading Data...");
		progress_bar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progress_bar.bar().setValue(0);
		progress_bar.bar().setString("Loading From File...");
		progress_bar.setVisible(true);

		// Setup BufferedReader
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(save_game));
		} catch (FileNotFoundException e) {
			Log.error(e.getMessage());
			System.exit(-1);
		}

		// Collect data
		DataNode data_root = new DataNode("Root", true);
		String line;
		String buffer = "";
		DataNode working_node = data_root;
		long char_count = 0;
		final char EQUALS = '=';
		final char NEWLIST = '{';
		final char ENDLIST = '}';
		final char QUOTE = '"';
		final char ENDLINE = '\n';
		final char SPACE = ' ';
		while ((line = br.readLine()) != null) {
			char_count += line.length();
			line = line.trim() + ENDLINE;
			boolean f_inQuote = false;
			boolean f_equals = false;
			boolean f_color = false;
			for (int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);

				if (f_color) {
					if (c == '\t' || c == '\n') {
						f_color = false;
						working_node.addNode(new DataNode(buffer.trim(), false));
						working_node = working_node.getParent();
						buffer = "";
						continue;
					} else {
						buffer += c;
						continue;
					}
				}


				// If in a list, not in a quote and c is a space, then switch it to
				// ENDLINE as the functionality is the same.
				if (!f_inQuote && c == SPACE)
					c = ENDLINE;

				// If we are in a quote, just record to buffer.
				if (f_inQuote) {
					buffer += c;
					// If the character is a quote, then this is the end of the quote.
					if (c == QUOTE) {
						f_inQuote = false;
						working_node.addNode(new DataNode(buffer));
						buffer = "";
						// If this was a key-value pair, return to parent.
						if (f_equals)
							working_node = working_node.getParent();
					}
				} else {
					switch (c) {
					case EQUALS:
						// Add new Node, set it to working node.
						DataNode newNodeE = new DataNode(buffer.trim());
						working_node.addNode(newNodeE);
						working_node = newNodeE;

						// Stupid color formatting means I need a specific case for colors.
						if (working_node.getKey().indexOf("color") == 0) {
							f_color = true;
						} else {
							// Flip Equals flag, this information is needed for lists and
							// returning from key-value pairs.
							f_equals = true;
						}
						// Clear buffer
						buffer = "";
						break;
					case NEWLIST:
						// Named List, adjust working_node.
						if (f_equals) {
							working_node.setList(true);
							f_equals = false;
						} else {
							// Unnamed List. Create new list node and set it to working_node.
							DataNode newNodeNL = new DataNode("", true);
							working_node.addNode(newNodeNL);
							working_node = newNodeNL;
						}
						break;
					case ENDLIST:
						// End of a list, set working_node to working_node's parent.
						working_node = working_node.getParent();
						break;
					case ENDLINE:
						// If buffer has a value, create a new node and clear buffer.
						if (buffer.trim().length() > 0) {
							DataNode newNodeEL = new DataNode(buffer.trim());
							working_node.addNode(newNodeEL);
							// If this was a key-value pair, return to parent.
							if (f_equals)
								working_node = working_node.getParent();
						}
						buffer = "";
						break;
					case QUOTE:
						// If we encounter a quote (and this will only ever be an opening
						// quote, see if-else before this switch for closing quote), then set
						// f_inQuote flag to true.
						f_inQuote = true;
						buffer += c;
						break;
					default:
						// Add the current character to the buffer, set f_equals = false;
						buffer += c;
					}
				}
			}
			progress_bar.setValue((int) (char_count / 1000));
		}
		progress_bar.bar().setString("Cleaning Data Nodes...");
		data_root.autoAssignParent(true);

		progress_bar.setValue(progress_bar.bar().getMaximum());
		progress_bar.dispose();

		br.close();
		Log.info("Load File Complete");

		return data_root;

	}


	/**
	 * Loads the Imperator: Rome save data from the provided file into a DataNode.
	 * 
	 * @param save_game {@link File} to load data from.
	 * @return {@link DataNode} containing the loaded data.
	 * @throws IOException
	 */
	private DataNode loadData_old(File save_game) throws IOException {
		Log.info("Loading File (OLD): " + save_game.getAbsolutePath());

		// ProgressBar
		int size_kb = (int) (save_game.length() / 1000);
		SimpleProgressBar progress_bar = new SimpleProgressBar(null, 0, size_kb);
		progress_bar.setTitle("Loading Data...");
		progress_bar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progress_bar.bar().setValue(0);
		progress_bar.bar().setString("Loading From File...");
		progress_bar.setVisible(true);

		// Setup BufferedReader
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(save_game));
		} catch (FileNotFoundException e) {
			Log.error(e.getMessage());
			System.exit(0);
			// e.printStackTrace();
		}

		// Root data node and dynamic path
		DataNode data_root = new DataNode("Root", true);
		List<DataNode> path = new LinkedList<DataNode>();
		path.add(data_root);

		// Collect data
		String line;
		long char_count = 0;
		char EQUALS = '=';
		char NEWNODE = '{';
		char RETURNPATH = '}';
		char QUOTE = '"';
		while ((line = br.readLine()) != null) {
			char_count += line.length();
			line = line.trim();
			boolean f_firstIteration = true;
			while (line != null && !line.equals("")) {

				// Get First Operator, Ignoring Quoted Strings
				int first = -1;
				boolean f_inQuote = false;
				for (int i = 0; i < line.length(); i++) {
					char character = line.charAt(i);
					if (character == QUOTE)
						f_inQuote = !f_inQuote;
					else if (!f_inQuote && (character == EQUALS || character == NEWNODE || character == RETURNPATH)) {
						first = i;
						break;
					}
				}

				// This is a line with no operators
				if (first < 0 && f_firstIteration) {
					path.get(path.size() - 1).addNode(new DataNode(line, false));
					line = null;

				} else if (first < 0 && !f_firstIteration) {
					Log.error("Unhandled Case: First < 0 && !f_firstIteration");
					System.exit(0);
				} else { // This is a line with operators
					// Get the first operator & split it off.
					char operator = line.charAt(first);
					String[] parts = line.split(Pattern.quote(operator + ""), 2);
					parts[0] = parts[0].trim();
					if (parts.length > 1)
						parts[1] = parts[1].trim();

					// Has other operators? Ignoring Quoted Strings
					int second = -1;
					for (int i = first + 1; i < line.length(); i++) {
						char character = line.charAt(i);
						if (character == QUOTE)
							f_inQuote = !f_inQuote;
						else if (!f_inQuote
								&& (character == EQUALS || character == NEWNODE || character == RETURNPATH)) {
							second = i;
							break;
						}
					}
					boolean other_operators = second >= 0;

					// First is an EQUALS
					if (operator == EQUALS) {
						// Has another operator
						if (other_operators) {
							if (line.charAt(second) == RETURNPATH) {
								DataNode newNode = new DataNode(parts[0], false);
								path.get(path.size() - 1).addNode(newNode);
								line = parts[1];
							} else {
								DataNode newNode = new DataNode(parts[0], true);
								path.get(path.size() - 1).addNode(newNode);
								path.add(newNode);
								line = parts[1];
							}
						} else { // Has no other operator :: Key-Value Pair
							path.get(path.size() - 1)
									.addNode(new DataNode(parts[0], new DataNode(parts[1], false), false));
							line = null;
						}
					} else if (operator == NEWNODE) { // First is a NEWNODE
						// Has another operator
						if (other_operators) {
							// Entering a nested node
							if (line.charAt(second) == NEWNODE) {
								DataNode newNode = new DataNode("", true);
								path.get(path.size() - 1).addNode(newNode);
								path.add(newNode);
								line = null;
								// Single-line Nested Node
							} else if (line.charAt(second) == RETURNPATH) {
								/*
								 * DataNode newNode = new DataNode(parts[0]); path.get(path.size() -
								 * 1).addNode(newNode); path.add(newNode);
								 */
								line = parts[1];
							} else if (line.charAt(second) == EQUALS) {
								line = parts[1];
							} else {
								Log.error("Unhandled Case in Operator==NEWNODE");
								System.exit(0);
							}
						} else {
							// Stand alone Nested Node
							if (f_firstIteration) {
								DataNode newNode = new DataNode("", true);
								path.get(path.size() - 1).addNode(newNode);
								path.add(newNode);
							}
							line = null;
						}
					} else if (operator == RETURNPATH) {
						// Just exiting a node
						if (parts[0].equals("")) {
							path.remove(path.size() - 1);
							line = null;
							// Listed Node
						} else {
							for (String s : parts[0].trim().split(" "))
								path.get(path.size() - 1).addNode(new DataNode(s, false));
							path.remove(path.size() - 1);
							line = null;
						}
					}
				}
				f_firstIteration = false;
			}
			f_firstIteration = true;
			progress_bar.setValue((int) (char_count / 1000));
		}
		progress_bar.bar().setString("Cleaning Data Nodes...");
		data_root.autoAssignParent(true);

		progress_bar.setValue(progress_bar.bar().getMaximum());
		progress_bar.dispose();

		br.close();
		Log.info("Load File Complete");

		return data_root;
	}


	/**
	 * Saves the provided data to a file at the provided location in a format
	 * readable by Imperator: Rome.
	 * 
	 * @param root          The root {@link DataNode} to use, where all the nested
	 *                      DataNodes are used to build the save file.
	 * @param save_location {@link File} pointing to where the file should be saved.
	 * @throws FileNotFoundException
	 */
	private void saveData(DataNode root, File save_location) throws FileNotFoundException {
		Log.info("Saving Data...");
		SimpleProgressBar progress_bar = new SimpleProgressBar(null, 0, (int) (m_data.byteLength()));
		Log.debug("Progress Bar Status:\n" + progress_bar.toString());
		progress_bar.setTitle("Saving Data...");
		progress_bar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progress_bar.bar().setValue(0);
		progress_bar.setVisible(true);

		Log.debug("Running PrintWriter");
		PrintWriter output = new PrintWriter(save_location);

		DataNode working_node, prev_node;
		prev_node = root;
		working_node = root.getNode(0);
		while (working_node != null) {
			// Parent, for ease of use.
			DataNode parent = working_node.getParent();

			// Determine if already printed if so, determine next node and
			// continue.
			// // If previous node is a child of working node, already printed.
			int index = working_node.indexOf(prev_node);
			if (index >= 0) {
				// if list, and previous node isn't last in the list, set next child
				// as working node.
				if (working_node.isList() && index < working_node.countChildren() - 1) {
					prev_node = working_node;
					working_node = working_node.getNode(index + 1);
				} else if (working_node.isList() && index == working_node.countChildren() - 1) {
					// If end of list
					// If end of root nodes, break;
					if (working_node.equals(root))
						working_node = null;
					else {
						String tab = "";
						if (f_save_readable)
							tab = (new String(new char[working_node.getDepth() - 1])).replace("\0", "\t");
						output.print(tab + "}\n");
						// Set parent as working node.
						prev_node = working_node;
						working_node = working_node.getParent();
					}
				} else {
					// Otherwise set parent as working node.
					prev_node = working_node;
					working_node = working_node.getParent();
				}
				continue;
			}

			// Determine Output

			String tab = "";
			try {
				if (parent.isList() && f_save_readable)
					tab = (new String(new char[working_node.getDepth() - 1])).replace("\0", "\t");
			} catch (Exception e) {
				Log.error(null, "This shouldn't have happened...", null);
			}
			String operator = "";

			// Print key
			output.print(tab + working_node.getKey());

			// Determine operator, if any
			if (working_node.isList()) { // IF list
				if (!working_node.getKey().equals("")) // Keyless List
					operator += "=";
				operator += "{\n"; // Keyed list
				if (working_node.isList() && working_node.countChildren() == 0) // List with no children
					operator += (f_save_readable ? "" : tab) + "}\n";
			} else if (working_node.getNodes().size() == 1) // Key-value pair
				operator += "=";
			else // Standalone value || end of list
				operator += "\n";
			output.print(operator);

			progress_bar.increment(working_node.getKey().length());
			prev_node = working_node;
			if (working_node.countChildren() > 0)
				working_node = working_node.getNode(0);
			else
				working_node = working_node.getParent();

		}

		output.close();
		progress_bar.bar().setValue(progress_bar.bar().getMaximum());

		Log.debug("PrintWriter Complete");
		Log.info("Save Complete");
	}


	/**
	 * Saves the provided data to a file at the provided location in a format
	 * readable by Imperator: Rome.
	 * 
	 * @param root          The root {@link DataNode} to use, where all the nested
	 *                      DataNodes are used to build the save file.
	 * @param save_location {@link File} pointing to where the file should be saved.
	 * @throws FileNotFoundException
	 */
	private void saveData_old(DataNode root, File save_location) throws FileNotFoundException {
		Log.info("Saving Data...");
		SimpleProgressBar progress_bar = new SimpleProgressBar(null, 0, (int) (m_data.byteLength()));
		Log.debug("Progress Bar Status:\n" + progress_bar.toString());
		progress_bar.setTitle("Saving Data...");
		progress_bar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progress_bar.bar().setValue(0);
		progress_bar.setVisible(true);

		Log.debug("Running PrintWriter");
		PrintWriter output = new PrintWriter(save_location);
		for (DataNode n : root.getNodes()) {
			getData(n, output);
			output.print("\n");
		}
		output.close();
		progress_bar.bar().setValue(progress_bar.bar().getMaximum());

		Log.debug("PrintWriter Complete");
		Log.info("Save Complete");
	}


	/**
	 * Converts all pops of a user-provided nation ID to the primary culture of that
	 * nation.
	 */
	public void cheatAssimilatePops() {
		Log.info("assimilatePopsCheat");

		String nation_id = null, province_id = null;

		// Nation ID or by Province ID?
		ListSelection n_or_p = KDialogs.showInputListDialog(m_editor, new ListSelection(
				"Assimilate by Nation ID or by Individual Provinces?", new String[] { "Nation ID", "ProvinceID" }, 0));
		if (n_or_p != null) {
			if (n_or_p.getValue().equals("Nation ID"))
				nation_id = getNationID();
			else
				province_id = getProvinceID();

			try {
				nation_id = province_id != null ? m_data.find("provinces", province_id, "owner").getNode(0).getKey()
						: nation_id;
			} catch (NullPointerException e) {
				Log.warn("Null Pointer Exception. Probably invalid province input.");
				nation_id = null;
			}
		}

		// Check Validity
		if (!isValidNationID(nation_id))
			nation_id = getNationIDFromTag(nation_id);

		// Nothing Entered, or invalid..
		if (nation_id == null) {
			Log.warn("cheatAssimilatePops: Null Response. Leaving function");
			Log.showMessageDialog(m_editor, "No or Invalid Response. Quitting Function");
			return;
		}

		// Get Culture
		String culture = getPrimaryCulture(nation_id);

		// Get Pops
		Log.info("cheatAssimilatePops: Getting Pops...");
		List<DataNode> pop_ids_to_convert;
		if (province_id == null)
			pop_ids_to_convert = getOwnedPops(nation_id);
		else
			pop_ids_to_convert = getPopsFromProvince(province_id);

		// Convert Pops
		List<DataNode> population = getPopObjectsFromIDs(pop_ids_to_convert);
		for (DataNode pop : population)
			pop.find("culture").getNode(0).setKey(culture);

		Log.info("cheatAssimilatePops: Done.");
		Log.showMessageDialog("Cheat Complete");
	}


	/**
	 * 
	 * 
	 */
	public void cheatConvertPops() {
		Log.info("convertPopsCheat");

		String nation_id = null, province_id = null;

		// Nation ID or by Province ID?
		ListSelection n_or_p = KDialogs.showInputListDialog(m_editor, new ListSelection(
				"Assimilate by Nation ID or by Individual Provinces?", new String[] { "Nation ID", "ProvinceID" }, 0));
		if (n_or_p != null) {
			if (n_or_p.getValue().equals("Nation ID"))
				nation_id = getNationID();
			else
				province_id = getProvinceID();

			try {
				nation_id = province_id != null ? m_data.find("provinces", province_id, "owner").getNode(0).getKey()
						: nation_id;
			} catch (NullPointerException e) {
				Log.warn("Null Pointer Exception. Probably invalid province input.");
				nation_id = null;
			}
		}

		// Check Validity
		if (!isValidNationID(nation_id))
			nation_id = getNationIDFromTag(nation_id);

		// Nothing Entered, or invalid..
		if (nation_id == null) {
			Log.warn("cheatConvertPops: Null Response. Leaving function");
			Log.showMessageDialog(m_editor, "No or Invalid Response. Quitting Function");
			return;
		}

		// Get Religion
		String religion = getPrimaryReligion(nation_id);

		// Get Pops
		Log.info("cheatConvertPops: Getting Pops...");
		List<DataNode> pop_ids_to_convert;
		if (province_id == null)
			pop_ids_to_convert = getOwnedPops(nation_id);
		else
			pop_ids_to_convert = getPopsFromProvince(province_id);

		// Convert Pops
		List<DataNode> population = getPopObjectsFromIDs(pop_ids_to_convert);
		for (DataNode pop : population)
			pop.find("religion").getNode(0).setKey(religion);

		Log.info("cheatConvertPops: Done.");
		Log.showMessageDialog("Cheat Complete");
	}


	/**
	 * Will open a dialog to edit various values of a specified province.
	 */
	public void cheatEditProvince() {
		String province_id = getProvinceID();
		if (province_id == null) {
			Log.warn(null, "No Province ID Selected. Exiting Function.", m_editor);
			return;
		}
		DataNode province = m_data.find("provinces", province_id.toString());
		ProvinceEditorDialog province_editor = new ProvinceEditorDialog(province);
		province_editor.runDialog();
	}


	/**
	 * Generates a population and spreads it among the owned provinces of a
	 * particular nation, preferring to fill the lowest-population provinces first.
	 */
	public void cheatGeneratePops() {
		Log.info("generatePopsCheat");

		String str_response;
		int int_response;

		String nation_id, culture, religion;
		int pop_number;
		int[] ratio;

		// Nation ID
		nation_id = getNationID();
		if (nation_id == null) {
			Log.debug("Null Response. Leaving function");
			Log.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		}

		// # Pops to Create
		str_response = JOptionPane.showInputDialog("Enter Number of Pops to Create: ");
		if (str_response == null || str_response.trim().equals("")) {
			Log.debug("Null Response. Leaving function");
			Log.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		} else
			pop_number = Integer.valueOf(str_response.trim());

		// Ratio of Pops
		ratio = getPopTypeRatio();

		// Culture
		int_response = JOptionPane.showConfirmDialog(m_editor, "Use Primary Culture?");
		if (int_response == JOptionPane.YES_OPTION)
			culture = getPrimaryCulture(nation_id);
		else {
			str_response = JOptionPane.showInputDialog("Enter Culture: ");
			if (str_response == null || str_response.trim().equals("")) {
				Log.debug("Null Response. Leaving function");
				Log.showMessageDialog(m_editor, "No Entry, Quitting Function");
				return;
			} else
				culture = str_response.trim();
		}

		// Religion
		int_response = JOptionPane.showConfirmDialog(m_editor, "Use Primary Religion?");
		if (int_response == JOptionPane.YES_OPTION)
			religion = getPrimaryReligion(nation_id);
		else {
			str_response = JOptionPane.showInputDialog("Enter Religion: ");
			if (str_response == null || str_response.trim().equals("")) {
				Log.debug("Null Response. Leaving function");
				Log.showMessageDialog(m_editor, "No Entry, Quitting Function");
				return;
			} else
				religion = str_response.trim();
		}

		////// Generate Pops //////
		final String[] TYPES = { "\"nobles\"", "\"citizen\"", "\"freemen\"", "\"tribesmen\"", "\"slaves\"" };

		// Put owned Provinces into Map -- Do this first to determine if
		// population can be assigned before creating the pops.
		DataNode provinces = m_data.find("provinces");
		Map<DataNode, Float> map_populations = new HashMap<DataNode, Float>();
		for (DataNode province : provinces.getNodes()) {
			DataNode owner = province.find("owner");
			if (owner != null && owner.getKeyValue().equals(nation_id)) {
				int pop_count = province.queryCount("pop");
				if (!province.find("province_rank").getKeyValue().equals("settlement"))
					pop_count /= 4.4f;
				map_populations.put(province, (float) pop_count);
			}
		}

		if (map_populations.size() <= 0) {
			Log.warn(null, "Generate Pops Cheat:\nNation doesn't have any provinces. Leaving function.", m_editor);
			return;
		}

		// Get existing population
		DataNode population = m_data.find("population", "population");
		List<DataNode> pops = population.getNodes();

		// First unused pop ID;
		int start_pop = pops.get(pops.size() - 1).getKeyAsInt() + 1;

		// Generate Pops
		int ratio_sum = Arrays.stream(ratio).sum(); // Sum of weights
		for (int i = 0; i < pop_number; i++) {
			DataNode newpop = new DataNode(String.valueOf(start_pop + i), true);

			// Type
			int step = i % ratio_sum;
			String type = "";
			for (int i2 = 0; i2 < ratio.length; i2++) {
				step -= ratio[i2];
				if (step <= 0) {
					type = TYPES[i2];
					break;
				}
			}
			newpop.addNode(new DataNode("type", new DataNode(type), false));

			// Culture and Religion
			newpop.addNode(new DataNode("culture", new DataNode(culture), false));
			newpop.addNode(new DataNode("religion", new DataNode(religion), false));

			// Add to population
			population.addNode(newpop);
		}
		population.autoAssignParent(true);

		////// Assign Pops to Provinces //////

		// Assign to Lowest Pop Provinces
		for (int i = start_pop; i < start_pop + pop_number; i++) {
			map_populations = sortByValue(map_populations, true); // Sort Map
			DataNode province = (DataNode) map_populations.keySet().toArray()[0]; // Get First (lowest pop count)
			Float count = map_populations.get(province);
			province.addNode(new DataNode("pop", new DataNode(i + ""), false)); // Add pop
			map_populations.put(province,
					count + ((!province.find("province_rank").getKeyValue().equals("settlement")) ? 1f / 4.4f : 1));
		}

		Log.showMessageDialog("Cheat Complete");
	}


	/**
	 * Will merge two cultures in a nation. The culture to be merged (converted) to
	 * and from will be defined by the user.
	 */
	public void cheatMergeCultures() {
		String nation_id, culture_from, culture_to;
		Object[] nationCultures;

		nation_id = getNationID();
		if (nation_id == null) {
			Log.debug("Null Response. Leaving function");
			Log.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		}

		nationCultures = getNationCultures(nation_id).toArray();
		// Ensure there are enough cultures to merge.
		if (nationCultures.length < 2) {
			Log.showMessageDialog(m_editor, "Not enough cultures to merge.");
			return;
		}

		try {
			culture_from = KDialogs
					.showInputListDialog(m_editor,
							new ListSelection("Select culture to convert from:", nationCultures, 0))
					.getValue().toString();
			// Don't allow user to pick the same culture to convert from and to.
			nationCultures = KArrays.remove(nationCultures, culture_from);
			culture_to = KDialogs
					.showInputListDialog(m_editor,
							new ListSelection("Select culture to convert to:", nationCultures,
									KArrays.indexOf(nationCultures, getPrimaryCulture(nation_id))))
					.getValue().toString();
		} catch (NullPointerException e) {
			Log.warn("Null Response, Leaving Function");
			Log.showMessageDialog(m_editor, "No Entry, Quiting Function.");
			return;
		}
		// Get Pops
		List<DataNode> pops_to_convert = getPopObjectsFromIDs(getOwnedPops(nation_id));

		// Iterate each pop, convert cultures.
		for (DataNode pop : pops_to_convert) {
			DataNode culture = pop.find("culture").getNode(0);
			if (culture.getKey().equals(culture_from))
				culture.setKey(culture_to);
		}

		Log.showMessageDialog("Cheat Complete");
	}


	public void cheatModSubjects() {
		// Subject Types
		final String[] subject_types = { "\"feudatory\"", "\"satrapy\"", "\"client_state\"", "\"vassal_tribe\"",
				"\"tributary\"", "\"subject_colony\"", "\"subject_mercenary_city_state\"",
				"\"subject_league_city_state\"" };

		// Nation ID
		String nation_id = getNationID();
		if (nation_id == null) {
			Log.info(null, "No Entry. Leaving function", m_editor);
			return;
		}

		// Get Dependencies
		List<DataNode> diplomacy = new LinkedList<DataNode>(m_data.find("diplomacy").getNodes());
		List<DataNode> dependencies = new LinkedList<DataNode>();
		for (DataNode n : diplomacy)
			if (n.getKey().equals("dependency") && n.find("first").find(nation_id) != null)
				dependencies.add(n);

		// No Subjects
		if (dependencies.size() <= 0) {
			JOptionPane.showMessageDialog(m_editor, "No Subjects To Modify");
			return;
		}

		// Get Cheat Option
		String[] choices = { "Change All Subjects to Type...", "Change All Subjects of Type A to Type B...",
				"Modifiy Individually." };
		String input = (String) JOptionPane.showInputDialog(m_editor, "Choose Mod Function", "Choose Mod Function",
				JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
		int cheat_option = -1;
		for (int i = 0; i < choices.length; i++)
			if (choices[i].equals(input)) {
				cheat_option = i;
				break;
			}

		if (cheat_option < 0) {
			Log.info(null, "No Choice Selected. Leaving Function", m_editor);
			return;
		}

		// Change All To...
		if (cheat_option == 0) {
			String type = (String) JOptionPane.showInputDialog(m_editor, "Select Type to Change Subjects To",
					"Select Type", JOptionPane.QUESTION_MESSAGE, null, subject_types, subject_types[0]);
			for (DataNode n : dependencies)
				n.find("subject_type").getNode(0).setKey(type);
		}
		// Change All Type A to Type B...
		else if (cheat_option == 1) {
			String typeA = (String) JOptionPane.showInputDialog(m_editor, "Select Type of Subjects to Change",
					"Select Type", JOptionPane.QUESTION_MESSAGE, null, subject_types, subject_types[0]);
			String typeB = (String) JOptionPane.showInputDialog(m_editor, "Select Type to Change Subjects To",
					"Select Type", JOptionPane.QUESTION_MESSAGE, null, subject_types, subject_types[0]);

			for (DataNode n : dependencies) {
				DataNode st = n.find("subject_type").getNode(0);
				if (st.getKey().equals(typeA))
					n.find("subject_type").getNode(0).setKey(typeB);
			}
		}
		// Modify Individually...
		else if (cheat_option == 2) {
			InputListDialog.ListSelection[] list_selections = new InputListDialog.ListSelection[dependencies.size()];
			for (int i = 0; i < list_selections.length; i++) {
				DataNode dependency = dependencies.get(i);
				Object message = dependency.find("second").getNode(0).getKey();
				String str_init = dependency.find("subject_type").getNode(0).getKey();
				int initial_value = KArrays.indexOf(subject_types, str_init);
				list_selections[i] = new InputListDialog.ListSelection(message, subject_types, initial_value);
			}

			// Show modification window and get results.
			list_selections = KDialogs.showInputListDialog(m_editor, list_selections);
			if (list_selections == null) {
				Log.info(null, "Canceled Cheat.", m_editor);
				return;
			}

			// Apply Results
			for (int i = 0; i < list_selections.length; i++) {
				DataNode dep_subject_type = dependencies.get(i).find("subject_type").getNode(0);
				dep_subject_type.setKey(list_selections[i].getValue().toString());
			}
		}

		Log.showMessageDialog(m_editor, "Cheat Complete");
	}


	/**
	 * Gets a Nation ID from the Nation Tag.
	 * 
	 * @param nation_tag Tag of the nation to search for.
	 * @return The Nation ID of the nation that matches the Nation Tag, or
	 *         <code>null</code> if no match was found.
	 */
	public String getNationIDFromTag(String nation_tag) {
		nation_tag = "\"" + nation_tag.toUpperCase() + "\"";
		DataNode country_database = m_data.find("country", "country_database");
		if (country_database == null)
			Log.warn("country_database was null. Probably because nothing was loaded.");
		else {
			for (DataNode node : country_database.getNodes()) {
				if (node.getKey().equals("599")) {
					Log.debug("bleh");
					node.find("tag").getKeyValue();
				}
				if (node.find("tag").getKeyValue().equals(nation_tag))
					return node.getKey();
			}
		}
		return null;
	}


	/**
	 * Determines if the nation ID is valid.
	 * 
	 * @param nation_id ID to query.
	 * @return <code>true</code> if the nation ID was found. <code>false</code> if
	 *         not.
	 */
	public boolean isValidNationID(String nation_id) {
		return m_data.find("country", "country_database", nation_id) != null;
	}


	/**
	 * Saves the data to a user-specified file in a format readable by Imperator:
	 * Rome.
	 */
	public void save() {
		Log.info("Saving File [version " + m_saveVersion + "]:");
		if (m_saveVersion == 1) {
			try {
				File save_location = getFile();
				if (save_location == null) {
					Log.warn(null, "No Save Location Selected. Cancelling Save.", m_editor);
					return;
				}
				Log.info("Save File: " + save_location.getAbsolutePath());
				saveData_old(m_data, save_location);
			} catch (FileNotFoundException e) {
				Log.error(null, e.getMessage(), m_editor);
				e.printStackTrace();
			}
		} else if (m_saveVersion == 0) {
			try {
				File save_location = getFile();
				if (save_location == null) {
					Log.warn(null, "No Save Location Selected. Cancelling Save.", m_editor);
					return;
				}
				Log.info("Save File: " + save_location.getAbsolutePath());
				saveData(m_data, save_location);
			} catch (FileNotFoundException e) {
				Log.error(null, e.getMessage(), m_editor);
				e.printStackTrace();
			}
		} else {
			Log.error("Invalid Save Version, Save Unsuccessful.");
			return;
		}
		Log.info("Save Complete");
	}
}
