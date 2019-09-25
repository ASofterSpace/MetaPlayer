/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.Record;

import java.util.ArrayList;
import java.util.List;


public class SongCtrl {

	private ConfigFile songConfig;

	private List<Song> songs;


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
	}

	public List<Song> getSongs() {
		return songs;
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
