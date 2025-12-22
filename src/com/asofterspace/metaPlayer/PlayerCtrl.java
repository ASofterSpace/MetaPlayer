/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.utils.Record;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayerCtrl {

	public final static String EXT_PLAYER_ASSOC_KEY = "externalPlayerAssociations";
	public final static String EXT_PLAYER_ASSOC_DEFAULT = "%DEFAULT%";

	private Map<String, String> assocs = new HashMap<>();

	private String defaultPlayer = null;


	public PlayerCtrl(Record config) {
		List<Record> extPlayerAssocs = config.getArray(EXT_PLAYER_ASSOC_KEY);
		assocs = new HashMap<>();
		for (Record assoc : extPlayerAssocs) {
			String key = assoc.getString("ext");
			String value = assoc.getString("play");
			if (EXT_PLAYER_ASSOC_DEFAULT.equals(key)) {
				this.defaultPlayer = value;
			} else {
				key = key.toLowerCase();
				assocs.put(key, value);
			}
		}
	}

	public String getPlayerForSong(Song song) {
		return getPlayerForFile(song.getPath());
	}

	public String getPlayerForFile(String filename) {

		String loFilename = filename.toLowerCase();

		for (Map.Entry<String, String> assoc : assocs.entrySet()) {
			if (loFilename.endsWith(assoc.getKey())) {
				return assoc.getValue();
			}
		}

		if (defaultPlayer != null) {
			return defaultPlayer;
		}

		System.out.println("No player associated with the file " + filename + " found, and no default specified!");

		return "";
	}
}
