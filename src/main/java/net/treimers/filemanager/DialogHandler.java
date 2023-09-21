package net.treimers.filemanager;

import javafx.scene.control.Alert.AlertType;

public interface DialogHandler {
	public void showError(Throwable throwable);

	public void showAlert(AlertType alertType, String title, String headerText, String contentText);

	public boolean showConfirmation(String title, String headerText, String contentText);
}
