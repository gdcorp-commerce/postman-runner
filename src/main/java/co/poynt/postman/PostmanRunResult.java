package co.poynt.postman;

import java.util.ArrayList;
import java.util.List;

public class PostmanRunResult {
	public int totalRequest = 0;
	public int failedRequest = 0;
	public int totalTest = 0;
	public int failedTest = 0;

	public List<String> failedRequestName = new ArrayList<String>();
	public List<String> failedTestName = new ArrayList<String>();

	@Override
	public String toString() {
		String s = "\n*************************************\n";
		s += "Total Requests = " + totalRequest + "\n";
		s += "Failed Requests = " + failedRequest + "\n";
		s += "Total Tests = " + totalTest + "\n";
		s += "Failed Tests = " + failedTest + "\n";
		s += "Failed Request Names: " + failedRequestName + "\n";
		s += "Failed Test Names: " + failedTestName + "\n";
		s += "*************************************\n";
		return s;
	}

	public boolean isSuccessful() {
		return failedRequest == 0 && failedTest == 0;
	}
}
