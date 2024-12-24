package com.fulinlin.utils;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.components.ServiceManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		// 提取 scope, type, 和 subject
		String scope = parseScope(message);
		String type = parseType(message);
		String subject = parseSubject(message);

		// 替换第一行内容
		message = message.replace(type + "(" + scope + "): " + subject + "\n\n", "");

		// 构造正则表达式：从 feat(123): 123 后提取所有正文内容，直到出现忽略的标签或文件结束
		String regex = "^(.*?)(?:\\n\\n|$)";

		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(message);

		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return "";
	}


	public static void main(String[] args) {
		String message = "feat(123): 123\n\n1231231\n123123\n12312\n31\n23\n123\n12\n3123";


		// 提取 scope, type, 和 subject
		String scope = parseScope(message);
		String type = parseType(message);
		String subject = parseSubject(message);

		// 替换第一行内容
		message = message.replace(type + "(" + scope + "): " + subject + "\n\n", "");

		// 构造正则表达式：从 feat(123): 123 后提取所有正文内容，直到出现忽略的标签或文件结束
		String regex = "^(.*?)(?:\\n\\n|$)";

		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(message);

		if (matcher.find()) {
			System.out.println("Matched content: " + matcher.group(1).trim());
		} else {
			System.out.println("No match found.");
		}
	}



}
