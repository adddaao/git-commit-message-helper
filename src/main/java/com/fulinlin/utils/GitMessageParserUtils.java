package com.fulinlin.utils;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.components.ServiceManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitMessageParserUtils {
	protected GitCommitMessageHelperSettings settings;

	public GitMessageParserUtils() {
		settings = ServiceManager.getService(GitCommitMessageHelperSettings.class);
	}

	public CommitTemplate parse(String commitMessage) {
		CommitTemplate commitTemplate = new CommitTemplate();
		commitTemplate.setScope(parseScope(commitMessage));
		commitTemplate.setType(parseType(commitMessage));
		commitTemplate.setSubject(parseSubject(commitMessage));
		commitTemplate.setBody(parseBody(commitMessage, settings.getDateSettings().getSkipCis()));
		commitTemplate.setChanges(parseBreakingChange(commitMessage));
		commitTemplate.setCloses(parseCloses(commitMessage));
		commitTemplate.setSkipCi(parseSkipCi(commitMessage));
		return commitTemplate;
	}


	private static String parseType(String message) {
		Pattern pattern = Pattern.compile("^(\\w+)(?:\\([^)]*\\))?:", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private static String parseScope(String message) {
		Pattern pattern = Pattern.compile("^\\w+\\(([^)]*)\\):", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private static String parseSubject(String message) {
		// 改进后的正则表达式
		Pattern pattern = Pattern.compile("^\\w+(?:\\([^)]*\\))?:\\s*([^\\[]+?)(?=\\n|\\s*\\[|$)", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private static String parseBody(String message) {
		Pattern pattern = Pattern.compile("^\\w+(?:\\([^)]*\\))?:\\s*.+\\n+([\\s\\S]*?)(?=\\n*BREAKING CHANGE:|\\n*Closes |\\n*\\[skip ci]|$)", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private static String parseBreakingChange(String message) {
		Pattern pattern = Pattern.compile("^BREAKING CHANGE:\\s*(.+)$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private static String parseCloses(String message) {
		Pattern pattern = Pattern.compile("^Closes\\s+(.+)$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

	private static String parseSkipCi(String message) {
		Pattern pattern = Pattern.compile("^\\[skip ci]$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			return "[skip ci]";
		}
		return "";
	}

	private static String parseBody(String message, List<String> ignoreTags) {
		// 使用 Pattern.quote 确保标签中的特殊字符不会影响正则匹配
		String ignoreTagsPattern = ignoreTags.stream()
				.map(Pattern::quote)
				.collect(Collectors.joining("|"));

		// 构建新的正则表达式
		// 主要修复了之前排除逻辑中的混乱
		String regex = "^\\w+(?:\\([^)]*\\))?:\\s*.+\\n+([\\s\\S]*?)(?=\\n*BREAKING CHANGE:|\\n*Closes |\\n*(?!"
				+ ignoreTagsPattern
				+ ")$)";

		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);

		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}

}
