package org.sagebionetworks.web.client.widget.accessrequirements.requestaccess;

import org.sagebionetworks.web.client.utils.Callback;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * View shows the first step of the wizard
 * 
 * @author Jay
 *
 */
public interface CreateDataAccessSubmissionWizardStep2View extends IsWidget {
	void setDUCVisible(boolean visible);
	void setDUCUploadWidget(IsWidget w);
	void setDUCUploadedFileWidget(IsWidget w);
	void setDUCTemplateFileWidget(IsWidget w);
	void setIRBVisible(boolean visible);
	void setIRBUploadWidget(IsWidget w);
	void setIRBUploadedFileWidget(IsWidget w);
	void setOtherDocumentUploadVisible(boolean visible);
	void setOtherDocumentUploadWidget(IsWidget w);
	void clearOtherDocumentsUploaded();
	void addOtherDocumentUploaded(IsWidget w);
	void setAccessorListWidget(IsWidget w);
	void showConfirmDialog(String title, String message, Callback yesCallback);
	void setPublicationsVisible(boolean visible);
	void setPublications(String text);
	String getPublications();
	
	void setSummaryOfUseVisible(boolean visible);
	void setSummaryOfUse(String text);
	String getSummaryOfUse();
	void showInfo(String message);
}
