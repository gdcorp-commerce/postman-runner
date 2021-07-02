package co.poynt.postman;

import co.poynt.postman.runner.PostmanCollectionRunner;
import co.poynt.postman.testrail.NewmanTestrailRunReporter;
import co.poynt.postman.testrail.PostmanTestrailSyncer;
import picocli.CommandLine;

@CommandLine.Command(name = "postman-tools", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
//@formatter:off
        subcommands = {
                PostmanCollectionRunner.class,
                PostmanTestrailSyncer.class,
                NewmanTestrailRunReporter.class
//@formatteer:on
        })
public class PostmanRunnerTools implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}