package org.sagebionetworks.web.client.widget.discussion.modal;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

public interface NewThreadModalView extends IsWidget{

	public interface Presenter {
		/**
		 * Show the new thread modal.
		 */
		public void show();

		public void onSave(String threadTitle, String messageMarkdown);

		public void onCancel();

		public void hide();

		Widget asWidget();

		void setForumId(String forumId);
	}

	void setPresenter(Presenter presenter);
	void showDialog();
	void hideDialog();

}
