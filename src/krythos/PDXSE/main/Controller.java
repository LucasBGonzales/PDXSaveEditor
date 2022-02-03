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
import krythos.util.swing.Dialogs;
import krythos.util.swing.SimpleProgressBar;
import krythos.util.swing.SwingMisc;
import krythos.util.system_utils.SystemUtils;

public class Controller {
	private DataNode m_data;
	private EditorGUI m_editor;


	/**
	 * Constructor will load the data from user-provided save file, then
	 * initialize the Editor GUI
	 */
	public Controller() {
		m_data = null;
		try {
			m_data = loadData(getFile());
		} catch (IOException e1) {
			Log.error(this, e1.getMessage());
			e1.printStackTrace();
		}

		m_editor = new EditorGUI(m_data, this);
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
	 * Converts all pops of a user-provided nation ID to the primary
	 * culture of that nation.
	 */
	public void convertPopsCheat() {
		Log.info(this, "convertPopsCheat");

		String nation_id = JOptionPane.showInputDialog("Enter Nation ID: ");
		if (nation_id == null || nation_id.trim().equals("")) {
			Log.debug(this, "popCheat: Null Response. Leaving function");
			return;
		} else
			nation_id = nation_id.trim();

		// Get Culture
		String culture = getPrimaryCulture(nation_id);

		// Get Pops
		Log.info(this, "convertPopsCheat: Getting Pops...");
		List<DataNode> pop_ids_to_convert = new LinkedList<DataNode>();

		DataNode provinces = m_data.find("provinces");
		for (DataNode province : provinces.getNodes()) {
			DataNode owner = province.find("owner");
			if (owner != null && owner.getNode(0).getKey().equals(nation_id)) {
				for (DataNode node : province.getNodes()) {
					if (node.getKey().equals("pop"))
						pop_ids_to_convert.add(node.getNode(0));
				}
			}
		}

		// Convert Pops
		Log.info(this, "convertPopsCheat: Converting Pops...");
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
		Log.info(this, "convertPopsCheat: Done.");
		Log.printDialog("Cheat Complete");
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
	 * Generates a population and spreads it among the owned provinces of
	 * a particular nation, preferring to fill the lowest-population
	 * provinces first.
	 */
	public void generatePopsCheat() {
		Log.info(this, "generatePopsCheat");

		String str_response;
		int int_response;

		String nation_id, culture, religion;
		int pop_number;
		int[] ratio;

		// Nation ID
		str_response = JOptionPane.showInputDialog("Enter Nation ID: ");
		if (str_response == null || str_response.trim().equals("")) {
			Log.debug(null, "Null Response. Leaving function");
			JOptionPane.showMessageDialog(m_editor, "No Entry, Quitting Function");
			return;
		} else
			nation_id = str_response.trim();

		// # Pops to Create
		str_response = JOptionPane.showInputDialog("Enter Number of Pops to Create: ");
		if (str_response == null || str_response.trim().equals("")) {
			Log.debug(null, "Null Response. Leaving function");
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
				Log.debug(null, "Null Response. Leaving function");
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
				Log.debug(null, "Null Response. Leaving function");
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
			Log.printDialog("Nation doesn't have any provinces.");
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

		Log.printDialog("Cheat Complete");
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
	 * Saves the data to a user-specified file in a format readable by
	 * Imperator: Rome.
	 */
	public void save() {
		Log.info(this, "Saving File:");
		try {
			File save_location = getFile();
			Log.info(this, "Save File: " + save_location.getAbsolutePath());
			saveData(m_data, save_location);
		} catch (FileNotFoundException e) {
			Log.error(this, e.getMessage());
			e.printStackTrace();
		}
		Log.info(this, "Save Complete");
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
		Log.info(this, "Filename: " + def_file.getPath());
		File[] files = Dialogs.fileChooser(false, null, def_file);
		return files != null && files.length > 0 ? files[0] : null;
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
		Log.info(this, "Saving Data...");
		Log.debug(this, "Running PrintWriter");
		SimpleProgressBar progress_bar = new SimpleProgressBar(null, 0, (int) (m_data.byteLength()));
		Log.debug(this, "Progress Bar Status:\n" + progress_bar.statusString());
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

		Log.debug(this, "PrintWriter Complete");
		Log.info(this, "Save Complete");
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
		// Log.debug(this, "GetData Node:" + node.toString());
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
	 * Loads the Imperator: Rome save data from the provided file into a
	 * DataNode.
	 * 
	 * @param save_game {@link File} to load data from.
	 * @return {@link DataNode} containing the loaded data.
	 * @throws IOException
	 */
	private DataNode loadData(File save_game) throws IOException {
		Log.info(this, "Loading File: " + save_game.getAbsolutePath());

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
			Log.error(this, e.getMessage());
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
		while ((line = br.readLine()) != null) {
			char_count += line.length();
			line = line.trim();
			boolean f_firstIteration = true;
			while (line != null && !line.equals("")) {
				// Get First Operator, Ignoring Quoted Strings
				int first = -1;
				boolean f_inQuote = false;
				char QUOTE = '"';
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
					Log.error(this, "Unhandled Case: First < 0 && !f_firstIteration");
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
							DataNode newNode = new DataNode(parts[0], true);
							path.get(path.size() - 1).addNode(newNode);
							path.add(newNode);
							line = parts[1];
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
								Log.error(this, "Unhandled Case in Operator==NEWNODE");
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
			progress_bar./*bar().*/setValue((int) (char_count / 1000));
		}
		progress_bar.setValue(progress_bar.bar().getMaximum());
		progress_bar.dispose();

		br.close();
		Log.info(this, "Load Complete");
		return data_root;
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

}
