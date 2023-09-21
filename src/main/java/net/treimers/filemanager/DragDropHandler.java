package net.treimers.filemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Duration;

// https://docs.oracle.com/javase/8/javafx/api/index.html?javafx/scene/control/package-summary.html
// https://docs.oracle.com/javafx/2/drag_drop/jfxpub-drag_drop.htm
// https://brianyoung.blog/2018/08/23/javafx-treeview-drag-drop/
public class DragDropHandler {
	private static final DataFormat JAVA_FORMAT = new DataFormat("application/x-java-serialized-object");
	private static final String DROP_HINT_STYLE = "-fx-border-color: #3399ff; -fx-border-width: 2 2 2 2; -fx-padding: 3 3 1 3";
	private TreeView<File> treeView;
	private DialogHandler dialogHandler;
	private PauseTransition pauseTransition;

	public DragDropHandler(TreeView<File> treeView, DialogHandler dialogHandler) {
		this.treeView = treeView;
		this.dialogHandler = dialogHandler;
		pauseTransition = new PauseTransition(Duration.seconds(1));
	}

	public void handleDragDetected(MouseEvent event, FileTreeCell sourceTreeCell) {
		FileTreeItem sourceTreeItem = (FileTreeItem) sourceTreeCell.getTreeItem();
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
		if (!event.getDragboard().hasContent(JAVA_FORMAT))
			return;
		FileTreeCell sourceTreeCell = (FileTreeCell) event.getGestureSource();
		FileTreeItem sourceTreeItem = (FileTreeItem) sourceTreeCell.getTreeItem();
		FileTreeItem targetTreeItem = (FileTreeItem) targetTreeCell.getTreeItem();
		if (dropAllowed(sourceTreeItem, targetTreeItem))
			event.acceptTransferModes(TransferMode.MOVE);
		event.consume();
	}

	public void handleOnDragEntered(DragEvent event, FileTreeCell targetTreeCell) {
		if (!event.getDragboard().hasContent(JAVA_FORMAT))
			return;
		FileTreeCell sourceTreeCell = (FileTreeCell) event.getGestureSource();
		FileTreeItem sourceTreeItem = (FileTreeItem) sourceTreeCell.getTreeItem();
		FileTreeItem targetTreeItem = (FileTreeItem) targetTreeCell.getTreeItem();
		if (dropAllowed(sourceTreeItem, targetTreeItem)) {
			targetTreeCell.setStyle(DROP_HINT_STYLE);
		}
		pauseTransition.setOnFinished((ActionEvent e) -> {
			targetTreeItem.setExpanded(true);
		});
		pauseTransition.playFromStart();
		event.consume();
	}

	public void handleOnDragExited(DragEvent event, FileTreeCell targetTreeCell) {
		targetTreeCell.setStyle("");
		pauseTransition.stop();
		event.consume();
	}

	public void handleDragDropped(DragEvent event, FileTreeCell targetTreeCell) {
		Dragboard db = event.getDragboard();
		FileTreeCell sourceTreeCell = (FileTreeCell) event.getGestureSource();
		FileTreeItem sourceTreeItem = (FileTreeItem) sourceTreeCell.getTreeItem();
		FileTreeItem sourceParent = (FileTreeItem) sourceTreeItem.getParent();
		FileTreeItem targetTreeItem = (FileTreeItem) targetTreeCell.getTreeItem();
		boolean success = db.hasContent(JAVA_FORMAT) && sourceParent != null;
		if (success) {
			File targetDir = targetTreeItem.getValue();
			File sourceFile = sourceTreeItem.getValue();
			try {
				System.out.println("Moving " + sourceFile + " to directory " + targetDir);
				File newTargetFile = new File(targetDir, sourceFile.getName());
				if (newTargetFile.exists()) {
					dialogHandler.showAlert(AlertType.ERROR, "Error", "Error moving File: " + sourceFile.getName(), "Sorry, file already exists in target");
					success = false;
				} else {
					Files.move(sourceFile.toPath(), Paths.get(targetDir.getPath(), sourceFile.getName()),
							StandardCopyOption.ATOMIC_MOVE);
					// remove item from source parent
					sourceParent.getChildren().remove(sourceTreeItem);
					// add item to target item, if target item is expanded
					// otherwise expand target item
					if (targetTreeItem.isExpanded()) {
						FileTreeItem newTargetTreeItem = new FileTreeItem(newTargetFile, dialogHandler);
						ObservableList<TreeItem<File>> children = targetTreeItem.getChildren();
						children.add(newTargetTreeItem);
						children.sort(Util.COMPARATOR);
						treeView.getSelectionModel().select(newTargetTreeItem);
					} else {
						targetTreeItem.setExpanded(true);
						treeView.getSelectionModel().select(targetTreeItem);
					}
				}
			} catch (IOException e) {
				dialogHandler.showAlert(AlertType.ERROR, "Error", "Error moving File: " + sourceFile.getName(), "Sorry, failed to move file: " + e.getMessage());
				success = false;
			}
		}
		event.setDropCompleted(success);
		event.consume();
	}

	/**
	 * The following situations will be rejected:
	 * - the source tree item is null
	 * - the target tree item is null
	 * - the source tree item and the target tree item are the same
	 * - the source tree item is already child of the target tree item
	 * - the target tree item is a leaf (and cannot contain children)
	 * - the source tree item is ancestor of the target tree item and cannot be
	 * moved into its own descendant
	 */
	private boolean dropAllowed(FileTreeItem sourceTreeItem, FileTreeItem targetTreeItem) {
		return sourceTreeItem != null
				&& targetTreeItem != null
				&& targetTreeItem != sourceTreeItem
				&& sourceTreeItem.getParent() != targetTreeItem
				&& !targetTreeItem.isLeaf()
				&& !sourceTreeItem.isAncestor(targetTreeItem);
	}
}
