package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BackupRestoreStatus;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestoreFile;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Helper class to make HttpServlet request.
 * 
 * Users can use all the static methods if they like.
 * 
 * Alternatively the instance methods add a level of convenience by managing the
 * deletion of entities created during testing and also the user account(s) to
 * be used during testing.
 * 
 * @author jmhill
 * 
 */
public class ServletTestHelper {

	private static final Log log = LogFactory.getLog(ServletTestHelper.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final String DEFAULT_USERNAME = TestUserDAO.TEST_USER_NAME;

	@Autowired
	// Used for cleanup
	private GenericEntityController entityController;
	@Autowired
	private UserManager userManager;

	private HttpServlet dispatchServlet = null;
	private UserInfo testUser = null;
	private List<String> toDelete = null;
	private String username = null;

	/**
	 * Setup the servlet, default test user, and entity list for test cleanup.
	 * 
	 * Create a Spring MVC DispatcherServlet so that we can test our URL
	 * mapping, request format, response format, and response status code.
	 * 
	 * @throws Exception
	 */
	public void setUp() throws Exception {
		MockServletConfig servletConfig = new MockServletConfig("repository");
		servletConfig.addInitParameter("contextConfigLocation",
				"classpath:test-context.xml");
		dispatchServlet = new DispatcherServlet();
		dispatchServlet.init(servletConfig);

		assertNotNull(entityController);
		toDelete = new ArrayList<String>();

		this.setTestUser(DEFAULT_USERNAME);
	}

	/**
	 * Change the test user
	 * 
	 * @param username
	 * @throws Exception
	 */
	public void setTestUser(String username) throws Exception {
		// Make sure we have a valid user.
		this.username = username;
		testUser = userManager.getUserInfo(this.username);
		UserInfo.validateUserInfo(testUser);
	}

	/**
	 * Cleanup the created entities and destroy the servlet
	 * 
	 * @throws Exception
	 */
	public void tearDown() throws Exception {
		if (entityController != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					entityController.deleteEntity(username, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
		// If we do not destroy the servlet we get connection leaks.
		if (dispatchServlet != null) {
			dispatchServlet.destroy();
		}
	}

	/**
	 * @param <T>
	 * @param entity
	 * @param extraParams
	 * @return the entity
	 * @throws Exception
	 */
	public <T extends Base> T createEntity(T entity,
			Map<String, String> extraParams) throws Exception {
		T returnedEntity = ServletTestHelper.createEntity(dispatchServlet,
				entity, username, extraParams);
		toDelete.add(returnedEntity.getId());
		return returnedEntity;
	}

	/**
	 * @param <T>
	 * @param clazz
	 * @param id
	 * @param extraParams
	 * @return the entity
	 * @throws Exception
	 */
	public <T extends Base> T getEntity(Class<? extends T> clazz, String id,
			Map<String, String> extraParams) throws Exception {
		return ServletTestHelper.getEntity(dispatchServlet, clazz, id,
				username, extraParams);
	}

	/**
	 * @param <T>
	 * @param entity
	 * @param extraParams
	 * @return
	 * @throws Exception
	 */
	public <T extends Base> T updateEntity(T entity,
			Map<String, String> extraParams) throws Exception {
		return ServletTestHelper.updateEntity(dispatchServlet, entity,
				username, extraParams);
	}

	/**
	 * @param <T>
	 * @param clazz
	 * @param id
	 * @param extraParams
	 * @throws Exception
	 */
	public <T extends Base> void deleteEntity(Class<? extends T> clazz, String id,
			Map<String, String> extraParams) throws Exception {
		ServletTestHelper.deleteEntity(dispatchServlet, clazz, id,
				username, extraParams);
	}
	
	/**
	 * Create the passed entity by making a request to the passed servlet.
	 * 
	 * @param dispatchServlet
	 * @param entity
	 * @param userId
	 * @param <T>
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * 
	 */
	public static <T extends Base> T createEntity(HttpServlet dispatchServlet,
			T entity, String userId) throws ServletException, IOException {
		return ServletTestHelper.createEntity(dispatchServlet, entity, userId,
				null);
	}

	/**
	 * Create the passed entity by making a request to the passed servlet.
	 * 
	 * @param dispatchServlet
	 * @param entity
	 * @param userId
	 * @param extraParams
	 * @param <T>
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * 
	 */
	public static <T extends Base> T createEntity(HttpServlet dispatchServlet,
			T entity, String userId, Map<String, String> extraParams)
			throws ServletException, IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(entity.getClass());
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix());
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		if (null != extraParams) {
			for (Map.Entry<String, String> param : extraParams.entrySet()) {
				request.setParameter(param.getKey(), param.getValue());
			}
		}
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, entity);
		String body = out.toString();
		request.setContent(body.getBytes("UTF-8"));
		log.debug("About to send: " + body);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		@SuppressWarnings("unchecked")
		T returnedEntity = (T) objectMapper.readValue(response
				.getContentAsString(), entity.getClass());
		return returnedEntity;
	}

	/**
	 * Get an entity using an id.
	 * 
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @param <T>
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * 
	 */
	public static <T extends Base> T getEntity(HttpServlet dispatchServlet,
			Class<? extends T> clazz, String id, String userId)
			throws ServletException, IOException {
		return ServletTestHelper.getEntity(dispatchServlet, clazz, id, userId,
				null);
	}

	/**
	 * Get an entity using an id.
	 * 
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @param extraParams
	 * @param <T>
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * 
	 */
	public static <T extends Base> T getEntity(HttpServlet dispatchServlet,
			Class<? extends T> clazz, String id, String userId,
			Map<String, String> extraParams) throws ServletException,
			IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		if (null != extraParams) {
			for (Map.Entry<String, String> param : extraParams.entrySet()) {
				request.setParameter(param.getKey(), param.getValue());
			}
		}
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (T) objectMapper.readValue(response.getContentAsString(), clazz);
	}

	/**
	 * Get an entity using an id.
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 * @throws Exception
	 */
	public static <T extends Versionable> T getEntityForVersion(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long versionNumber, String userId) throws ServletException,
			IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id
				+ UrlHelpers.VERSION + "/" + versionNumber);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (T) objectMapper.readValue(response.getContentAsString(), clazz);
	}

	/**
	 * Get the annotations for an entity
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> Annotations getEntityAnnotations(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			String userId) throws ServletException, IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id
				+ UrlHelpers.ANNOTATIONS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return objectMapper.readValue(response.getContentAsString(),
				Annotations.class);
	}

	/**
	 * Get the annotations for an entity
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static <T extends Base> List<EntityHeader> getEntityPath(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			String userId) throws ServletException, IOException, JSONException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id + UrlHelpers.PATH);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (List<EntityHeader>) createEntityHeaderList(response
				.getContentAsString());
	}

	/**
	 * Get the annotations for a given version.
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param versionNumber
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> Annotations getEntityAnnotationsForVersion(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long versionNumber, String userId) throws ServletException,
			IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id
				+ UrlHelpers.VERSION + "/" + versionNumber
				+ UrlHelpers.ANNOTATIONS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return objectMapper.readValue(response.getContentAsString(),
				Annotations.class);
	}

	/**
	 * Update the annotations for an entity.
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param updatedAnnos
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> Annotations updateEntityAnnotations(
			HttpServlet dispatchServlet, Class<? extends T> clazz,
			Annotations updatedAnnos, String userId) throws ServletException,
			IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + updatedAnnos.getId()
				+ UrlHelpers.ANNOTATIONS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader(ServiceConstants.ETAG_HEADER, updatedAnnos.getEtag());
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, updatedAnnos);
		String body = out.toString();
		request.setContent(body.getBytes("UTF-8"));
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return objectMapper.readValue(response.getContentAsString(),
				Annotations.class);
	}

	/**
	 * Update an entity.
	 * 
	 * @param dispatchServlet
	 * @param entity
	 * @param userId
	 * @param <T>
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> T updateEntity(HttpServlet dispatchServlet,
			T entity, String userId) throws ServletException, IOException {
		return ServletTestHelper.updateEntity(dispatchServlet, entity, userId,
				null);
	}

	/**
	 * Update an entity.
	 * 
	 * @param dispatchServlet
	 * @param entity
	 * @param userId
	 * @param extraParams
	 * @param <T>
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Base> T updateEntity(HttpServlet dispatchServlet,
			T entity, String userId, Map<String, String> extraParams)
			throws ServletException, IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(entity.getClass());
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + entity.getId());
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		if (null != extraParams) {
			for (Map.Entry<String, String> param : extraParams.entrySet()) {
				request.setParameter(param.getKey(), param.getValue());
			}
		}
		request.addHeader(ServiceConstants.ETAG_HEADER, entity.getEtag());
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, entity);
		String body = out.toString();
		request.setContent(body.getBytes("UTF-8"));
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (T) objectMapper.readValue(response.getContentAsString(), entity
				.getClass());
	}

	/**
	 * Update an entity.
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param entity
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Versionable> T createNewVersion(
			HttpServlet dispatchServlet, T entity, String userId)
			throws ServletException, IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(entity.getClass());
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + entity.getId()
				+ UrlHelpers.VERSION);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader(ServiceConstants.ETAG_HEADER, entity.getEtag());
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, entity);
		String body = out.toString();
		request.setContent(body.getBytes("UTF-8"));
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (T) objectMapper.readValue(response.getContentAsString(), entity
				.getClass());
	}

	/**
	 * Get all objects of type.
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 * @throws JSONException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Base> PaginatedResults<T> getAllEntites(
			HttpServlet dispatchServlet, Class<? extends T> clazz,
			Integer offset, Integer limit, String sort, Boolean ascending,
			String userId) throws ServletException, IOException, JSONException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		if (offset != null) {
			request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM,
					offset.toString());
		}
		if (limit != null) {
			request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, limit
					.toString());
		}
		if (sort != null) {
			request.setParameter(ServiceConstants.SORT_BY_PARAM, sort);
		}
		if (ascending != null) {
			request.setParameter(ServiceConstants.ASCENDING_PARAM, ascending
					.toString());
		}
		request.setRequestURI(type.getUrlPrefix());
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return createPaginatedResultsFromJSON(response.getContentAsString(),
				clazz);
	}

	/**
	 * Get all objects of type.
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 * @throws JSONException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Versionable> PaginatedResults<T> getAllVersionsOfEntity(
			HttpServlet dispatchServlet, Class<? extends T> clazz,
			String entityId, Integer offset, Integer limit, String userId)
			throws ServletException, IOException, JSONException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		if (offset != null) {
			request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM,
					offset.toString());
		}
		if (limit != null) {
			request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, limit
					.toString());
		}
		request.setRequestURI(type.getUrlPrefix() + "/" + entityId
				+ UrlHelpers.VERSION);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return createPaginatedResultsFromJSON(response.getContentAsString(),
				clazz);
	}

	/**
	 * We need extra help to convert from JSON to a PaginatedResults
	 * 
	 * @param <T>
	 * @param json
	 * @param clazz
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public static <T extends Base> PaginatedResults<T> createPaginatedResultsFromJSON(
			String jsonString, Class<? extends T> clazz) throws JSONException,
			JsonParseException, JsonMappingException, IOException {
		JSONObject root = new JSONObject(jsonString);
		JSONArray array = root.getJSONArray("results");
		// We need to convert each object to the corret type
		List<T> list = new ArrayList<T>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject object = array.getJSONObject(i);
			T entity = objectMapper.readValue(object.toString(), clazz);
			list.add(entity);
		}
		PaginatedResults<T> result = new PaginatedResults<T>();
		result.setTotalNumberOfResults(root.getLong("totalNumberOfResults"));
		result.setResults(list);
		return result;
	}

	/**
	 * Convert from a JSONArray to a list.
	 * 
	 * @param jsonString
	 * @return
	 * @throws JSONException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static List<EntityHeader> createEntityHeaderList(String jsonString)
			throws JSONException, JsonParseException, JsonMappingException,
			IOException {
		JSONArray array = new JSONArray(jsonString);
		List<EntityHeader> list = new ArrayList<EntityHeader>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject object = array.getJSONObject(i);
			EntityHeader entity = objectMapper.readValue(object.toString(),
					EntityHeader.class);
			list.add(entity);
		}
		return list;
	}

	/**
	 * Get all objects of type.
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 * @throws JSONException
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Base> PaginatedResults<T> getAllChildrenEntites(
			HttpServlet dispatchServlet, ObjectType parentType,
			String parentId, Class<? extends T> childClass, Integer offset,
			Integer limit, String sort, Boolean ascending, String userId)
			throws ServletException, IOException, JSONException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(childClass);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		if (offset != null) {
			request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM,
					offset.toString());
		}
		if (limit != null) {
			request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, limit
					.toString());
		}
		if (sort != null) {
			request.setParameter(ServiceConstants.SORT_BY_PARAM, sort);
		}
		if (ascending != null) {
			request.setParameter(ServiceConstants.ASCENDING_PARAM, ascending
					.toString());
		}
		String url = parentType.getUrlPrefix() + "/" + parentId
				+ type.getUrlPrefix();
		request.setRequestURI(url);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return createPaginatedResultsFromJSON(response.getContentAsString(),
				childClass);
	}

	/**
	 * Delete an entity
	 * 
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @param <T>
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> void deleteEntity(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			String userId) throws ServletException, IOException {
		ServletTestHelper.deleteEntity(dispatchServlet, clazz, id, userId, null);
	}

	/**
	 * Delete an entity
	 * 
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @param extraParams
	 * @param <T>
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> void deleteEntity(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			String userId, Map<String, String> extraParams)
			throws ServletException, IOException {
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		if (null != extraParams) {
			for (Map.Entry<String, String> param : extraParams.entrySet()) {
				request.setParameter(param.getKey(), param.getValue());
			}
		}
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
	}

	/**
	 * Delete a specfic versoin of an entity
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 * @throws Exception
	 */
	public static <T extends Base> void deleteEntityVersion(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long versionNumber, String userId) throws ServletException,
			IOException {
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id
				+ UrlHelpers.VERSION + "/" + versionNumber);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
	}

	/**
	 * Get the schema
	 * 
	 * @param <T>
	 * @param requestUrl
	 * @param clazz
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public static <T extends Base> String getSchema(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String userId)
			throws Exception {
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + UrlHelpers.SCHEMA);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return response.getContentAsString();
	}

	/**
	 * create the Access Control List (ACL) for an entity.
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> AccessControlList createEntityACL(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			AccessControlList entityACL, String userId)
			throws ServletException, IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id + UrlHelpers.ACL);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, entityACL);
		String body = out.toString();
		request.setContent(body.getBytes("UTF-8"));
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new IllegalArgumentException(response.getErrorMessage() + " "
					+ response.getStatus() + " for\n" + body);
		}
		return objectMapper.readValue(response.getContentAsString(),
				AccessControlList.class);
	}

	/**
	 * Get the Access Control List (ACL) for an entity.
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws ACLInheritanceException
	 */
	public static <T extends Base> AccessControlList getEntityACL(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			String userId) throws ServletException, IOException,
			ACLInheritanceException {
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id + UrlHelpers.ACL);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() == HttpStatus.NOT_FOUND.value()) {
			// This occurs when we try to access an ACL from an entity that
			// inherits its permission.
			throw new ACLInheritanceException(response.getErrorMessage());
		}
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return objectMapper.readValue(response.getContentAsString(),
				AccessControlList.class);
	}

	/**
	 * Update an entity ACL
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param entityACL
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> AccessControlList updateEntityAcl(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			AccessControlList entityACL, String userId)
			throws ServletException, IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id + UrlHelpers.ACL);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader(ServiceConstants.ETAG_HEADER, entityACL.getEtag());
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, entityACL);
		String body = out.toString();
		request.setContent(body.getBytes("UTF-8"));
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return objectMapper.readValue(response.getContentAsString(),
				AccessControlList.class);
	}

	/**
	 * Delete an entity ACL
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param entityACL
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> void deleteEntityACL(
			HttpServlet dispatchServlet, Class<? extends T> clazz,
			String resourceId, String userId) throws ServletException,
			IOException {
		if (dispatchServlet == null)
			throw new IllegalArgumentException("Servlet cannot be null");
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + resourceId
				+ UrlHelpers.ACL);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
	}

	/**
	 * Get the principals
	 * 
	 * @param dispatchServlet
	 * @param userId
	 * @return the principals
	 * @throws ServletException
	 * @throws IOException
	 */
	public static Collection<Map<String, Object>> getUsers(
			HttpServlet dispatchServlet, String userId)
			throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.USER);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		@SuppressWarnings("unchecked")
		Collection<Map<String, Object>> us = objectMapper.readValue(response
				.getContentAsString(), Collection.class);
		return us;
	}

	/**
	 * Get the principals
	 * 
	 * @param dispatchServlet
	 * @param userId
	 * @return the principals
	 * @throws ServletException
	 * @throws IOException
	 */
	public static Collection<Map<String, Object>> getGroups(
			HttpServlet dispatchServlet, String userId)
			throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.USERGROUP);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		@SuppressWarnings("unchecked")
		Collection<Map<String, Object>> us = objectMapper.readValue(response
				.getContentAsString(), Collection.class);
		return us;
	}

	/**
	 * calls 'hasAccess'
	 * 
	 * @param <T>
	 * @param dispatchServlet
	 * @param clazz
	 * @param id
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Base> BooleanResult hasAccess(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			String userId, String accessType) throws ServletException,
			IOException {
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id
				+ UrlHelpers.ACCESS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter(UrlHelpers.ACCESS_TYPE_PARAM, accessType);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (BooleanResult) objectMapper.readValue(response
				.getContentAsString(), BooleanResult.class);
	}

	/**
	 * Start the a system backup.
	 * 
	 * @param dispatchServlet
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static BackupRestoreStatus startBackup(HttpServlet dispatchServlet,
			String userId) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.START_BACKUP_DAEMON);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (BackupRestoreStatus) objectMapper.readValue(response
				.getContentAsString(), BackupRestoreStatus.class);
	}

	/**
	 * Get the status of a backup/restore daemon
	 * 
	 * @param dispatchServlet
	 * @param userId
	 * @param id
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static BackupRestoreStatus getStatus(HttpServlet dispatchServlet,
			String userId, String id) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.GET_DAEMON_STATUS_PREFIX + "/" + id);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (BackupRestoreStatus) objectMapper.readValue(response
				.getContentAsString(), BackupRestoreStatus.class);
	}

	/**
	 * Start a system restore daemon
	 * 
	 * @param dispatchServlet
	 * @param uesrId
	 * @param fileName
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static BackupRestoreStatus startRestore(HttpServlet dispatchServlet,
			String uesrId, RestoreFile file) throws ServletException,
			IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.START_RESTORE_DAEMON);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, uesrId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, file);
		String body = out.toString();
		request.setContent(body.getBytes("UTF-8"));
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (BackupRestoreStatus) objectMapper.readValue(response
				.getContentAsString(), BackupRestoreStatus.class);
	}

	public static void terminateDaemon(HttpServlet dispatchServlet,
			String userId, String id) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.TERMINATE_DAEMON_PREFIX + "/" + id);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
	}

	public static EntityHeader getEntityType(HttpServlet dispatchServlet,
			String id, String userId) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + id + UrlHelpers.TYPE);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (EntityHeader) objectMapper.readValue(response
				.getContentAsString(), EntityHeader.class);
	}

	/**
	 * Get the PermissionInfo for a given entity.
	 * 
	 * @param dispatchServlet
	 * @param id
	 * @param type
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public static <T extends Nodeable> EntityHeader getEntityBenefactor(
			HttpServlet dispatchServlet, String id, Class<? extends T> clazz,
			String userId) throws ServletException, IOException {
		ObjectType type = ObjectType.getNodeTypeForClass(clazz);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(type.getUrlPrefix() + "/" + id
				+ UrlHelpers.BENEFACTOR);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatchServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new IllegalArgumentException(response.getErrorMessage());
		}
		return (EntityHeader) objectMapper.readValue(response
				.getContentAsString(), EntityHeader.class);
	}

}