/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.Utils;

import java.util.Date;

import javax.swing.SwingUtilities;


public class MetaPlayer {

	public final static String PROGRAM_TITLE = "MetaPlayer";
	public final static String VERSION_NUMBER = "0.0.1.3(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "25. September 2019 - 9. August 2022";

	private static ConfigFile config;

	private static TimingCtrl timingCtrl;
	private static PlayerCtrl playerCtrl;
	private static SongCtrl songCtrl;


	/**
	 * TODO:
	 * add search for any artist (maybe submenu with F... > Fo... > FooBarista)
	 * add more detailed ratings (nice video, energetic music, etc.) and metadata (e.g. language)
	 * create search tool that searches through music folders and checks if any songs are missing from the songs.cnf
	 * add software tests (e.g. for the Song > hasArtist function)
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

		try {
			// load config
			config = new ConfigFile("settings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON(
					"{\"" + PlayerCtrl.EXT_PLAYER_ASSOC_KEY + "\":[]," +
					"\"" + GUI.CONFIG_KEY_PLAYLISTS + "\":[]," +
					"\"" + GUI.CONFIG_KEY_MAIN_ARTISTS + "\":[]}"
				));
			}
		} catch (JsonParseException e) {
			System.err.println("Loading the settings failed:");
			System.err.println(e);
			System.exit(1);
		}

		// create timer
		timingCtrl = new TimingCtrl();

		// load player associations
		playerCtrl = new PlayerCtrl(config.getAllContents());

		try {
			// load songs
			songCtrl = new SongCtrl();
		} catch (JsonParseException e) {
			System.err.println("Loading the songs failed:");
			System.err.println(e);
			System.exit(1);
		}

		System.out.println("All songs have been loaded; MetaPlayer ready!");

		GUI gui = new GUI(timingCtrl, playerCtrl, songCtrl, config);
		songCtrl.setGUI(gui);
		SwingUtilities.invokeLater(gui);

		Date now = DateUtils.now();
		String topSongFileName = "top_songs_" + DateUtils.getYear(now) + "_" +
			StrUtils.leftPad0(DateUtils.getMonth(now) + "", 2) + ".json";
		JsonFile topSongs = new JsonFile(config.getParentDirectory(), topSongFileName);
		if (!topSongs.exists()) {
			Record rec = Record.emptyArray();
			for (Song song : songCtrl.getTopRatedSongs(256)) {
				rec.append(song.toRecord());
			}
			topSongs.save(rec);
		}
	}

}
