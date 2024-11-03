package com.fulinlin.action;

import com.fulinlin.model.CommitTemplate;
import com.fulinlin.model.MessageStorage;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.fulinlin.storage.GitCommitMessageStorage;
import com.fulinlin.ui.commit.CommitDialog;
import com.fulinlin.utils.GitMessageParserUtilsFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.vcs.commit.CommitProjectPanelAdapter;
import org.jetbrains.annotations.Nullable;


/**
 * @author fulin
 */
public class CreateCommitAction extends AnAction implements DumbAware {

	private final GitCommitMessageHelperSettings settings;
	// Hold the reference to dialog object for further processing if needed
	private CommitDialog dialog;

	public CreateCommitAction() {
		this.settings = ServiceManager.getService(GitCommitMessageHelperSettings.class);
	}

	// Flag to indicate if dialog is already open
	private boolean isDialogOpen = false;

	@Override
	public void actionPerformed(@Nullable AnActionEvent actionEvent) {
		if (isDialogOpen) {
			// 如果对话框已经打开，则直接返回，避免重复打开
			return;
		}

		final CommitMessageI commitPanel = getCommitPanel(actionEvent);
		if (commitPanel == null) {
			return;
		}
		Project project = actionEvent.getProject();
		assert project != null;
		GitCommitMessageStorage storage = project.getService(GitCommitMessageStorage.class);
		GitCommitMessageStorage state = storage.getState();
		assert state != null;
		MessageStorage messageStorage = state.getMessageStorage();
		String commitMessage = ((CommitProjectPanelAdapter) commitPanel).getCommitMessage();

		CommitTemplate parse = GitMessageParserUtilsFactory.getInstance().parse(commitMessage);
		messageStorage.setCommitTemplate(parse);
		dialog = new CommitDialog(
				project, settings,
				messageStorage.getCommitTemplate(), result -> {
			// 当对话框关闭时，将 isDialogOpen 标记为 false
			isDialogOpen = false;
			if (result) {
				commitPanel.setCommitMessage(dialog.getCommitMessage(settings).toString());
				storage.getMessageStorage().setCommitTemplate(null);
			} else {
				CommitTemplate commitMessageTemplate = dialog.getCommitMessageTemplate();
				storage.getMessageStorage().setCommitTemplate(commitMessageTemplate);
			}
		}
		);
		// 标记对话框已打开
		isDialogOpen = true;

		dialog.show();


	}

	@Nullable
	private static CommitMessageI getCommitPanel(@Nullable AnActionEvent e) {
		if (e == null) {
			return null;
		}
		Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
		if (data instanceof CommitMessageI) {
			return (CommitMessageI) data;
		}
		return VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext());
	}


}