/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.Utils;

import java.util.List;

import javax.swing.SwingUtilities;


public class Main {

	public final static String PROGRAM_TITLE = "MetaPlayer";
	public final static String VERSION_NUMBER = "0.0.0.3(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "25. September 2019 - 30. September 2019";

	private static ConfigFile config;

	private static TimingCtrl timingCtrl;
	private static PlayerCtrl playerCtrl;
	private static SongCtrl songCtrl;


	/**
	 * TODO:
	 * create importer for legacy metaplayer playlists
	 * add playlist selection - playlists are either all music, or just a specific selection of existing songs
	 * add more detailed ratings (nice video, energetic music, etc.) and metadata (e.g. language)
	 * create importer to add new songs
	 * create search tool that searches through music folders and checks if any songs are missing from the songs.cnf
	 */
	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		if (args.length > 0) {
			if (args[0].equals("--version")) {
				System.out.println(Utils.getFullProgramIdentifierWithDate());
				return;
			}

			if (args[0].equals("--version_for_zip")) {
				System.out.println("version " + Utils.getVersionNumber());
				return;
			}
		}

		// load config
		config = new ConfigFile("settings", true);

		// create a default config file, if necessary
		if (config.getAllContents().isEmpty()) {
			config.setAllContents(new JSON("{\"" + PlayerCtrl.EXT_PLAYER_ASSOC_KEY + "\":[],\"playlists\":[]}"));
		}

		// create timer
		timingCtrl = new TimingCtrl();

		// load player associations
		playerCtrl = new PlayerCtrl(config.getAllContents());

		// load songs
		songCtrl = new SongCtrl();

		System.out.println("All songs have been loaded; MetaPlayer ready!");

		SwingUtilities.invokeLater(new GUI(timingCtrl, playerCtrl, songCtrl, config));
	}

}
