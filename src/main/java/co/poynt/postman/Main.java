package co.poynt.postman;

import picocli.CommandLine;

public class Main {
	public static void main(String[] args) {
		PostmanRunnerTools tools = new PostmanRunnerTools();
		try {
			CommandLine cmd = new CommandLine(tools);

			cmd.parseWithHandlers(
					new CommandLine.RunLast().useOut(System.out).useAnsi(CommandLine.Help.Ansi.ON),
					CommandLine.defaultExceptionHandler().useErr(System.err).useAnsi(CommandLine.Help.Ansi.OFF),
					args);
		} catch (Exception e) {
			CommandLine.usage(tools, System.err);
		}
	}
}
