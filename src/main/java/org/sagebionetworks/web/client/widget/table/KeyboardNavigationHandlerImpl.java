package org.sagebionetworks.web.client.widget.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;




import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ValueBoxBase;

/**
 * This implementation keeps track of the address of each widget and listens to key down events.
 * When navigation key event occurs, the address of the source source widget is used to calculate
 * the new focus widget.
 * 
 * @author jhill
 *
 */
public class KeyboardNavigationHandlerImpl implements KeyboardNavigationHandler {

	ArrayList<RowOfWidgets> rows;
	Map<IsWidget, Address> cellAddressMap;
	int columnCount;
	boolean needsRecalcualteAdressess;

	public KeyboardNavigationHandlerImpl() {
		rows = new ArrayList<RowOfWidgets>();
		cellAddressMap = new HashMap<IsWidget, Address>();
	}

	@Override
	public void bindRow(RowOfWidgets row) {
		// Add this row.
		rows.add(row);
		// Listen to each row
		columnCount = 0;
		for (int i=0; i<row.getWidgetCount(); i++) {
			IsWidget cell = row.getWidget(i);
			bindEditor(cell);
			columnCount++;
		}
		needsRecalcualteAdressess = true;
	}

	private void bindEditor(final IsWidget editor) {
		// Can only listen to widget that implement HasKeyDownHandlers
		if(editor instanceof HasKeyDownHandlers){
			HasKeyDownHandlers keyDownCell = (HasKeyDownHandlers) editor;
			keyDownCell.addKeyDownHandler(new KeyDownHandler() {
				@Override
				public void onKeyDown(KeyDownEvent event) {
					editorKeyPressed(editor, event);
				}
			});
		}
	}

	@Override
	public void removeRow(RowOfWidgets row) {
		rows.remove(row);
		needsRecalcualteAdressess = true;
	}

	@Override
	public void removeAllRows() {
		this.rows.clear();
		this.needsRecalcualteAdressess = true;
	}
	
	/**
	 * Recalculate the address of each cell if needed.
	 * Recalculation is needed after rows are added or removed from the table.
	 */
	private void recalculateAddressesIfNeeded() {
		if(needsRecalcualteAdressess){
			// Start with a clean address map
			cellAddressMap.clear();
			// Walk cells and calculate their address.
			int rowIndex = 0;
			for (RowOfWidgets row : rows) {
				int columnIndex = 0;
				for (int i=0; i<row.getWidgetCount(); i++) {
					IsWidget widget = row.getWidget(i);
					Address address = cellAddressMap.get(widget);
					if (address == null) {
						address = new Address();
						cellAddressMap.put(widget, address);
					}
					address.columnIndex = columnIndex;
					address.rowIndex = rowIndex;
					columnIndex++;
				}
				rowIndex++;
			}
		}
		needsRecalcualteAdressess = false;
	}

	/**
	 * The address of a cell in the table.
	 *
	 */
	private static class Address {
		int columnIndex;
		int rowIndex;
		@Override
		public String toString() {
			return "Address [columnIndex=" + columnIndex + ", rowIndex="
					+ rowIndex + "]";
		}
	}

	/**
	 * Key pressed on this editor.
	 * 
	 * @param editor
	 * @param event
	 */
	private void editorKeyPressed(IsWidget editor, KeyDownEvent event) {
		// Check the addresses. If they are stale then they will need to be
		// recalculated before proceeding.
		recalculateAddressesIfNeeded();
		// Event Switch.
		switch (event.getNativeEvent().getKeyCode()) {
		case KeyCodes.KEY_ENTER:
			onDown(editor);
			break;
		case KeyCodes.KEY_DOWN:
			onDown(editor);
			break;
		case KeyCodes.KEY_UP:
			onUp(editor);
			break;
		case KeyCodes.KEY_LEFT:
			onLeft(editor);
			break;
		case KeyCodes.KEY_RIGHT:
			onRight(editor);
			break;
		case KeyCodes.KEY_TAB:
			onRight(editor);
			break;
		}
	}

	/**
	 * Give focus to the cell that is right of the passed cell.
	 * @param editor
	 */
	private void onRight(IsWidget editor) {
		Address current = cellAddressMap.get(editor);
		attemptSetFocus(current.columnIndex+1, current.rowIndex);
	}

	/**
	 * Give focus to the cell that is left of the passed cell.
	 * @param editor
	 */
	private void onLeft(IsWidget editor) {
		Address current = cellAddressMap.get(editor);
		attemptSetFocus(current.columnIndex-1, current.rowIndex);
	}

	/**
	 * Give focus to the cell that is bellow the passed cell.
	 * @param editor
	 */
	private void onDown(IsWidget editor) {
		Address current = cellAddressMap.get(editor);
		attemptSetFocus(current.columnIndex, current.rowIndex+1);
	}
	
	/**
	 * Give focus to the cell that is above the passed cell.
	 * @param editor
	 */
	private void onUp(IsWidget editor) {
		Address current = cellAddressMap.get(editor);
		attemptSetFocus(current.columnIndex, current.rowIndex-1);
	}

	/**
	 * Attempt to set the focus on the cell at the given address.
	 * If the requested address is out-of-range of the table
	 * @param columnIndex
	 * @param rowIndex
	 */
	public void attemptSetFocus(int columnIndex, int rowIndex) {
		if (rowIndex < rows.size() && rowIndex > -1) {
			RowOfWidgets row = rows.get(rowIndex);
			if (columnIndex < row.getWidgetCount() && columnIndex > -1) {
				IsWidget editor = row.getWidget(columnIndex);
				setFocus(editor);
			}
		}
	}
	

	private void setFocus(final IsWidget widget) {
		// Can only set focus on a focusable
		if(widget instanceof Focusable){
			final Focusable focusableWidget = (Focusable) widget;
			Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
				@Override 
				public void execute() {
					focusableWidget.setFocus(true);
					// Select all if we can.
					if(widget instanceof ValueBoxBase){
						((ValueBoxBase)widget).selectAll();
					}
				}
			});
		}
	}

}
