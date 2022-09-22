/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
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

	public void selectSongsOfPlaylistsWithCurSong(Song currentlyPlayedSong, List<Record> allPlaylists) {

		currentSongs = new ArrayList<>();
		currentSongs.add(currentlyPlayedSong);

		List<Record> playlistsContainingSong = getPlaylistsContainingSong(
			currentlyPlayedSong, allPlaylists);

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

	public void selectSongsByName(String title) {

		currentSongs = new ArrayList<>();

		if (title == null) {
			return;
		}

		if (title.contains(" (")) {
			title = title.substring(0, title.indexOf(" ("));
		}

		title = title.toLowerCase().trim();

		for (Song song : allSongs) {
			String songTitle = song.getTitle();
			if (songTitle == null) {
				continue;
			}

			if (songTitle.contains(" (")) {
				songTitle = songTitle.substring(0, songTitle.indexOf(" ("));
			}

			songTitle = songTitle.toLowerCase().trim();

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

	public List<Record> getPlaylistsContainingSong(Song song, List<Record> allPlaylists) {
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
		return result;
	}

	private List<Song> getSongsForPlaylist(Record playlist, List<Record> allPlaylists, List<Song> allConsideredSongs) {

		List<Song> result = new ArrayList<>();

		List<String> songList = playlist.getArrayAsStringList(PLAYLIST_SONGS_KEY);

		for (String curSongStr : songList) {
			String curSongLoStr = curSongStr.toLowerCase();
			for (Song song : allConsideredSongs) {
				if (song.toString().toLowerCase().equals(curSongLoStr)) {
					if (!result.contains(song)) {
						result.add(song);
					}
				}
			}
		}

		List<String> artistList = playlist.getArrayAsStringList(PLAYLIST_ARTISTS_KEY);

		for (String curArtistStr : artistList) {
			for (Song song : allConsideredSongs) {
				if (song.hasArtist(curArtistStr)) {
					if (!result.contains(song)) {
						result.add(song);
					}
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

	public void sort(final SortCriterion criterion) {
		Collections.sort(currentSongs, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				String aStr;
				String bStr;
				switch (criterion) {
					case ARTIST:
						aStr = a.getArtist() + " - " + a.getTitle() + ")";
						bStr = b.getArtist() + " - " + b.getTitle() + ")";
						return aStr.toLowerCase().compareTo(bStr.toLowerCase());
					case TITLE:
						aStr = a.getTitle() + " (" + a.getArtist() + ")";
						bStr = b.getTitle() + " (" + b.getArtist() + ")";
						return aStr.toLowerCase().compareTo(bStr.toLowerCase());
					case RATING:
						return b.getRatingInt() - a.getRatingInt();
					default:
						return 0;
				}
			}
		});
	}

	public void invertOrder() {
		Collections.reverse(currentSongs);
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
			if (curSong.is(otherSong.getArtist(), otherSong.getTitle())) {
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

	public Song getPreviousSong(Song currentlyPlayedSong) {

		// iterate over all songs...
		for (int i = 1; i < currentSongs.size(); i++) {
			// ... and once we found the current song...
			if (currentSongs.get(i).equals(currentlyPlayedSong)) {
				int j = i-1;
				boolean escape = false;
				// ... keep on iterating until we find a song that is not being skipped...
				while (gui.skippingSong(currentSongs.get(j))) {
					j--;
					if (j < 0) {
						escape = true;
						break;
					}
				}
				if (escape) {
					break;
				}
				// ... and then play it!
				return currentSongs.get(j);
			}
		}

		// on the other hand, if we found no song to play after the current song, retry from the end
		int j = currentSongs.size() - 1;
		while (gui.skippingSong(currentSongs.get(j))) {
			j--;
			if (j < 0) {
				// finally give up, if we again found no playable song in the second iteration
				return null;
			}
		}
		return currentSongs.get(j);
	}

	public Song getNextSong(Song currentlyPlayedSong) {

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

		// ... until we find a song that is not being skipped...
		for (int j = startIteratingFrom; j < currentSongs.size(); j++) {
			if (!gui.skippingSong(currentSongs.get(j))) {
				// ... and then play it!
				return currentSongs.get(j);
			}
		}

		// on the other hand, if we found no song to play after the current song, retry from the beginning
		for (int j = 0; j < startIteratingFrom; j++) {
			if (!gui.skippingSong(currentSongs.get(j))) {
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

	/**
	 * Gets a list of artists that have performed the most songs, without knowing in advance
	 * how many songs that would be - but no more than a certain maximum amount
	 */
	public List<String> getTopArtists(int maxAmount) {
		return getTopArtists(maxAmount, null);
	}

	public List<String> getTopArtists(int maxAmount, String bucketName) {

		List<Artist> allArtists = new ArrayList<>();
		char bucketChar = '*';
		if (bucketName != null) {
			bucketChar = bucketName.toUpperCase().charAt(0);
		}

		for (Song song : allSongs) {

			List<String> curArtists = song.getArtists();

			for (String curArtistStr : curArtists) {

				if ((curArtistStr == null) || (curArtistStr.length() == 0)) {
					continue;
				}

				if (bucketName != null) {
					char firstChar = curArtistStr.toUpperCase().charAt(0);
					if (bucketName.length() == 1) {
						if (firstChar != bucketChar) {
							continue;
						}
					} else {
						// "other" bucket
						if ((firstChar >= 'A') && (firstChar <= 'Z')) {
							continue;
						}
					}
				}

				Artist curArtist = new Artist(curArtistStr);
				int contained = allArtists.indexOf(curArtist);
				if (contained >= 0) {
					allArtists.get(contained).addSong();
				} else {
					allArtists.add(curArtist);
				}
			}
		}

		Collections.sort(allArtists, new Comparator<Artist>() {
			public int compare(Artist a, Artist b) {
				return b.getSongAmount() - a.getSongAmount();
			}
		});

		List<String> result = new ArrayList<>();

		for (int i = 0; i < allArtists.size(); i++) {
			result.add(allArtists.get(i).getName());
			if (i >= maxAmount) {
				break;
			}
		}

		return result;
	}

	public Record getSongData() {

		Collections.sort(allSongs, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				return a.toString().toLowerCase().compareTo(b.toString().toLowerCase());
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
}
