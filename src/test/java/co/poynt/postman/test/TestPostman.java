package co.poynt.postman.test;

import java.util.Collections;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import co.poynt.postman.PostmanReader;
import co.poynt.postman.PostmanRequestRunner;
import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanFolder;
import co.poynt.postman.model.PostmanItem;
import co.poynt.postman.model.PostmanVariables;

public class TestPostman {

	@Test(enabled = true)
	public void testLoadFile() throws Exception {
		PostmanRunResult runResult = new PostmanRunResult();
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		String fileName = "PostmanRunnerRegression.postman_collection.json";
		PostmanReader reader = new PostmanReader();

		PostmanCollection c = reader.readCollectionFileClasspath(fileName);
		c.init();
		// System.out.println(om.writeValueAsString(c));

		String envFile = "PostmanRunnerRegression.postman_environment.json";
		PostmanEnvironment e = reader.readEnvironmentFileClasspath(envFile);
		e.init();
		// System.out.println(om.writeValueAsString(e));

		PostmanVariables var = new PostmanVariables(e);
		PostmanRequestRunner.Observer observer = new PostmanRequestRunner.Observer() {

			@Override
			public void preTransport(PostmanItem item, HttpRequestBase httpRequest) {
				// TODO Auto-generated method stub

			}

			@Override
			public void postTransport(PostmanItem item, HttpResponse httpResponse) {
				// TODO Auto-generated method stub

			}

		};
		PostmanRequestRunner runner = new PostmanRequestRunner(var, false, Collections.singletonList(observer));

		// For each folder, execute each request in order
		for (PostmanFolder f : c.item) {
			for (PostmanItem item : f.item) {
				runner.run(item, runResult);
			}
		}
	}
}
