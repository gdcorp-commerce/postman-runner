# Postman Tools

A module to work with [POSTMAN](https://www.getpostman.com/) collections.

[TestRail](https://www.gurock.com/testrail/) test case sync integration was added as of version 3.x.x.

# Command-Line Runner

## Postman Runner

To run a POSTMAN collection:

```
cd bin
./postman-tools run --collection=[POSTMAN_COLLECTION_FILE] --environment=[POSTMAN_ENVIRONMENT_FILE] --folder="[POSTMAN_FOLDER_TO_RUN]" --haltonerror=false
```

Run `postman-tools` without any argument to see all the options.

## TestRail

To sync your postman collection to TestRail, run the following:

```
cd bin
./postman-tools sync-testrail --testrail=[TR_HOST] --collection=my_postman_collection.json --project=[TR_PROJECT_ID] --user=[TR_USERNAME] --api-key=[TR_API_KEY]
```

The only requirement is that you have already created your TestRail project and you know the ID. Note remove the `P` prefix from your project id when specifying it in the `--project` argument.

The mapping between postman and TestRails are as follow:

|*Postman*|*TestRail*|
|---------|----------|
|Collection|Suite|
|Folder|Section|
|Request|Case|
|Request body|Case custom field `custom_steps`|
|Request Test|Case custom field `custom_expected`|

Unfortunately, since TestRail API does not permit external ID assignment for any of their API object model, all matching are based on the object name.

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

3.0.0 is currently the latest version of postman-runner.

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
