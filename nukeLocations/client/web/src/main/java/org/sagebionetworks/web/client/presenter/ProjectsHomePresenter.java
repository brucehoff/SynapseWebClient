package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.ProjectsHomeView;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class ProjectsHomePresenter extends AbstractActivity implements ProjectsHomeView.Presenter {
		
	private ProjectsHome place;
	private ProjectsHomeView view;
	private PlaceController placeController;
	private PlaceChanger placeChanger;
	private GlobalApplicationState globalApplicationState;
	private NodeServiceAsync nodeService;
	private JSONObjectAdapter jsonObjectAdapter;
	private NodeModelCreator nodeModelCreator;
	private AuthenticationController authenticationController;
	
	@Inject
	public ProjectsHomePresenter(ProjectsHomeView view,
			GlobalApplicationState globalApplicationState,
			NodeServiceAsync nodeService, JSONObjectAdapter jsonObjectAdapter,
			NodeModelCreator nodeModelCreator, AuthenticationController authenticationController) {
		this.view = view;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		this.nodeService = nodeService;
		this.jsonObjectAdapter = jsonObjectAdapter;
		this.nodeModelCreator = nodeModelCreator;
		this.authenticationController = authenticationController;
		
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(ProjectsHome place) {
		this.place = place;
		view.setPresenter(this);
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	@Override
    public String mayStop() {
        view.clear();
        return null;
    }

	@Override
	public void createProject(String name) {
		org.sagebionetworks.repo.model.Project proj = new org.sagebionetworks.repo.model.Project();
		proj.setName(name);
		JSONObjectAdapter json = jsonObjectAdapter.createNew();
		try {
			proj.writeToJSONObject(json);		
			nodeService.createNode(NodeType.PROJECT, json.toJSONString(), new AsyncCallback<String>() {			
				@Override
				public void onSuccess(String result) {
					org.sagebionetworks.repo.model.Project resultProject = null;
					try {
						resultProject = nodeModelCreator.createEntity(result, Project.class);
						view.showInfo(DisplayConstants.LABEL_PROJECT_CREATED, DisplayConstants.LABEL_PROJECT_CREATED);
						globalApplicationState.getPlaceChanger().goTo(new Synapse(resultProject.getId()));						
					} catch (RestServiceException ex) {					
						if(!DisplayUtils.handleServiceException(ex, placeChanger, authenticationController.getLoggedInUser())) {					
							onFailure(null);					
						} 
						return;
					}
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage(DisplayConstants.ERROR_GENERIC_RELOAD);
				}
			});
		} catch (JSONObjectAdapterException e) {
			view.showErrorMessage(DisplayConstants.ERROR_GENERIC);
		}
		
	}
	
}