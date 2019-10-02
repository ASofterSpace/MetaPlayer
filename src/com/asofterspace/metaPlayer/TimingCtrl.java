/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;


// TODO :: actually link this back into the GUI,
// and count down a timer there every second that
// shows how long the song is still playing
public class TimingCtrl {

	// we only have one song end task at the same time!
	private SongEndTask currentEndTask;
	private Long executeSongEndAt;

	private SongEndTask pausedEndTask;
	private Long pausedTimeLeft;

	private boolean timerRunning;


	public TimingCtrl() {

		timerRunning = true;

		Thread timerThread = new Thread() {

			public void run() {

				while (timerRunning) {
					try{
						SongEndTask task = currentEndTask;

						if (task != null) {
							if (executeSongEndAt < System.currentTimeMillis()) {
								task.songIsOver();
								currentEndTask = null;
							}
						}

						Thread.sleep(1000);

					} catch (InterruptedException e) {
						// just keep sleeping...
					}
				}
			}
		};
	}

	public void close() {
		timerRunning = false;
	}

	public void schedule(SongEndTask songEndTask, int songLengthInMilliseconds) {

		this.executeSongEndAt = System.currentTimeMillis() + songLengthInMilliseconds;

		this.currentEndTask = songEndTask;
	}

	public void stopPlaying() {

		// actually destroy the current player right now
		if (currentEndTask != null) {
			currentEndTask.stopPlayer();
			currentEndTask = null;
		}
	}

	public void pauseSong() {
		if (currentEndTask != null) {
			pausedEndTask = currentEndTask;
			currentEndTask = null;
			pausedTimeLeft = executeSongEndAt - System.currentTimeMillis();
		}
	}

	public void continueSong() {
		if (pausedEndTask != null) {
			executeSongEndAt = System.currentTimeMillis() + pausedTimeLeft;
			currentEndTask = pausedEndTask;
			pausedEndTask = null;
		}
	}

}
