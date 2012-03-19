package org.sagebionetworks.web.client.widget.entity;

import java.util.List;

import org.sagebionetworks.web.client.ClientLogger;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.widget.entity.row.EntityRowList;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.DomEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.form.TriggerField;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;

/**
 * As list editor.
 * 
 * @author John
 * 
 * @param <T>
 */
public class ListFieldEditor extends TriggerField<String> {

//	private ListMenu menu;
	Menu menu;
	ListEditorGrid<String> editorGrid;
	EntityRowList<String> rowList;
	IconsImageBundle iconBundle;
	ClientLogger log;

	public ListFieldEditor(EntityRowList<String> rowList, IconsImageBundle iconBundle, ClientLogger log) {
		setTriggerStyle("x-form-date-trigger");
		this.rowList = rowList;
		this.iconBundle = iconBundle;
		this.log = log;
		this.setEditable(false);
	}

	
	// Apply the list from the picker
	public void applyListFromPicker() {
		// stop editing if currently editing.
		editorGrid.stopEditing();
		List<String> value = editorGrid.getList();
		setValue(value.toString());
		rowList.setValue(value);
	}

	protected void expand() {
		List<String> value = getList();
		editorGrid.setList(value, new TextField<String>());

		// handle case when down arrow is opening menu
		DeferredCommand.addCommand(new Command() {
			public void execute() {
				menu.show(el().dom, "tl-bl?");
			}
		});
	}

	@Override
	protected void onKeyDown(FieldEvent fe) {
		super.onKeyDown(fe);
		if (fe.getKeyCode() == KeyCodes.KEY_DOWN) {
			fe.stopEvent();
			if (menu == null || !menu.isAttached()) {
				expand();
			}
		}
	}

	@Override
	protected void onRender(Element target, int index) {
		super.onRender(target, index);
		// the menu shows the ListPicker
		menu = new Menu();
		// the editor will handle the scrolling
		menu.setEnableScrolling(false);
		menu.setFocusOnShow(true);
		menu.setAutoHeight(true);

		
		// The picker is added to the menu
		editorGrid = new ListEditorGrid<String>(iconBundle, log);
		menu.add(editorGrid);
		
		// On hide, we apply
		menu.addListener(Events.Hide, new Listener<ComponentEvent>() {
			public void handleEvent(ComponentEvent be) {

				applyListFromPicker();
				focus();
			}
		});


		new KeyNav<FieldEvent>(this) {

			@Override
			public void onEsc(FieldEvent fe) {
				if (menu != null && menu.isAttached()) {
					menu.hide();
				}
			}
		};

		if (GXT.isAriaEnabled()) {
			getInputEl().dom.setAttribute("title", "Aria text");
		}
	}

	@Override
	protected void onTriggerClick(ComponentEvent ce) {
		super.onTriggerClick(ce);
		expand();
	}

	@Override
	protected boolean validateBlur(DomEvent e, Element target) {
		return menu == null || (menu != null && !menu.isVisible());
	}
	
	public void setList(List<String> list){
		this.rowList.setValue(list);
		if(list == null){
			setValue(null);
		}else{
			// the display for this list is the list string
			setValue(list.toString());
		}
	}
	
	public List<String> getList(){
		return this.rowList.getValue();
	}

}