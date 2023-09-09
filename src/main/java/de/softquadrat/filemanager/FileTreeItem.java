package de.softquadrat.filemanager;

import java.io.File;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// From Java Doc: https://docs.oracle.com/javafx/2/api/javafx/scene/control/TreeItem.html

public class FileTreeItem extends TreeItem<File> {
	// From https://icons8.com/icons/set
	private static final Image FOLDER_ICON = new Image(FileTreeItem.class.getResourceAsStream("folder.png"));
	private static final Image FILE_ICON =  new Image(FileTreeItem.class.getResourceAsStream("file.png"));
	// We cache whether the File is a leaf or not. A File is a leaf if
	// it is not a directory and does not have any files contained within
	// it. We cache this as isLeaf() is called often, and doing the
	// actual check on File is expensive.
	private boolean isLeaf;
	// We do the leaf testing only once, and then set this
	// boolean to false so that we do not check again during this
	// run.
	private boolean isFirstTimeLeaf = true;

	public FileTreeItem(File file) {
		super(file, new ImageView(file.isDirectory() ? FOLDER_ICON : FILE_ICON));
		expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				ObservableList<TreeItem<File>> children = getChildren();
				if (newValue != null && newValue.booleanValue())
					children.setAll(buildChildren());
				else
					children.clear();
				System.out.println(children);
			}
		});
	}

	@Override
	public boolean isLeaf() {
		if (isFirstTimeLeaf) {
			isFirstTimeLeaf = false;
			File f = (File) getValue();
			isLeaf = f.isFile();
		}
		return isLeaf;
	}

	// TODO this must be done asynchronously
	
	private ObservableList<TreeItem<File>> buildChildren() {
		File f = getValue();
		if (f != null && f.isDirectory()) {
			File[] files = f.listFiles();
			if (files != null) {
				ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
				for (File childFile : files) {
					children.add(new FileTreeItem(childFile));
				}
				return children;
			}
		}
		return FXCollections.emptyObservableList();
	}
}
