package co.poynt.postman.runner;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.poynt.postman.CmdBase;
import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanFolder;
import co.poynt.postman.model.PostmanItem;
import co.poynt.postman.model.PostmanReader;
import co.poynt.postman.model.PostmanVariables;

import picocli.CommandLine;

@CommandLine.Command(name = "run", description = "run a postman collection")
public class PostmanCollectionRunner extends CmdBase implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(PostmanCollectionRunner.class);
	@CommandLine.Option(names = { "-c",
			"--collection" }, required = true, description = "File name of the POSTMAN collection.")
	private String colFilename;

	@CommandLine.Option(names = { "-e",
			"--environment" }, required = true, description = "File name of the POSTMAN environment variables.")
	private String envFilename;

	@CommandLine.Option(names = { "-f",
			"--folder" }, required = false, description = "(Optional) Specific folder to run")
	private String folderName;

	@CommandLine.Option(names = { "-s",
			"--haltonerror" }, required = false, description = "(Optional) Stop on first error in POSTMAN folder.")
	private boolean haltOnError;

	private PostmanVariables sharedPostmanEnvVars;

	@Override
	public void run() {
		try {
			runCollection(colFilename, envFilename, folderName, haltOnError, false, null);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
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
