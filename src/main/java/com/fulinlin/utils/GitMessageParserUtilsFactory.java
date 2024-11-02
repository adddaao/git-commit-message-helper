package com.fulinlin.utils;

public class GitMessageParserUtilsFactory {
	private static final GitMessageParserUtils instance = new GitMessageParserUtils();

	public static GitMessageParserUtils getInstance() {
		return instance;
	}
}