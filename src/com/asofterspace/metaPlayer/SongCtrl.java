/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.Record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class SongCtrl {

	private ConfigFile songConfig;

	private List<Song> songs;

	private Random randomizer;


	public SongCtrl() {

		songConfig = new ConfigFile("songs", true);

		// create a default config file, if necessary
		if (songConfig.getAllContents().isEmpty()) {
			songConfig.setAllContents(new JSON("[]"));
		}

		JSON songRecordContainer = songConfig.getAllContents();

		List<Record> songRecords = songRecordContainer.getValues();

		songs = new ArrayList<>();

		for (Record record : songRecords) {
			Song song = new Song(record);
			songs.add(song);
		}

		randomizer = new Random();
	}

	public void randomize() {
		Collections.shuffle(songs, randomizer);
	}

	public void sort() {
		Collections.sort(songs, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				return a.toString().toLowerCase().compareTo(b.toString().toLowerCase());
			}
		});
	}

	/**
	 * Ensure that every song is only present once;
	 * if a song is present several times, take the longest length
	 * and the highest score :)
	 */
	public void cullMultiples() {

		boolean continueCulling = true;
		int index = 0;

		while (continueCulling) {

			continueCulling = false;

			while (index < songs.size()) {
				if (culledSong(songs.get(index), index)) {
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

		for (int j = index + 1; j < songs.size(); j++) {
			Song otherSong = songs.get(j);
			if (curSong.equals(otherSong)) {
				songs.remove(j);
				// if this song's artist or title are missing, take the artist/title pair of the other song
				if (otherSong.getArtist() != null) {
					if (curSong.getArtist() == null) {
						curSong.setArtist(otherSong.getArtist());
						curSong.setTitle(otherSong.getTitle());
					}
				}
				if (otherSong.getTitle() != null) {
					if (curSong.getTitle() == null) {
						curSong.setArtist(otherSong.getArtist());
						curSong.setTitle(otherSong.getTitle());
					}
				}
				// take the longer length...
				if (otherSong.getLength() != null) {
					if (curSong.getLength() == null) {
						curSong.setLength(otherSong.getLength());
					}
					if (curSong.getLength() < otherSong.getLength()) {
						curSong.setLength(otherSong.getLength());
					}
				}
				// ... and the higher rating
				if (otherSong.getRating() != null) {
					if (curSong.getRating() == null) {
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
		return songs;
	}

	public int getSongAmount() {
		return songs.size();
	}

	public void add(Song song) {
		songs.add(song);
	}

	public void addUnlessAlreadyPresent(Song song) {

		for (Song curSong : songs) {
			if (song.equals(curSong)) {
				return;
			}
		}

		add(song);
	}

	public Song getPreviousSong(Song currentlyPlayedSong) {

		for (int i = 0; i < songs.size() - 1; i++) {
			if (songs.get(i).equals(currentlyPlayedSong)) {
				return songs.get(i-1);
			}
		}

		if (songs.size() > 0) {
			return songs.get(songs.size() - 1);
		}

		return null;
	}

	public Song getNextSong(Song currentlyPlayedSong) {

		for (int i = 0; i < songs.size() - 1; i++) {
			if (songs.get(i).equals(currentlyPlayedSong)) {
				return songs.get(i+1);
			}
		}

		if (songs.size() > 0) {
			return songs.get(0);
		}

		return null;
	}

	public Record getSongData() {

		JSON result = new JSON();
		result.makeArray();

		for (Song song : songs) {
			result.append(song.toRecord());
		}

		return result;
	}

	public void save() {
		songConfig.setAllContents(getSongData());
	}
}
