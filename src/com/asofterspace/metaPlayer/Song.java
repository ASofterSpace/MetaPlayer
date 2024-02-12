/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.utils.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Song {

	private Integer HASH_CODE = null;
	private String STRING_REPRESENTATION = null;
	private String STRING_REPRESENTATION_LOW = null;
	private String STRING_REPRESENTATION_LOW_TRIM = null;
	private String TITLE_LOW = null;
	private String TITLE_LOW_TRIM = null;
	private String TITLE_LOW_TRIM_NO_BRACKETS = null;
	private String PATH_LOW = null;
	private String ARTIST_LOW = null;
	private String SORT_STR_ARTIST_TITLE = null;
	private String SORT_STR_TITLE_ARTIST = null;
	private String CAPTION_STRING = null;
	private Map<String, Boolean> HAS_ARTIST_MAP = new HashMap<>();

	private String artist;
	private String title;
	private String path;
	private Integer length;
	private Integer rating;
	private boolean usedAsHeraMorningSong;
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
		this.usedAsHeraMorningSong = record.getBoolean("usedAsHeraMorningSong", false);
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
		if (usedAsHeraMorningSong) {
			result.set("usedAsHeraMorningSong", usedAsHeraMorningSong);
		}
		result.set("fileExists", fileExists);
		return result;
	}

	private void resetPreComputations() {
		HASH_CODE = null;
		STRING_REPRESENTATION = null;
		STRING_REPRESENTATION_LOW = null;
		STRING_REPRESENTATION_LOW_TRIM = null;
		TITLE_LOW = null;
		TITLE_LOW_TRIM = null;
		TITLE_LOW_TRIM_NO_BRACKETS = null;
		PATH_LOW = null;
		ARTIST_LOW = null;
		SORT_STR_ARTIST_TITLE = null;
		SORT_STR_TITLE_ARTIST = null;
		CAPTION_STRING = null;
		HAS_ARTIST_MAP = new HashMap<>();
	}

	public String getArtist() {
		if (artist == null) {
			return "";
		}
		return artist;
	}

	public String getLowArtist() {
		if (ARTIST_LOW == null) {
			ARTIST_LOW = getArtist().toLowerCase();
		}
		return ARTIST_LOW;
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

		Boolean storedResult = HAS_ARTIST_MAP.get(potentialArtist);
		if (storedResult != null) {
			return storedResult;
		}

		boolean result = hasArtistInternal(potentialArtist);
		HAS_ARTIST_MAP.put(potentialArtist, result);
		return result;
	}

	private boolean hasArtistInternal(String potentialArtist) {

		boolean matchOnlyThatArtistExactly = false;
		if (potentialArtist.endsWith(" %ONLY%")) {
			potentialArtist = potentialArtist.substring(0, potentialArtist.length() - 7);
			matchOnlyThatArtistExactly = true;
		}

		String loArtist = getLowArtist();
		String loPotentialArtist = potentialArtist.toLowerCase();

		// matches Foo - Bar for argument "foo"
		if (loArtist.equals(loPotentialArtist)) {
			return true;
		}

		// if we are only interested in that artist exactly and no cooperations,
		// then return false if we did not match so far
		if (matchOnlyThatArtistExactly) {
			return false;
		}

		loArtist += " ";

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
		resetPreComputations();
	}

	public String getTitle() {
		if (title == null) {
			return "";
		}
		return title;
	}

	public String getLowTitle() {
		if (TITLE_LOW == null) {
			TITLE_LOW = getTitle().toLowerCase();
		}
		return TITLE_LOW;
	}

	public String getLowTrimTitle() {
		if (TITLE_LOW_TRIM == null) {
			TITLE_LOW_TRIM = getLowTitle().trim();
		}
		return TITLE_LOW_TRIM;
	}

	public String getLowTrimTitleWithoutBrackets() {
		if (TITLE_LOW_TRIM_NO_BRACKETS == null) {
			TITLE_LOW_TRIM_NO_BRACKETS = removeTitleBrackets(getLowTrimTitle());
		}
		return TITLE_LOW_TRIM_NO_BRACKETS;
	}

	public static String removeTitleBrackets(String cur) {
		if (cur.contains(" (")) {
			cur = cur.substring(0, cur.indexOf(" ("));
		}
		cur = cur.trim().toLowerCase();
		return cur;
	}

	public boolean hasTitle() {
		return title != null;
	}

	public void setTitle(String title) {
		this.title = title;
		resetPreComputations();
	}

	public String getPath() {
		return path;
	}

	public String getLowPath() {
		if (PATH_LOW == null) {
			PATH_LOW = getPath().toLowerCase();
		}
		return PATH_LOW;
	}

	public String getArtistTitleSortStr() {
		if (SORT_STR_ARTIST_TITLE == null) {
			SORT_STR_ARTIST_TITLE = getLowArtist() + " - " + getLowTitle();
		}
		return SORT_STR_ARTIST_TITLE;
	}

	public String getTitleArtistSortStr() {
		if (SORT_STR_TITLE_ARTIST == null) {
			SORT_STR_TITLE_ARTIST = getLowTitle() + " - " + getLowArtist();
		}
		return SORT_STR_TITLE_ARTIST;
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

		// the order matters, as we want to display it
		boolean orderMatters = true;
		boolean showParentLists = false;
		List<Record> playlistsWithThisSong = songCtrl.getPlaylistsContainingSong(this, allPlaylists, orderMatters, showParentLists);
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

	public String getPlaylistText(SongCtrl songCtrl, List<Record> allPlaylists) {
		StringBuilder result = new StringBuilder();

		// the order matters, as we want to display it
		boolean orderMatters = true;
		boolean showParentLists = false;
		List<Record> playlistsWithThisSong = songCtrl.getPlaylistsContainingSong(this, allPlaylists, orderMatters, showParentLists);
		if (playlistsWithThisSong.size() < 1) {
			result.append("(not included in any playlists)");
		}
		String sep = "";
		for (Record playlistRecord : playlistsWithThisSong) {
			result.append(sep);
			sep = ", ";
			result.append(playlistRecord.getString(SongCtrl.PLAYLIST_NAME_KEY));
		}
		return result.toString();
	}

	public void setPath(String path) {
		this.path = path;
		resetPreComputations();
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
		if (STRING_REPRESENTATION == null) {
			if (hasArtist()) {
				STRING_REPRESENTATION = getArtist() + " - " + getTitle();
			} else {
				STRING_REPRESENTATION = getTitle();
			}
		}
		return STRING_REPRESENTATION;
	}

	public String toLowString() {
		if (STRING_REPRESENTATION_LOW == null) {
			STRING_REPRESENTATION_LOW = toString().toLowerCase();
		}
		return STRING_REPRESENTATION_LOW;
	}

	public String toLowTrimString() {
		if (STRING_REPRESENTATION_LOW_TRIM == null) {
			STRING_REPRESENTATION_LOW_TRIM = toLowString().trim();
		}
		return STRING_REPRESENTATION_LOW_TRIM;
	}

	public String getCaptionString(SongCtrl songCtrl, List<Record> allPlaylists) {

		if (CAPTION_STRING == null) {

			// the order does not matter, as we just want to get the amount anyway
			boolean orderMatters = false;
			boolean showParentLists = false;
			List<Record> playlistsWithThisSong = songCtrl.getPlaylistsContainingSong(this, allPlaylists, orderMatters, showParentLists);
			String heraStr = "";
			if (usedAsHeraMorningSong) {
				heraStr = "(H) ";
			}

			if (playlistsWithThisSong.size() < 1) {
				CAPTION_STRING = heraStr + toString() + "   (not included in any playlists)";
			} else {
				CAPTION_STRING = heraStr + toString() + "   (included in " + playlistsWithThisSong.size() + " playlist" +
					((playlistsWithThisSong.size() == 1) ? "" : "s") + ")";
			}
		}

		return CAPTION_STRING;
	}

	/**
	 * Compares if the two things point to the same path and are therefore the same
	 */
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
			return otherSong.getLowPath().equals(getLowPath());
		}
		return false;
	}

	/**
	 * Compares if the other song and this one have the same artist and title,
	 * and are therefore factually same-ish, even though they may NOT point to
	 * the same path on disk - used to find multiples in the database!
	 */
	public boolean is(Song other) {

		if (other == null) {
			return false;
		}

		if (!getLowArtist().equals(other.getLowArtist())) {
			return false;
		}

		if (!getLowTitle().equals(other.getLowTitle())) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		if (HASH_CODE == null) {
			if (path == null) {
				HASH_CODE = 0;
			} else {
				HASH_CODE = path.hashCode();
			}
		}
		return HASH_CODE;
	}

}
