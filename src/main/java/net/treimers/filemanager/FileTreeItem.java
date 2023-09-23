package net.treimers.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * Instances of this class are used as tree items in the FileManager navigation.
 * 
 * Base on https://docs.oracle.com/javafx/2/api/javafx/scene/control/TreeItem.html
 */
public class FileTreeItem extends TreeItem<File> implements Supplier<File[]>, BiConsumer<File[], Throwable> {
	/** The folder icon image. */
	private static final Image FOLDER_ICON = new Image(FileTreeItem.class.getResourceAsStream("folder.png"));
	/** The file icon image. */
	private static final Image FILE_ICON = new Image(FileTreeItem.class.getResourceAsStream("file.png"));
	/** The hour glass icon image. */
	private static final Image HOURGLASS_ICON = new Image(FileTreeItem.class.getResourceAsStream("hourglass.png"));
	/** Cache flag for leaf (plain file) or not (directory). */
	private boolean isLeaf;
	/** Indicator used to do leaf testing only once. */
	private boolean isFirstTimeLeaf = true;
	/** A completable future used for asynchronous loading. */
	private CompletableFuture<File[]> completableFuture;
	/** Time line used for animation. */
	private Timeline timeLine;
	/** A dialog handler used to show dialogs. */
	private DialogHandler dialogHandler;

	/**
	 * Creates a new instance.
	 * @param file the underlying file.
	 * @param dialogHandler the dialog handler used to show dialogs.
	 */
	public FileTreeItem(File file, DialogHandler dialogHandler) {
		super(file, new ImageView(file.isDirectory() ? FOLDER_ICON : FILE_ICON));
		this.dialogHandler = dialogHandler;
		expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldEpxanded,
					Boolean newExpanded) {
				if (newExpanded != null && newExpanded.booleanValue()) {
					applyGraphic(HOURGLASS_ICON, true);
					completableFuture = CompletableFuture.supplyAsync(FileTreeItem.this);
					completableFuture.whenComplete(FileTreeItem.this);
				} else
					getChildren().clear();
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

	/**
	 * Reloads this tree item.
	 */
	public void refresh() {
		setExpanded(false);
		setExpanded(true);
	}

	/**
	 * Loads the children of a directory.
	 * 
	 * The method implements the Supplier interface and is invoked asynchronously.
	 * 
	 * Any RunTimeExceptions and Errors thrown inside this method will be forwarded
	 * to the BiConsumer method.
	 */
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

	/**
	 * Handles the result after loading the children of a directory.
	 * 
	 * The method implements the BiConsumer interface.
	 * 
	 * @param files     the loaded children of a directory.
	 * @param throwable any Throwable thrown during the load operation or null if
	 *                  load was successful.
	 */
	@Override
	public void accept(File[] files, Throwable throwable) {
		if (throwable != null) {
			dialogHandler.showError(throwable);
			resetFolderIcon();
		} else {
			try {
				// avoid NullPointerException
				if (files == null)
					files = new File[0];
				// create FileTreeItem container for all files (outside JavaFX thread)
				FileTreeItem[] treeItems = new FileTreeItem[files.length];
				for (int i = 0; i < files.length; i++)
					treeItems[i] = new FileTreeItem(files[i], dialogHandler);
				// add all children to this item (in JavaFX thread)
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						getChildren().setAll(treeItems);
						resetFolderIcon();
					}

				});
			} catch (RuntimeException e) {
				dialogHandler.showError(e);
				resetFolderIcon();
			}
		}
	}

	/**
	 * Checks whether this FileTreeItem is an ancestor of another FileTreeItem.
	 * @param other the other FileTreeItem.
	 * @return true, if this FileTreeItem is an ancestor of other FileTreeItem, false otherwise.
	 */
	public boolean isAncestor(FileTreeItem other) {
		FileTreeItem current = other;
		FileTreeItem last = null;
		boolean retval = false;
		while (current != null && last != current) {
			if (equals(current)) {
				retval = true;
				break;
			}
			last = current;
			current = (FileTreeItem) current.getParent();
		}
		return retval;
	}

	/**
	 * Creates a context menu for this FileTreeItem.
	 * @return a context menu for this FileTreeItem.
	 */
	public ContextMenu createContextMenu() {
		ContextMenu contextMenu = new ContextMenu();
		ObservableList<MenuItem> menuItems = contextMenu.getItems();
		if (getValue().isDirectory()) {
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
					if (isExpanded()) {
						refresh();
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
					File dir = getValue();
					if (!dir.isDirectory())
						return;
					String fileName = dialogHandler.showTextInputDialog("New File", "Creating New File", "Please enter name of new file:", "file.txt");
					if (fileName == null)
						return;
					File file = new File(dir, fileName);
					try {
						if (file.exists()) {
							dialogHandler.showAlert(AlertType.WARNING, "Warning", "File exists: " + file.getName(),
									"Sorry, ignoring your request because file already exists!");
						} else {
							file.createNewFile();
							FileTreeItem newTreeItem = new FileTreeItem(file, dialogHandler);
							ObservableList<TreeItem<File>> children = getChildren();
							children.add(newTreeItem);
							children.sort(Util.COMPARATOR);
						}
						setExpanded(true);
					} catch (IOException e) {
						dialogHandler.showAlert(AlertType.ERROR, "Error", "Error creating File: " + file.getName(),
								"Sorry, failed to create file: " + e.getMessage());
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
					File dir = getValue();
					if (!dir.isDirectory())
						return;
					String dirName = dialogHandler.showTextInputDialog("New Directory", "Creating New Directory", "Please enter name of new directory:", "directory");
					if (dirName == null)
						return;
					File file = new File(dir, dirName);
					if (file.exists()) {
						dialogHandler.showAlert(AlertType.WARNING, "Warning", "Directory exists: " + file.getName(),
								"Sorry, ignoring your request because directory already exists!");
					} else {
						boolean success = file.mkdir();
						if (!success) {
							dialogHandler.showAlert(AlertType.ERROR, "Error",
									"Error creating Directory: " + file.getName(),
									"Sorry, failed to create directory!");
						} else {
							FileTreeItem newTreeItem = new FileTreeItem(file, dialogHandler);
							ObservableList<TreeItem<File>> children = getChildren();
							children.add(newTreeItem);
							children.sort(Util.COMPARATOR);
						}
					}
					setExpanded(true);
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
				File file = getValue();
				boolean result = dialogHandler.showConfirmation("Delete File", "Delete File: " + file.getName(),
						"Are you sure?");
				if (result) {
					boolean delete = file.delete();
					if (!delete) {
						dialogHandler.showAlert(AlertType.ERROR, "Error", "Error deleting File: " + file.getName(),
								"Sorry, deletion failed! Maybe non-empty directory?");
					} else {
						FileTreeItem parent = (FileTreeItem) getParent();
						parent.getChildren().remove(FileTreeItem.this);
					}
				}
			}
		});
		return contextMenu;
	}

	// private methods

	/**
	 * Sets the icon of this FileTreeItem.
	 * @param image the icon of this FileTreeItem.
	 */
	private void applyGraphic(Image image) {
		applyGraphic(image, false);
	}

	/**
	 * Sets the icon of this FileTreeItem.
	 * @param image the icon of this FileTreeItem.
	 * @param animate true, if the icon should be animated.
	 */
	private void applyGraphic(Image image, boolean animate) {
		ImageView icon = new ImageView(image);
		setGraphic(icon);
		if (animate) {
			timeLine = new Timeline(
					new KeyFrame(Duration.seconds(0), new KeyValue(icon.rotateProperty(), 0)),
					new KeyFrame(Duration.seconds(1), new KeyValue(icon.rotateProperty(), 360)));
			timeLine.setCycleCount(Animation.INDEFINITE);
			timeLine.play();
		}
	}

	/**
	 * Resets the folder icon to its default and stops the animation.
	*/
	private void resetFolderIcon() {
		timeLine.stop();
		applyGraphic(FOLDER_ICON);
	}
}
