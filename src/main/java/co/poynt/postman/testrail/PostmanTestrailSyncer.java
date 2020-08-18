package co.poynt.postman.testrail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import co.poynt.postman.CmdBase;
import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEvent;
import co.poynt.postman.model.PostmanFolder;
import co.poynt.postman.model.PostmanItem;
import co.poynt.postman.model.PostmanReader;
import picocli.CommandLine;

@CommandLine.Command(name = "sync-testrail", description = "Sync Postman collection to TestRail")
public class PostmanTestrailSyncer extends CmdBase implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(PostmanTestrailSyncer.class);

	public static final int HTTP_OK = 200;

	@CommandLine.Option(names = { "-t",
			"--testrail" }, required = true, description = "Testrail HOSTNAME ONLY. Do not include protocol. i.e. xxxxx.testrail.io")
	private String trHost;

	private String testrailBaseUrl;

	@CommandLine.Option(names = { "-c",
			"--collection" }, required = true, description = "File name of the POSTMAN collection.")
	private String colFilename;

	@CommandLine.Option(names = { "-p", "--project" }, required = true, description = "Testrail project id.")
	private String trProjectId;

	@CommandLine.Option(names = { "-u", "--user" }, required = true, description = "Testrail Username")
	private String trUser;

	@CommandLine.Option(names = { "-k", "--api-key" }, required = true, description = "Testrail API key.")
	private String trApiKey;

	// Pattern1: tests["THIS IS A TEST NAME1"] = someExpression;
	// Pattern2: pm.test("THIS IS A TEST NAME2", function()...;
	private static final Pattern TESTNAME_PATTERN1 = Pattern.compile("tests\\[(.*?)\\].*");
	private static final Pattern TESTNAME_PATTERN2 = Pattern.compile("pm.test\\((.*?),.*");

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
			syncCollection(c);
		} catch (Exception e) {
			logger.error("Failed to sync.", e);
		}
	}

	private void syncCollection(PostmanCollection collection) {
		// First we need to find the suite by matching its name with the postman
		// collection
		try {
			HttpResponse<JsonNode> response = Unirest.get(testrailBaseUrl + "/get_suites/" + trProjectId)
					.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).asJson();

			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to get suites: {} {}", response.getStatus(), response.getBody());
				return;
			}

			JSONArray suitesArray = response.getBody().getArray();

			String suiteName = FilenameUtils.getBaseName(colFilename);
			JSONObject suite = null;
			for (int i = 0; i < suitesArray.length(); i++) {
				JSONObject s = suitesArray.getJSONObject(i);
				if (suiteName.equals(s.getString("name"))) {
					suite = s;
				}
			}

			if (suite == null) {
				suite = createSuite(suiteName);
			} else {
				logger.info("Suite already exist id: {}, name: {}", suite.getBigInteger("id"), suiteName);
			}

			syncFolders(collection.item, suite);

		} catch (UnirestException e) {
			logger.error("Failed to sync collections.", e);
		}
	}

	private void syncFolders(List<PostmanFolder> folders, JSONObject suite) {
		try {
			String suiteId = String.valueOf(suite.getInt("id"));
			HttpResponse<JsonNode> response = Unirest.get(testrailBaseUrl + "/get_sections/" + trProjectId)
					.queryString("suite_id", suiteId).header("Content-Type", "application/json")
					.basicAuth(trUser, trApiKey).asJson();

			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to get sections: {} {}", response.getStatus(), response.getBody());
				return;
			}

			Map<String, JSONObject> allSections = new HashMap<>();
			JSONArray arraySections = response.getBody().getArray();
			for (int i = 0; i < arraySections.length(); i++) {
				JSONObject section = arraySections.getJSONObject(i);
				allSections.put(section.getString("name"), section);
			}

			for (PostmanFolder folder : folders) {
				JSONObject section = allSections.get(folder.name);
				if (section == null) {
					section = createSection(folder, suite.getInt("id"));
					allSections.put(folder.name, section);
				} else {
					logger.info("===> Section already exist id: {}, name: {}", section.getInt("id"), folder.name);
				}

				syncRequests(folder.item, section);
			}

		} catch (UnirestException e) {
			logger.error("Failed to sync folder.", e);
		}
	}

	private void syncRequests(List<PostmanItem> items, JSONObject section) {
		try {
			String sectionId = String.valueOf(section.getInt("id"));
			String suiteId = String.valueOf(section.getInt("suite_id"));
			HttpResponse<JsonNode> response = Unirest.get(testrailBaseUrl + "/get_cases/" + trProjectId)
					.queryString("suite_id", suiteId).queryString("section_id", sectionId)
					.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).asJson();

			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to get cases: {} {}", response.getStatus(), response.getBody());
				return;
			}

			JSONArray arrayCases = response.getBody().getArray();
			Map<String, JSONObject> allCases = new HashMap<>();
			for (int i = 0; i < arrayCases.length(); i++) {
				JSONObject c = arrayCases.getJSONObject(i);
				allCases.put(c.getString("title"), c);
			}

			for (PostmanItem request : items) {
				JSONObject trCase = allCases.get(request.name);
				if (trCase == null) {
					trCase = createCase(request, section.getInt("id"));
					allCases.put(request.name, trCase);
				} else {
					logger.info("======> Case already exist id: {}, name: {}", trCase.getInt("id"), request.name);

					trCase = updateCase(request, trCase);
					allCases.put(request.name, trCase);
				}
			}

			// TODO: add tests under cases

		} catch (Exception e) {
			logger.error("Failed to sync request.", e);
		}
	}

	private List<String> findTests(List<String> script) {
		List<String> result = new ArrayList<>();
		for (String line : script) {
			Matcher matcher1 = TESTNAME_PATTERN1.matcher(line);
			Matcher matcher2 = TESTNAME_PATTERN2.matcher(line);

			if (matcher1.find(0)) {
				result.add(matcher1.group(1));
			} else if (matcher2.find(0)) {
				result.add(matcher2.group(1));
			}
		}
		return result;
	}

	private String buildExpectedResult(PostmanItem item) {
		String result = "";
		if (item.event != null) {
			for (PostmanEvent event : item.event) {
				if (event.listen.equals("test")) {
					List<String> foundTests = findTests(event.script.exec);
					for (int i = 0; i < foundTests.size(); i++) {
						result += i + ". " + foundTests.get(i) + "\n";
					}
				}
			}
		}
		return result;
	}

	private JSONObject createSuite(String suiteName) {
		try {
			logger.info("Creating new suite: {}", suiteName);
			JSONObject suite = new JSONObject();
			suite.put("name", suiteName);
			suite.put("description", suiteName);

			HttpResponse<JsonNode> response = Unirest.post(testrailBaseUrl + "/add_suite/" + trProjectId)
					.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).body(suite).asJson();

			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to create suite {}: {} {}", suiteName, response.getStatus(), response.getBody());
				throw new UnirestException(
						"Failed to create suite " + suiteName + ": " + response.getStatus() + " " + response.getBody());
			}

			// logger.info(response.toString());
			return response.getBody().getObject();

		} catch (UnirestException e) {
			logger.error("Failed to create suite {}.", suiteName, e);
			throw new RuntimeException("Failed to create suite.", e);
		}
	}

	private JSONObject createSection(PostmanFolder folder, int suiteId) {
		try {
			logger.info("Creating new section: {}", folder.name);
			JSONObject section = new JSONObject();
			section.put("name", folder.name);
			section.put("suite_id", suiteId);

			HttpResponse<JsonNode> response = Unirest.post(testrailBaseUrl + "/add_section/" + trProjectId)
					.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).body(section).asJson();

			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to create section {}: {} {}", folder.name, response.getStatus(),
						response.getBody());
				throw new UnirestException("Failed to create section " + folder.name + ": " + response.getStatus() + " "
						+ response.getBody());
			}

			// logger.info(response.toString());
			return response.getBody().getObject();

		} catch (UnirestException e) {
			logger.error("Failed to create suite {}.", folder.name, e);
			throw new RuntimeException("Failed to create suite.", e);
		}
	}

	private String buildStepsDescription(PostmanItem item) {
		String stepsDescription = item.request.method + " " + item.request.url.raw + "\n";
		if (item.request.body != null && item.request.body.raw != null) {
			if (item.request.body.raw.startsWith("{")) {
				try {
					JSONObject body = new JSONObject(item.request.body.raw);
					stepsDescription += body.toString(2);
				} catch (JSONException e) {
					// logger.error("Failed to prettify raw json: {}", item.request.body.raw, e);
					// ignore
					stepsDescription += item.request.body.raw;
				}
			} else {
				stepsDescription += item.request.body.raw;
			}
		}
		return stepsDescription;
	}

	private JSONObject updateCase(PostmanItem item, JSONObject existingCase) {
		String caseId = String.valueOf(existingCase.getInt("id"));
		String stepsDescription = buildStepsDescription(item);
		String expectedResult = buildExpectedResult(item);

		String existingSteps = existingCase.optString(TestRailConstants.FLD_CUSTOM_STEPS);
		String existingExpected = existingCase.optString(TestRailConstants.FLD_CUSTOM_EXPECTED);

		if (!stepsDescription.equals(existingSteps) || !expectedResult.equals(existingExpected)) {
			JSONObject update = new JSONObject();
			update.put(TestRailConstants.FLD_CUSTOM_STEPS, stepsDescription);
			update.put(TestRailConstants.FLD_CUSTOM_EXPECTED, expectedResult);
			try {
				logger.info("======> Updating existing test case: {}", item.name);
				HttpResponse<JsonNode> response = Unirest.post(testrailBaseUrl + "/update_case/" + caseId)
						.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).body(update).asJson();

				if (response.getStatus() != HTTP_OK) {
					logger.error("Failed to update case {}: {} {}", item.name, response.getStatus(),
							response.getBody());
					throw new UnirestException("Failed to update case " + item.name + ": " + response.getStatus() + " "
							+ response.getBody());
				}

				existingCase = response.getBody().getObject();
			} catch (UnirestException e) {
				logger.error("Failed to update existing test case: {}", item.name, e);
				throw new RuntimeException("Failed to update existing test case.", e);
			}
		}
		return existingCase;
	}

	private JSONObject createCase(PostmanItem item, int sectionId) {
		try {
			logger.info("======> Creating new case: {}", item.name);
			JSONObject trCase = new JSONObject();
			trCase.put("title", item.name);
			trCase.put("type_id", 2); // 2 == Functionality
			trCase.put("section_id", sectionId);
			trCase.put(TestRailConstants.FLD_CUSTOM_STEPS, buildStepsDescription(item));
			trCase.put(TestRailConstants.FLD_CUSTOM_EXPECTED, buildExpectedResult(item));

			HttpResponse<JsonNode> response = Unirest.post(testrailBaseUrl + "/add_case/" + String.valueOf(sectionId))
					.header("Content-Type", "application/json").basicAuth(trUser, trApiKey).body(trCase).asJson();

			if (response.getStatus() != HTTP_OK) {
				logger.error("Failed to create case {}: {} {}", item.name, response.getStatus(), response.getBody());
				throw new UnirestException("Failed to create case {}" + item.name + ": " + response.getStatus() + " "
						+ response.getBody());
			}

			// logger.info(response.toString());
			return response.getBody().getObject();

		} catch (UnirestException e) {
			logger.error("Failed to create case {}.", item.name);
			throw new RuntimeException("Failed to create case.", e);
		}
	}
}
