package co.poynt.postman;

import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanCollection;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PostmanReader {
	ObjectMapper om;
	
	public PostmanReader() {
		om = new ObjectMapper();
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public PostmanCollection readCollectionFileClasspath(String fileOnClasspath) throws JsonParseException, JsonMappingException, IOException {
		String fileName = fileOnClasspath.substring(fileOnClasspath.indexOf(":")+1);
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		
		PostmanCollection collection = om.readValue(stream, PostmanCollection.class);
		stream.close();
		return collection;
	}
	
	public PostmanEnvironment readEnvironmentFileClasspath(String fileOnClasspath) throws JsonParseException, JsonMappingException, IOException {
		String fileName = fileOnClasspath.substring(fileOnClasspath.indexOf(":")+1);
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		
		PostmanEnvironment env = om.readValue(stream, PostmanEnvironment.class);
		stream.close();
		return env;
	}
	
	public PostmanCollection readCollectionFile(String filePath) throws Exception {
		if (filePath.startsWith("classpath:")) {
			return readCollectionFileClasspath(filePath);
		}
		InputStream stream = new FileInputStream(new File(filePath));
		PostmanCollection collection = om.readValue(stream, PostmanCollection.class);
		stream.close();
		return collection;
	}

	public PostmanEnvironment readEnvironmentFile(String filePath) throws Exception {
		if (filePath == null) {
			return new PostmanEnvironment();
		}
		if (filePath.startsWith("classpath:")) {
			return readEnvironmentFileClasspath(filePath);
		}
		InputStream stream = new FileInputStream(new File(filePath));
		PostmanEnvironment env = om.readValue(stream, PostmanEnvironment.class);
		stream.close();
		return env;
	}
}
