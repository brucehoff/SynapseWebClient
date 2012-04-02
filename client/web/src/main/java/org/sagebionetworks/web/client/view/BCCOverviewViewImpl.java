package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.place.users.PasswordReset;
import org.sagebionetworks.web.client.widget.editpanels.NodeEditor;
import org.sagebionetworks.web.client.widget.entity.TempPropertyPresenter;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.table.QueryServiceTableResourceProvider;
import org.sagebionetworks.web.shared.BCCSignupProfile;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class BCCOverviewViewImpl extends Composite implements BCCOverviewView {

	public interface BCCOverviewViewImplUiBinder extends UiBinder<Widget, BCCOverviewViewImpl> {}

	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField
	SimplePanel applyForChallenge;
		
	private Presenter presenter;
	private IconsImageBundle icons;
	private Header headerWidget;
	
	private TempPropertyPresenter props;

	
	@Inject
	public BCCOverviewViewImpl(BCCOverviewViewImplUiBinder binder,
			Header headerWidget, Footer footerWidget, IconsImageBundle icons,
			SageImageBundle imageBundle,
			QueryServiceTableResourceProvider queryServiceTableResourceProvider,
			final NodeEditor nodeEditor, TempPropertyPresenter props) {		
		initWidget(binder.createAndBindUi(this));

		this.icons = icons;
		this.headerWidget = headerWidget;
		
		this.props = props;
		this.props.initializeWithTestData();
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());		
				
	}



	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;
		headerWidget.refresh();	

	}

	@Override
	public void showOverView() {
		Button applyForChallengeButton = new Button("Apply to Join the Challenge", null, new SelectionListener<ButtonEvent>() {			
			@Override
			public void componentSelected(ButtonEvent ce) {
				BCCSignupProfile profile = presenter.getBCCSignupProfile();
				final BCCCallback callback = presenter.getBCCSignupCallback();
				BCCSignupHelper.showDialog(profile, callback);				
			}
		});
		applyForChallenge.clear();
		applyForChallenge.add(applyForChallengeButton);
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showLoading() {
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void clear() {		
	}



	@Override
	public void showSubmissionAcknowledgement() {
		Window.alert("Your submission has been received.");
	}



	@Override
	public void showSubmissionError() {
		Window.alert("There was an error during submission.  Please try again or contact Sage Bionetworks.");
	}

}