package org.sagebionetworks.web.unitclient.widget.entity.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerImpl.DELETED;
import static org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerImpl.DELETE_PREFIX;
import static org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerImpl.MOVE_PREFIX;
import static org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerImpl.RENAME_PREFIX;
import static org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerImpl.THE;
import static org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerImpl.WAS_SUCCESSFULLY_DELETED;

import java.util.Set;

import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.extras.bootbox.client.callback.PromptCallback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils.SelectedHandler;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.place.Synapse.EntityArea;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.entity.EvaluationSubmitter;
import org.sagebionetworks.web.client.widget.entity.MarkdownEditorWidget;
import org.sagebionetworks.web.client.widget.entity.RenameEntityModalWidget;
import org.sagebionetworks.web.client.widget.entity.browse.EntityFinder;
import org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerImpl;
import org.sagebionetworks.web.client.widget.entity.controller.EntityActionControllerView;
import org.sagebionetworks.web.client.widget.entity.controller.PreflightController;
import org.sagebionetworks.web.client.widget.entity.download.UploadDialogWidget;
import org.sagebionetworks.web.client.widget.entity.menu.v2.Action;
import org.sagebionetworks.web.client.widget.entity.menu.v2.ActionMenuWidget;
import org.sagebionetworks.web.client.widget.sharing.AccessControlListModalWidget;
import org.sagebionetworks.web.shared.WikiPageKey;
import org.sagebionetworks.web.shared.exceptions.BadRequestException;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;
import org.sagebionetworks.web.shared.exceptions.UnauthorizedException;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class EntityActionControllerImplTest {

	EntityActionControllerView mockView;
	PreflightController mockPreflightController;
	SynapseClientAsync mockSynapseClient;
	GlobalApplicationState mockGlobalApplicationState;
	PlaceChanger mockPlaceChanger;
	AuthenticationController mockAuthenticationController;
	AccessControlListModalWidget mockAccessControlListModalWidget;
	RenameEntityModalWidget mockRenameEntityModalWidget;
	EntityFinder mockEntityFinder;
	EvaluationSubmitter mockSubmitter;
	UploadDialogWidget mockUploader;
	
	ActionMenuWidget mockActionMenu;
	EntityUpdatedHandler mockEntityUpdatedHandler;
	
	EntityBundle entityBundle;
	UserEntityPermissions permissions;

	EntityActionControllerImpl controller;
	String parentId;
	String entityId;
//	String entityDispalyType;
	String currentUserId = "12344321";
	String wikiPageId = "999";
	MarkdownEditorWidget mockMarkdownEditorWidget;
	Reference selected;

	@Before
	public void before() {
		mockView = Mockito.mock(EntityActionControllerView.class);
		mockPreflightController = Mockito.mock(PreflightController.class);
		mockSynapseClient = Mockito.mock(SynapseClientAsync.class);
		mockGlobalApplicationState = Mockito.mock(GlobalApplicationState.class);
		mockPlaceChanger = Mockito.mock(PlaceChanger.class);
		mockRenameEntityModalWidget = Mockito.mock(RenameEntityModalWidget.class);
		mockAuthenticationController = Mockito
				.mock(AuthenticationController.class);
		mockMarkdownEditorWidget = Mockito.mock(MarkdownEditorWidget.class);
		mockAccessControlListModalWidget = Mockito
				.mock(AccessControlListModalWidget.class);
		
		mockActionMenu = Mockito.mock(ActionMenuWidget.class);
		mockEntityUpdatedHandler = Mockito.mock(EntityUpdatedHandler.class);
		mockEntityFinder = Mockito.mock(EntityFinder.class);
		mockSubmitter = Mockito.mock(EvaluationSubmitter.class);
		mockUploader = Mockito.mock(UploadDialogWidget.class);
		
		when(mockAuthenticationController.isLoggedIn()).thenReturn(true);
		when(mockAuthenticationController.getCurrentUserPrincipalId()).thenReturn(currentUserId);
		when(mockGlobalApplicationState.getPlaceChanger()).thenReturn(mockPlaceChanger);
		
		// The controller under test.
		controller = new EntityActionControllerImpl(mockView,
				mockPreflightController,
				mockSynapseClient, mockGlobalApplicationState,
				mockAuthenticationController, mockAccessControlListModalWidget,
				mockRenameEntityModalWidget, mockEntityFinder, mockSubmitter, mockUploader,
				mockMarkdownEditorWidget);
		
		parentId = "syn456";
		entityId = "syn123";
		Entity table = new TableEntity();
		table.setId(entityId);
		table.setParentId(parentId);
		permissions = new UserEntityPermissions();
		permissions.setCanChangePermissions(true);
		permissions.setCanDelete(true);
		permissions.setCanPublicRead(true);
		permissions.setCanUpload(true);
		permissions.setCanAddChild(true);
		permissions.setCanEdit(true);
		entityBundle = new EntityBundle();
		entityBundle.setEntity(table);
		entityBundle.setPermissions(permissions);
		
		selected = new Reference();
		selected.setTargetId("syn9876");
		// Setup the mock entity selector to select an entity.
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation)
					throws Throwable {
				SelectedHandler<Reference> handler = (SelectedHandler<Reference>) invocation.getArguments()[1];
				handler.onSelected(selected);
				return null;
			}
		}).when(mockEntityFinder).configure(anyBoolean(), any(SelectedHandler.class));
	}

	@Test
	public void testConfigure(){
		controller.configure(mockActionMenu, entityBundle, wikiPageId, mockEntityUpdatedHandler);
		verify(mockAccessControlListModalWidget).configure(any(Entity.class), anyBoolean());
		//delete
		verify(mockActionMenu).setActionEnabled(Action.DELETE_ENTITY, true);
		verify(mockActionMenu).setActionVisible(Action.DELETE_ENTITY, true);
		verify(mockActionMenu).setActionText(Action.DELETE_ENTITY, DELETE_PREFIX+EntityType.table.getDisplayName());
		verify(mockActionMenu).addActionListener(Action.DELETE_ENTITY, controller);
		// share
		verify(mockActionMenu).setActionEnabled(Action.SHARE, true);
		verify(mockActionMenu).setActionVisible(Action.SHARE, true);
		verify(mockActionMenu).addActionListener(Action.SHARE, controller);
		// Rename
		verify(mockActionMenu).setActionEnabled(Action.CHANGE_ENTITY_NAME, true);
		verify(mockActionMenu).setActionVisible(Action.CHANGE_ENTITY_NAME, true);
		verify(mockActionMenu).setActionText(Action.CHANGE_ENTITY_NAME, RENAME_PREFIX+EntityType.table.getDisplayName());
		verify(mockActionMenu).addActionListener(Action.CHANGE_ENTITY_NAME, controller);
		// upload
		verify(mockActionMenu).setActionEnabled(Action.UPLOAD_NEW_FILE, false);
		verify(mockActionMenu).setActionVisible(Action.UPLOAD_NEW_FILE, false);
	}
	
	@Test
	public void testConfigurePublicRead(){
		entityBundle.getPermissions().setCanPublicRead(true);
		controller.configure(mockActionMenu, entityBundle, wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionIcon(Action.SHARE, IconType.GLOBE);
	}
	
	@Test
	public void testConfigureNotPublic(){
		entityBundle.getPermissions().setCanPublicRead(false);
		controller.configure(mockActionMenu, entityBundle, wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionIcon(Action.SHARE, IconType.LOCK);
	}
	
	@Test
	public void testConfigureNoWiki(){
		entityBundle.setEntity(new Folder());
		entityBundle.setRootWikiId(null);
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.EDIT_WIKI_PAGE, true);
		verify(mockActionMenu).setActionVisible(Action.EDIT_WIKI_PAGE, true);
		verify(mockActionMenu).addActionListener(Action.EDIT_WIKI_PAGE, controller);
	}
	
	@Test
	public void testConfigureWiki(){
		entityBundle.setEntity(new Folder());
		entityBundle.setRootWikiId("7890");
		controller.configure(mockActionMenu, entityBundle,wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.EDIT_WIKI_PAGE, true);
		verify(mockActionMenu).setActionVisible(Action.EDIT_WIKI_PAGE, true);
		verify(mockActionMenu).addActionListener(Action.EDIT_WIKI_PAGE, controller);
	}
	
	@Test
	public void testConfigureWikiCannotEdit(){
		entityBundle.setEntity(new Folder());
		entityBundle.setRootWikiId("7890");
		permissions.setCanEdit(false);
		controller.configure(mockActionMenu, entityBundle,wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.EDIT_WIKI_PAGE, false);
		verify(mockActionMenu).setActionVisible(Action.EDIT_WIKI_PAGE, false);
		verify(mockActionMenu).addActionListener(Action.EDIT_WIKI_PAGE, controller);
	}
	
	@Test
	public void testConfigureWikiNoWikiTable(){
		entityBundle.setEntity(new TableEntity());
		entityBundle.setRootWikiId(null);
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.EDIT_WIKI_PAGE, false);
		verify(mockActionMenu).setActionVisible(Action.EDIT_WIKI_PAGE, false);
	}
	

	@Test
	public void testConfigureViewWikiSource(){
		entityBundle.setEntity(new Folder());
		entityBundle.setRootWikiId("7890");
		controller.configure(mockActionMenu, entityBundle,wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.VIEW_WIKI_SOURCE, false);
		verify(mockActionMenu).setActionVisible(Action.VIEW_WIKI_SOURCE, false);
		verify(mockActionMenu).addActionListener(Action.VIEW_WIKI_SOURCE, controller);
	}
	
	@Test
	public void testConfigureViewWikiSourceCannotEdit(){
		entityBundle.setEntity(new Folder());
		entityBundle.setRootWikiId("7890");
		permissions.setCanEdit(false);
		controller.configure(mockActionMenu, entityBundle,wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.VIEW_WIKI_SOURCE, true);
		verify(mockActionMenu).setActionVisible(Action.VIEW_WIKI_SOURCE, true);
		verify(mockActionMenu).addActionListener(Action.VIEW_WIKI_SOURCE, controller);
	}
	
	@Test
	public void testConfigureViewWikiSourceWikiTable(){
		entityBundle.setEntity(new TableEntity());
		entityBundle.setRootWikiId("22");
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.VIEW_WIKI_SOURCE, false);
		verify(mockActionMenu).setActionVisible(Action.VIEW_WIKI_SOURCE, false);
	}
	
	@Test
	public void testConfigureMoveTable(){
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.MOVE_ENTITY, false);
		verify(mockActionMenu).setActionVisible(Action.MOVE_ENTITY, false);
	}
	
	@Test
	public void testConfigureMove(){
		entityBundle.setEntity(new Folder());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.MOVE_ENTITY, true);
		verify(mockActionMenu).setActionVisible(Action.MOVE_ENTITY, true);
		verify(mockActionMenu).setActionText(Action.MOVE_ENTITY, MOVE_PREFIX+EntityType.folder.getDisplayName());
		verify(mockActionMenu).addActionListener(Action.MOVE_ENTITY, controller);
	}
	
	@Test
	public void testConfigureUploadNewFile(){
		entityBundle.setEntity(new FileEntity());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.UPLOAD_NEW_FILE, true);
		verify(mockActionMenu).setActionVisible(Action.UPLOAD_NEW_FILE, true);
		verify(mockActionMenu).addActionListener(Action.UPLOAD_NEW_FILE, controller);
	}
	
	
	@Test
	public void testConfigureUploadNewFileNoUpload(){
		entityBundle.getPermissions().setCanAddChild(false);
		entityBundle.setEntity(new FileEntity());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.UPLOAD_NEW_FILE, false);
		verify(mockActionMenu).setActionVisible(Action.UPLOAD_NEW_FILE, false);
		verify(mockActionMenu).addActionListener(Action.UPLOAD_NEW_FILE, controller);
	}
	
	@Test
	public void testOnDeleteConfirmCancel(){
		/*
		 *  The user must be shown a confirm dialog before a delete.  Confirm is signaled via the Callback.invoke()
		 *  in this case we do not want to confirm.
		 */
		AsyncMockStubber.callNoInvovke().when(mockView).showConfirmDialog(anyString(), anyString(), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// the call under tests
		controller.onAction(Action.DELETE_ENTITY);
		verify(mockView).showConfirmDialog(anyString(), anyString(), any(Callback.class));
		// should not make it to the pre-flight check
		verify(mockPreflightController, never()).checkDeleteEntity(any(EntityBundle.class), any(Callback.class));
	}
	
	@Test
	public void testOnDeleteConfirmedPreFlightFailed(){
		// confirm the delete
		AsyncMockStubber.callWithInvoke().when(mockView).showConfirmDialog(anyString(), anyString(), any(Callback.class));
		/*
		 * The preflight check is confirmed by calling Callback.invoke(), in this case it must not be invoked.
		 */
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkDeleteEntity(any(EntityBundle.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// the call under test
		controller.onAction(Action.DELETE_ENTITY);
		verify(mockView).showConfirmDialog(anyString(), anyString(), any(Callback.class));
		verify(mockPreflightController).checkDeleteEntity(any(EntityBundle.class), any(Callback.class));
		// Must not make it to the actual delete since preflight failed.
		verify(mockSynapseClient, never()).deleteEntityById(anyString(), any(AsyncCallback.class));
	}
	
	@Test
	public void testOnDeleteConfirmedPreFlightPassedDeleteFailed(){
		// confirm the delete
		AsyncMockStubber.callWithInvoke().when(mockView).showConfirmDialog(anyString(), anyString(), any(Callback.class));
		// confirm pre-flight
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkDeleteEntity(any(EntityBundle.class), any(Callback.class));
		String error = "some error";
		AsyncMockStubber.callFailureWith(new Throwable(error)).when(mockSynapseClient).deleteEntityById(anyString(), any(AsyncCallback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// the call under test
		controller.onAction(Action.DELETE_ENTITY);
		verify(mockView).showConfirmDialog(anyString(), anyString(), any(Callback.class));
		verify(mockPreflightController).checkDeleteEntity(any(EntityBundle.class), any(Callback.class));
		// an attempt to delete should be made
		verify(mockSynapseClient).deleteEntityById(anyString(), any(AsyncCallback.class));
		verify(mockView).showErrorMessage(DisplayConstants.ERROR_ENTITY_DELETE_FAILURE);
	}
	
	@Test
	public void testOnDeleteConfirmedPreFlightPassedDeleteSuccess(){
		// confirm the delete
		AsyncMockStubber.callWithInvoke().when(mockView).showConfirmDialog(anyString(), anyString(), any(Callback.class));
		// confirm pre-flight
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkDeleteEntity(any(EntityBundle.class), any(Callback.class));
		AsyncMockStubber.callSuccessWith(null).when(mockSynapseClient).deleteEntityById(anyString(), any(AsyncCallback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// the call under test
		controller.onAction(Action.DELETE_ENTITY);
		verify(mockView).showConfirmDialog(anyString(), anyString(), any(Callback.class));
		verify(mockPreflightController).checkDeleteEntity(any(EntityBundle.class), any(Callback.class));
		// an attempt to delete should be made
		verify(mockSynapseClient).deleteEntityById(anyString(), any(AsyncCallback.class));
		verify(mockView).showInfo(DELETED, THE + EntityType.table.getDisplayName() + WAS_SUCCESSFULLY_DELETED);
		verify(mockGlobalApplicationState).gotoLastPlace(new Synapse(parentId, null, EntityArea.TABLES, null) );
	}
	
	@Test
	public void testCreateDeletePlaceNullParentId(){
		entityBundle.getEntity().setParentId(null);
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// call under test
		Place result =controller.createDeletePlace();
		assertTrue(result instanceof Profile);
		assertEquals(currentUserId, ((Profile)result).getUserId());
	}
	
	@Test
	public void testCreateDeletePlaceProject(){
		// setup a project
		Entity project = new Project();
		project.setId(entityId);
		project.setParentId(parentId);
		entityBundle = new EntityBundle();
		entityBundle.setEntity(project);
		entityBundle.setPermissions(permissions);
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// call under test
		Place result = controller.createDeletePlace();
		assertTrue(result instanceof Profile);
		assertEquals(currentUserId, ((Profile)result).getUserId());
	}
	
	@Test
	public void testCreateDeletePlaceFile(){
		// setup a project
		Entity file = new FileEntity();
		file.setId(entityId);
		file.setParentId(parentId);
		entityBundle.setEntity(file);
		entityBundle.setPermissions(entityBundle.getPermissions());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// call under test
		Place result = controller.createDeletePlace();
		Place expected = new Synapse(parentId);
		assertEquals(expected, result);
	}
	
	@Test
	public void testOnShareNoChange(){
		/*
		 * Share change is confirmed by calling Callback.invoke(), in this case it must not be invoked.
		 */
		AsyncMockStubber.callNoInvovke().when(mockAccessControlListModalWidget).showSharing(any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// method under test
		controller.onAction(Action.SHARE);
		verify(mockAccessControlListModalWidget).showSharing(any(Callback.class));
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testOnShareWithChange(){
		// invoke this time
		AsyncMockStubber.callWithInvoke().when(mockAccessControlListModalWidget).showSharing(any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// method under test
		controller.onAction(Action.SHARE);
		verify(mockAccessControlListModalWidget).showSharing(any(Callback.class));
		verify(mockEntityUpdatedHandler).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testRenameHappy(){
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		AsyncMockStubber.callWithInvoke().when(mockRenameEntityModalWidget).onRename(any(Entity.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// method under test
		controller.onAction(Action.CHANGE_ENTITY_NAME);
		verify(mockRenameEntityModalWidget).onRename(any(Entity.class), any(Callback.class));
		verify(mockEntityUpdatedHandler).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testRenameNoChange(){
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		AsyncMockStubber.callNoInvovke().when(mockRenameEntityModalWidget).onRename(any(Entity.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// method under test
		controller.onAction(Action.CHANGE_ENTITY_NAME);
		verify(mockRenameEntityModalWidget).onRename(any(Entity.class), any(Callback.class));
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testRenameFailedPreFlight(){
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		AsyncMockStubber.callNoInvovke().when(mockRenameEntityModalWidget).onRename(any(Entity.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		// method under test
		controller.onAction(Action.CHANGE_ENTITY_NAME);
		verify(mockRenameEntityModalWidget, never()).onRename(any(Entity.class), any(Callback.class));
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testOnAddWikiNoUpdate(){
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		entityBundle.setRootWikiId(null);
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.EDIT_WIKI_PAGE);
		verify(mockSynapseClient, never()).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testOnAddWikiCanUpdate(){
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		AsyncMockStubber.callSuccessWith(new WikiPage()).when(mockSynapseClient).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		entityBundle.setRootWikiId(null);
		controller.configure(mockActionMenu, entityBundle, null,mockEntityUpdatedHandler);
		controller.onAction(Action.EDIT_WIKI_PAGE);
		verify(mockMarkdownEditorWidget).configure(any(WikiPageKey.class), any(CallbackP.class));
	}
	
	@Test
	public void testOnViewWikiSource(){
		WikiPage page = new WikiPage();
		String markdown = "hello markdown";
		page.setMarkdown(markdown);
		AsyncMockStubber.callSuccessWith(page).when(mockSynapseClient).getV2WikiPageAsV1(any(WikiPageKey.class),any(AsyncCallback.class));
		entityBundle.setRootWikiId("111");
		controller.configure(mockActionMenu, entityBundle, null,mockEntityUpdatedHandler);
		controller.onAction(Action.VIEW_WIKI_SOURCE);
		verify(mockView).showInfoDialog(anyString(), eq(markdown));
	}
	
	@Test
	public void testOnViewWikiSourceError(){
		AsyncMockStubber.callFailureWith(new Exception()).when(mockSynapseClient).getV2WikiPageAsV1(any(WikiPageKey.class),any(AsyncCallback.class));
		entityBundle.setRootWikiId("111");
		controller.configure(mockActionMenu, entityBundle, null,mockEntityUpdatedHandler);
		controller.onAction(Action.VIEW_WIKI_SOURCE);
		verify(mockView).showErrorMessage(anyString());
	}
	
	@Test
	public void testIsMovableType(){
		assertFalse(controller.isMovableType(new Project()));
		assertFalse(controller.isMovableType(new TableEntity()));
		assertTrue(controller.isMovableType(new FileEntity()));
		assertTrue(controller.isMovableType(new Folder()));
		assertTrue(controller.isMovableType(new Link()));
	}
	
	@Test
	public void testIsWikableType(){
		assertTrue(controller.isWikiableType(new Project()));
		assertFalse(controller.isWikiableType(new TableEntity()));
		assertTrue(controller.isWikiableType(new FileEntity()));
		assertTrue(controller.isWikiableType(new Folder()));
		assertFalse(controller.isWikiableType(new Link()));
	}
	
	@Test
	public void testIsLinkType(){
		assertTrue(controller.isLinkType(new Project()));
		assertTrue(controller.isLinkType(new TableEntity()));
		assertTrue(controller.isLinkType(new FileEntity()));
		assertTrue(controller.isLinkType(new Folder()));
		assertFalse(controller.isLinkType(new Link()));
	}
	
	@Test
	public void testIsSubmittableType(){
		assertFalse(controller.isSubmittableType(new Project()));
		assertFalse(controller.isSubmittableType(new TableEntity()));
		assertTrue(controller.isSubmittableType(new FileEntity()));
		assertFalse(controller.isSubmittableType(new Folder()));
		assertFalse(controller.isSubmittableType(new Link()));
	}
	
	@Test
	public void testOnMoveNoUpdate(){
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		entityBundle.setEntity(new Folder());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.MOVE_ENTITY);
		verify(mockEntityFinder, never()).configure(anyBoolean(), any(SelectedHandler.class));
		verify(mockEntityFinder, never()).show();
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testOnMoveCanUpdateFailed(){
		String error = "An error";
		AsyncMockStubber.callFailureWith(new Throwable(error)).when(mockSynapseClient).updateEntity(any(Entity.class), any(AsyncCallback.class));
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		entityBundle.setEntity(new Folder());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.MOVE_ENTITY);
		verify(mockEntityFinder).configure(anyBoolean(), any(SelectedHandler.class));
		verify(mockEntityFinder).show();
		verify(mockEntityFinder).hide();
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
		verify(mockView).showErrorMessage(error);
	}
	
	@Test
	public void testOnMoveCanUpdateSuccess(){
		AsyncMockStubber.callSuccessWith(new Folder()).when(mockSynapseClient).updateEntity(any(Entity.class), any(AsyncCallback.class));
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		entityBundle.setEntity(new Folder());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.MOVE_ENTITY);
		verify(mockEntityFinder).configure(anyBoolean(), any(SelectedHandler.class));
		verify(mockEntityFinder).show();
		verify(mockEntityFinder).hide();
		verify(mockSynapseClient).updateEntity(any(Entity.class), any(AsyncCallback.class));
		verify(mockEntityUpdatedHandler).onPersistSuccess(any(EntityUpdatedEvent.class));
		verify(mockView, never()).showErrorMessage(anyString());
	}
	
	@Test
	public void testCreateLinkBadRequest(){
		AsyncMockStubber.callFailureWith(new BadRequestException("bad")).when(mockSynapseClient).createEntity(any(Entity.class), any(AsyncCallback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.createLink("syn9876");
		verify(mockView).showErrorMessage(DisplayConstants.ERROR_CANT_MOVE_HERE);
	}
	
	@Test
	public void testCreateLinkNotFound(){
		AsyncMockStubber.callFailureWith(new NotFoundException("not found")).when(mockSynapseClient).createEntity(any(Entity.class), any(AsyncCallback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.createLink("syn9876");
		verify(mockView).showErrorMessage(DisplayConstants.ERROR_NOT_FOUND);
	}
	
	@Test
	public void testCreateLinkUnauthorizedException(){
		AsyncMockStubber.callFailureWith(new UnauthorizedException("no way")).when(mockSynapseClient).createEntity(any(Entity.class), any(AsyncCallback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.createLink("syn9876");
		verify(mockView).showErrorMessage(DisplayConstants.ERROR_NOT_AUTHORIZED);
	}
	
	@Test
	public void testCreateLinkUnknownException(){
		String error = "some error";
		AsyncMockStubber.callFailureWith(new Throwable(error)).when(mockSynapseClient).createEntity(any(Entity.class), any(AsyncCallback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.createLink("syn9876");
		verify(mockView).showErrorMessage(error);
	}
	
	@Test
	public void testCreateLink(){
		entityBundle.getEntity().setId("syn123");
		ArgumentCaptor<Entity> argument = ArgumentCaptor.forClass(Entity.class);
		AsyncMockStubber.callSuccessWith(new Link()).when(mockSynapseClient).createEntity(argument.capture(), any(AsyncCallback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		String target = "syn9876";
		controller.createLink(target);
		verify(mockView, never()).showErrorMessage(anyString());
		verify(mockView).showInfo(DisplayConstants.TEXT_LINK_SAVED, DisplayConstants.TEXT_LINK_SAVED);
		Entity capture = argument.getValue();
		assertNotNull(capture);
		assertTrue(capture instanceof Link);
		Link link = (Link) capture;
		assertEquals(target, link.getParentId());
		assertEquals(entityBundle.getEntity().getName(), link.getName());
		Reference ref = link.getLinksTo();
		assertNotNull(ref);
		assertEquals(entityBundle.getEntity().getId(), ref.getTargetId());
	}
	
	@Test
	public void testOnLinkNoUpdate(){
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.CREATE_LINK);
		verify(mockEntityFinder, never()).configure(anyBoolean(), any(SelectedHandler.class));
		verify(mockEntityFinder, never()).show();
		verify(mockView, never()).showInfo(anyString(), anyString());
	}
	
	@Test
	public void testOnLink(){
		AsyncMockStubber.callSuccessWith(new Link()).when(mockSynapseClient).createEntity(any(Entity.class), any(AsyncCallback.class));
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.CREATE_LINK);
		verify(mockEntityFinder).configure(anyBoolean(), any(SelectedHandler.class));
		verify(mockEntityFinder).show();
		verify(mockView).showInfo(DisplayConstants.TEXT_LINK_SAVED, DisplayConstants.TEXT_LINK_SAVED);
	}
	
	@Test
	public void testOnSubmitNoUpdate(){
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.SUBMIT_TO_CHALLENGE);
		verify(mockSubmitter, never()).configure(any(Entity.class), any(Set.class));
	}
	
	@Test
	public void testOnSubmitWithUdate(){
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle,wikiPageId, mockEntityUpdatedHandler);
		controller.onAction(Action.SUBMIT_TO_CHALLENGE);
		verify(mockSubmitter).configure(any(Entity.class), any(Set.class));
	}
	
	@Test
	public void testOnUploadNewFileNoUpload(){
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkUploadToEntity(any(EntityBundle.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.UPLOAD_NEW_FILE);
		verify(mockUploader, never()).show();
	}
	
	@Test
	public void testOnUploadNewFileWithUpload(){
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUploadToEntity(any(EntityBundle.class), any(Callback.class));
		controller.configure(mockActionMenu, entityBundle,wikiPageId, mockEntityUpdatedHandler);
		controller.onAction(Action.UPLOAD_NEW_FILE);
		verify(mockUploader).show();
	}

	@Test
	public void testToolsButtonVisibilityForAnonymous() {
		when(mockAuthenticationController.isLoggedIn()).thenReturn(false);
		controller.configure(mockActionMenu, entityBundle, wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setToolsButtonVisible(false);
	}

	@Test
	public void testToolsButtonVisibilityForLogin() {
		controller.configure(mockActionMenu, entityBundle, wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setToolsButtonVisible(true);
	}
	
	@Test
	public void testConfigureNoWikiSubpageProject(){
		entityBundle.setEntity(new Project());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.ADD_WIKI_SUBPAGE, true);
		verify(mockActionMenu).setActionVisible(Action.ADD_WIKI_SUBPAGE, true);
		verify(mockActionMenu).addActionListener(Action.ADD_WIKI_SUBPAGE, controller);
	}
	
	@Test
	public void testConfigureWikiSubpageFolder(){
		entityBundle.setEntity(new Folder());
		controller.configure(mockActionMenu, entityBundle,wikiPageId, mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.ADD_WIKI_SUBPAGE, false);
		verify(mockActionMenu).setActionVisible(Action.ADD_WIKI_SUBPAGE, false);
	}
	
	@Test
	public void testConfigureWikiSubpageTable(){
		entityBundle.setEntity(new TableEntity());
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		verify(mockActionMenu).setActionEnabled(Action.ADD_WIKI_SUBPAGE, false);
		verify(mockActionMenu).setActionVisible(Action.ADD_WIKI_SUBPAGE, false);
	}
	
	@Test
	public void testOnAddWikiSubpageNoUpdate(){
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		entityBundle.setRootWikiId(null);
		controller.configure(mockActionMenu, entityBundle, wikiPageId,mockEntityUpdatedHandler);
		controller.onAction(Action.ADD_WIKI_SUBPAGE);
		verify(mockSynapseClient, never()).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testOnRootAddWikiSubpageCanUpdate(){
		//Edge case.  User attempts to add a subpage on a project that does not yet have a wiki.  Verify a root page is created (and page refreshed)...
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		AsyncMockStubber.callSuccessWith(new WikiPage()).when(mockSynapseClient).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		entityBundle.setRootWikiId(null);
		controller.configure(mockActionMenu, entityBundle, null,mockEntityUpdatedHandler);
		controller.onAction(Action.ADD_WIKI_SUBPAGE);
		verify(mockSynapseClient).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		verify(mockEntityUpdatedHandler).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testOnAddWikiSubpageCanUpdate(){
		//Set up so that we are on the root wiki page, and we run the add subpage command.
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkUpdateEntity(any(EntityBundle.class), any(Callback.class));
		AsyncMockStubber.callSuccessWith(new WikiPage()).when(mockSynapseClient).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		entityBundle.setRootWikiId("123");
		controller.configure(mockActionMenu, entityBundle, "123",mockEntityUpdatedHandler);
		controller.onAction(Action.ADD_WIKI_SUBPAGE);
		//verify that it has not yet created the wiki page
		verify(mockSynapseClient, never()).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
		//it prompts the user for a wiki page name
		ArgumentCaptor<PromptCallback> callbackCaptor = ArgumentCaptor.forClass(PromptCallback.class);
		verify(mockView).showPromptDialog(anyString(), callbackCaptor.capture());
		PromptCallback capturedCallback = callbackCaptor.getValue();
		//if called back with an undefined value, a wiki page is still not created
		capturedCallback.callback("");
		verify(mockSynapseClient, never()).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
		
		capturedCallback.callback(null);
		verify(mockSynapseClient, never()).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		verify(mockEntityUpdatedHandler, never()).onPersistSuccess(any(EntityUpdatedEvent.class));
		
		capturedCallback.callback("a valid name");
		verify(mockSynapseClient).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		verify(mockView).showInfo(anyString(), anyString());
		verify(mockEntityUpdatedHandler).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testCreateWikiPageFailure(){
		//Set up so that we are on the root wiki page, and we run the add subpage command.
		String error = "goodnight";
		AsyncMockStubber.callFailureWith(new Exception(error)).when(mockSynapseClient).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		entityBundle.setRootWikiId("123");
		controller.configure(mockActionMenu, entityBundle, "123",mockEntityUpdatedHandler);
		controller.createWikiPage("foo");
		
		verify(mockSynapseClient).createV2WikiPageWithV1(anyString(), anyString(), any(WikiPage.class),any(AsyncCallback.class));
		verify(mockView).showErrorMessage(anyString());
	}
}
