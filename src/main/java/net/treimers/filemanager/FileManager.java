package net.treimers.filemanager;

import java.io.InputStream;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class FileManager extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("File Manager");
		InputStream resource = FileManager.class.getResourceAsStream("filemanager.png");
		if (resource != null) {
			Image icon = new Image(resource);
			primaryStage.getIcons().add(icon);
		}
		// load the view
		FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
		Parent root = loader.load();
		// get view's controller and propagate stage to controller
		Controller controller = loader.getController();
		controller.setStage(primaryStage);
		// create the scene
		Scene scene = new Scene(root);
		// apply the scene to the stage and show the stage
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void run(String[] args) {
		launch(args);
	}
}
