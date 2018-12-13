# postman-runner

A module to run a [POSTMAN](https://www.getpostman.com/) collections.

# Command-Line Runner

To run a POSTMAN collection:

```
cd bin
./postman-runner.sh -c [POSTMAN_COLLECTION_FILE] -e [POSTMAN_ENVIRONMENT_FILE] -f "[POSTMAN_FOLDER_TO_RUN]" -haltonerror false
```

Run `postman-runner.sh` without any argument to see all the options.

# Invoking from Java

Add the following maven dependency:

```xml
		<dependency>
			<groupId>co.poynt.postman.runner</groupId>
			<artifactId>postman-runner</artifactId>
			<version>X.X.X</version>
			<scope>test</scope>
		</dependency>
```
where X.X.X is the latest version of this artifact.

2.0.2 is currently the latest version of postman-runner.

From your test driver class, make the following call:

```java
	public void testRunPostman() throws Exception {
		PostmanCollectionRunner cr = new PostmanCollectionRunner();

		boolean isSuccessful = cr.runCollection(
				"classpath:MyTestCollection.postman_collection",
				"classpath:MyTestCollection.postman_environment",
				"My use cases", false).isSuccessful();
		
		Assert.assertTrue(isSuccessful);
	}
```
# Postman Compatibility

The current version of postman-runner is compatible with the Postman Collection v2.1 format.  However, the support for certain global variables in test scripts introduced in the latest version of Postman (i.e. `pm`) is not yet available.  There is a git issue (#10) open for it and we will add support for this as soon as we can.
