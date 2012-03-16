package org.sagebionetworks.web.client.widget.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AutoGenFactory;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ExampleEntity;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.ClientLogger;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.EntitySchemaCache;
import org.sagebionetworks.web.client.model.EntityBundle;
import org.sagebionetworks.web.client.widget.entity.row.EntityFormModel;
import org.sagebionetworks.web.client.widget.entity.row.EntityRow;
import org.sagebionetworks.web.client.widget.entity.row.EntityRowFactory;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.user.client.Element;
import com.google.inject.Inject;

/**
 * A temp presenter
 * @author jmhill
 *
 */
public class TempPropertyPresenter extends LayoutContainer {

	EntitySchemaCache cache;
	AdapterFactory factory;
	EntityPropertyGrid view;
	EntityBundle bundle;
	List<EntityRow<?>> rows;
	AutoGenFactory entityFactory;
	VerticalPanel vp;
	FormFieldFactory formFactory;
	ClientLogger log;
	
	@Inject
	public TempPropertyPresenter(EntitySchemaCache cache,
			AdapterFactory factory, EntityPropertyGrid view, FormFieldFactory formFactory, ClientLogger log) {
		super();
		this.cache = cache;
		this.factory = factory;
		this.view = view;
		this.entityFactory = new AutoGenFactory();
		this.formFactory = formFactory;
		this.log = log;
		// test the log
		log.debug("Testing a debug message");
		log.info("Testing an info message");
		log.error("Testing an error message");
		log.error("Testing an error with a basic stack info", IllegalArgumentException.class.getName(), TempPropertyPresenter.class.getName(), "<init>", 69);
		
	}
	
	@Override
	protected void onRender(Element parent, int index) {
		super.onRender(parent, index);
		this.clearState();
		// Create the grid
		vp = new VerticalPanel();
		vp.setSpacing(10);
		Button editButton = new Button();
		editButton.setText("Edit");
		editButton.addSelectionListener(new SelectionListener<ButtonEvent>() {				
	    	@Override
	    	public void componentSelected(ButtonEvent ce) {
	    	    Entity entity = bundle.getEntity();
	    		final Dialog window = new Dialog();
	    		window.setMaximizable(false);
	    	    window.setSize(600, 700);
	    	    window.setPlain(true);  
	    	    window.setModal(true);  
	    	    window.setBlinkModal(true);  
	    	    window.setHeading("Edit: "+entity.getId());  
	    	    window.setLayout(new FitLayout());
	    	    // We want okay to say save
	    	    window.okText = "Save";
	    	    window.setButtons(Dialog.OKCANCEL);
	    	    window.setHideOnButtonClick(true);
	    	    
	    	    // Create a new Adapter to capture the editor's changes
	    	    final JSONObjectAdapter newAdapter = factory.createNew();
	    	    EntityPropertyForm editor = new EntityPropertyForm(formFactory);
	    	    try {
	    	    	entity.writeToJSONObject(newAdapter);
	    	    	// We want to filter out all transient properties.
	    	    	ObjectSchema schema = cache.getSchemaEntity(entity);
	    	    	Set<String> filter = new HashSet<String>();
	    			ObjectSchema versionableScheam = cache.getEntitySchema(Versionable.EFFECTIVE_SCHEMA, Versionable.class);
	    			filter.addAll(versionableScheam.getProperties().keySet());
	    	    	// Filter transient fields
	    	    	EntityRowFactory.addTransientToFilter(schema, filter);
	    	    	// Filter objects
	    	    	EntityRowFactory.addObjectTypeToFilter(schema, filter);
	    	    	EntityFormModel model = EntityRowFactory.createEntityRowList(newAdapter, schema, null, filter);
	    	    	editor.setList(model);
				} catch (JSONObjectAdapterException e) {
					throw new RuntimeException(e);
				}
	    	    window.add(editor, new FitData(0));
	    	    // List for the button selection
	    	    Button saveButton = window.getButtonById(Dialog.OK);
	    	    saveButton.addSelectionListener(new SelectionListener<ButtonEvent>() {
					@Override
					public void componentSelected(ButtonEvent ce) {
						// first create a new entity
						JSONEntity newEntity;
						try {
							newEntity = entityFactory.newInstance(bundle.getEntity().getClass().getName());
							newEntity.initializeFromJSONObject(newAdapter);
							EntityBundle newBundel = new EntityBundle((Entity) newEntity, null, null, null, null);
							setEntity(newBundel);
						} catch (JSONObjectAdapterException e) {
							DisplayUtils.showErrorMessage("Failed to apply the changes. Error: "+e.getMessage());
						}
					}
	    	    });
	    	    // show the window
	    	    window.show();
	    	 
	    	}
	    });
		vp.add(editButton);
		ContentPanel cp = new ContentPanel();
		cp.setLayout(new FitLayout());
		cp.setHeight(10);
		cp.setBorders(false);
		cp.setBodyBorder(false);
		cp.setHeaderVisible(false);
		vp.add(cp);
		vp.add(this.view);
		this.add(vp);
	}
	


	public void setEntity(EntityBundle bundle){
		this.bundle = bundle;
		Entity entity = bundle.getEntity();
		// Create an adapter
		JSONObjectAdapter adapter = factory.createNew();
		try {
			entity.writeToJSONObject(adapter);
			ObjectSchema schema = cache.getSchemaEntity(entity);
			// Get the list of rows
			// Filter out all versionable properties
			ObjectSchema versionableScheam = cache.getEntitySchema(Versionable.EFFECTIVE_SCHEMA, Versionable.class);
			Set<String> filter = new HashSet<String>();
			// filter out all properties from versionable
			filter.addAll(versionableScheam.getProperties().keySet());
			// Filter all transient properties
			EntityRowFactory.addTransientToFilter(schema, filter);
			// Add all objects to the filter
			EntityRowFactory.addObjectTypeToFilter(schema, filter);
			rows = EntityRowFactory.createEntityRowListForProperties(adapter, schema, filter);
			// Pass the rows to the two views
			view.setRows(rows);
			this.layout(true);
//			view.repaint();
			// Create the list of fields
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * Create a sample entity
	 * 
	 * @return
	 */
	public static ExampleEntity createSample() {
		ExampleEntity example = new ExampleEntity();
		example.setId("12345");
		example.setName("My name is coolness");
		example.setConcept("Concept value");
		example.setSingleDate(new Date(System.currentTimeMillis()));
		example.setEntityType(example.getClass().getName());
		example.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque risus sapien, elementum a elementum adipiscing, laoreet condimentum odio. Vivamus pretium purus ac tellus tempor lobortis. Suspendisse nec nibh sit amet ligula consectetur tincidunt in vitae mi. Donec in pretium odio. Quisque lacus nunc, condimentum tincidunt placerat at, convallis in leo. Aliquam erat volutpat. Suspendisse sodales nisl sit amet quam eleifend fermentum. Vivamus interdum, arcu at tempor gravida, dolor neque tempus mi, ac viverra risus quam sit amet lacus. Fusce eget purus magna, nec lacinia neque. Pellentesque pretium metus ac velit mattis sed tempus sem adipiscing. Vivamus molestie lorem in dui viverra interdum. Sed nec elementum diam. ");
		example.setSingleString("This could be a very long string that wraps way off the screen. When that happens we want a portion of it to be shown on the screen, but the rest on tool tips.");
		example.setSingleDouble(123.45);
		example.setSingleInteger(42l);
		example.setStringList(new ArrayList<String>());
		example.getStringList().add("one");
		example.getStringList().add("two");
		example.getStringList().add("three");
		
		return example;
	}

	public void initializeWithTestData() {
		// Create an example entity
		ExampleEntity example = createSample();
		EntityBundle bundle = new EntityBundle(example, null, null, null, null);
		this.setEntity(bundle);
	}
	
}
