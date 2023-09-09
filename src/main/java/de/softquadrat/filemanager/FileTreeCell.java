package de.softquadrat.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;

public class FileTreeCell extends TreeCell<File> {
	private ContextMenu contextMenu;

	public FileTreeCell() {
		contextMenu = new ContextMenu();
		MenuItem newFile = new MenuItem("Add File");
		contextMenu.getItems().add(newFile);
		newFile.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				File dir = getTreeItem().getValue();
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
					if (file.exists())
						return;
					file.createNewFile();
					FileTreeItem treeItem = new FileTreeItem(file);
					getTreeItem().getChildren().add(treeItem);
				} catch (IOException e) {
					// TODO handle exception correctly
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void updateItem(File file, boolean empty) {
		super.updateItem(file, empty);
		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			setText(getItem() == null ? "" : getItem().getName());
			setGraphic(getTreeItem().getGraphic());
			if (!getTreeItem().isLeaf())
				setContextMenu(contextMenu);
		}
	}
}
