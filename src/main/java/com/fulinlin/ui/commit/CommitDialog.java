package com.fulinlin.ui.commit;

import com.fulinlin.localization.PluginBundle;
import com.fulinlin.model.CommitTemplate;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

public class CommitDialog extends DialogWrapper {

	private final CommitPanel panel;
	private final Consumer<Boolean> callback; // 回调函数，用来返回对话框结果

	public CommitDialog(@Nullable Project project, GitCommitMessageHelperSettings settings, CommitTemplate commitMessageTemplate, Consumer<Boolean> callback) {
		// 设置为非模态对话框 applicationModalIfPossible 表示为非模态（非阻塞）
		super(project, false, true);
		panel = new CommitPanel(project, settings, commitMessageTemplate);
		this.callback = callback; // 赋值回调函数

		setModal(false); // 设置为非模态
		setTitle(PluginBundle.get("commit.panel.title"));
		setOKButtonText(PluginBundle.get("commit.panel.ok.button"));
		setCancelButtonText(PluginBundle.get("commit.panel.cancel.button"));
		init();
	}


	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return panel.getMainPanel();
	}


	public CommitMessage getCommitMessage(GitCommitMessageHelperSettings settings) {
		return panel.getCommitMessage(settings);
	}

	public CommitTemplate getCommitMessageTemplate() {
		return panel.getCommitMessageTemplate();
	}

	// 重写 OK 按钮的操作逻辑
	@Override
	protected void doOKAction() {
		// 可以添加确认按钮的相关逻辑，比如获取输入内容等
		close(OK_EXIT_CODE); // 关闭对话框并返回 OK 状态
		if (callback != null) {
			callback.accept(true); // 通过回调函数返回操作结果
		}
	}

	// 重写 Cancel 按钮的操作逻辑
	@Override
	public void doCancelAction() {
		// 你可以添加需要的取消逻辑，比如清理一些状态等
		close(CANCEL_EXIT_CODE); // 关闭对话框并返回取消状态
		if (callback != null) {
			callback.accept(false); // 通过回调函数返回操作结果
		}
	}

}