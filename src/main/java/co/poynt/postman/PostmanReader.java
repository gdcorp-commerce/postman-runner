package co.poynt.postman;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.poynt.postman.model.PostmanCollection;
import co.poynt.postman.model.PostmanEnvironment;

public class PostmanReader {

	private ObjectMapper om;

	public PostmanReader() {

		this.om = new ObjectMapper();
		this.om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	}

	public PostmanCollection readCollectionFileClasspath(final String fileOnClasspath) throws IOException {

		final String fileName = fileOnClasspath.substring(fileOnClasspath.indexOf(':')+1);
		final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		return deserializeToTypeReferenceAndCloseStream(stream, new TypeReference<PostmanCollection>(){});

	}

	public PostmanEnvironment readEnvironmentFileClasspath(final String fileOnClasspath) throws IOException {

		final String fileName = fileOnClasspath.substring(fileOnClasspath.indexOf(':')+1);
		final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
		return deserializeToTypeReferenceAndCloseStream(stream, new TypeReference<PostmanEnvironment>(){});

	}

	public PostmanCollection readCollectionFile(final String filePath) throws IOException {

		if (filePath.startsWith("classpath:")) {
			return readCollectionFileClasspath(filePath);
		}
		final InputStream stream = new FileInputStream(new File(filePath));
		return deserializeToTypeReferenceAndCloseStream(stream, new TypeReference<PostmanCollection>(){});

	}

	public PostmanEnvironment readEnvironmentFile(final String filePath) throws IOException {

		if (filePath == null) {
			return new PostmanEnvironment();
		}
		if (filePath.startsWith("classpath:")) {
			return readEnvironmentFileClasspath(filePath);
		}
		final InputStream stream = new FileInputStream(new File(filePath));
		return deserializeToTypeReferenceAndCloseStream(stream, new TypeReference<PostmanEnvironment>(){});

	}

	public PostmanCollection readCollectionFromString(final String collection) throws IOException {

		final InputStream stream = IOUtils.toInputStream(collection);
		return deserializeToTypeReferenceAndCloseStream(stream, new TypeReference<PostmanCollection>(){});

	}

	public PostmanEnvironment readEnvironmentFromString(final String environment) throws IOException {

		final InputStream stream = IOUtils.toInputStream(environment);
		return deserializeToTypeReferenceAndCloseStream(stream, new TypeReference<PostmanEnvironment>(){});

	}

	private <T> T deserializeToTypeReferenceAndCloseStream(final InputStream inputStream, final TypeReference typeReference) throws IOException {

		final T postmanClassInstance = this.om.readValue(inputStream, typeReference);
		inputStream.close();
		return postmanClassInstance;

	}

}