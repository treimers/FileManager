package net.treimers.filemanager;

import java.io.File;
import java.util.Comparator;

import javafx.scene.control.TreeItem;

/**
 * Utility class with helper method.
 */
public class Util {
	/** A tree item comparator used to sort items in a tree. */
	public static final Comparator<? super TreeItem<File>> COMPARATOR = new Comparator<>() {
		@Override
		public int compare(TreeItem<File> o1, TreeItem<File> o2) {
			return o1.getValue().toPath().compareTo(o2.getValue().toPath());
		}
	};
}
