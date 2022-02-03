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
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileSystemView;

import krythos.PDXSE.database.DataNode;
import krythos.util.logger.Log;
import krythos.util.swing.Dialogs;
import krythos.util.swing.SimpleProgressBar;
import krythos.util.system_utils.SystemUtils;

public class Controller {
	private DataNode m_data;
	private EditorGUI m_editor;


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


	public void convertPopsCheat() {
		Log.info(this, "convertPopsCheat");

		String nation_id = JOptionPane.showInputDialog("Enter Nation ID: ");
		if (nation_id == null || nation_id.trim().equals("")) {
			Log.debug(this, "popCheat: Null Response. Leaving function");
			return;
		} else
			nation_id = nation_id.trim();

		// Get Culture
		String culture = m_data.find(
				Arrays.asList((Object[]) new String[] { "country", "country_database", nation_id, "primary_culture" })).getNode(0).getKey();
		Log.info(this, "convertPopsCheat: Got Culture");

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
	}


	public void generatePopsCheat() {
		Log.info(this, "generatePopsCheat");

		String response = Dialogs.showInputAreaDialog(m_editor, "Enter Data", "owner ID here\n" + "# of pops\n"
				+ "Ratio (slaves:citizen:nobles:freemen:tribesmen)\n" + "culture\n" + "religion");

		if (response == null || response.equals("")) {
			Log.debug(this, "popCheat: Null Response. Leaving function");
			return;
		}

		String[] parts = response.split("\n");
		final int ID = 0;
		final int POPS = 1;
		final int RATIO = 2;
		final int CULTURE = 3;
		final int RELIGION = 4;
		final String[] TYPES = { "slaves", "citizen", "nobles", "freemen", "tribesmen" };

		////// Generate Pops //////
		DataNode population = m_data.find(Arrays.asList("population", "population"));
		List<DataNode> pops = population.getNodes();

		// First unused pop ID;
		int start_pop = Integer.valueOf(pops.get(pops.size() - 1).getKey()) + 1;

		// Generate Pops
		String[] s_ratio = parts[RATIO].split(":"); // Get the ratio strings
		int[] ratio = new int[s_ratio.length];
		for (int i = 0; i < ratio.length; i++) // Convert to int
			ratio[i] = Integer.valueOf(s_ratio[i]);
		int ratio_sum = Arrays.stream(ratio).sum(); // Sum of weights
		for (int i = 0; i < Integer.valueOf(parts[POPS]); i++) {
			DataNode newpop = new DataNode("" + (start_pop + i), true);

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
			newpop.addNode(new DataNode("culture", new DataNode(parts[CULTURE]), false));
			newpop.addNode(new DataNode("religion", new DataNode(parts[RELIGION]), false));

			// Add to population
			population.addNode(newpop);
		}

		////// Assign Pops to Provinces //////
		// Put owned Provinces into Map
		DataNode provinces = m_data.find("provinces");
		Map<DataNode, Integer> map_populations = new HashMap<DataNode, Integer>();
		for (DataNode province : provinces.getNodes()) {
			DataNode owner = province.find("owner");
			if (owner != null && owner.getNode(0).getKey().equals(parts[ID])) {
				int pop_count = province.queryCount("pop");
				map_populations.put(province, pop_count);
			}
		}
		// Assign to Lowest Pop Provinces
		for (int i = start_pop; i < start_pop + Integer.valueOf(parts[POPS]); i++) {
			map_populations = sortByValue(map_populations, true); // Sort Map
			DataNode province = (DataNode) map_populations.keySet().toArray()[0]; // Get First (lowest pop count)
			Integer count = map_populations.get(province);
			province.addNode(new DataNode("pop", new DataNode(i + ""), false)); // Add pop
			map_populations.put(province, count + 1); // increment count
		}

	}


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
			getData(n, output, progress_bar.bar());
			output.print("\n");
		}
		output.close();

		// progress_bar.dispose();

		Log.debug(this, "PrintWriter Complete");
		Log.info(this, "Save Complete");
	}


	private void getData(DataNode node, PrintWriter stream, JProgressBar bar) {
		// Log.debug(this, "GetData Node:" + node.toString());
		String output = node.getKey();

		// Determine Operator
		if (node.isList()) {
			if (!node.getKey().equals(""))
				output += "=";
			output += "{\n";
		} else if (node.getNodes().size() == 1)
			output += "=";

		if (bar != null)
			bar.setValue(bar.getValue() + output.length());

		stream.print(output);
		output = "";

		// Build Output
		for (DataNode n : node.getNodes()) {
			// Is Value
			if (n.getNodes().size() <= 0 && !n.isList()) {
				output = n.getKey() + (node.getNodes().size() == 1 ? "\n" : " ");

				if (bar != null)
					bar.setValue(bar.getValue() + output.length());

				stream.print(output);
				output = "";
			} else // Is Key-List
				getData(n, stream, bar);

		}
		if (node.isList()) {
			stream.print(output + " } \n");
		}
	}


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

}
