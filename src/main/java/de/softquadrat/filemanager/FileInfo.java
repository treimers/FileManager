package de.softquadrat.filemanager;

import java.io.File;
import java.util.Date;

import javafx.beans.property.SimpleStringProperty;

public class FileInfo {
	private SimpleStringProperty nameProperty;
	private SimpleStringProperty dateProperty;
	private SimpleStringProperty sizeProperty;

	public FileInfo(File file) {
		String name = file.getName();
		String date = new Date(file.lastModified()).toString();
		String length = "" + file.length();
		this.nameProperty = new SimpleStringProperty(name);
		this.dateProperty = new SimpleStringProperty(date);
		this.sizeProperty = new SimpleStringProperty(length);
	}

	public String getName() {
		return nameProperty.get();
	}

	public String getDate() {
		return dateProperty.get();
	}

	public String getSize() {
		return sizeProperty.get();
	}
}
