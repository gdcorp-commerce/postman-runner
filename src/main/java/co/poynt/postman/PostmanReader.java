package co.poynt.postman;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEnvironment;

public class PostmanReader {
	ObjectMapper om;
	
	public PostmanReader() {
		om = new ObjectMapper();
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public PostmanCollection readCollectionFromFile(String fileOnClasspath) throws JsonParseException, JsonMappingException, IOException {
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileOnClasspath);
		
		PostmanCollection collection = om.readValue(stream, PostmanCollection.class);
		stream.close();
		return collection;
	}
	
	public PostmanEnvironment readEnvironmentFromFile(String fileOnClasspath) throws JsonParseException, JsonMappingException, IOException {
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileOnClasspath);
		
		PostmanEnvironment env = om.readValue(stream, PostmanEnvironment.class);
		stream.close();
		return env;		
	}
}
