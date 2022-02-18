package krythos.PDXSE.main;

import java.awt.Container;
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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileSystemView;

import krythos.PDXSE.database.DataNode;
import krythos.util.logger.Log;
import krythos.util.misc.KArrays;
import krythos.util.misc.SystemUtils;
import krythos.util.swing.Dialogs;
import krythos.util.swing.SimpleProgressBar;
import krythos.util.swing.SwingMisc;
import krythos.util.swing.dialogs.InputListDialog;

public class Controller {
	private DataNode m_data;
	private EditorGUI m_editor;


	/**
	 * Constructor will load the data from user-provided save file, then
	 * initialize the Editor GUI
	 */
	@SuppressWarnings("unused")
	public Controller(boolean load_files) {
		m_data = null;
		if (load_files) {
			try {
				File f = getFile();
				if (f == null)
					load_files = false;
				else
					m_data = loadData(f);
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
	 * Sorts a Map in ascending or descending order based on the values of
	 * the key-value pairs.
	 * 
	 * @param unsortMap The unsorted map to sort.
	 * @param order     Ascending (<code>true</code>) or Descending
	 *                  (<code>false</code>).
	 * @return A sorted Map<DataNode, Integer>.
	 */
	private static Map<DataNode, Integer> sortByValue(Map<DataNode, Integer> unsortMap, final boolean order) {
		List<Entry<DataNode, Integer>> list = new LinkedList<>(unsortMap.entrySet());

		// Sorting the list based on values
		list.sort((o1, o2) -> order
				? o1.getValue().compareTo(o2.getValue()) == 0 ? o1.getKey().getKey().compareTo(o2.getKey().getKey())
						: o1.getValue().compareTo(o2.getValue())
				: o2.getValue().compareTo(o1.getValue()) == 0 ? o2.getKey().getKey().compareTo(o1.getKey().getKey())
						: o2.getValue().compareTo(o1.getValue()));
		return list.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));
	}


	/**
	 * Partner function for {@link #saveData(DataNode, File) saveData}
	 * that primarily retrieves and writes data to a PrintWriter stream.
	 * It is recursive, calling itself for each nested within the provided
	 * {@link DataNode}.
	 * 
	 * @param node   {@link DataNode} to write data from.
	 * @param stream {@link PrintWriter} to write data to.
	 */
	private void getData(DataNode node, PrintWriter stream) {
		String output = node.getKey();

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
			stream.print(output + " } \n");
		}
	}


	/**
	 * Prompts the user for a file location. It will attempt to default
	 * the view to the save-file location for Imperator: Rome save files.
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
		File[] files = Dialogs.fileChooser(false, null, def_file);
		return files != null && files.length > 0 ? files[0] : null;
	}


	/**
	 * Gets the Pop IDs of every pop in a province owned by the specified
	 * nation.
	 * 
	 * @param nation_id ID of the nation we want to get pops from.
	 * @return {@link List} of {@link DataNode}s representing the IDs of
	 *         the owned pops.
	 */
	private List<DataNode> getOwnedPops(Object nation_id) {
		Log.info("convertPopsCheat: Getting Pops...");
		List<DataNode> pop_ids = new LinkedList<DataNode>();

		DataNode provinces = m_data.find("provinces");
		for (DataNode province : provinces.getNodes()) {
			DataNode owner = province.find("owner");
			if (owner != null && owner.getNode(0).getKey().equals(nation_id.toString()))
				for (DataNode node : province.getNodes())
					if (node.getKey().equals("pop"))
						pop_ids.add(node.getNode(0));
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
	 * Gets a population ratio from the user.
	 * 
	 * @return {@link Integer} array representing the desired pop ratio.
	 */
	private int[] getPopTypeRatio() {
		return (new RatiosDialog()).showDialog();
	}


	/**
	 * Returns the Primary Culture of the provided nation ID.
	 * 
	 * @param nation_id ID of the nation to get the primary culture from.
	 * @return {@link String} of the primary culture.
	 */
	private String getPrimaryCulture(String nation_id) {
		return m_data
				.find(Arrays.asList(
						(Object[]) new String[] { "country", "country_database", nation_id, "primary_culture" }))
				.getNode(0).getKey();
	}


	/**
	 * Returns the Primary Religion of the provided nation ID.
	 * 
	 * @param nation_id ID of the nation to get the primary religion from.
	 * @return {@link String} of the primary religion.
	 */
	private String getPrimaryReligion(String nation_id) {
		return m_data
				.find(Arrays.asList((Object[]) new String[] { "country", "country_database", nation_id, "religion" }))
				.getNode(0).getKey();
	}


	/**
	 * Loads the Imperator: Rome save data from the provided file into a
	 * DataNode.
	 * 
	 * @param save_game {@link File} to load data from.
	 * @return {@link DataNode} containing the loaded data.
	 * @throws IOException
	 */
	private DataNode loadData(File save_game) throws IOException {
		Log.info("Loading File: " + save_game.getAbsolutePath());

		// ProgressBar
		int size_kb = (int) (save_game.length() / 1000);
		SimpleProgressBar progress_bar = new SimpleProgressBar(null, 0, size_kb);
		progress_bar.setTitle("Loading Data...");
		progress_bar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progress_bar.bar().setValue(0);
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
		progress_bar.setValue(progress_bar.bar().getMaximum());
		progress_bar.dispose();

		br.close();
		Log.info("Load Complete");
		return data_root;
	}


	/**
	 * Saves the provided data to a file at the provided location in a
	 * format readable by Imperator: Rome.
	 * 
	 * @param root          The root {@link DataNode} to use, where all
	 *                      the nested DataNodes are used to build the
	 *                      save file.
	 * @param save_location {@link File} pointing to where the file should
	 *                      be saved.
	 * @throws FileNotFoundException
	 */
	private void saveData(DataNode root, File save_location) throws FileNotFoundException {
		Log.info("Saving Data...");
		Log.debug("Running PrintWriter");
		SimpleProgressBar progress_bar = new SimpleProgressBar(null, 0, (int) (m_data.byteLength()));
		Log.debug("Progress Bar Status:\n" + progress_bar.statusString());
		progress_bar.setTitle("Saving Data...");
		progress_bar.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		progress_bar.bar().setValue(0);
		progress_bar.setVisible(true);

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


	private String getNationID() {
		String nation_id = JOptionPane.showInputDialog("Enter Nation ID: ");
		if (nation_id == null || nation_id.trim().equals("")) {
			return null;
		} else
			return nation_id.trim();
	}


	/**
	 * Converts all pops of a user-provided nation ID to the primary
	 * culture of that nation.
	 */
	public void cheatAssimilatePops() {
		Log.info("assimilatePopsCheat");

		String nation_id = getNationID();
		if (nation_id == null) {
			Log.debug("popCheat: Null Response. Leaving function");
			JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		}

		// Get Culture
		String culture = getPrimaryCulture(nation_id);

		// Get Pops
		Log.info("assimilatePopsCheat: Getting Pops...");
		List<DataNode> pop_ids_to_convert = getOwnedPops(nation_id);

		// Convert Pops
		boolean use_old = false;
		if (use_old) {
			Log.info("assimilatePopsCheat: Assimilating Pops...");
			List<DataNode> population = m_data.find("population").find("population").getNodes();
			for (DataNode popID : pop_ids_to_convert) {
				// Find the popID
				for (DataNode pop : population) {
					if (pop.getKey().equals(popID.getKey())) {
						pop.find("culture").getNode(0).setKey(culture);
						break;
					}
				}
			}
		} else {
			List<DataNode> population = getPopObjectsFromIDs(pop_ids_to_convert);
			for (DataNode pop : population)
				pop.find("culture").getNode(0).setKey(culture);
		}
		Log.info("assimilatePopsCheat: Done.");
		Log.showMessageDialog("Cheat Complete");
	}


	public void cheatConvertPops() {
		Log.info("convertPopsCheat");

		String nation_id = getNationID();
		if (nation_id == null) {
			Log.debug("popCheat: Null Response. Leaving function");
			JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		}

		// Get Religion
		String religion = getPrimaryReligion(nation_id);

		// Get Pops
		Log.info("convertPopsCheat: Getting Pops...");
		List<DataNode> pop_ids_to_convert = getOwnedPops(nation_id);

		// Convert Pops
		List<DataNode> population = getPopObjectsFromIDs(pop_ids_to_convert);
		for (DataNode pop : population)
			pop.find("religion").getNode(0).setKey(religion);

		Log.info("convertPopsCheat: Done.");
		Log.showMessageDialog("Cheat Complete");
	}


	/**
	 * Generates a population and spreads it among the owned provinces of
	 * a particular nation, preferring to fill the lowest-population
	 * provinces first.
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
			JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		}

		// # Pops to Create
		str_response = JOptionPane.showInputDialog("Enter Number of Pops to Create: ");
		if (str_response == null || str_response.trim().equals("")) {
			Log.debug("Null Response. Leaving function");
			JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
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
				JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
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
				JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
				return;
			} else
				religion = str_response.trim();
		}

		final String[] TYPES = { "nobles", "citizen", "freemen", "tribesmen", "slaves" };


		////// Generate Pops //////

		// Put owned Provinces into Map -- Do this first to determine if
		// population can be assigned before creating the pops.
		DataNode provinces = m_data.find("provinces");
		Map<DataNode, Integer> map_populations = new HashMap<DataNode, Integer>();
		for (DataNode province : provinces.getNodes()) {
			DataNode owner = province.find("owner");
			if (owner != null && owner.getNode(0).getKey().equals(nation_id)) {
				int pop_count = province.queryCount("pop");
				map_populations.put(province, pop_count);
			}
		}

		if (map_populations.size() <= 0) {
			Log.showMessageDialog("Nation doesn't have any provinces.");
			return;
		}


		// Get existing population
		DataNode population = m_data.find(Arrays.asList("population", "population"));
		List<DataNode> pops = population.getNodes();

		// First unused pop ID;
		int start_pop = Integer.valueOf(pops.get(pops.size() - 1).getKey()) + 1;

		// Generate Pops
		int ratio_sum = Arrays.stream(ratio).sum(); // Sum of weights
		for (int i = 0; i < Integer.valueOf(pop_number); i++) {
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

		////// Assign Pops to Provinces //////

		// Assign to Lowest Pop Provinces
		for (int i = start_pop; i < start_pop + Integer.valueOf(pop_number); i++) {
			map_populations = sortByValue(map_populations, true); // Sort Map
			DataNode province = (DataNode) map_populations.keySet().toArray()[0]; // Get First (lowest pop count)
			Integer count = map_populations.get(province);
			province.addNode(new DataNode("pop", new DataNode(i + ""), false)); // Add pop
			map_populations.put(province, count + 1); // increment count
		}

		Log.showMessageDialog("Cheat Complete");
	}


	/**
	 * Will merge two cultures in a nation. The culture to be merged
	 * (converted) to and from will be defined by the user.
	 */
	public void cheatMergeCultures() {
		String str_response;
		String nation_id, culture_from, culture_to;

		nation_id = getNationID();
		if (nation_id == null) {
			Log.debug("Null Response. Leaving function");
			JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		}

		str_response = JOptionPane.showInputDialog("Enter culture to convert from: ");
		if (str_response == null || str_response.trim().equals("")) {
			Log.debug("Null Response. Leaving function");
			JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		} else
			culture_from = '"' + str_response.trim() + '"';

		str_response = JOptionPane.showInputDialog("Enter culture to convert to: ");
		if (str_response == null || str_response.trim().equals("")) {
			Log.debug("Null Response. Leaving function");
			JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		} else
			culture_to = '"' + str_response.trim() + '"';

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


	/**
	 * Saves the data to a user-specified file in a format readable by
	 * Imperator: Rome.
	 */
	public void save() {
		Log.info("Saving File:");
		try {
			File save_location = getFile();
			Log.info("Save File: " + save_location.getAbsolutePath());
			saveData(m_data, save_location);
		} catch (FileNotFoundException e) {
			Log.error(e.getMessage());
			e.printStackTrace();
		}
		Log.info("Save Complete");
	}


	/**
	 * Specialized {@link JDialog} to get and provide an {@link Integer}
	 * array
	 * representing a ratio of pop types.
	 * 
	 * Use {@link RatiosDialog#showDialog() showDialog} to use this
	 * dialog.
	 * 
	 * @author Krythos
	 */
	@SuppressWarnings("serial")
	private class RatiosDialog extends JDialog {
		public int[] ratio;


		/**
		 * Create GUI
		 */
		public RatiosDialog() {
			this.setModalityType(ModalityType.APPLICATION_MODAL);

			// GUI //
			Container contentPane = getContentPane();
			contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

			((JComponent) contentPane).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			// JLabel
			JTextArea lblDisplay = new JTextArea("Enter ratio of pops:\nN:C:F:T:S");
			lblDisplay.setEditable(false);
			lblDisplay.setOpaque(false);
			lblDisplay.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
			contentPane.add(lblDisplay);

			// Ratio Boxes
			JPanel pnlRatios = new JPanel();
			pnlRatios.setLayout(new BoxLayout(pnlRatios, BoxLayout.X_AXIS));

			JTextField txtNobles = new JTextField("1");
			pnlRatios.add(txtNobles);
			pnlRatios.add(new JLabel(":"));

			JTextField txtCitizens = new JTextField("1");
			pnlRatios.add(txtCitizens);
			pnlRatios.add(new JLabel(":"));

			JTextField txtFreemen = new JTextField("1");
			pnlRatios.add(txtFreemen);
			pnlRatios.add(new JLabel(":"));

			JTextField txtTribesmen = new JTextField("0");
			pnlRatios.add(txtTribesmen);
			pnlRatios.add(new JLabel(":"));

			JTextField txtSlaves = new JTextField("1");
			pnlRatios.add(txtSlaves);

			contentPane.add(pnlRatios);

			// Confirm Button
			JButton btnConfirm = new JButton("Confirm");
			btnConfirm.addActionListener(e -> {
				String br = ":";
				String[] parts = (txtNobles.getText() + br + txtCitizens.getText() + br + txtFreemen.getText() + br
						+ txtTribesmen.getText() + br + txtSlaves.getText()).split(br);
				ratio = new int[parts.length];
				for (int i = 0; i < parts.length; i++)
					ratio[i] = Integer.valueOf(parts[i]);
				this.setVisible(false);
			});
			contentPane.add(btnConfirm);

			this.pack();
			SwingMisc.centerWindow(this);
		}


		/**
		 * Use this function to show the dialog and get the result.
		 * 
		 * @return
		 */
		public int[] showDialog() {
			this.setVisible(true);
			return ratio;
		}
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
			list_selections = Dialogs.showInputListDialog(m_editor, list_selections);
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

}

