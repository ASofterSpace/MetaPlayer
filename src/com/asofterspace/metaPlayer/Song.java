/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.Record;

import java.io.File;
import java.io.IOException;


public class Song {

	private String artist;
	private String title;
	private String path;
	private Integer length;
	private Integer rating;


	public Song(File file) {
		this(fileToPath(file));
	}

	private static String fileToPath(File file) {
		try {
			return file.getCanonicalPath();
		} catch (IOException ex) {
			return file.getAbsolutePath();
		}
	}

	public Song(String path) {
		this(path, null);
	}

	public Song(String path, String artistAndTitle) {

		this.setPath(path);

		if ((artistAndTitle == null) || !artistAndTitle.contains(" - ")) {
			String altSongName = path;
			if (altSongName.contains("\\")) {
				altSongName = altSongName.substring(altSongName.lastIndexOf("\\") + 1);
			}
			if (altSongName.contains("/")) {
				altSongName = altSongName.substring(altSongName.lastIndexOf("/") + 1);
			}
			if ((artistAndTitle == null) || altSongName.contains(" - ")) {
				artistAndTitle = altSongName;
			}
		}
		String[] songNames = artistAndTitle.split(" - ");
		title = null;
		if (songNames.length > 1) {
			artist = songNames[0];
			if (songNames.length > 2) {
				title = songNames[1] + " - " + songNames[2];
			} else {
				title = songNames[1];
			}
		} else {
			title = songNames[0];
		}
		if (title != null) {
			if (title.endsWith(".mp4") ||
				title.endsWith(".mp3") ||
				title.endsWith(".mkv") ||
				title.endsWith(".avi") ||
				title.endsWith(".flv") ||
				title.endsWith(".wmv") ||
				title.endsWith(".wma")) {
				title = title.substring(0, title.length() - 4);
			}
		}
	}

	public Song(Record record) {
		this.artist = record.getString("artist");
		this.title = record.getString("title");
		this.path = record.getString("path");
		this.length = record.getInteger("length");
		this.rating = record.getInteger("rating");
	}

	public Record toRecord() {
		Record result = new Record();
		result.set("artist", new Record(artist));
		result.set("title", new Record(title));
		result.set("path", new Record(path));
		result.set("length", new Record(length));
		result.set("rating", new Record(rating));
		return result;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	public void setLength(String length) {
		try {
			this.length = Integer.parseInt(length);
		} catch (NumberFormatException e) {}
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public void setRating(String rating) {
		try {
			this.rating = Integer.parseInt(rating);
		} catch (NumberFormatException e) {}
	}

	@Override
	public String toString() {
		if (artist == null) {
			return title;
		}
		return artist + " - " + title;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof Song) {
			Song otherSong = (Song) other;
			if (otherSong.path == null) {
				if (path == null) {
					return true;
				} else {
					return false;
				}
			}
			return otherSong.path.equals(path);
		}
		return false;
	}

	public boolean is(String otherArtist, String otherTitle) {

		if (artist == null) {
			if (otherArtist != null) {
				return false;
			}
		} else {
			if (otherArtist == null) {
				return false;
			}
			if (!artist.equals(otherArtist)) {
				return false;
			}
		}

		if (title == null) {
			if (otherTitle != null) {
				return false;
			}
		} else {
			if (otherTitle == null) {
				return false;
			}
			if (!title.equals(otherTitle)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		if (path == null) {
			return 0;
		}
		return path.hashCode();
	}

}
