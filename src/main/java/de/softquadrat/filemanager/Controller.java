package de.softquadrat.filemanager;

import java.io.File;
import java.net.URL;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Controller implements Initializable {
	@FXML
	private TableView<FileInfo> tableView;
	@FXML
	private TreeView<File> treeView;
	private FileTreeItem root;
	private Stage primaryStage;
	private DragDropHandler dragHandler;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		dragHandler = new DragDropHandler(treeView);
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
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About");
		alert.setHeaderText("About File Manager");
		alert.setContentText("File Manager is used to demonstrate JavaFX TreeView and Drag-And-Drop!");
		alert.showAndWait();
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
			root = new FileTreeItem(dir);
			treeView.setRoot(root);
		}
	}

	public void setStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}
}