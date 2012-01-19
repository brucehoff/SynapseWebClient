package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * The DatabaseObject for references.
 * 
 * @author jmhill
 *
 */
public class DBOReference implements DatabaseObject<DBOReference> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("owner", COL_REFERENCE_OWNER_NODE, true),
		new FieldColumn("targetId", COL_REFERENCE_TARGET_NODE, true),
		new FieldColumn("targetRevision", COL_REFERENCE_TARGET_REVISION_NUMBER, true),
		new FieldColumn("groupName", COL_REFERENCE_GROUP_NAME, true),
		};

	@Override
	public TableMapping<DBOReference> getTableMapping() {
		return new TableMapping<DBOReference>(){

			@Override
			public DBOReference mapRow(ResultSet rs, int rowNum)throws SQLException {
				DBOReference ref = new DBOReference();
				ref.setOwner(rs.getLong(COL_REFERENCE_OWNER_NODE));
				ref.setTargetId(rs.getLong(COL_REFERENCE_TARGET_NODE));
				ref.setTargetRevision(rs.getLong(COL_REFERENCE_TARGET_REVISION_NUMBER));
				ref.setGroupName(rs.getString(COL_REFERENCE_GROUP_NAME));
				return ref;
			}

			@Override
			public String getTableName() {
				return TABLE_REFERENCE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_REFERENCE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOReference> getDBOClass() {
				return DBOReference.class;
			}};
	}
	
	private Long owner;
	private Long targetId;
	private Long targetRevision;
	private String groupName;

	public Long getOwner() {
		return owner;
	}
	public void setOwner(Long owner) {
		this.owner = owner;
	}
	public Long getTargetId() {
		return targetId;
	}
	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}
	public Long getTargetRevision() {
		return targetRevision;
	}
	public void setTargetRevision(Long targetRevision) {
		this.targetRevision = targetRevision;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result
				+ ((targetId == null) ? 0 : targetId.hashCode());
		result = prime * result
				+ ((targetRevision == null) ? 0 : targetRevision.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOReference other = (DBOReference) obj;
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (targetId == null) {
			if (other.targetId != null)
				return false;
		} else if (!targetId.equals(other.targetId))
			return false;
		if (targetRevision == null) {
			if (other.targetRevision != null)
				return false;
		} else if (!targetRevision.equals(other.targetRevision))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBOReference [owner=" + owner + ", targetId=" + targetId
				+ ", targetRevision=" + targetRevision + ", groupName="
				+ groupName + "]";
	}


}
