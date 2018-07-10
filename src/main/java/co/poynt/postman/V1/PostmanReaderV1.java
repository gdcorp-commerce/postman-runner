package co.poynt.postman.V1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.poynt.postman.modelV1.PostmanCollectionV1;
import co.poynt.postman.model.PostmanEnvironment;

public class PostmanReaderV1 {
	ObjectMapper om;
	
	public PostmanReaderV1() {
		om = new ObjectMapper();
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	public PostmanCollectionV1 readCollectionFileClasspath(String fileOnClasspath) throws JsonParseException, JsonMappingException, IOException {
		String fileName = fileOnClasspath.substring(fileOnClasspath.indexOf(":")+1);
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		
		PostmanCollectionV1 collection = om.readValue(stream, PostmanCollectionV1.class);
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
	
	public PostmanCollectionV1 readCollectionFile(String filePath) throws Exception {
		if (filePath.startsWith("classpath:")) {
			return readCollectionFileClasspath(filePath);
		}
		InputStream stream = new FileInputStream(new File(filePath));
		PostmanCollectionV1 collection = om.readValue(stream, PostmanCollectionV1.class);
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
