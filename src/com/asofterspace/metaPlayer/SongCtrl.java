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

	public void selectSongsOfArtist(String artist) {

		currentSongs = new ArrayList<>();

		for (Song song : allSongs) {
			if (song.hasArtist(artist)) {
				currentSongs.add(song);
			}
		}
	}

	public void selectPlaylist(Record playlist) {

		currentSongs = new ArrayList<>();

		List<String> songList = playlist.getArrayAsStringList(PLAYLIST_SONGS_KEY);

		for (String curSongStr : songList) {
			String curSongLoStr = curSongStr.toLowerCase();
			for (Song song : allSongs) {
				if (song.toString().toLowerCase().equals(curSongLoStr)) {
					if (!currentSongs.contains(song)) {
						currentSongs.add(song);
					}
				}
			}
		}

		List<String> artistList = playlist.getArrayAsStringList(PLAYLIST_ARTISTS_KEY);

		for (String curArtistStr : artistList) {
			for (Song song : allSongs) {
				if (song.hasArtist(curArtistStr)) {
					if (!currentSongs.contains(song)) {
						currentSongs.add(song);
					}
				}
			}
		}
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
						return b.getRating() - a.getRating();
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
					if (!curSong.hasRating()) {
						curSong.setRating(otherSong.getRating());
					}
					if (curSong.getRating() < otherSong.getRating()) {
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

		// iterate over all songs...
		for (int i = 0; i < currentSongs.size() - 1; i++) {
			// ... and once we found the current song...
			if (currentSongs.get(i).equals(currentlyPlayedSong)) {
				int j = i+1;
				boolean escape = false;
				// ... keep on iterating until we find a song that is not being skipped...
				while (gui.skippingSong(currentSongs.get(j))) {
					j++;
					if (j >= currentSongs.size()) {
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

		// on the other hand, if we found no song to play after the current song, retry from the beginning
		int j = 0;
		while (gui.skippingSong(currentSongs.get(j))) {
			j++;
			if (j >= currentSongs.size()) {
				// finally give up, if we again found no playable song in the second iteration
				return null;
			}
		}
		return currentSongs.get(j);
	}

	/**
	 * Gets a list of artists that have performed the most songs, without knowing in advance
	 * how many songs that would be - but no more than a certain maximum amount
	 */
	public List<String> getTopArtists(int maxAmount) {

		List<Artist> allArtists = new ArrayList<>();

		for (Song song : allSongs) {
			String curArtistStr = song.getArtist();
			Artist curArtist = new Artist(curArtistStr);
			int contained = allArtists.indexOf(curArtist);
			if (contained >= 0) {
				allArtists.get(contained).addSong();
			} else {
				allArtists.add(curArtist);
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
