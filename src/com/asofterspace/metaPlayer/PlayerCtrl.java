/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.utils.Record;

import java.util.List;


public class PlayerCtrl {

	public final static String EXT_PLAYER_ASSOC_KEY = "externalPlayerAssociations";

	private List<Record> extPlayerAssocs;


	public PlayerCtrl(JSON config) {
		extPlayerAssocs = config.getArray(EXT_PLAYER_ASSOC_KEY);
	}

	public String getPlayerForSong(Song song) {
		return getPlayerForFile(song.getPath());
	}

	public String getPlayerForFile(String filename) {

		for (Record assoc : extPlayerAssocs) {
			if (filename.endsWith(assoc.getString("ext"))) {
				return assoc.getString("play");
			}
		}

		System.out.println("No player associated with the file " + filename + " found!");

		return "";
	}
}
