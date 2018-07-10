package co.poynt.postman.test;


import co.poynt.postman.PostmanReader;
import co.poynt.postman.PostmanRequestRunner;
import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.*;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TestPostman {

	@Test(enabled = false)
	public void testLoadFile() throws Exception {
		PostmanRunResult runResult = new PostmanRunResult();
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		String fileName = "PoyntRegression.postman_collection";
		PostmanReader reader = new PostmanReader();

		PostmanCollection c = reader.readCollectionFileClasspath(fileName);
		c.init();
		// System.out.println(om.writeValueAsString(c));

		String envFile = "PoyntRegression.postman_environment";
		PostmanEnvironment e = reader.readEnvironmentFileClasspath(envFile);
		e.init();
		// System.out.println(om.writeValueAsString(e));

		PostmanVariables var = new PostmanVariables(e);
		PostmanRequestRunner runner = new PostmanRequestRunner(var, false);
		
		//For each folder, execute each request in order
		for (PostmanFolder f : c.item) {
			if (f.name.equals("Terminal use-cases")) {
				for (PostmanItem item : f.item){
					runner.run(item, runResult);
				}
			}
		}
	}
}
