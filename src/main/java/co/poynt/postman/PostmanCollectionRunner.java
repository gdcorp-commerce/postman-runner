package co.poynt.postman;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanFolder;
import co.poynt.postman.model.PostmanRequest;
import co.poynt.postman.model.PostmanVariables;

public class PostmanCollectionRunner {
	public static final String ARG_COLLECTION = "c";
	public static final String ARG_ENVIRONMENT = "e";
	public static final String ARG_FOLDER = "f";

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(ARG_COLLECTION, true,
				"File name of the POSTMAN collection.");
		options.addOption(ARG_ENVIRONMENT, true,
				"File name of the POSTMAN environment variables.");
		options.addOption(ARG_FOLDER, true,
				"(Optional) POSTMAN collection folder (group) to execute i.e. \"My Use Cases\"");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		String colFilename = cmd.getOptionValue(ARG_COLLECTION);
		String envFilename = cmd.getOptionValue(ARG_ENVIRONMENT);
		String folderName = cmd.getOptionValue(ARG_FOLDER);

		if (colFilename == null || colFilename.isEmpty() || envFilename == null
				|| envFilename.isEmpty()) {
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("postman-runner", options);
			return;
		}

		PostmanReader reader = new PostmanReader();
		PostmanCollection c = reader.readCollectionFileClasspath(colFilename);
		c.init();
		PostmanEnvironment e = reader.readEnvironmentFileClasspath(envFilename);
		e.init();

		PostmanFolder folder = null;
		if (folderName != null && !folderName.isEmpty()) {
			folder = c.folderLookup.get(folderName);
		}

		PostmanVariables var = new PostmanVariables(e);
		PostmanRequestRunner runner = new PostmanRequestRunner(var);

		if (folder != null) {
			for (String reqId : folder.order) {
				PostmanRequest r = c.requestLookup.get(reqId);
				runner.run(r);
			}
		} else {
			// Execute all folder all requests
			for (PostmanFolder pf : c.folders) {
				runFolder(runner, var, c, pf);
			}
		}
	}

	private static void runFolder(PostmanRequestRunner runner, PostmanVariables var,
			PostmanCollection c, PostmanFolder folder) {
		for (String reqId : folder.order) {
			PostmanRequest r = c.requestLookup.get(reqId);
			runner.run(r);
		}
	}
}
