package co.poynt.postman;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.testng.log4testng.Logger;

import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanFolder;
import co.poynt.postman.model.PostmanRequest;
import co.poynt.postman.model.PostmanVariables;

public class PostmanCollectionRunner {
	private static final Logger logger = Logger
			.getLogger(PostmanCollectionRunner.class);
	public static final String ARG_COLLECTION = "c";
	public static final String ARG_ENVIRONMENT = "e";
	public static final String ARG_FOLDER = "f";
	public static final String ARG_HALTONERROR = "haltonerror";

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(ARG_COLLECTION, true,
				"File name of the POSTMAN collection.");
		options.addOption(ARG_ENVIRONMENT, true,
				"File name of the POSTMAN environment variables.");
		options.addOption(ARG_FOLDER, true,
				"(Optional) POSTMAN collection folder (group) to execute i.e. \"My Use Cases\"");
		options.addOption(ARG_HALTONERROR, false,
				"(Optional) Stop on first error in POSTMAN folder.");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		String colFilename = cmd.getOptionValue(ARG_COLLECTION);
		String envFilename = cmd.getOptionValue(ARG_ENVIRONMENT);
		String folderName = cmd.getOptionValue(ARG_FOLDER);
		boolean haltOnError = cmd.hasOption(ARG_HALTONERROR);

		if (colFilename == null || colFilename.isEmpty() || envFilename == null
				|| envFilename.isEmpty()) {
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("postman-runner", options);
			return;
		}

		PostmanCollectionRunner pcr = new PostmanCollectionRunner();
		pcr.runCollection(colFilename, envFilename, folderName, haltOnError);
	}

	public boolean runCollection(String colFilename, String envFilename,
			String folderName, boolean haltOnError) throws Exception {
		System.out.println("@@@@@ POSTMAN Runner start!");
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

		PostmanVariables var = new PostmanVariables(e);
		PostmanRequestRunner runner = new PostmanRequestRunner(var, haltOnError);
		boolean isSuccessful = true;
		if (folder != null) {
			isSuccessful = runFolder(haltOnError, runner, var, c, folder, runResult);
		} else {
			// Execute all folder all requests
			for (PostmanFolder pf : c.folders) {
				isSuccessful = runFolder(haltOnError, runner, var, c, pf, runResult)
						&& isSuccessful;
				if (haltOnError && !isSuccessful) {
					return isSuccessful;
				}
			}
		}

		System.out.println("@@@@@ Yay! All Done!");
		System.out.println(runResult);
		return isSuccessful;
	}

	private boolean runFolder(boolean haltOnError, PostmanRequestRunner runner,
			PostmanVariables var, PostmanCollection c, PostmanFolder folder, PostmanRunResult runResult) {
		System.out.println("==> POSTMAN Folder: " + folder.name);
		boolean isSuccessful = true;
		for (String reqId : folder.order) {
			runResult.totalRequest++;
			PostmanRequest r = c.requestLookup.get(reqId);
			System.out.println("======> POSTMAN request: " + r.name);
			try {
				boolean runSuccess = runner.run(r, runResult);
				if (!runSuccess) {
					runResult.failedRequest++;
					runResult.failedRequestName.add(r.name);
				}
				isSuccessful = runSuccess && isSuccessful;
				if (haltOnError && !isSuccessful) {
					return isSuccessful;
				}
			} catch (HaltTestFolderException e) {
				return false;
			}
		}
		return isSuccessful;
	}
}
