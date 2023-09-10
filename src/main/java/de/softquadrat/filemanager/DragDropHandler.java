package de.softquadrat.filemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javafx.scene.control.TreeItem;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

// https://docs.oracle.com/javase/8/javafx/api/index.html?javafx/scene/control/package-summary.html
// https://docs.oracle.com/javafx/2/drag_drop/jfxpub-drag_drop.htm
// https://brianyoung.blog/2018/08/23/javafx-treeview-drag-drop/
public class DragDropHandler {
	private static final DataFormat JAVA_FORMAT = new DataFormat("application/x-java-serialized-object");
	private static final String DROP_HINT_STYLE = "-fx-border-color: #3399ff; -fx-border-width: 2 2 2 2; -fx-padding: 3 3 1 3";

	public void handleDragDetected(MouseEvent event, FileTreeCell sourceTreeCell) {
		// System.out.println("handleDragDetected - sourceTreeCell: " + sourceTreeCell);
		TreeItem<File> sourceTreeItem = sourceTreeCell.getTreeItem();
		// Do not allow moving root
		if (sourceTreeItem.getParent() == null)
			return;
		Dragboard db = sourceTreeCell.startDragAndDrop(TransferMode.MOVE);
		ClipboardContent content = new ClipboardContent();
		content.put(JAVA_FORMAT, sourceTreeItem.getValue());
		db.setContent(content);
		db.setDragView(sourceTreeCell.snapshot(null, null));
		event.consume();
	}

	public void handleDragOver(DragEvent event, FileTreeCell targetTreeCell) {
		// System.out.println("handleDragOver - targetTreeCell: " + targetTreeCell);
		if (!event.getDragboard().hasContent(JAVA_FORMAT))
			return;
		TreeItem<File> sourceTreeItem = ((FileTreeCell) event.getGestureSource()).getTreeItem();
		TreeItem<File> targetTreeItem = targetTreeCell.getTreeItem();
		if (dropAllowed(sourceTreeItem, targetTreeItem))
			event.acceptTransferModes(TransferMode.MOVE);
		event.consume();
	}

	public void handleOnDragEntered(DragEvent event, FileTreeCell targetTreeCell) {
		// System.out.println("handleOnDragEntered - targetTreeCell: " +
		// targetTreeCell);
		if (!event.getDragboard().hasContent(JAVA_FORMAT))
			return;
		TreeItem<File> sourceTreeItem = ((FileTreeCell) event.getGestureSource()).getTreeItem();
		TreeItem<File> targetTreeItem = targetTreeCell.getTreeItem();
		if (dropAllowed(sourceTreeItem, targetTreeItem))
			targetTreeCell.setStyle(DROP_HINT_STYLE);
		event.consume();
	}

	public void handleOnDragExited(DragEvent event, FileTreeCell targetTreeCell) {
		// System.out.println("handleOnDragExited - targetTreeCell: " + targetTreeCell);
		targetTreeCell.setStyle("");
		event.consume();
	}

	public void handleDragDropped(DragEvent event, FileTreeCell targetTreeCell) {
		// System.out.println("handleDragDropped - targetTreeCell: " + targetTreeCell);
		// Object target = event.getGestureTarget();
		// System.out.println("handleDragDone - target: " + target);
		Dragboard db = event.getDragboard();
		TreeItem<File> sourceTreeItem = ((FileTreeCell) event.getGestureSource()).getTreeItem();
		TreeItem<File> sourceParent = sourceTreeItem.getParent();
		TreeItem<File> targetTreeItem = targetTreeCell.getTreeItem();
		boolean success = db.hasContent(JAVA_FORMAT) && sourceParent != null;
		if (success) {
			File targetDir = targetTreeItem.getValue();
			File sourceFile = sourceTreeItem.getValue();
			try {
				System.out.println("Moving " + sourceFile + " to directory " + targetDir);
				Files.move(sourceFile.toPath(), Paths.get(targetDir.getPath(), sourceFile.getName()),
						StandardCopyOption.ATOMIC_MOVE);
				// refresh source & target dir
				sourceParent.setExpanded(false);
				sourceParent.setExpanded(true);
				targetTreeItem.setExpanded(false);
				targetTreeItem.setExpanded(true);
			} catch (IOException e) {
				// TODO: Open a pop-up error dialog
				e.printStackTrace();
				success = false;
			}
		}
		event.setDropCompleted(success);
		event.consume();
	}

	private boolean dropAllowed(TreeItem<File> sourceTreeItem, TreeItem<File> targetTreeItem) {
		return sourceTreeItem != null && targetTreeItem != null && targetTreeItem != sourceTreeItem
				&& sourceTreeItem.getParent() != targetTreeItem && !targetTreeItem.isLeaf();
	}
}
