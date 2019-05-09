package co.poynt.postman.test;

import org.testng.Assert;
import org.testng.annotations.Test;

import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanVariables;

public class TestPostmanVariables {

	@Test
	public void testVariableSubstitution() throws Exception {
		PostmanEnvironment env = new PostmanEnvironment();

		env.setEnvironmentVariable("foo", "bar");

		PostmanVariables var = new PostmanVariables(env);

		String result = var.replace("{{foo}}");

		Assert.assertEquals(result, "bar");
	}

	@Test
	public void testVariableSubstitutionWithWhitespace() throws Exception {
		PostmanEnvironment env = new PostmanEnvironment();

		env.setEnvironmentVariable("foo", "bar");

		PostmanVariables var = new PostmanVariables(env);

		String result = var.replace("{{ foo    }}");

		Assert.assertEquals(result, "bar");
	}
}
