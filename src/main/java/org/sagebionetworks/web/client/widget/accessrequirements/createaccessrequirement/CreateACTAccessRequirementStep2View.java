package org.sagebionetworks.web.client.widget.accessrequirements.createaccessrequirement;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * View shows the second step of the wizard for ACT AR
 * 
 * @author Jay
 *
 */
public interface CreateACTAccessRequirementStep2View extends IsWidget {
	void setAreOtherAttachmentsRequired(boolean value);
	void setExpirationPeriod(Long days);
	void setIsCertifiedUserRequired(boolean value);
	void setIsDUCRequired(boolean value);
	void setIsIDUPublic(boolean value);
	void setIsIRBApprovalRequired(boolean value);
	void setIsValidatedProfileRequired(boolean value);
	void setOldTermsVisible(boolean visible);
	void setOldTerms(String terms);
	boolean areOtherAttachmentsRequired();
	long getExpirationPeriod();
	boolean isCertifiedUserRequired();
	boolean isDUCRequired();
	boolean isIDUPublic();
	boolean isIRBApprovalRequired();
	boolean isValidatedProfileRequired();
	boolean getHasRequests();
	
	void setWikiPageRenderer(IsWidget w);
	void setDUCTemplateUploadWidget(IsWidget w);
	void setDUCTemplateWidget(IsWidget w);
	public void showHasRequestUI(boolean hasRequest);
	public void setPresenter(Presenter p);
	/*
	 * Presenter interface
	 */
	public interface Presenter {
		void onEditWiki();
	}
}
