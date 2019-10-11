/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;


public class TimingCtrl {

	// we only have one song end task at the same time!
	private SongEndTask currentEndTask;
	private Long executeSongEndAt;

	private SongEndTask pausedEndTask;
	private Long pausedTimeLeft;

	private Long lastSongStart;

	private boolean timerRunning;

	private GUI gui;


	public TimingCtrl() {
		timerRunning = false;
	}

	public void close() {
		timerRunning = false;
	}

	public void startPlaying(SongEndTask songEndTask, Integer songLengthInMilliseconds) {

		this.lastSongStart = System.currentTimeMillis();

		if (songLengthInMilliseconds == null) {
			this.executeSongEndAt = null;
		} else {
			this.executeSongEndAt = lastSongStart + songLengthInMilliseconds;
		}

		this.currentEndTask = songEndTask;
	}

	public Long getElapsedTimeSinceLastSongStart() {

		return System.currentTimeMillis() - this.lastSongStart;
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
			if (executeSongEndAt == null) {
				pausedTimeLeft = null;
			} else {
				pausedTimeLeft = executeSongEndAt - System.currentTimeMillis();
			}
		}
	}

	public void continueSong() {
		if (pausedEndTask != null) {
			if (pausedTimeLeft == null) {
				executeSongEndAt = null;
			} else {
				executeSongEndAt = System.currentTimeMillis() + pausedTimeLeft;
			}
			currentEndTask = pausedEndTask;
			pausedEndTask = null;
		}
	}

	public void resetSongLength() {
		executeSongEndAt = null;
	}

	public void setGui(GUI gui) {
		this.gui = gui;
		startTimerThread();
	}

	private void startTimerThread() {

		timerRunning = true;

		Thread timerThread = new Thread() {

			public void run() {

				while (timerRunning) {
					try {
						if (executeSongEndAt == null) {
							gui.setRemainingTime(null);
						} else {
							long remainingTime = executeSongEndAt - System.currentTimeMillis();
							gui.setRemainingTime(remainingTime);
							if ((remainingTime < 0) && (currentEndTask != null)) {
								SongEndTask task = currentEndTask;
								currentEndTask = null;
								task.songIsOver();
							}
						}

						Thread.sleep(1000);

					} catch (InterruptedException e) {
						// just keep sleeping...
					}
				}
			}
		};
		timerThread.start();
	}

}
