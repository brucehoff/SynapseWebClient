package org.sagebionetworks.repo.model.jdo;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAccessControlList;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A utility for translating between the JDO and DTO.
 * 
 * @author jmhill
 * 
 */
public class AccessControlListUtil {

	/**
	 * Create a new JDO from a given DTO.
	 * 
	 * @param dto
	 * @param owner
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException 
	 * @throws NotFoundException 
	 */
	public static JDOAccessControlList createJdoFromDto(AccessControlList dto,	JDONode owner, UserGroupCache cache) throws DatastoreException, InvalidModelException, NotFoundException {
		JDOAccessControlList jdo = new JDOAccessControlList();
		updateJdoFromDto(jdo, dto, owner, cache);
		return jdo;
	}

	/**
	 * Update an existing JDO from a given DTO
	 * 
	 * @param jdo
	 * @param dto
	 * @throws DatastoreException
	 * @throws InvalidModelException 
	 * @throws NotFoundException 
	 */
	public static void updateJdoFromDto(JDOAccessControlList jdo, AccessControlList dto, JDONode owner, UserGroupCache cache) throws DatastoreException, InvalidModelException, NotFoundException {
		jdo.setId(owner.getId());
		jdo.setResource(owner);
		jdo.setCreatedBy(dto.getCreatedBy());
		jdo.setCreationDate(dto.getCreationDate());
		jdo.setModifiedBy(dto.getModifiedBy());
		jdo.setModifiedOn(dto.getModifiedOn() == null ? null : dto.getModifiedOn().getTime());
		Set<JDOResourceAccess> ras = new HashSet<JDOResourceAccess>();
		for (ResourceAccess raDto : dto.getResourceAccess()) {
			JDOResourceAccess raJdo = ResourceAccessUtil.createJdoFromDto(raDto, cache.getIdForUserGroupName(raDto.getGroupName()));
			ras.add(raJdo);
		}
		jdo.setResourceAccess(ras);
	}

	/**
	 * Create a DTO from a JDO.
	 * 
	 * @param jdo
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public static AccessControlList createDtoFromJdo(JDOAccessControlList jdo, String eTag, UserGroupCache cache)
			throws DatastoreException, NotFoundException {
		AccessControlList dto = new AccessControlList();
		updateDtoFromJdo(jdo, dto, eTag, cache);
		return dto;
	}

	/**
	 * Update a Dto from the JDO.
	 * 
	 * @param jdo
	 * @param dto
	 * @throws DatastoreException
	 * @throws NotFoundException 
	 */
	public static void updateDtoFromJdo(JDOAccessControlList jdo,
			AccessControlList dto, String eTag, UserGroupCache cache) throws DatastoreException, NotFoundException {
		dto.setCreatedBy(jdo.getCreatedBy());
		dto.setEtag(eTag);
		dto.setCreationDate(jdo.getCreationDate());
		dto.setModifiedBy(jdo.getModifiedBy());
		dto.setModifiedOn(new Date(jdo.getModifiedOn()));
		dto.setId(jdo.getId() == null ? null : KeyFactory.keyToString(jdo.getId()));
		dto.setEtag(jdo.getResource() == null ? null : KeyFactory.keyToString(jdo.getResource().geteTag()));
		dto.setId(jdo.getResource() == null ? null : KeyFactory.keyToString(jdo.getResource().getId()));
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		dto.setResourceAccess(ras);
		for (JDOResourceAccess raJdo : jdo.getResourceAccess()) {
			ResourceAccess ra = ResourceAccessUtil.createDtoFromJdo(raJdo, cache.getUserGroupNameForId(raJdo.getUserGroupId()));
			ras.add(ra);
		}
	}

}