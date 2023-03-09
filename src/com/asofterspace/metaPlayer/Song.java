/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.utils.Record;

import java.util.ArrayList;
import java.util.List;


public class Song {

	private String artist;
	private String title;
	private String path;
	private Integer length;
	private Integer rating;
	private Integer playAmount;
	private boolean fileExists;
	private String clipboardText = null;


	public Song(File file) {
		this(fileToPath(file));
	}

	private static String fileToPath(File file) {
		return file.getCanonicalFilename();
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
				title.endsWith(".mpg") ||
				title.endsWith(".avi") ||
				title.endsWith(".flv") ||
				title.endsWith(".wmv") ||
				title.endsWith(".wma") ||
				title.endsWith(".webm")) {
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
		this.playAmount = record.getInteger("playAmount");
		this.fileExists = (new File(path)).exists();
	}

	public Record toRecord() {
		Record result = new Record();
		result.set("artist", artist);
		result.set("title", title);
		result.set("path", path);
		result.set("length", length);
		if ((rating != null) && (rating < 0)) {
			rating = null;
		}
		result.set("rating", rating);
		if ((playAmount != null) && (playAmount > 0)) {
			result.set("playAmount", playAmount);
		}
		result.set("fileExists", fileExists);
		return result;
	}

	public String getArtist() {
		if (artist == null) {
			return "";
		}
		return artist;
	}

	public List<String> getArtists() {

		List<String> result = new ArrayList<>();

		if (artist == null) {
			return result;
		}

		String artistsStr = artist.replaceAll(" und ", ", ");
		artistsStr = artistsStr.replaceAll(" and ", ", ");
		artistsStr = artistsStr.replaceAll(" ft. ", ", ");
		artistsStr = artistsStr.replaceAll(" feat ", ", ");
		artistsStr = artistsStr.replaceAll(" feat. ", ", ");
		artistsStr = artistsStr.replaceAll(" featuring ", ", ");
		artistsStr = artistsStr.replaceAll(" & ", ", ");

		String[] artists = artistsStr.split(",");

		for (String cur : artists) {
			result.add(cur.trim());
		}

		return result;
	}

	public boolean hasArtist() {
		return artist != null;
	}

	public boolean hasArtist(String potentialArtist) {

		if ((artist == null) || (potentialArtist == null)) {
			return false;
		}

		String loArtist = artist.toLowerCase() + " ";
		String loPotentialArtist = potentialArtist.toLowerCase();

		// matches Foo - Bar for argument "foo"
		if (loArtist.equals(loPotentialArtist)) {
			return true;
		}

		// matches Foo Bar - Bar for argument "foo",
		// but does not match FooBar - Barbara
		if (loArtist.startsWith(loPotentialArtist + " ")) {
			return true;
		}

		// matches Foo, Bar - Bar for argument "foo",
		if (loArtist.startsWith(loPotentialArtist + ",")) {
			return true;
		}

		// matches Bar, Foo - Bar for argument "foo"
		if (loArtist.contains(", " + loPotentialArtist + " ")) {
			return true;
		}

		// matches Bar, Foo, Barbara - Bar for argument "foo"
		if (loArtist.contains(", " + loPotentialArtist + ",")) {
			return true;
		}

		// matches Bar und Foo - Bar for argument "foo"
		if (loArtist.contains(" und " + loPotentialArtist + " ")) {
			return true;
		}

		// matches Bar and Foo - Bar for argument "foo"
		if (loArtist.contains(" and " + loPotentialArtist + " ")) {
			return true;
		}

		// matches Bar feat Foo - Bar for argument "foo"
		if (loArtist.contains(" ft. " + loPotentialArtist + " ")) {
			return true;
		}

		// matches Bar feat Foo - Bar for argument "foo"
		if (loArtist.contains(" feat " + loPotentialArtist + " ")) {
			return true;
		}

		// matches Bar feat Foo - Bar for argument "foo"
		if (loArtist.contains(" feat. " + loPotentialArtist + " ")) {
			return true;
		}

		// matches Bar feat Foo - Bar for argument "foo"
		if (loArtist.contains(" featuring " + loPotentialArtist + " ")) {
			return true;
		}

		// matches Bar & Foo - Bar for argument "foo"
		if (loArtist.contains(" & " + loPotentialArtist + " ")) {
			return true;
		}

		return false;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getTitle() {
		if (title == null) {
			return "";
		}
		return title;
	}

	public boolean hasTitle() {
		return title != null;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPath() {
		return path;
	}

	public String getClipboardText(SongCtrl songCtrl, List<Record> allPlaylists) {
		if (clipboardText != null) {
			return clipboardText;
		}

		StringBuilder result = new StringBuilder();
		result.append(toString() + " (" + getPath() + ")");
		result.append("\n");
		result.append("\n");
		result.append("Included in:");

		List<Record> playlistsWithThisSong = songCtrl.getPlaylistsContainingSong(this, allPlaylists);
		if (playlistsWithThisSong.size() < 1) {
			result.append("\n(not included in any playlists)");
		}
		for (Record playlistRecord : playlistsWithThisSong) {
			result.append("\n");
			result.append(playlistRecord.getString(SongCtrl.PLAYLIST_NAME_KEY));
		}

		result.append("\n");
		result.append("\n");
		result.append("cd ");
		Directory hereDir = new Directory(System.getProperty("java.class.path") + "/..");
		result.append(hereDir.getCanonicalDirname());
		result.append("\n");
		result.append("run.bat --song \"" + toString() + "\"");

		clipboardText = result.toString();

		return clipboardText;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getLength() {
		if (length == null) {
			return 0;
		}
		return length;
	}

	public boolean hasLength() {
		return length != null;
	}

	public void setLength(Integer length) {
		this.length = length;
		if (!(this.length == null)) {
			// 9 seconds minimum length
			int minLength = 9*1000;
			if (this.length < minLength) {
				this.length = minLength;
			}
		}
	}

	public void setStrLength(String length) {
		try {
			setLength(Integer.parseInt(length));
		} catch (NumberFormatException e) {}
	}

	public Integer getRating() {
		return rating;
	}

	public int getRatingInt() {
		if (rating == null) {
			return -1;
		}
		return rating;
	}

	public boolean hasRating() {
		return (rating != null) && (rating >= 0);
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public void setRating(String rating) {
		try {
			this.rating = Integer.parseInt(rating);
		} catch (NumberFormatException e) {}
	}

	public int getPlayAmount() {
		if (playAmount == null) {
			return 0;
		}
		return playAmount;
	}

	public void incPlayAmount() {
		this.playAmount = getPlayAmount() + 1;
	}

	public void resetPlayAmount() {
		this.playAmount = null;
	}

	@Override
	public String toString() {
		if (hasArtist()) {
			return getArtist() + " - " + getTitle();
		}
		return getTitle();
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
			if (path == null) {
				return false;
			}
			return otherSong.path.toLowerCase().equals(path.toLowerCase());
		}
		return false;
	}

	public boolean is(String otherArtist, String otherTitle) {

		if (!getArtist().toLowerCase().equals(otherArtist.toLowerCase())) {
			return false;
		}

		if (!getTitle().toLowerCase().equals(otherTitle.toLowerCase())) {
			return false;
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
