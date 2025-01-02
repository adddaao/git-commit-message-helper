package com.fulinlin.utils;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.components.ServiceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitMessageParserUtils {
	protected GitCommitMessageHelperSettings settings;
	private static final String BREAKING_CHANGE = "BREAKING CHANGE:";

	public GitMessageParserUtils() {
		settings = ServiceManager.getService(GitCommitMessageHelperSettings.class);
	}

	public CommitTemplate parse(String commitMessage) {
		CommitTemplate commitTemplate = new CommitTemplate();
		commitTemplate.setScope(parseScope(commitMessage));
		commitTemplate.setType(parseType(commitMessage));
		commitTemplate.setSubject(parseSubject(commitMessage));
		commitTemplate.setChanges(parseBreakingChange(commitMessage, settings.getDateSettings().getSkipCis()));
		commitTemplate.setCloses(parseCloses(commitMessage));
		commitTemplate.setSkipCi(parseSkipCi(commitMessage));
		commitTemplate.setBody(parseBody(commitMessage, commitTemplate));
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
		message = message.split("\n")[0];
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

	// 解析 BREAKING CHANGE 部分
	public static String parseBreakingChange(String message) {
		StringBuilder breakingChangeContent = new StringBuilder();

		// 构建正则表达式来匹配 BREAKING CHANGE: 后的多行内容
		String regex = "(?s)^BREAKING CHANGE:\\s*(.*?)(?=^Closes|^\\[|$)";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(message);

		// 查找匹配
		while (matcher.find()) {
			breakingChangeContent.append(matcher.group(1).trim()).append("\n");
		}

		return breakingChangeContent.toString().trim();
	}

	// 解析 BREAKING CHANGE 部分
	public static String parseBreakingChange(String message, List<String> ignoreTags) {
		String[] lines = message.split("\n");
		StringBuilder breakingChangeContent = new StringBuilder();

		boolean inBreakingChange = false;
		boolean in2BreakingChange = false;

		for (String line : lines) {
			// 检测到 BREAKING CHANGE: 开始收集内容
			if (line.startsWith(BREAKING_CHANGE)) {
				inBreakingChange = true;
				breakingChangeContent.append(line.substring(BREAKING_CHANGE.length()).trim()).append("\n");
				continue;
			}

			// 如果在 BREAKING CHANGE 中，遇到空行停止
			if (inBreakingChange) {
				if (line.trim().isEmpty() && in2BreakingChange) {
					break;
				}
				if (line.trim().isEmpty() || ignoreTags.stream().anyMatch(line::startsWith)) {
					in2BreakingChange = true;
				}
				breakingChangeContent.append(line.trim()).append("\n");
			}
		}

		return breakingChangeContent.toString().trim();
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

		// 替换第一行内容，如果没有换行符则替换
		String replacement = type + (scope.isEmpty() ? ":" : "(" + scope + "):") + (subject.isEmpty() ? "" : " " + subject);
		if (message.contains(replacement + "\n\n")) {
			message = message.replace(replacement + "\n\n", "");
		} else if (message.contains(replacement + "\n")) {
			// 如果只有一个换行符
			message = message.replace(replacement + "\n", "");
		} else {
			// 如果没有换行符，则直接替换
			message = message.replace(replacement, "");
		}
		// 按行分割
		String[] lines = message.split("\n");

		// 过滤掉需要删除的行
		List<String> filteredLines = new ArrayList<>();
		boolean deleteLines = false;  // 用于标记是否处于需要删除的状态

		for (String line : lines) {
			// 检查是否开始删除
			if (line.startsWith(BREAKING_CHANGE) || line.startsWith("Closes") || ignoreTags.stream().anyMatch(line::startsWith)) {
				deleteLines = true;
			}

			// 如果处于删除状态，跳过当前行
			if (deleteLines) {
				filteredLines.add(null);  // 可以将删除的行标记为 null
			} else {
				filteredLines.add(line);
			}

			// 如果是 `BREAKING CHANGE:` 或 `Closes` 后的最后一行，可以停止删除
			if (deleteLines && line.trim().isEmpty()) {
				deleteLines = false;
			}
		}

		// 合并剩下的行，跳过标记为 null 的行
		message = filteredLines.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.joining("\n"))
				.trim();

		return message;
	}

	private static String parseBody(String message, CommitTemplate commitTemplate) {
		// 提取 scope, type, 和 subject
		String scope = parseScope(message);
		String type = parseType(message);
		String subject = parseSubject(message);

		// 替换第一行内容，如果没有换行符则替换
		String replacement = type + (scope.isEmpty() ? ":" : "(" + scope + "):") + (subject.isEmpty() ? "" : " " + subject);
		if (message.contains(replacement + "\n\n")) {
			message = message.replace(replacement + "\n\n", "");
		} else if (message.contains(replacement + "\n")) {
			// 如果只有一个换行符
			message = message.replace(replacement + "\n", "");
		} else {
			// 如果没有换行符，则直接替换
			message = message.replace(replacement, "");
		}
		message = message.replace(BREAKING_CHANGE + " " + commitTemplate.getChanges(), "");
		message = message.replace("Closes " + commitTemplate.getCloses(), "");
		message = message.replace(commitTemplate.getSkipCi(), "");

		return message;
	}

	public static void main(String[] args) {
		String message = "feat(123): 123";

		List<String> ignoreTags = Arrays.asList("[skip ci]", "[ci skip]", "[no ci]", "[skip actions]", "[actions skip]", "skip-checks:true", "skip-checks: true");

		// 使用 Pattern.quote 确保忽略标签的特殊字符不会影响正则匹配
		String ignoreTagsPattern = ignoreTags.stream()
				.map(Pattern::quote)
				.collect(Collectors.joining("|"));
		// 提取 scope, type, 和 subject
		String scope = parseScope(message);
		String type = parseType(message);
		String subject = parseSubject(message);

		// 替换第一行内容
		message = message.replace(type + "(" + scope + "): " + subject + "\n\n", "");

		// 构造正则表达式：从 feat(123): 123 后提取所有正文内容，直到出现忽略的标签或文件结束
		String regex = "^(.*?)(?:\\n\\n|\\n|$)(?!\\n*(BREAKING CHANGE:|Closes |" + ignoreTagsPattern + ")).*";

		Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(message);

		if (matcher.find()) {
			// System.out.println("Matched content: " + matcher.group(1).trim());
		} else {
			// System.out.println("No match found.");
		}
	}

}
