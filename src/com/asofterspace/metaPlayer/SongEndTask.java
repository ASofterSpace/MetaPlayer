/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;



public class SongEndTask {

	private GUI gui;

	private Process process;


	public SongEndTask(GUI gui, Process process) {
		this.gui = gui;
		this.process = process;
	}

	public void songIsOver() {

		stopPlayer();

		gui.currentSongPlayedAllTheWayThrough();

		gui.playNextSong();
	}

	public void stopPlayer() {
		try {
			process.destroy();
		} catch (Throwable t) {
			// catching throwables is bad? well... oopsie!
		}
	}

}
