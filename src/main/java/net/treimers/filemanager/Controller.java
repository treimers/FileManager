package net.treimers.filemanager;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
/**
 * Controller of FileManager application.
 */
public class Controller implements Initializable, DialogHandler {
	@FXML
	private MenuBar menuBar;
	@FXML
	private TableView<FileInfo> tableView;
	@FXML
	private TreeView<File> treeView;
	private FileTreeItem root;
	private Stage primaryStage;
	private DragDropHandler dragHandler;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		menuBar.setUseSystemMenuBar(true);
		dragHandler = new DragDropHandler(treeView, this);
		TableColumn<FileInfo, String> name = new TableColumn<>("Name");
		name.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("name"));
		tableView.getColumns().add(name);
		TableColumn<FileInfo, String> date = new TableColumn<>("Date");
		date.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("date"));
		tableView.getColumns().add(date);
		TableColumn<FileInfo, String> size = new TableColumn<>("Size");
		size.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("size"));
		tableView.getColumns().add(size);
		treeView.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {
			@Override
			public TreeCell<File> call(TreeView<File> param) {
				return new FileTreeCell(dragHandler);
			}
		});
		treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<File>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<File>> observable, TreeItem<File> oldValue,
					TreeItem<File> newValue) {
				tableView.getItems().clear();
				if (newValue == null)
					return;
				File file = newValue.getValue();
				ObservableList<FileInfo> list = FXCollections.observableArrayList(new FileInfo(file));
				tableView.setItems(list);
			}
		});
	}

	@FXML
	void handleAbout(ActionEvent event) {
		showAlert(AlertType.INFORMATION, "About", "About File Manager",
				"File Manager is used to demonstrate JavaFX TreeView and Drag-And-Drop!");
	}

	@FXML
	void handleExit(ActionEvent event) {
		Platform.exit();
	}

	@FXML
	void handleOpen(ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Open Directory");
		File dir = directoryChooser.showDialog(primaryStage);
		if (dir != null) {
			root = new FileTreeItem(dir, this);
			treeView.setRoot(root);
		}
	}

	public void setStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	// DialogHandler methods

	@Override
	public void showError(Throwable throwable) {
		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.ERROR);
			alert.initOwner(primaryStage);
			alert.setTitle("Error");
			alert.setHeaderText("An error occured!");
			alert.setContentText(throwable.getMessage());
			// Create expandable Exception.
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			throwable.printStackTrace(pw);
			String exceptionText = sw.toString();
			Label label = new Label("Stacktrace:");
			TextArea textArea = new TextArea(exceptionText);
			textArea.setEditable(false);
			textArea.setWrapText(true);
			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setVgrow(textArea, Priority.ALWAYS);
			GridPane.setHgrow(textArea, Priority.ALWAYS);
			GridPane expContent = new GridPane();
			expContent.setMaxWidth(Double.MAX_VALUE);
			expContent.add(label, 0, 0);
			expContent.add(textArea, 0, 1);
			// Set expandable Exception into the dialog pane.
			alert.getDialogPane().setExpandableContent(expContent);
			alert.showAndWait();
		});
	}

	@Override
	public void showAlert(AlertType alertType, String title, String headerText, String contentText) {
		Alert alert = new Alert(alertType);
		alert.initOwner(primaryStage);
		alert.setTitle(title);
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);
		alert.showAndWait();
	}

	@Override
	public boolean showConfirmation(String title, String headerText, String contentText) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.initOwner(primaryStage);
		alert.setTitle(title);
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);
		Optional<ButtonType> result = alert.showAndWait();
		return result.get() == ButtonType.OK;
	}

	@Override
	public String showTextInputDialog(String title, String headerText, String contentText, String defaultValue) {
		TextInputDialog dialog = new TextInputDialog(defaultValue);
		dialog.initOwner(primaryStage);
		dialog.setTitle(title);
		dialog.setHeaderText(headerText);
		dialog.setContentText(contentText);
		Optional<String> result = dialog.showAndWait();
		if (!result.isPresent())
			return null;
		else
			return result.get();
	}
}