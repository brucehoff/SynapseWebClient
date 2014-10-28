package org.sagebionetworks.web.client.widget.entity.menu.v2;

import org.gwtbootstrap3.client.ui.AnchorListItem;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public class ActionMenuItemImpl extends AnchorListItem implements ActionMenuItem{

	@Override
	public void setPresenter(final Presenter presenter, final Action action) {
		this.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				presenter.onClicked(action);
			}
		});
	}
}
