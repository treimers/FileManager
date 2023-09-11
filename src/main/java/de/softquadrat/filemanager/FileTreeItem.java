package de.softquadrat.filemanager;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

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
	Timeline timeLine;

	public FileTreeItem(File file) {
		super(file, new ImageView(file.isDirectory() ? FOLDER_ICON : FILE_ICON));
		expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue != null && newValue.booleanValue()) {
					completableFuture = CompletableFuture.supplyAsync(FileTreeItem.this);
					completableFuture.thenAccept(FileTreeItem.this);
					ImageView icon = new ImageView(HOURGLASS_ICON);
					timeLine = new Timeline(
							new KeyFrame(Duration.seconds(0), new KeyValue(icon.rotateProperty(), 0)),
							new KeyFrame(Duration.seconds(1), new KeyValue(icon.rotateProperty(), 360)));
					timeLine.setCycleCount(Animation.INDEFINITE);
					timeLine.play();
					setGraphic(icon);
				} else {
					timeLine.stop();
					getChildren().clear();
					ImageView icon = new ImageView(FOLDER_ICON);
					setGraphic(icon);
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
		// avoid NullPointerException
		if (files == null)
			files = new File[0];
		// create FileTreeItem container for all files (outside JavaFX thread)
		FileTreeItem[] treeItems = new FileTreeItem[files.length];
		for (int i = 0; i < files.length; i++)
			treeItems[i] = new FileTreeItem(files[i]);
		// add all FileTreeItem children to this FileTreeItem (in JavaFX thread)
		Platform.runLater(() -> {
			getChildren().setAll(treeItems);
			ImageView icon = new ImageView(FOLDER_ICON);
			setGraphic(icon);
			timeLine.stop();
		});
	}

	public boolean isAncestor(FileTreeItem other) {
		FileTreeItem current = other;
		FileTreeItem last = null;
		boolean retval = false;
		while (current != null && last != current) {
			if (this.equals(current)) {
				retval = true;
				break;
			}
			last = current;
			current = (FileTreeItem) current.getParent();
		}
		return retval;
	}
}
