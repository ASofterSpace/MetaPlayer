/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.MathUtils;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.Utils;

import java.util.Date;

import javax.swing.SwingUtilities;


public class MetaPlayer {

	public final static String PROGRAM_TITLE = "MetaPlayer";
	public final static String VERSION_NUMBER = "0.0.4.6(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "25. September 2019 - 13. December 2025";

	private static ConfigFile config;
	private static ConfigFile playlistConfig;

	private static TimingCtrl timingCtrl;
	private static PlayerCtrl playerCtrl;
	private static SongCtrl songCtrl;

	private static String afterStartupPlaySong = null;
	private static String afterStartupPlayPlaylist = null;


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

		// get --song Artist - Title or --playlist Playlist Name or both (!)
		if (args.length > 1) {
			int pos = 0;
			while (true) {
				StringBuilder allArgsAfterFirst = new StringBuilder();
				String sep = "";
				boolean doContinue = false;
				int i = pos + 1;
				for (; i < args.length; i++) {
					if (args[i].equals("--song") ||
						args[i].equals("--playlist")) {
						doContinue = true;
						break;
					}
					allArgsAfterFirst.append(sep);
					allArgsAfterFirst.append(args[i]);
					sep = " ";
				}

				if (args[pos].equals("--song")) {
					afterStartupPlaySong = allArgsAfterFirst.toString();
				}
				if (args[pos].equals("--playlist")) {
					afterStartupPlayPlaylist = allArgsAfterFirst.toString();
				}
				if (doContinue) {
					pos = i;
				} else {
					break;
				}
			}
		}

		try {
			// load settings
			config = new ConfigFile("settings", true);

			// create a default config file, if necessary
			if (config.getAllContents().isEmpty()) {
				config.setAllContents(new JSON(
					"{\"" + PlayerCtrl.EXT_PLAYER_ASSOC_KEY + "\":[]}"
				));
			}
		} catch (JsonParseException e) {
			complainThenExit("settings", e);
		}

		try {
			// load playlists
			playlistConfig = new ConfigFile("playlists", true);

			JSON playlistRoot = playlistConfig.getAllContents();

			// create a default config file, if necessary
			if (playlistRoot.isEmpty()) {
				playlistConfig.setAllContents(new JSON(
					"{\"" + GUI.CONFIG_KEY_PLAYLISTS + "\":[]}"
				));
			}

			String playlistStr = playlistRoot.toString();
			if (playlistStr.contains(Song.WONKY_DASH_1) || playlistStr.contains(Song.WONKY_DASH_2)) {
				playlistStr = StrUtils.replaceAll(playlistStr, Song.WONKY_DASH_1, Song.DASH);
				playlistStr = StrUtils.replaceAll(playlistStr, Song.WONKY_DASH_2, Song.DASH);
				playlistConfig.setAllContents(new JSON(playlistStr));
			}

			JsonFile backupPlaylistConf = new JsonFile(playlistConfig.getParentDirectory(),
				"playlists_backup_" + MathUtils.randomInteger(10) + ".cnf");
			backupPlaylistConf.save(playlistConfig.getAllContents());

		} catch (JsonParseException e) {
			complainThenExit("playlists", e);
		}

		// create timer
		timingCtrl = new TimingCtrl();

		// load player associations
		playerCtrl = new PlayerCtrl(config.getAllContents());

		try {
			// load songs
			songCtrl = new SongCtrl();
		} catch (JsonParseException e) {
			complainThenExit("songs", e);
		}

		System.out.println("All songs have been loaded; MetaPlayer ready!");

		GUI gui = new GUI(timingCtrl, playerCtrl, songCtrl, config, playlistConfig);
		songCtrl.setGUI(gui);
		SwingUtilities.invokeLater(gui);

		Date now = DateUtils.now();
		String topSongFileName = "top_songs_" + DateUtils.getYear(now) + "_" +
			StrUtils.leftPad0(DateUtils.getMonth(now) + "", 2) + ".json";
		JsonFile topSongs = new JsonFile(config.getParentDirectory(), topSongFileName);
		if (!topSongs.exists()) {
			Record rec = Record.emptyObject();

			Record topByRating = Record.emptyArray();
			for (Song song : songCtrl.getTopRatedSongs(256)) {
				topByRating.append(song.toRecord());
			}
			rec.set("top_by_rating", topByRating);

			Record topByPlayAmount = Record.emptyArray();
			for (Song song : songCtrl.getTopPlayedSongs(256)) {
				if (song.getPlayAmount() > 0) {
					topByPlayAmount.append(song.toRecord());
				}
			}
			rec.set("top_by_play_amount", topByPlayAmount);

			topSongs.save(rec);

			// save after resetting the top play amounts for all songs
			songCtrl.resetTopPlayAmounts();
			songCtrl.save();
		}

		// save playlists on start so that the JSON is well-formatted
		playlistConfig.create();
	}

	public static String getAfterStartupPlaySong() {
		return afterStartupPlaySong;
	}

	public static String getAfterStartupPlayPlaylist() {
		return afterStartupPlayPlaylist;
	}

	private static void complainThenExit(String whichFile, Exception e) {
		String complainStr = "Loading the " + whichFile + " failed:";
		System.err.println(complainStr);
		System.err.println(e);
		GuiUtils.complain(complainStr + "\n" + e);
		System.exit(1);
	}
}
