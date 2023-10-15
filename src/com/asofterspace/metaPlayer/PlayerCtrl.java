/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.utils.Record;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayerCtrl {

	public final static String EXT_PLAYER_ASSOC_KEY = "externalPlayerAssociations";

	private Map<String, String> assocs = new HashMap<>();


	public PlayerCtrl(JSON config) {
		List<Record> extPlayerAssocs = config.getArray(EXT_PLAYER_ASSOC_KEY);
		assocs = new HashMap<>();
		for (Record assoc : extPlayerAssocs) {
			assocs.put(assoc.getString("ext").toLowerCase(), assoc.getString("play"));
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

		System.out.println("No player associated with the file " + filename + " found!");

		return "";
	}
}
