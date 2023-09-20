package net.treimers.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

// From Java Doc: https://docs.oracle.com/javafx/2/api/javafx/scene/control/TreeItem.html
public class FileTreeItem extends TreeItem<File> implements Supplier<File[]>, BiConsumer<File[], Throwable> {
	// From https://icons8.com/icons/set
	private static final Image FOLDER_ICON = new Image(FileTreeItem.class.getResourceAsStream("folder.png"));
	private static final Image FILE_ICON = new Image(FileTreeItem.class.getResourceAsStream("file.png"));
	private static final Image HOURGLASS_ICON = new Image(FileTreeItem.class.getResourceAsStream("hourglass.png"));
	// We cache whether the File is a leaf (plain file) or not (directory).
	// We cache this as isLeaf() is called often, and doing the actual check on File
	// is expensive.
	private boolean isLeaf;
	// We do the leaf testing only once, and then set this boolean to false so that
	// we do not check again during this run.
	private boolean isFirstTimeLeaf = true;
	private CompletableFuture<File[]> completableFuture;
	private Timeline timeLine;
	private ExceptionHandler exceptionHandler;
	private int count;

	public FileTreeItem(File file, ExceptionHandler exceptionHandler) {
		super(file, new ImageView(file.isDirectory() ? FOLDER_ICON : FILE_ICON));
		this.exceptionHandler = exceptionHandler;
		expandedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldEpxanded, Boolean newExpanded) {
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
			exceptionHandler.showError(throwable);
			resetFolderIcon();
		} else {
			try {
				// avoid NullPointerException
				if (files == null)
					files = new File[0];
				// create FileTreeItem container for all files (outside JavaFX thread)
				FileTreeItem[] treeItems = new FileTreeItem[files.length];
				for (int i = 0; i < files.length; i++)
					treeItems[i] = new FileTreeItem(files[i], exceptionHandler);
				// add all children to this item (in JavaFX thread)
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						getChildren().setAll(treeItems);
						resetFolderIcon();
					}

				});
			} catch (RuntimeException e) {
				exceptionHandler.showError(e);
				resetFolderIcon();
			}
		}
	}

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
							FileTreeItem newTreeItem = new FileTreeItem(file, exceptionHandler);
							ObservableList<TreeItem<File>> children = getChildren();
							children.add(newTreeItem);
							children.sort(Util.COMPARATOR);
						}
						setExpanded(true);
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
					File dir = getValue();
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
							FileTreeItem newTreeItem = new FileTreeItem(file, exceptionHandler);
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
						FileTreeItem parent = (FileTreeItem) getParent();
						parent.getChildren().remove(FileTreeItem.this);
					}
				}
			}
		});
		return contextMenu;
	}

	// private methods

	private void applyGraphic(Image image) {
		applyGraphic(image, false);
	}

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

	private void resetFolderIcon() {
		timeLine.stop();
		applyGraphic(FOLDER_ICON);
	}
}
