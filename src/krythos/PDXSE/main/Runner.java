package krythos.PDXSE.main;

import krythos.util.logger.Log;

public class Runner {
	private static final int HELP = 0;
	private static final int LOAD_FILE = 1;
	private static final int LOG = 2;
	private static final String[] ARG_COMS = { "help", "load_file", "log" };
	private static final String[] ARG_COMS_DESC = { "View Command Descriptions.",
			"True to prompt to load a file. False to skip loading a file. (For debug, to view the GUI quickly.)",
			"Set the log level. DISABLED = 0; LEVEL_ERROR = 1; LEVEL_WARNING = 2; LEVEL_INFO = 3; LEVEL_DEBUG = 4" };


	public static void main(String[] args) {
		Log.setLevel(Log.LEVEL_DISABLED);
		boolean load_file = true;

		// Handle Arguments
		for (String arg : args) {
			String[] parts = arg.split("=");

			if (parts[0].equals(ARG_COMS[LOAD_FILE]) && parts.length > 1)
				load_file = Boolean.valueOf(parts[1]);

			else if (parts[0].equals(ARG_COMS[LOG]) && parts.length > 1)
				Log.setLevel(Integer.valueOf(parts[1]));

			else if (parts[0].equals(ARG_COMS[HELP])) {
				for (int i = 0; i < ARG_COMS.length; i++)
					Log.println(ARG_COMS[i] + (ARG_COMS[i].length() < 8 ? "\t" : "") + "\t:\t" + ARG_COMS_DESC[i]);
				return;
			}

		}
		new Controller(load_file);
	}

}

