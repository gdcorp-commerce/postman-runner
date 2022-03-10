package co.poynt.postman.testrail;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRailUtil {
    private static final Logger logger = LoggerFactory.getLogger(TestRailUtil.class);
    public static final int HTTP_OK = 200;

    public static JSONArray getPaginatedResults(String key, HttpRequest request) throws UnirestException {
        int offset = 0;
        JSONArray resultList = new JSONArray();
        while(true) {
            try {
                HttpResponse<JsonNode> response = request.asJson();

                if (response.getStatus() != HTTP_OK) {
                    logger.error("Failed to get all values: {} {}", response.getStatus(), response.getBody());
                    throw new RuntimeException("Failed to get all values.");
                }

                JSONObject result = response.getBody().getObject();
                JSONArray keyArray = response.getBody().getObject().getJSONArray(key);
                if (keyArray.length() == 0) {
                    // if empty, break
                    break;
                }
                for (int i = 0; i < keyArray.length(); i++) {
                    // Adding cases to result list
                    resultList.put(keyArray.get(i));
                }
                JSONObject paginatedLinks = result.getJSONObject("_links");
                Object nextLink = paginatedLinks.get("next");
                if (JSONObject.NULL.equals(nextLink)) {
                    break;
                }
                int limit = result.getInt("limit");
                offset += limit;
                request = request
                        .queryString("offset", offset);
            } catch (UnirestException e) {
                logger.error("Failed to sync request.", e);
                throw new RuntimeException("Failed to get cases.", e);
            }
        }
        logger.info("Found {} existing values for key {}.", resultList.length(), key);
        return resultList;

    }
}
