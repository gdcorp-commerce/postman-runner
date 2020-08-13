package co.poynt.postman;

import co.poynt.postman.runner.PostmanCollectionRunner;
import picocli.CommandLine;

@CommandLine.Command(name = "postman-tools", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
//@formatter:off
        subcommands = {
                PostmanCollectionRunner.class
//@formatteer:on
        })
public class PostmanRunnerTools implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}