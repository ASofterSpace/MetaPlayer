/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TimingCtrl {

	// we only have one song end task at the same time!
	private SongEndTask currentEndTask;
	private Long executeSongEndAt;

	private SongEndTask pausedEndTask;
	private Long pausedTimeLeft;

	private Long lastSongStartMillis;
	private String lastPlayerStrFirst;
	private List<String> lastPlayerStrList;
	private Long pausedSongAtMillis;

	private boolean timerRunning;

	private GUI gui;

	private SongCtrl songCtrl;

	private String ffmpegPath = null;


	public TimingCtrl(Record config, SongCtrl songCtrl) {
		this.timerRunning = false;
		this.songCtrl = songCtrl;
		this.ffmpegPath = config.getString("ffmpegPath");
	}

	public void close() {
		timerRunning = false;
		stopPlaying();
	}

	public void startPlaying(SongEndTask songEndTask, Song song, String playerStrFirst, List<String> playerStrList) {

		this.lastSongStartMillis = System.currentTimeMillis();
		this.lastPlayerStrFirst = playerStrFirst;
		this.lastPlayerStrList = playerStrList;

		Integer songLengthInMilliseconds = song.getLength();

		// if no length is given, first try to automagically get the length
		if ((songLengthInMilliseconds == null) || (songLengthInMilliseconds < 1)) {
			if (ffmpegPath != null) {
				File songFile = song.getFile();
				List<String> outputLines = IoUtils.execute(
					ffmpegPath, songFile.getParentDirectory(), "-i", songFile.getLocalFilename());
				if (outputLines != null) {
					for (String line : outputLines) {
						String trimLine = line.trim();
						if (trimLine.startsWith("Duration: ")) {
							trimLine = trimLine.substring(10, 21);
							Integer hours = StrUtils.strToInt(trimLine.substring(0, 2));
							Integer minutes = StrUtils.strToInt(trimLine.substring(3, 5));
							Integer seconds = StrUtils.strToInt(trimLine.substring(6, 8));
							Integer afterDot = StrUtils.strToInt(trimLine.substring(9, 11));
							if ((hours != null) && (minutes != null) && (seconds != null) && (afterDot != null)) {
								// make a tiny bit longer than the second actually is
								seconds += 3;
								songLengthInMilliseconds =
									(60*60*1000*hours)+(60*1000*minutes)+(1000*seconds)+(10*afterDot);
								song.setLength(songLengthInMilliseconds);
								if (songCtrl != null) {
									songCtrl.save();
								}
							}
							break;
						}
					}
				}
			}
		}

		// if still no length has been found, then just accept that ^^
		if ((songLengthInMilliseconds == null) || (songLengthInMilliseconds < 1)) {
			this.executeSongEndAt = null;
		} else {
			this.executeSongEndAt = lastSongStartMillis + songLengthInMilliseconds;
		}

		this.currentEndTask = songEndTask;
	}

	public Long getElapsedTimeSinceLastSongStart() {

		return System.currentTimeMillis() - this.lastSongStartMillis;
	}

	public void stopPlaying() {

		// actually destroy the current player right now
		if (currentEndTask != null) {
			currentEndTask.stopPlayer();
			currentEndTask = null;
		}
	}
	public void pauseSong(boolean managePauseContinue) {
		if (lastSongStartMillis != null) {
			pausedSongAtMillis = System.currentTimeMillis() - lastSongStartMillis;
		}

		if (currentEndTask != null) {
			pausedEndTask = currentEndTask;
			currentEndTask = null;
			if (executeSongEndAt == null) {
				pausedTimeLeft = null;
			} else {
				pausedTimeLeft = executeSongEndAt - System.currentTimeMillis();
			}
			if (managePauseContinue) {
				pausedEndTask.stopPlayer();
			}
		}
	}

	public void continueSong(boolean managePauseContinue) {
		// if anything was paused at all...
		if (pausedEndTask != null) {
			long restartAtSeconds = 0;
			if (pausedSongAtMillis != null) {
				restartAtSeconds = pausedSongAtMillis / 1000;
				lastSongStartMillis = System.currentTimeMillis() - pausedSongAtMillis;
			}

			// ... actually resume playing from where we left off if we want to manage the player...
			if (managePauseContinue) {
				try {
					List<String> curPlayerStrList = new ArrayList<>();
					for (String s : lastPlayerStrList) {
						curPlayerStrList.add(s);
					}
					// in mpv: --start=SECONDS
					// in vlc: --start-time=SECONDS
					if (lastPlayerStrFirst.contains("mpv")) {
						curPlayerStrList.add("--start=" + restartAtSeconds);
					} else if (lastPlayerStrFirst.contains("vlc")) {
						curPlayerStrList.add("--start-time=" + restartAtSeconds);
					}
					Process process = IoUtils.executeAsync(lastPlayerStrFirst, curPlayerStrList);
					pausedEndTask = new SongEndTask(gui, process);

				} catch (IOException ex) {
					System.err.println("Could not continuge playing with player " + lastPlayerStrFirst + " due to: " + ex);
				}
			}

			// ... and in either case, update the timestamps ^^
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
						// while we are not paused...
						if (pausedEndTask == null) {
							// ... either set to unknown time remaining...
							if (executeSongEndAt == null) {
								gui.setRemainingTime(null);
							} else {
								// ... or to a clearly known time!
								long remainingTime = executeSongEndAt - System.currentTimeMillis();
								gui.setRemainingTime(remainingTime);
								if ((remainingTime < 0) && (currentEndTask != null)) {
									SongEndTask task = currentEndTask;
									currentEndTask = null;
									task.songIsOver();
								}
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
