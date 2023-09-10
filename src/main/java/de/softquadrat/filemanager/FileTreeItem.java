package de.softquadrat.filemanager;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// From Java Doc: https://docs.oracle.com/javafx/2/api/javafx/scene/control/TreeItem.html

public class FileTreeItem extends TreeItem<File> implements Supplier<File[]>, Consumer<File[]> {
	// From https://icons8.com/icons/set
	private static final Image FOLDER_ICON = new Image(FileTreeItem.class.getResourceAsStream("folder.png"));
	private static final Image FILE_ICON = new Image(FileTreeItem.class.getResourceAsStream("file.png"));
	private static final Image HOURGLASS_ICON = new Image(FileTreeItem.class.getResourceAsStream("hourglass.png"));
	// We cache whether the File is a leaf or not. A File is a leaf if
	// it is not a directory and does not have any files contained within
	// it. We cache this as isLeaf() is called often, and doing the
	// actual check on File is expensive.
	private boolean isLeaf;
	// We do the leaf testing only once, and then set this
	// boolean to false so that we do not check again during this
	// run.
	private boolean isFirstTimeLeaf = true;
	private CompletableFuture<File[]> completableFuture;

	public FileTreeItem(File file) {
		super(file, new ImageView(file.isDirectory() ? FOLDER_ICON : FILE_ICON));
		expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue != null && newValue.booleanValue()) {
					loadChildren(file);
					ImageView icon = new ImageView(HOURGLASS_ICON);
					setGraphic(icon);
				} else {
					getChildren().clear();
					ImageView icon = new ImageView(FOLDER_ICON);
					setGraphic(icon);
					System.out.println("children: " + getChildren());
				}
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

	private void buildChildren(File[] files) {
		Platform.runLater(() -> {
			if (files != null) {
				for (File childFile : files) {
					getChildren().add(new FileTreeItem(childFile));
				}
			}
			ImageView icon = new ImageView(FOLDER_ICON);
			setGraphic(icon);
			System.out.println("children: " + getChildren());
		});
	}

	@Override
	public String toString() {
		return getValue().getName();
	}

	/*
	 * Called from
	 * - context menu "refresh"
	 * - drag and drop operation
	 */
	public void refresh() {
		setExpanded(false);
		setExpanded(true);
	}

	private void loadChildren(File file) {
		completableFuture = CompletableFuture.supplyAsync(this);
		completableFuture.thenAccept(this);
	}

	// Supplier method (will be called to start loading children)
	@Override
	public File[] get() {
		File[] retval = new File[0];
		File file = getValue();
		if (file != null && file.isDirectory()) {
			retval = file.listFiles();
			if (retval == null)
				retval = new File[0];
			else
				Arrays.sort(retval);
		}
		return retval;
	}

	// Consumer method (will be called when loading children completed)
	@Override
	public void accept(File[] files) {
		buildChildren(files);
	}
}
