package de.softquadrat.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;

public class FileTreeCell extends TreeCell<File> {
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
				setContextMenu(createContextMenu(file));
			}
			setGraphic(getTreeItem().getGraphic());
		}
	}

	private ContextMenu createContextMenu(File item) {
		ContextMenu contextMenu = new ContextMenu();
		ObservableList<MenuItem> menuItems = contextMenu.getItems();
		if (item.isDirectory()) {
			/*
			 * Refresh Action
			 */
			MenuItem refresh = new MenuItem("Refresh");
			menuItems.add(refresh);
			// We simply close and reopen the folder tree item which will empty the children
			// list.
			// But only if it was already open before refresh.
			refresh.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					FileTreeItem treeItem = (FileTreeItem) getTreeItem();
					// Avoid NullPointerException!
					if (treeItem != null && treeItem.isExpanded()) {
						treeItem.refresh();
					}
				}
			});
			/*
			 * Separator
			 */
			SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
			menuItems.add(separatorMenuItem);
			/*
			 * Create New File Action
			 */
			MenuItem newFile = new MenuItem("New File");
			menuItems.add(newFile);
			newFile.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					FileTreeItem treeItem = (FileTreeItem) getTreeItem();
					File dir = treeItem.getValue();
					if (!dir.isDirectory())
						return;
					TextInputDialog dialog = new TextInputDialog("file.txt");
					dialog.setTitle("New File");
					dialog.setHeaderText("Creating New File");
					dialog.setContentText("Please enter name of new file:");
					Optional<String> result = dialog.showAndWait();
					if (!result.isPresent())
						return;
					File file = new File(dir, result.get());
					try {
						if (file.exists()) {
							Alert error = new Alert(AlertType.WARNING);
							error.setTitle("Warning");
							error.setHeaderText("File exists: " + file.getName());
							error.setContentText("Sorry, ignoring your request because file already exists!");
							error.showAndWait();
						} else {
							file.createNewFile();
							FileTreeItem newTreeItem = new FileTreeItem(file);
							ObservableList<TreeItem<File>> children = treeItem.getChildren();
							children.add(newTreeItem);
							children.sort(Util.COMPARATOR);
						}
						treeItem.setExpanded(true);
					} catch (IOException e) {
						Alert error = new Alert(AlertType.ERROR);
						error.setTitle("Error");
						error.setHeaderText("Error creating File: " + file.getName());
						error.setContentText("Sorry, failed to create file: " + e.getMessage());
						error.showAndWait();
					}
				}
			});
			/*
			 * Create New Directory Action
			 */
			MenuItem newDir = new MenuItem("New Directory");
			menuItems.add(newDir);
			newDir.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					FileTreeItem treeItem = (FileTreeItem) getTreeItem();
					File dir = treeItem.getValue();
					if (!dir.isDirectory())
						return;
					TextInputDialog dialog = new TextInputDialog("directory");
					dialog.setTitle("New Directory");
					dialog.setHeaderText("Creating New Directory");
					dialog.setContentText("Please enter name of new directory:");
					Optional<String> result = dialog.showAndWait();
					if (!result.isPresent())
						return;
					File file = new File(dir, result.get());
					if (file.exists()) {
						Alert error = new Alert(AlertType.WARNING);
						error.setTitle("Warning");
						error.setHeaderText("Directory exists: " + file.getName());
						error.setContentText("Sorry, ignoring your request because directory already exists!");
						error.showAndWait();
					} else {
						boolean success = file.mkdir();
						if (!success) {
							Alert error = new Alert(AlertType.ERROR);
							error.setTitle("Error");
							error.setHeaderText("Error creating Directory: " + file.getName());
							error.setContentText("Sorry, failed to create directory!");
							error.showAndWait();
						} else {
							FileTreeItem newTreeItem = new FileTreeItem(file);
							ObservableList<TreeItem<File>> children = treeItem.getChildren();
							children.add(newTreeItem);
							children.sort(Util.COMPARATOR);
						}
					}
					treeItem.setExpanded(true);
				}
			});
		}
		/*
		 * Delete File Action
		 */
		MenuItem deleteFile = new MenuItem("Delete");
		menuItems.add(deleteFile);
		deleteFile.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileTreeItem treeItem = (FileTreeItem) getTreeItem();
				File file = treeItem.getValue();
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Delete File");
				alert.setHeaderText("Delete File: " + file.getName());
				alert.setContentText("Are you sure?");
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK) {
					boolean delete = file.delete();
					if (!delete) {
						Alert error = new Alert(AlertType.ERROR);
						error.setTitle("Error");
						error.setHeaderText("Error deleting File: " + file.getName());
						error.setContentText("Sorry, deletion failed! Maybe non-empty directory?");
						error.showAndWait();
					} else {
						FileTreeItem parent = (FileTreeItem) treeItem.getParent();
						parent.getChildren().remove(treeItem);
					}
				}
			}
		});
		return contextMenu;
	}
}
