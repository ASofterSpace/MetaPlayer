/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.utils.MathUtils;
import com.asofterspace.toolbox.utils.Record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class SongCtrl {

	public final static String PLAYLIST_SONGS_KEY = "songs";
	public final static String PLAYLIST_ARTISTS_KEY = "artists";
	public final static String PLAYLIST_EXTENDS_KEY = "extends";
	public final static String PLAYLIST_EXT_MIN_RATING_KEY = "extendMinimumRating";
	public final static String PLAYLIST_NAME_KEY = "name";
	public final static String PLAYLIST_SUBLISTS_KEY = "sublists";

	private ConfigFile songConfig;

	// all the songs that this SongCtrl has loaded
	private List<Song> allSongs;

	// the songs that are currently being played, e.g. the current playlist,
	// or all songs, or all songs of a certain artist, etc.
	private List<Song> currentSongs;

	private Random randomizer;

	private GUI gui;


	public SongCtrl() throws JsonParseException {

		songConfig = new ConfigFile("songs", true);

		// create a default config file, if necessary
		if (songConfig.getAllContents().isEmpty()) {
			songConfig.setAllContents(Record.emptyArray());
		}

		JsonFile backupSongConf = new JsonFile(songConfig.getParentDirectory(),
			"songs_backup_" + MathUtils.randomInteger(10) + ".cnf");
		backupSongConf.save(songConfig.getAllContents());

		JSON songRecordContainer = songConfig.getAllContents();

		List<Record> songRecords = songRecordContainer.getValues();

		allSongs = new ArrayList<>();

		for (Record record : songRecords) {
			Song song = new Song(record);
			allSongs.add(song);
		}

		randomizer = new Random();

		selectAllSongs();
	}

	public void setGUI(GUI gui) {
		this.gui = gui;
	}

	public void selectAllSongs() {

		currentSongs = new ArrayList<>();

		for (Song song : allSongs) {
			currentSongs.add(song);
		}
	}

	public List<Song> getTopRatedSongs(int amount) {
		List<Song> sortedSongs = new ArrayList<>(allSongs);

		Collections.sort(sortedSongs, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				return b.getRatingInt() - a.getRatingInt();
			}
		});

		return sortedSongs.subList(0, amount);
	}

	public List<Song> getTopPlayedSongs(int amount) {
		List<Song> sortedSongs = new ArrayList<>(allSongs);

		Collections.sort(sortedSongs, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				// sort by play amount...
				int res = b.getPlayAmount() - a.getPlayAmount();
				if (res != 0) {
					return res;
				}
				// ... but in case of same play amount, sort by rating
				return b.getRatingInt() - a.getRatingInt();
			}
		});

		return sortedSongs.subList(0, amount);
	}

	public void selectSongsOfPlaylistsWithCurSong(Song currentlyPlayedSong, List<Record> allPlaylists) {

		currentSongs = new ArrayList<>();
		currentSongs.add(currentlyPlayedSong);

		boolean orderMatters = false;
		boolean includeParentLists = true;

		List<Record> playlistsContainingSong = getPlaylistsContainingSong(
			currentlyPlayedSong, allPlaylists, orderMatters, includeParentLists);

		List<Song> songs = new ArrayList<>();

		for (Record playlist : playlistsContainingSong) {
			songs.addAll(getSongsForPlaylist(playlist, allPlaylists, allSongs));
		}

		for (Song song : songs) {
			if (!currentSongs.contains(song)) {
				currentSongs.add(song);
			}
		}
	}

	public void selectSongsOfArtist(String artist) {

		currentSongs = new ArrayList<>();

		for (Song song : allSongs) {
			if (song.hasArtist(artist)) {
				if (!currentSongs.contains(song)) {
					currentSongs.add(song);
				}
			}
		}
	}

	public void selectSongsOfArtists(List<String> artists) {

		currentSongs = new ArrayList<>();

		if (artists == null) {
			return;
		}

		for (Song song : allSongs) {
			for (String artist : artists) {
				if (song.hasArtist(artist)) {
					if (!currentSongs.contains(song)) {
						currentSongs.add(song);
					}
				}
			}
		}
	}

	public void selectSongsOfArtistNameStartingWith(char startWithLetter) {

		currentSongs = new ArrayList<>();

		if (startWithLetter == '*') {

			for (Song song : allSongs) {
				if (song.hasArtistNameStartingWithNonLetter()) {
					if (!currentSongs.contains(song)) {
						currentSongs.add(song);
					}
				}
			}

		} else {

			startWithLetter = Character.toLowerCase(startWithLetter);

			for (Song song : allSongs) {
				if (song.hasArtistNameStartingWithLowLetter(startWithLetter)) {
					if (!currentSongs.contains(song)) {
						currentSongs.add(song);
					}
				}
			}
		}
	}

	public void selectSongsByName(String title) {

		currentSongs = new ArrayList<>();

		if (title == null) {
			return;
		}

		title = Song.removeTitleBrackets(title);

		for (Song song : allSongs) {
			String songTitle = song.getLowTrimTitleWithoutBrackets();

			if (title.equals(songTitle)) {
				if (!currentSongs.contains(song)) {
					currentSongs.add(song);
				}
			}
		}
	}

	public void selectPlaylist(Record playlistToSelect, List<Record> allPlaylists) {

		currentSongs = getSongsForPlaylist(playlistToSelect, allPlaylists, allSongs);
	}

	public List<Record> getPlaylistsContainingSong(Song song, List<Record> allPlaylists, boolean orderMatters,
		boolean includeParentLists) {

		// we are only interested in whether this one song is in there or not
		List<Song> allConsideredSongs = new ArrayList<>();
		allConsideredSongs.add(song);

		List<Record> result = new ArrayList<>();
		for (Record playlistRecord : allPlaylists) {
			List<Song> songs = getSongsForPlaylist(playlistRecord, allPlaylists, allConsideredSongs);
			if (songs.contains(song)) {
				result.add(playlistRecord);
			}
		}

		// keep mostly the order as it comes from the playlist record,
		// but do make some changes
		if (orderMatters) {
			List<Record> resultBegin = new ArrayList<>();
			List<Record> resultEnd = new ArrayList<>();
			for (Record playlistRec : result) {
				if (playlistRec.getString(PLAYLIST_NAME_KEY).contains("Karaoke")) {
					resultEnd.add(playlistRec);
				} else {
					resultBegin.add(playlistRec);
				}
			}
			result = resultBegin;
			result.addAll(resultEnd);
		}

		// if parent lists are not wanted, filter them out!
		if (!includeParentLists) {
			List<Record> newResult = new ArrayList<>();
			for (Record curPlaylist : result) {
				List<Record> sublists = curPlaylist.getArray(PLAYLIST_SUBLISTS_KEY);
				if (sublists.size() < 1) {
					newResult.add(curPlaylist);
				}
			}
			result = newResult;
		}

		return result;
	}

	// in here, allPlaylists is a flattened list of all playlists - also sublists have been recursively added to it
	// so one playlist can extend from another one's sublist easy peasy
	// buuut if you are missing a song in a playlist that extends another, check that the song's rating is above the minimum
	// that is selected for the extension ;)
	// also, we still have to explicitly go over the sublists in case we extend from a parent, and the song is in a sublist!
	private List<Song> getSongsForPlaylist(Record playlist, List<Record> allPlaylists, List<Song> allConsideredSongs) {

		List<Song> result = new ArrayList<>();

		List<String> songList = playlist.getArrayAsStringList(PLAYLIST_SONGS_KEY);

		for (String curSongStr : songList) {
			boolean foundOne = false;
			String curSongLoStr = curSongStr.toLowerCase();
			for (Song song : allConsideredSongs) {
				if (song.toLowString().equals(curSongLoStr)) {
					foundOne = true;
					if (!result.contains(song)) {
						result.add(song);
					}
				}
			}
			// sometimes we only give a single song as input instead of all songs - do not complain then!
			if ((!foundOne) && (allConsideredSongs.size() > 1)) {
				System.out.println("Playlist '" + playlist.getString(PLAYLIST_NAME_KEY) + "' contains song '" +
					curSongStr + "' - but this song does not exist!");
			}
		}

		List<String> artistList = playlist.getArrayAsStringList(PLAYLIST_ARTISTS_KEY);

		for (String curArtistStr : artistList) {
			boolean foundOne = false;
			for (Song song : allConsideredSongs) {
				if (song.hasArtist(curArtistStr)) {
					foundOne = true;
					if (!result.contains(song)) {
						result.add(song);
					}
				}
			}
			// sometimes we only give a single song as input instead of all songs - do not complain then!
			if ((!foundOne) && (allConsideredSongs.size() > 1)) {
				System.out.println("Playlist '" + playlist.getString(PLAYLIST_NAME_KEY) + "' contains songs " +
					"from artist '" + curArtistStr + "' - but this artist does not exist!");
			}
		}

		for (Record sublistRec : playlist.getArray(PLAYLIST_SUBLISTS_KEY)) {
			for (Song song : getSongsForPlaylist(sublistRec, allPlaylists, allConsideredSongs)) {
				if (!result.contains(song)) {
					result.add(song);
				}
			}
		}

		Integer extMinRating = playlist.getInteger(PLAYLIST_EXT_MIN_RATING_KEY);

		for (String extendedPlaylist : playlist.getArrayAsStringList(PLAYLIST_EXTENDS_KEY)) {
			for (Record curPlaylist : allPlaylists) {
				if (curPlaylist.getString(PLAYLIST_NAME_KEY).equals(extendedPlaylist)) {
					for (Song song : getSongsForPlaylist(curPlaylist, allPlaylists, allConsideredSongs)) {

						// if there is an extend minimum rating...
						if (extMinRating != null) {
							// ... and if the song has a rating (songs without ratings are assumed to be awesome)...
							if (song.hasRating()) {
								// ... and if the song's rating is below it...
								if (song.getRatingInt() < extMinRating) {
									// do not add it to the playlist
									continue;
								}
							}
						}

						// if the playlist does not already contain the song, add it
						if (!result.contains(song)) {
							result.add(song);
						}
					}
				}
			}
		}

		return result;
	}

	public void randomize() {
		Collections.shuffle(currentSongs, randomizer);
	}

	public void uniquify() {
		List<Song> newSongs = new ArrayList<>();
		List<String> encounteredSongs = new ArrayList<>();
		for (Song song : currentSongs) {
			String songStr = song.toLowTrimString();
			if (!encounteredSongs.contains(songStr)) {
				encounteredSongs.add(songStr);
				newSongs.add(song);
			}
		}
		currentSongs = newSongs;
	}

	public void sort(final SortCriterion criterion) {
		Collections.sort(currentSongs, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				switch (criterion) {
					case ARTIST:
						return a.getArtistTitleSortStr().compareTo(b.getArtistTitleSortStr());
					case TITLE:
						return a.getTitleArtistSortStr().compareTo(b.getTitleArtistSortStr());
					default:
						return b.getRatingInt() - a.getRatingInt();
				}
			}
		});
	}

	public void invertOrder() {
		Collections.reverse(currentSongs);
	}

	public void purgeNonExisting() {

		boolean foundOne = false;
		StringBuilder purgeList = new StringBuilder();
		for (int j = allSongs.size() - 1; j >= 0; j--) {
			if (!allSongs.get(j).getFileExists()) {
				purgeList.append(allSongs.get(j).toString());
				purgeList.append("\n");
				allSongs.remove(j);
				foundOne = true;
			}
		}
		if (foundOne) {
			GuiUtils.notify("Purged:\n" + purgeList.toString());
		} else {
			GuiUtils.notify("Nothing to purge, all good!");
		}
	}

	public void purgeNotActuallySongs() {

		boolean foundOne = false;
		StringBuilder purgeList = new StringBuilder();
		for (int j = allSongs.size() - 1; j >= 0; j--) {
			if (!fileIsASong(allSongs.get(j).getLowPath())) {
				purgeList.append(allSongs.get(j).toString());
				purgeList.append("\n");
				allSongs.remove(j);
				foundOne = true;
			}
		}
		if (foundOne) {
			GuiUtils.notify("Purged:\n" + purgeList.toString());
		} else {
			GuiUtils.notify("Nothing to purge, all good!");
		}
	}

	/**
	 * Ensure that every song is only present once;
	 * if a song is present several times, take the longest length
	 * and the highest score :)
	 */
	public void cullMultiples() {

		// remove previous (2) multiplicity markers
		for (Song song : allSongs) {
			if (song.getTitle() != null) {
				while (song.getTitle().endsWith(" (2)")) {
					song.setTitle(song.getTitle().substring(0, song.getTitle().length() - 4));
				}
			}
		}

		// actually do the culling - that is, remove multiple entries currentSongs that are the same file
		// and add (2) behind song titles that have the same artist + title combination otherwise
		// but are *different* files
		boolean continueCulling = true;
		int index = 0;

		while (continueCulling) {

			continueCulling = false;

			while (index < allSongs.size()) {
				if (culledSong(allSongs.get(index), index)) {
					continueCulling = true;
					break;
				}
				index++;
			}
		}
	}

	/**
	 * Tries to cull a song and returns true if it actually managed to do so
	 */
	private boolean culledSong(Song curSong, int index) {

		for (int j = index + 1; j < allSongs.size(); j++) {
			Song otherSong = allSongs.get(j);
			if (curSong.equals(otherSong)) {
				allSongs.remove(j);
				int currentSongsJ = currentSongs.indexOf(otherSong);
				if (currentSongsJ >= 0) {
					currentSongs.remove(currentSongsJ);
				}
				// if this song's artist or title are missing, take the artist/title pair of the other song
				if (otherSong.hasArtist()) {
					if (!curSong.hasArtist()) {
						curSong.setArtist(otherSong.getArtist());
						curSong.setTitle(otherSong.getTitle());
					}
				}
				if (otherSong.hasTitle()) {
					if (!curSong.hasTitle()) {
						curSong.setArtist(otherSong.getArtist());
						curSong.setTitle(otherSong.getTitle());
					}
				}
				// take the longer length...
				if (otherSong.hasLength()) {
					if (!curSong.hasLength()) {
						curSong.setLength(otherSong.getLength());
					}
					if (curSong.getLength() < otherSong.getLength()) {
						curSong.setLength(otherSong.getLength());
					}
				}
				// ... and the higher rating
				if (otherSong.hasRating()) {
					if (curSong.hasRating()) {
						if (curSong.getRatingInt() < otherSong.getRatingInt()) {
							curSong.setRating(otherSong.getRating());
						}
					} else {
						curSong.setRating(otherSong.getRating());
					}
				}
				return true;
			}
			if (curSong.is(otherSong)) {
				otherSong.setTitle(otherSong.getTitle() + " (2)");
			}
		}

		return false;
	}

	public List<Song> getSongs() {
		return currentSongs;
	}

	public Song getSong(Integer index) {
		if ((index != null) && (index >= 0) && (index < currentSongs.size())) {
			return currentSongs.get(index);
		}
		return null;
	}

	public int getSongPosition(Song song) {

		if (song == null) {
			return 0;
		}

		for (int i = 0; i < currentSongs.size(); i++) {
			if (song.equals(currentSongs.get(i))) {
				return i;
			}
		}

		return 0;
	}

	public int getSongAmount() {
		return currentSongs.size();
	}

	public void add(Song song) {
		allSongs.add(song);
	}

	public void addUnlessAlreadyPresent(Song song) {

		for (Song curSong : allSongs) {
			if (song.equals(curSong)) {
				return;
			}
		}

		add(song);
	}

	private int getAverageRatingOfPlaylistIfNeeded() {
		if (!gui.isAverageRatingOfPlaylistNeeded()) {
			return 0;
		}

		int counter = 0;
		int amount = 0;
		for (Song song : currentSongs) {
			Integer curRating = song.getRating();
			if (curRating != null) {
				counter += curRating;
				amount++;
			}
		}

		return counter / amount;
	}

	public Song getPreviousSong(Song currentlyPlayedSong) {

		if (currentSongs.size() < 1) {
			return null;
		}

		int startIteratingFrom = 0;

		// iterate over all songs...
		for (int i = 0; i < currentSongs.size(); i++) {
			// ... and once we found the current song...
			if (currentSongs.get(i).equals(currentlyPlayedSong)) {

				// ... keep on iterating from the next one ...
				startIteratingFrom = i-1;
				break;
			}
		}
		if (startIteratingFrom < 0) {
			startIteratingFrom = currentSongs.size() - 1;
		}

		int averageRatingOfPlaylist = getAverageRatingOfPlaylistIfNeeded();

		// ... until we find a song that is not being skipped...
		for (int j = startIteratingFrom; j >= 0; j--) {
			if (!gui.skippingSong(currentSongs.get(j), averageRatingOfPlaylist)) {
				// ... and then play it!
				return currentSongs.get(j);
			}
		}

		// on the other hand, if we found no song to play after the current song, retry from the beginning
		for (int j = currentSongs.size() - 1; j > startIteratingFrom; j--) {
			if (!gui.skippingSong(currentSongs.get(j), averageRatingOfPlaylist)) {
				// ... and then play it!
				return currentSongs.get(j);
			}
		}

		// alrighty, ALL songs that we found are being skipped - so ignore skipping this time, just go for the next one!
		if (startIteratingFrom >= 0) {
			return currentSongs.get(startIteratingFrom);
		}
		if (0 < currentSongs.size()) {
			return currentSongs.get(0);
		}

		// finally give up, if we again found no playable song, even without any skipping at all!
		return null;
	}

	public Song getNextSong(Song currentlyPlayedSong) {

		if (currentSongs.size() < 1) {
			return null;
		}

		int startIteratingFrom = 0;

		// iterate over all songs...
		for (int i = 0; i < currentSongs.size() - 1; i++) {
			// ... and once we found the current song...
			if (currentSongs.get(i).equals(currentlyPlayedSong)) {

				// ... keep on iterating from the next one ...
				startIteratingFrom = i+1;
				break;
			}
		}

		int averageRatingOfPlaylist = getAverageRatingOfPlaylistIfNeeded();

		// ... until we find a song that is not being skipped...
		for (int j = startIteratingFrom; j < currentSongs.size(); j++) {
			if (!gui.skippingSong(currentSongs.get(j), averageRatingOfPlaylist)) {
				// ... and then play it!
				return currentSongs.get(j);
			}
		}

		// on the other hand, if we found no song to play after the current song, retry from the beginning
		for (int j = 0; j < startIteratingFrom; j++) {
			if (!gui.skippingSong(currentSongs.get(j), averageRatingOfPlaylist)) {
				// ... and then play it!
				return currentSongs.get(j);
			}
		}

		// alrighty, ALL songs that we found are being skipped - so ignore skipping this time, just go for the next one!
		if (startIteratingFrom < currentSongs.size()) {
			return currentSongs.get(startIteratingFrom);
		}
		if (0 < currentSongs.size()) {
			return currentSongs.get(0);
		}

		// finally give up, if we again found no playable song, even without any skipping at all!
		return null;
	}

	public void resetTopPlayAmounts() {
		for (Song song : allSongs) {
			song.resetPlayAmount();
		}
	}

	/**
	 * Gets a list of artists that have performed the most songs, without knowing in advance
	 * how many songs that would be - but no more than a certain maximum amount
	 */
	public List<Artist> getTopArtists(int maxAmount) {
		return getTopArtists(maxAmount, null);
	}

	public List<Artist> getTopArtists(int maxAmount, Character bucketChar) {

		List<Artist> result = new ArrayList<>();

		for (Song song : allSongs) {

			List<String> curArtists = song.getArtists();

			for (String curArtistStr : curArtists) {

				if ((curArtistStr == null) || (curArtistStr.length() == 0)) {
					continue;
				}

				if (bucketChar != null) {
					char firstChar = curArtistStr.toUpperCase().charAt(0);
					if (bucketChar == '*') {
						// "other" bucket
						if ((firstChar >= 'A') && (firstChar <= 'Z')) {
							continue;
						}
					} else {
						if (firstChar != bucketChar) {
							continue;
						}
					}
				}

				Artist curArtist = new Artist(curArtistStr);
				int contained = result.indexOf(curArtist);
				if (contained >= 0) {
					result.get(contained).addSong();
				} else {
					result.add(curArtist);
				}
			}
		}

		Collections.sort(result, new Comparator<Artist>() {
			public int compare(Artist a, Artist b) {
				return b.getSongAmount() - a.getSongAmount();
			}
		});

		if (result.size() > maxAmount) {
			return result.subList(0, maxAmount);
		}
		return result;
	}

	public Record getSongData() {

		Collections.sort(allSongs, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				return a.toLowString().compareTo(b.toLowString());
			}
		});

		JSON result = new JSON();
		result.makeArray();

		for (Song song : allSongs) {
			result.append(song.toRecord());
		}

		return result;
	}

	public void save() {
		songConfig.setAllContents(getSongData());
	}

	public static boolean fileIsASong(String filename) {
		filename = filename.toLowerCase();
		return !(filename.endsWith(".jpg") ||
				 filename.endsWith(".jpeg") ||
				 filename.endsWith(".png") ||
				 filename.endsWith(".gif") ||
				 filename.endsWith(".bmp") ||
				 filename.endsWith(".webp") ||
				 filename.endsWith(".txt") ||
				 filename.endsWith(".ini") ||
				 filename.endsWith(".lnk") ||
				 filename.endsWith(".srt") ||
				 filename.endsWith(".pdf") ||
				 filename.endsWith(".wpl") ||
				 filename.endsWith(".orig"));
	}

}
