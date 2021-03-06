package com.github.bookong.zest.core;

import java.io.File;

import org.junit.runners.model.FrameworkMethod;

/**
 * @author jiangxu
 *
 */
public class ZestFrameworkMethod extends FrameworkMethod {
	private String testCaseFilePath;
	private String testCaseFileName;

	public ZestFrameworkMethod(FrameworkMethod fmethod, String testCaseFilePath) {
		super(fmethod.getMethod());
		this.testCaseFilePath = testCaseFilePath;
		this.testCaseFileName = testCaseFilePath.substring(testCaseFilePath.lastIndexOf(File.separator) + 1);
	}
	
	@Override
    public String getName() {
		return String.format("%s [%s] ", super.getName(), testCaseFileName);
	}

	public String getTestCaseFilePath() {
		return testCaseFilePath;
	}

	public void setTestCaseFilePath(String testCaseFilePath) {
		this.testCaseFilePath = testCaseFilePath;
	}

	public String getTestCaseFileName() {
		return testCaseFileName;
	}

	public void setTestCaseFileName(String testCaseFileName) {
		this.testCaseFileName = testCaseFileName;
	}
}
