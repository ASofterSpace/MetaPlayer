/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.utils.DateHolder;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Song {

	public final static String DASH = "-";
	public final static String SPACED_DASH = " " + DASH + " ";
	public final static String WONKY_DASH_1 = "–";
	public final static String WONKY_DASH_2 = "—";

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
	private List<String> ARTIST_LIST = null;
	private List<String> ARTIST_LIST_LOW = null;

	private String artist;
	private String title;
	private String path;
	private Integer length;
	private Integer rating;
	private boolean usedAsMorningSong;
	private DateHolder usedAsMorningSongDate;
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

		this.path = path;
		initFromPathArtistTitle(path, artistAndTitle);

		renameFileIfNecessary();
	}

	private void initFromPathArtistTitle(String path, String artistAndTitle) {

		if ((artistAndTitle == null) || !artistAndTitle.contains(SPACED_DASH)) {
			String altSongName = path;
			if (altSongName.contains("\\")) {
				altSongName = altSongName.substring(altSongName.lastIndexOf("\\") + 1);
			}
			if (altSongName.contains("/")) {
				altSongName = altSongName.substring(altSongName.lastIndexOf("/") + 1);
			}
			if ((artistAndTitle == null) || altSongName.contains(SPACED_DASH)) {
				artistAndTitle = altSongName;
			}
		}
		String[] songNames = artistAndTitle.split(SPACED_DASH);
		title = null;
		if (songNames.length > 1) {
			artist = songNames[0];
			if (songNames.length > 2) {
				title = songNames[1] + SPACED_DASH + songNames[2];
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
				title.endsWith(".wav")) {
				title = title.substring(0, title.length() - 4);
			} else {
				if (title.endsWith(".webm")) {
					title = title.substring(0, title.length() - 5);
				}
			}
		}
	}

	public Song(Record record) {
		this.artist = record.getString("artist");
		this.title = record.getString("title");
		this.path = record.getString("path");

		renameFileIfNecessary();

		this.length = record.getInteger("length");
		this.rating = record.getInteger("rating");
		this.usedAsMorningSong = record.getBoolean("usedAsMorningSong", false);
		this.usedAsMorningSongDate = record.getDateHolder("usedAsMorningSongDate");
		this.playAmount = record.getInteger("playAmount");
		this.fileExists = (new File(this.path)).exists();
	}

	private void renameFileIfNecessary() {
		if (path.contains(WONKY_DASH_1) || path.contains(WONKY_DASH_2)) {
			String newpath = path;
			newpath = StrUtils.replaceAll(newpath, WONKY_DASH_1, DASH);
			newpath = StrUtils.replaceAll(newpath, WONKY_DASH_2, DASH);
			try {
				System.out.println("Moving " + path + " to " + newpath + "...");
				Files.move(Paths.get(path), Paths.get(newpath));
			} catch (IOException e) {
				System.out.println("Oops - could not move " + path + " to " + newpath + "!");
			}
			setPath(newpath);
			initFromPathArtistTitle(this.path, null);
		}
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
		if (usedAsMorningSong) {
			result.set("usedAsMorningSong", usedAsMorningSong);
		}
		if (usedAsMorningSongDate != null) {
			if (!usedAsMorningSongDate.getIsNull()) {
				result.set("usedAsMorningSongDate", usedAsMorningSongDate);
			}
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
		ARTIST_LIST = null;
		ARTIST_LIST_LOW = null;
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

		if (ARTIST_LIST != null) {
			return ARTIST_LIST;
		}

		List<String> ARTIST_LIST = new ArrayList<>();

		if (artist == null) {
			return ARTIST_LIST;
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
			ARTIST_LIST.add(cur.trim());
		}

		return ARTIST_LIST;
	}

	public List<String> getLowArtists() {

		if (ARTIST_LIST_LOW != null) {
			return ARTIST_LIST_LOW;
		}

		List<String> ARTIST_LIST_LOW = new ArrayList<>();

		for (String curArtist : getArtists()) {
			if (curArtist != null) {
				ARTIST_LIST_LOW.add(curArtist.toLowerCase());
			}
		}
		return ARTIST_LIST_LOW;
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

	public boolean hasArtistNameStartingWithLowLetter(char startWithLetter) {

		for (String curLowArtist : getLowArtists()) {
			if ((curLowArtist != null) && (curLowArtist.length() > 0)) {
				if (curLowArtist.charAt(0) == startWithLetter) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasArtistNameStartingWithNonLetter() {
		for (String curLowArtist : getLowArtists()) {
			if ((curLowArtist != null) && (curLowArtist.length() > 0)) {
				char firstChar = curLowArtist.charAt(0);
				if ((firstChar < 'a') || (firstChar > 'z')) {
					return true;
				}
			}
		}
		return false;
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

	public String getSubtitlePath() {
		String result = path;
		if ((result == null) || (!result.contains("."))) {
			return null;
		}
		result = result.substring(0, result.lastIndexOf(".")) + ".srt";
		File testFile = new File(result);
		if (testFile.exists()) {
			return result;
		}
		return null;
	}

	public String getArtistTitleSortStr() {
		if (SORT_STR_ARTIST_TITLE == null) {
			SORT_STR_ARTIST_TITLE = getLowArtist() + SPACED_DASH + getLowTitle();
		}
		return SORT_STR_ARTIST_TITLE;
	}

	public String getTitleArtistSortStr() {
		if (SORT_STR_TITLE_ARTIST == null) {
			SORT_STR_TITLE_ARTIST = getLowTitle() + SPACED_DASH + getLowArtist();
		}
		return SORT_STR_TITLE_ARTIST;
	}

	public String getClipboardText(SongCtrl songCtrl, List<Record> allPlaylists, boolean forMorningSong) {

		if ((!forMorningSong) && (clipboardText != null)) {
			return clipboardText;
		}

		StringBuilder result = new StringBuilder();
		result.append(toString());
		if (!forMorningSong) {
			result.append(" (" + getPath() + ")");
		}
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
		if (forMorningSong) {
			result.append("youtube " + toString());
		} else {
			result.append("cd ");
			Directory hereDir = new Directory(System.getProperty("java.class.path") + "/..");
			result.append(hereDir.getCanonicalDirname());
			result.append("\n");
			result.append("./run.sh --song \"" + toString() + "\"");
		}

		if (!forMorningSong) {
			clipboardText = result.toString();
			return clipboardText;
		}
		return result.toString();
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
				STRING_REPRESENTATION = getArtist() + SPACED_DASH + getTitle();
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
			if (usedAsMorningSong) {
				heraStr = "(M) ";
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

	public boolean getUsedAsMorningSong() {
		return usedAsMorningSong;
	}

	public void setUsedAsMorningSong(boolean usedAsMorningSong) {
		if (usedAsMorningSong) {
			if ((usedAsMorningSongDate == null) || usedAsMorningSongDate.getIsNull()) {
				usedAsMorningSongDate = DateUtils.nowHolder();
			}
		}
		this.usedAsMorningSong = usedAsMorningSong;
		resetPreComputations();
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
