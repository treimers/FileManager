package net.treimers.filemanager;

import java.io.File;
import java.util.Date;

import javafx.beans.property.SimpleStringProperty;
/**
 * Instances are used to represent file data.
 */
public class FileInfo {
	/** The file name property. */
	private SimpleStringProperty nameProperty;
	/** The file date property. */
	private SimpleStringProperty dateProperty;
	/** The file size property. */
	private SimpleStringProperty sizeProperty;

	/**
	 * Creates a new file info instance.
	 * @param file the corresponding file.
	 */
	public FileInfo(File file) {
		String name = file.getName();
		String date = new Date(file.lastModified()).toString();
		String length = "" + file.length();
		this.nameProperty = new SimpleStringProperty(name);
		this.dateProperty = new SimpleStringProperty(date);
		this.sizeProperty = new SimpleStringProperty(length);
	}

	/**
	 * Gets the file name.
	 * @return the file name.
	 */
	public String getName() {
		return nameProperty.get();
	}

	/**
	 * Gets the file date.
	 * @return the file date.
	 */
	public String getDate() {
		return dateProperty.get();
	}

	/**
	 * Gets the file size.
	 * @return the file size.
	 */
	public String getSize() {
		return sizeProperty.get();
	}
}
