package co.poynt.postman;

import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanFolder;
import co.poynt.postman.model.PostmanItem;
import co.poynt.postman.model.PostmanVariables;

public class PostmanCollectionRunner {
	private static final Logger logger = LoggerFactory.getLogger(PostmanCollectionRunner.class);
	public static final String ARG_COLLECTION = "c";
	public static final String ARG_ENVIRONMENT = "e";
	public static final String ARG_FOLDER = "f";
	public static final String ARG_HALTONERROR = "haltonerror";

	private PostmanVariables sharedPostmanEnvVars;

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(ARG_COLLECTION, true, "File name of the POSTMAN collection.");
		options.addOption(ARG_ENVIRONMENT, true, "File name of the POSTMAN environment variables.");
		options.addOption(ARG_FOLDER, true,
				"(Optional) POSTMAN collection folder (group) to execute i.e. \"My Use Cases\"");
		options.addOption(ARG_HALTONERROR, false, "(Optional) Stop on first error in POSTMAN folder.");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		String colFilename = cmd.getOptionValue(ARG_COLLECTION);
		String envFilename = cmd.getOptionValue(ARG_ENVIRONMENT);
		String folderName = cmd.getOptionValue(ARG_FOLDER);
		boolean haltOnError = cmd.hasOption(ARG_HALTONERROR);

		if (colFilename == null || colFilename.isEmpty() || envFilename == null || envFilename.isEmpty()) {
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("postman-runner", options);
			return;
		}

		PostmanCollectionRunner pcr = new PostmanCollectionRunner();
		pcr.runCollection(colFilename, envFilename, folderName, haltOnError, false, null);
	}

	public PostmanRunResult runCollection(String colFilename, String envFilename, String folderName,
			boolean haltOnError) throws Exception {
		return runCollection(colFilename, envFilename, folderName, haltOnError, false, null);
	}

	/**
	 *
	 * @param colFilename          - collection file
	 * @param envFilename          - environment file
	 * @param folderName           - folder the files are in
	 * @param haltOnError          - stop on error
	 * @param useSharedPostmanVars Use a single set of postman variable(s) across
	 *                             all your tests. This allows for running tests
	 *                             between a select few postman folders while
	 *                             retaining environment variables between each run
	 * @return The run result object has statistics of the execution.
	 * @throws Exception
	 */
	public PostmanRunResult runCollection(String colFilename, String envFilename, String folderName,
			boolean haltOnError, boolean useSharedPostmanVars, List<PostmanRequestRunner.Observer> observers)
			throws Exception {
		logger.info("@@@@@ POSTMAN Runner start: {}", colFilename);
		PostmanRunResult runResult = new PostmanRunResult();

		PostmanReader reader = new PostmanReader();
		PostmanCollection c = reader.readCollectionFile(colFilename);
		c.init();
		PostmanEnvironment e = reader.readEnvironmentFile(envFilename);
		e.init();
		PostmanFolder folder = null;
		if (folderName != null && !folderName.isEmpty()) {
			folder = c.folderLookup.get(folderName);
		}

		PostmanVariables var;
		if (useSharedPostmanVars) {
			if (sharedPostmanEnvVars == null) {
				sharedPostmanEnvVars = new PostmanVariables(e);
			}
			var = sharedPostmanEnvVars;
		} else {
			var = new PostmanVariables(e);
		}

		PostmanRequestRunner runner = new PostmanRequestRunner(var, haltOnError, observers);
		boolean isSuccessful = true;
		if (folder != null) {
			isSuccessful = runFolder(haltOnError, runner, var, folder, runResult);
		} else {
			// Execute all folder all requests
			for (PostmanFolder pf : c.item) {
				isSuccessful = runFolder(haltOnError, runner, var, pf, runResult) && isSuccessful;
				if (haltOnError && !isSuccessful) {
					return runResult;
				}
			}
		}

		logger.info("@@@@@ Yay! All Done!");
		logger.info(runResult.toString());
		return runResult;
	}

	private boolean runFolder(boolean haltOnError, PostmanRequestRunner runner, PostmanVariables var,
			PostmanFolder folder, PostmanRunResult runResult) {
		logger.info("==> POSTMAN Folder: " + folder.name);
		boolean isSuccessful = true;
		for (PostmanItem fItem : folder.item) {
			runResult.totalRequest++;
			logger.info("======> POSTMAN request: " + fItem.name);
			try {
				boolean runSuccess = runner.run(fItem, runResult);
				if (!runSuccess) {
					runResult.failedRequest++;
					runResult.failedRequestName.add(folder.name + "." + fItem.name);
				}
				isSuccessful = runSuccess && isSuccessful;
				if (haltOnError && !isSuccessful) {
					return isSuccessful;
				}
			} catch (Throwable e) {
				e.printStackTrace();
				runResult.failedRequest++;
				runResult.failedRequestName.add(folder.name + "." + fItem.name);
				return false;
			}
		}
		return isSuccessful;
	}
}
