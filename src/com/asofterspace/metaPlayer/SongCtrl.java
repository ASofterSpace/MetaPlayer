/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.Record;

import java.util.ArrayList;
import java.util.List;


public class SongCtrl {

	private List<Song> songs;


	public SongCtrl(JSON config) {
		List<Record> songRecords = config.getValues();

		songs = new ArrayList<>();

		for (Record record : songRecords) {
			Song song = new Song(record);
			songs.add(song);
		}
	}

	public Record getData() {

		JSON result = new JSON();
		result.makeArray();

		for (Song song : songs) {
			result.append(song.toRecord());
		}

		return result;
	}
}
