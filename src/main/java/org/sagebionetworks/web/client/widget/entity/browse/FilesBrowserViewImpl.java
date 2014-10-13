package org.sagebionetworks.web.client.widget.entity.browse;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.entity.SharingAndDataUseConditionWidget;
import org.sagebionetworks.web.client.widget.entity.download.QuizInfoWidget;
import org.sagebionetworks.web.client.widget.entity.download.UploadDialogWidget;

import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.WindowEvent;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class FilesBrowserViewImpl extends FlowPanel implements FilesBrowserView {

	private Presenter presenter;
	private EntityTreeBrowser entityTreeBrowser;
	private UploadDialogWidget uploader;
	private QuizInfoWidget quizInfoWidget;
	private SharingAndDataUseConditionWidget sharingAndDataUseWidget;
	private PortalGinInjector ginInjector;
	
	@Inject
	public FilesBrowserViewImpl(SageImageBundle sageImageBundle,
			IconsImageBundle iconsImageBundle,
			UploadDialogWidget uploader,
			CookieProvider cookies,
			SharingAndDataUseConditionWidget sharingAndDataUseWidget,
			QuizInfoWidget quizInfoWidget,
			PortalGinInjector ginInjector) {
		this.uploader = uploader;
		this.ginInjector = ginInjector;
		this.sharingAndDataUseWidget = sharingAndDataUseWidget;
		this.quizInfoWidget = quizInfoWidget;
	}

	@Override
	public void configure(String entityId, boolean canCertifiedUserAddChild) {
		this.clear();
		this.add(uploader.asWidget());	//add the upload dialog
		entityTreeBrowser = ginInjector.getEntityTreeBrowser();
		FlowPanel fp = new FlowPanel();
		FlowPanel topbar = new FlowPanel();
		
		if(canCertifiedUserAddChild) {
			Button upload = getUploadButton(entityId);
			upload.addStyleName("margin-right-5");
			// AbstractImagePrototype.create(iconsImageBundle.synapseFolderAdd16())
			Button addFolder = DisplayUtils.createIconButton(DisplayConstants.ADD_FOLDER, DisplayUtils.ButtonType.DEFAULT, "glyphicon-plus");
			addFolder.addClickHandler(new ClickHandler() {				
				@Override
				public void onClick(ClickEvent event) {
					//for additional functionality, it now creates the folder up front, and the dialog will rename (and change share and data use)
					presenter.addFolderClicked();
				}
			});
		
			topbar.add(upload);
			topbar.add(addFolder);
		}
		
		SimplePanel files = new SimplePanel();
		files.addStyleName("highlight-box padding-top-0-imp");
		entityTreeBrowser.configure(entityId, true);
		Widget etbW = entityTreeBrowser.asWidget();
		etbW.addStyleName("margin-top-10");
		files.setWidget(etbW);
		//If we are showing the buttons or a title, then add the topbar.  Otherwise don't
		if (canCertifiedUserAddChild) {
			fp.add(topbar);
		}
		fp.add(files);
		this.add(fp);
	}
	
	@Override
	public void showQuizInfoDialog(final CallbackP<Boolean> callback) {
		FilesBrowserViewImpl.showQuizInfoDialog(callback, quizInfoWidget);
	}
	
	public static void showQuizInfoDialog(final CallbackP<Boolean> callback, QuizInfoWidget quizInfoWidget) {
		final Window dialog = new Window();
		dialog.setMaximizable(false);
		dialog.setSize(420, 270);
		dialog.setPlain(true);
		dialog.setModal(true);
		dialog.setLayout(new FitLayout());
		dialog.setBorders(false);
		dialog.setHeading("Join the Synapse Certified User Community");

		quizInfoWidget.configure(new CallbackP<Boolean>() {
			@Override
			public void invoke(Boolean tutorialClicked) {
				dialog.hide();
				callback.invoke(tutorialClicked);
			}
		});
		dialog.add(quizInfoWidget.asWidget());
		dialog.show();
		DisplayUtils.center(dialog);
	}
	
	@Override
	public void showUploadDialog(String entityId){
		EntityUpdatedHandler handler = new EntityUpdatedHandler() {				
			@Override
			public void onPersistSuccess(EntityUpdatedEvent event) {
				presenter.fireEntityUpdatedEvent();
			}
		};
		uploader.configure(DisplayConstants.TEXT_UPLOAD_FILE_OR_LINK, null, entityId, handler, null, true);
		uploader.show();
	}
	
	@Override
	public void showFolderEditDialog(final String folderEntityId) {
		SimplePanel sharingAndDataUseContainer = new SimplePanel();
		Callback refreshSharingAndDataUseWidget = new Callback() {
			@Override
			public void invoke() {
				//entity was updated by the sharing and data use widget.
				sharingAndDataUseWidget.setEntity(folderEntityId);
			}
		};
		sharingAndDataUseWidget.configure(folderEntityId, true, refreshSharingAndDataUseWidget);
		sharingAndDataUseContainer.add(sharingAndDataUseWidget.asWidget());

		final Dialog dialog = new Dialog();
		dialog.setMaximizable(false);
		dialog.setSize(460, 260);
		dialog.setPlain(true);
		dialog.setModal(true);
		dialog.setHideOnButtonClick(true);
		dialog.setLayout(new FitLayout());
		dialog.setBorders(false);
		dialog.setButtons(Dialog.OKCANCEL);
		dialog.setHeading("New Folder");

		final FormPanel panel = new FormPanel();
		panel.setHeaderVisible(false);
		panel.setFrame(false);
		panel.setBorders(false);
		panel.setShadow(false);
		panel.setBodyBorder(false);
		panel.setFieldWidth(345);

		final TextField<String> nameField = new TextField<String>();
				nameField.setFieldLabel(DisplayConstants.LABEL_NAME);
		panel.add(nameField);			
		panel.add(sharingAndDataUseContainer);
		dialog.getButtonBar().removeAll();
		final com.extjs.gxt.ui.client.widget.button.Button okButton = new com.extjs.gxt.ui.client.widget.button.Button(DisplayConstants.OK);
		okButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				dialog.hide(okButton);
				String nameVal = nameField.getValue();
				nameField.clear();
				presenter.updateFolderName(nameVal, folderEntityId);
			}
		});
		dialog.addButton(okButton);
		
		final com.extjs.gxt.ui.client.widget.button.Button cancelButton = new com.extjs.gxt.ui.client.widget.button.Button(DisplayConstants.BUTTON_CANCEL, new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				dialog.hide();
			}
		});
		dialog.addButton(cancelButton);
		
		dialog.addListener(Events.Hide, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				//window is hiding.  if it was because of any reason other than the ok button being clicked, then cancel folder creation (delete it)
				if (be instanceof WindowEvent && ((WindowEvent)be).getButtonClicked() != okButton) {
					cancelFolderCreation(dialog, nameField, folderEntityId);
				}
			};
		});
		// Enter key in name field submits
		new KeyNav<ComponentEvent>(nameField) {
			@Override
			public void onEnter(ComponentEvent ce) {
				super.onEnter(ce);
				if(okButton.isEnabled())
					okButton.fireEvent(Events.Select);
			}
		};

		//and name textfield should have focus by default
		nameField.focus();
		
		dialog.add(panel);
		dialog.show();
	}

	private void cancelFolderCreation(Dialog dialog, TextField<String> nameField, String folderEntityId){
		nameField.clear();
		presenter.deleteFolder(folderEntityId, true);
	}
	
	@Override
	public Widget asWidget() {
		return this;
	}	

	@Override 
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
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
		super.clear();
	}

	@Override
	public void refreshTreeView(String entityId) {
		entityTreeBrowser.configure(entityId, true);
	}
	
	/**
	 * TODO : this should be replaced by DisplayUtils.getUploadButton with the locationable uploader able to create 
	 * an entity and upload file in a single transaction it modified to create a new 
	 */
	private Button getUploadButton(final String entityId) {
		Button uploadButton = DisplayUtils.createIconButton(DisplayConstants.TEXT_UPLOAD_FILE_OR_LINK, DisplayUtils.ButtonType.DEFAULT, "glyphicon-arrow-up");
		uploadButton.addStyleName("left display-inline");
		
		uploadButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				presenter.uploadButtonClicked();
			}
		});
		return uploadButton;
	}
	
	

}
