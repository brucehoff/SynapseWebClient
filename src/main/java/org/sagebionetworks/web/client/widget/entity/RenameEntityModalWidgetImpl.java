package org.sagebionetworks.web.client.widget.entity;

import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.client.StringUtils;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.utils.Callback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * A simple modal dialog for creating a TableEntity.
 * This widget is a bootstrap modal and must be added to the page. 
 * @author John
 *
 */
public class RenameEntityModalWidgetImpl implements EntityNameModalView.Presenter, RenameEntityModalWidget {

	public static final String BUTTON_TEXT = "Rename";
	public static final String LABLE_SUFFIX = " name";
	public static final String TITLE_PREFIX = "Rename ";

	public static final String NAME_MUST_INCLUDE_AT_LEAST_ONE_CHARACTER = "Name must include at least one character.";
	
	EntityNameModalView view;
	SynapseClientAsync synapseClient;
	EntityTypeProvider typeProvider;
	String parentId;
	Entity toRename;
	String startingName;
	Callback handler;
	
	@Inject
	public RenameEntityModalWidgetImpl(EntityNameModalView view,
			SynapseClientAsync synapseClient, EntityTypeProvider typeProvider) {
		super();
		this.view = view;
		this.synapseClient = synapseClient;
		this.view.setPresenter(this);
		this.typeProvider = typeProvider;
	}
	
	
	/**
	 * Create the table.
	 * @param name
	 */
	private void updateEntity(final String name) {
		view.setLoading(true);
		toRename.setName(name);
		synapseClient.updateEntity(toRename, new AsyncCallback<Entity>() {
			@Override
			public void onSuccess(Entity result) {
				view.hide();
				handler.invoke();
			}
			@Override
			public void onFailure(Throwable caught) {
				// put the name back.
				toRename.setName(startingName);
				view.showError(caught.getMessage());
				view.setLoading(false);
			}
		});
	}

	/**
	 * Should be Called when the create button is clicked on the dialog.
	 */
	@Override
	public void onPrimary() {
		String name = StringUtils.trimWithEmptyAsNull(view.getName());
		if(name == null){
			view.showError(NAME_MUST_INCLUDE_AT_LEAST_ONE_CHARACTER);
		}else if(this.startingName.equals(name)){
			// just hide the view if the name has not changed.
			view.hide();
		}else{
			// Create the table
			updateEntity(name);
		}
	}
	

	@Override
	public Widget asWidget() {
		return view.asWidget();
	}

	@Override
	public void onRename(Entity toRename, Callback handler) {
		this.handler = handler;
		String typeName = typeProvider.getEntityDispalyName(toRename);
		this.toRename = toRename;
		this.startingName = toRename.getName();
		this.view.clear();
		this.view.configure(TITLE_PREFIX+typeName, typeName+LABLE_SUFFIX, BUTTON_TEXT, toRename.getName());
		this.view.show();
	}


}
