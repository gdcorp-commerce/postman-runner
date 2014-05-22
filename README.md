# postman-runner

This module will be able to run any .postman_collection + .postman_environment scripts.

Features:
* Reads in POSTMAN collection and environment variables
* Evaluate tests (JavaScripts) defined in POSTMAN requests.

# Running

1. Clone this repository.
2. Place your .postman_collection and .postman_environment in `src/main/resources` folder
3. `mvn install`
