package net.treimers.filemanager;

import javafx.scene.control.Alert.AlertType;

/**
 * The dialog handler interface provides some methods used to open different dialogs.
 */
public interface DialogHandler {
	/**
	 * Shows an exception / error dialog.
	 * @param throwable the exception or error.
	 */
	public void showError(Throwable throwable);

	/**
	 * Shows an alert dialog.
	 * @param alertType the alert type.
	 * @param title the title.
	 * @param headerText the header text.
	 * @param contentText the content text.
	 */
	public void showAlert(AlertType alertType, String title, String headerText, String contentText);

	/**
	 * Shows a confirmation dialog.
	 * @param title the title.
	 * @param headerText the header text.
	 * @param contentText the content text.
	 * @return true if confirmed, false otherwise.
	 */
	public boolean showConfirmation(String title, String headerText, String contentText);

	/**
	 * Shows a text input dialog.
	 * @param title the title.
	 * @param headerText the header text.
	 * @param contentText the content text.
	 * @param defaultValue the default value.
	 * @return the user input string.
	 */
	public String showTextInputDialog(String title, String headerText, String contentText, String defaultValue);
}
