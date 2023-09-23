package net.treimers.filemanager;

import java.io.File;

import javafx.scene.control.TreeCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;

/**
 * Instances of this class are used to render tree items.
 */
public class FileTreeCell extends TreeCell<File> {
	/**
	 * Creates a new instance.
	 * @param dragHandler the handler this tree cell should notify on drag and drop operations.
	 */
	public FileTreeCell(DragDropHandler dragHandler) {
		setOnDragDetected((MouseEvent event) -> dragHandler.handleDragDetected(event, this));
		setOnDragOver((DragEvent event) -> dragHandler.handleDragOver(event, this));
		setOnDragEntered((DragEvent event) -> dragHandler.handleOnDragEntered(event, this));
		setOnDragExited((DragEvent event) -> dragHandler.handleOnDragExited(event, this));
		setOnDragDropped((DragEvent event) -> dragHandler.handleDragDropped(event, this));
	}

	@Override
	protected void updateItem(File file, boolean empty) {
		super.updateItem(file, empty);
		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			if (file == null) {
				setText("");
			} else {
				setText(file.getName());
				FileTreeItem fileTreeItem = (FileTreeItem) getTreeItem();
				setContextMenu(fileTreeItem.createContextMenu());
			}
			setGraphic(getTreeItem().getGraphic());
		}
	}
}
