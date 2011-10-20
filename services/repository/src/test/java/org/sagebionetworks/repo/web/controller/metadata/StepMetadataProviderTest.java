/**
 * 
 */
package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.EnvironmentDescriptor;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Step;

/**
 * @author deflaux
 *
 */
public class StepMetadataProviderTest {

	private StepMetadataProvider provider = new StepMetadataProvider();
	
	/**
	 * Test method for {@link org.sagebionetworks.repo.web.controller.metadata.StepMetadataProvider#validateEntity(org.sagebionetworks.repo.model.Step, org.sagebionetworks.repo.web.controller.metadata.EntityEvent)}.
	 * @throws Exception 
	 */
	@Test(expected=InvalidModelException.class)
	public void testValidateEntityStartDate() throws Exception {
		Step step = new Step();

		provider.validateEntity(step, new EntityEvent(EventType.CREATE, null, null));
		assertNotNull(step.getStartDate());
		
		// its not valid to clear out the startDate at a later time, but you can change it to a different time
		
		step.setStartDate(null);
		try {
			provider.validateEntity(step, new EntityEvent(EventType.UPDATE, null, null));
		} catch (Exception e) {
			assertEquals("startDate cannot changed to null", e.getMessage());
			throw e;
		}
		
	}
	
	/**
	 * Test method for {@link org.sagebionetworks.repo.web.controller.metadata.StepMetadataProvider#validateEntity(org.sagebionetworks.repo.model.Step, org.sagebionetworks.repo.web.controller.metadata.EntityEvent)}.
	 * @throws Exception 
	 */
	@Test
	public void testEnvironmentDescriptorTransformsEntity() throws Exception {
		EnvironmentDescriptor descriptor = new EnvironmentDescriptor();
		Set<EnvironmentDescriptor> descriptors = new HashSet<EnvironmentDescriptor>();
		Step step = new Step();

		descriptor.setType("OS");
		descriptor.setName("x86_64-apple-darwin9.8.0/x86_64");
		descriptor.setQuantifier("64-bit");
		descriptors.add(descriptor);
		
		descriptor = new EnvironmentDescriptor();
		descriptor.setType("application");
		descriptor.setName("R");
		descriptor.setQuantifier("2.13.0");
		descriptors.add(descriptor);

		descriptor = new EnvironmentDescriptor();
		descriptor.setType("rPackage");
		descriptor.setName("synapseClient");
		descriptor.setQuantifier("0.8-0");
		descriptors.add(descriptor);

		descriptor = new EnvironmentDescriptor();
		descriptor.setType("rPackage");
		descriptor.setName("Biobase");
		descriptor.setQuantifier("2.12.2");
		descriptors.add(descriptor);
	
		step.setEnvironmentDescriptors(descriptors);
		
		provider.validateEntity(step, new EntityEvent(EventType.CREATE, null, null));
		assertNotNull(step.getEnvironmentDescriptorsBlob());
		assertNull(step.getEnvironmentDescriptors());
		
		provider.addTypeSpecificMetadata(step, null, null, null);
		assertNull(step.getEnvironmentDescriptorsBlob());
		assertNotNull(step.getEnvironmentDescriptors());
		assertEquals(4, step.getEnvironmentDescriptors().size());

	}

}