package co.poynt.postman.testrail;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.mashape.unirest.request.HttpRequest;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import co.poynt.postman.CmdBase;
import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanReader;
import picocli.CommandLine;

@CommandLine.Command(name = "newman-report-testrail", description = "Send newman report to TestRail")
public class NewmanTestrailRunReporter extends CmdBase implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(NewmanTestrailRunReporter.class);

	public static final int HTTP_OK = 200;

	@CommandLine.Option(names = { "-t",
			"--testrail" }, required = true, description = "Testrail HOSTNAME ONLY. Do not include protocol. i.e. xxxxx.testrail.io")
	private String trHost;

	private String testrailBaseUrl;

	@CommandLine.Option(names = { "-c",
			"--collection" }, required = true, description = "File name of the POSTMAN collection.")
	private String colFilename;

	@CommandLine.Option(names = { "-n", "--newman" }, required = true, description = "File name of the newman report.")
	private String newmanReportFilename;

	@CommandLine.Option(names = { "-p", "--project" }, required = true, description = "Testrail project id.")
	private String trProjectId;

	@CommandLine.Option(names = { "-u", "--user" }, required = true, description = "Testrail Username")
	private String trUser;

	@CommandLine.Option(names = { "-k", "--api-key" }, required = true, description = "Testrail API key.")
	private String trApiKey;

	@CommandLine.Option(names = { "-r",
			"--runid" }, required = false, description = "A custom run id. Typically Jenkins job id.")
	private String externalRunId;

	private class NewmanExecResult {
		public String name;
		public boolean passed;
		public String tests;

		public NewmanExecResult(String name, boolean passed, String tests) {
			this.name = name;
			this.passed = passed;
			this.tests = tests;
		}
	}
	/*
	 * IMPORTANT: at a high level, this is the mapping we will use between Postman
	 * and TestRail
	 * 
	 * @formatter:off
	 * 
	 * TestRail + Postman
	 * TestRail Project == YOUR PROJECT 
	 * TestRail Suite == Postman Collection 
	 * TestRail Section == Postman Folder 
	 * TestRail Test Case == Postman Request
	 * TestRail Test Case[custom_expected] == Postman Request Test
	 * 
	 * @formatter:on
	 */

	@Override
	public void run() {
		testrailBaseUrl = TestRailConstants.BASE_URL_PATTERN.replace("{HOSTNAME}", trHost);

		HttpClient httpClient = HttpClients.custom().disableCookieManagement().build();
		Unirest.setHttpClient(httpClient);
		PostmanReader reader = new PostmanReader();
		try {
			PostmanCollection c = reader.readCollectionFile(colFilename);
			String suiteName = FilenameUtils.getBaseName(colFilename);
			JSONObject suite = findSuite(suiteName);
			JSONArray allCases = getAllCases(suiteName, suite);

			com.fasterxml.jackson.databind.JsonNode report = om.readTree(new File(newmanReportFilename));
			List<NewmanExecResult> allResults = extractNewmanResults(c, report);

			// ALL GOOD: Start reporting
			reportResult(suiteName, suite, allCases, allResults);
		} catch (Exception e) {
			logger.error("Failed to report run.", e);
		}
	}
	
	private void reportResult(String suiteName, JSONObject suite, JSONArray allCases, List<NewmanExecResult> newmanResults) {
		JSONObject results = buildResultRequest(allCases, newmanResults);
		
		JSONObject run = createRun(suiteName, suite);
		String runId = String.valueOf(run.getInt("id"));
		try {
			HttpResponse<JsonNode> response = Unirest.post(testrailBaseUrl + "/add_results_for_cases/" + runId)
					.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).body(results).asJson();
			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to report results: {} {}", response.getStatus(), response.getBody());
				throw new RuntimeException("Failed to report results.");
			}
			logger.info("result posted to testrail successfully");
		} catch (UnirestException e) {
			logger.error("Failed to find suite.", e);
			throw new RuntimeException("Failed to get suite.", e);
		}

	}
	
	private JSONObject buildResultRequest(JSONArray allCases, List<NewmanExecResult> newmanResults) {
		JSONObject results = new JSONObject();
		JSONArray resultArray = new JSONArray();
		results.put("results", resultArray);
		
		if (allCases.length() != newmanResults.size()) {
			logger.error("Number of cases does not match: {} {}", allCases.length(), newmanResults.size());
			throw new RuntimeException("Number of cases does not match results.");
		}
		
		//IMPORTANT: we rely on the order of the test cases to match the list of newman results
		for (int i = 0; i < allCases.length(); i++) {
			JSONObject c = allCases.getJSONObject(i);
			String caseId = String.valueOf(c.getInt("id"));
			String caseTitle = c.getString("title");
			NewmanExecResult newmanExecResult = newmanResults.remove(0);
			if (!caseTitle.equals(newmanExecResult.name)) {
				logger.error("Found mis-match case result: '{}' '{}'", caseTitle, newmanExecResult.name);
				throw new RuntimeException("Found mis-match case result.");
			}
			
			JSONObject r = new JSONObject();
			resultArray.put(i, r);
			r.put("case_id", caseId);
			r.put("status_id", newmanExecResult.passed ? 1 : 5);
			r.put("comment", newmanExecResult.tests);
		}
		
		return results;
	}

	private JSONObject findSuite(String suiteName) {
		// First we need to find the suite by matching its name with the postman
		// collection
		JSONObject suite = null;
		try {
			HttpResponse<JsonNode> response = Unirest.get(testrailBaseUrl + "/get_suites/" + trProjectId)
					.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).asJson();

			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to get suites: {} {}", response.getStatus(), response.getBody());
				throw new RuntimeException("Failed to get suite.");
			}

			JSONArray suitesArray = response.getBody().getArray();

			for (int i = 0; i < suitesArray.length(); i++) {
				JSONObject s = suitesArray.getJSONObject(i);
				if (suiteName.equals(s.getString("name"))) {
					suite = s;
				}
			}
		} catch (UnirestException e) {
			logger.error("Failed to find suite.", e);
			throw new RuntimeException("Failed to get suite.", e);
		}
		if (suite == null) {
			throw new RuntimeException("Project does not have suite with name: " + suiteName);
		}
		logger.info("Found suite {}", suiteName);
		return suite;
	}

	private JSONArray getAllCases(String suiteName, JSONObject suite) {
		try {
			String suiteId = String.valueOf(suite.getInt("id"));
			HttpRequest testRailHttpReq = Unirest.get(testrailBaseUrl + "/get_cases/" + trProjectId)
					.queryString("suite_id", suiteId)
					.header("Content-Type", "application/json")
					.basicAuth(trUser, trApiKey);

			return TestRailUtil.getPaginatedResults("cases", testRailHttpReq);
		} catch (UnirestException e) {
			logger.error("Failed to find suite.", e);
			throw new RuntimeException("Failed to get suite.", e);
		}
	}

	private JSONObject createRun(String suiteName, JSONObject suite) {
		JSONObject result = null;
		try {
			long suiteId = suite.getInt("id");
			logger.info("Suite exist id: {}, name: {}", suiteId, suiteName);
			JSONObject run = new JSONObject();
			run.put("suite_id", suiteId);
			// default: include_all = true
			if (externalRunId == null) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				Calendar now = Calendar.getInstance();
				externalRunId = sdf.format(now.getTime());
			}
			run.put("name", suiteName + ": " + externalRunId);

			HttpResponse<JsonNode> response = Unirest.post(testrailBaseUrl + "/add_run/" + trProjectId)
					.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).body(run).asJson();

			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to create run for {}: {} {}", suiteName, response.getStatus(), response.getBody());
				throw new UnirestException("Failed to create run for " + suiteName + ": " + response.getStatus() + " "
						+ response.getBody());
			}

			// logger.info(response.toString());
			result = response.getBody().getObject();
		} catch (Exception e) {
			logger.error("Failed to create run.", e);
			throw new RuntimeException("Failed to create run.", e);
		}
		return result;
	}

	private List<NewmanExecResult> extractNewmanResults(PostmanCollection collection,
			com.fasterxml.jackson.databind.JsonNode report) {
		ObjectNode reportRun = (ObjectNode) report.get("run");
		ArrayNode executions = (ArrayNode) reportRun.get("executions");
		List<NewmanExecResult> allResults = new ArrayList<>(executions.size());

		for (com.fasterxml.jackson.databind.JsonNode exec : executions) {
			ObjectNode item = (ObjectNode) exec.get("item");
			ArrayNode assertions = (ArrayNode) exec.get("assertions");
			String name = item.get("name").asText().trim();
			StringBuilder expectedResults = new StringBuilder();

			boolean passed = true;
			if (assertions != null) {
				for (com.fasterxml.jackson.databind.JsonNode assertion : assertions) {
					expectedResults.append("\n");
					boolean hasError = assertion.has("error");
					passed = passed && !hasError; // once failed, always failed
					expectedResults.append("   ").append(assertion.get("assertion")).append(": ")
							.append((hasError ? "FAILED\n" : "PASSED\n"));
				}
			}
			allResults.add(new NewmanExecResult(name, passed, expectedResults.toString()));
		}
		return allResults;
	}

}
