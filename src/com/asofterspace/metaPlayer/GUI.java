/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.gui.Arrangement;
import com.asofterspace.toolbox.gui.BarListener;
import com.asofterspace.toolbox.gui.BarMenuItemForMainMenu;
import com.asofterspace.toolbox.gui.MainWindow;
import com.asofterspace.toolbox.gui.MenuItemForMainMenu;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.utils.ProcessUtils;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.TextEncoding;
import com.asofterspace.toolbox.Utils;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class GUI extends MainWindow {

	private final static String CONFIG_KEY_LAST_SONG_DIRECTORY = "songDir";
	private final static String CONFIG_KEY_LAST_LEGACY_DIRECTORY = "legacyDir";
	public final static String CONFIG_KEY_MAIN_ARTISTS = "mainArtists";
	public final static String CONFIG_KEY_PLAYLISTS = "playlists";

	private TimingCtrl timingCtrl;
	private PlayerCtrl playerCtrl;
	private SongCtrl songCtrl;

	private Song currentlyPlayedSong = null;
	private Song currentlySelectedSong = null;

	private JPanel mainPanelRight;

	private JPanel searchPanel;
	private JTextField searchField;

	private JMenuItem songItem;
	private AbstractButton pauseItem;
	private AbstractButton timeRemainingItem;
	private AbstractButton minimizeMaximize;
	private BarMenuItemForMainMenu ratingItem;

	private ConfigFile configuration;
	private JList<String> songListComponent;
	private JPopupMenu songListPopup;
	private String[] strSongs;
	private JScrollPane songListScroller;


	public GUI(TimingCtrl timingCtrl, PlayerCtrl playerCtrl, SongCtrl songCtrl, ConfigFile config) {

		this.timingCtrl = timingCtrl;
		timingCtrl.setGui(this);

		this.playerCtrl = playerCtrl;

		this.songCtrl = songCtrl;

		this.configuration = config;
	}

	@Override
	public void run() {

		super.create();

		refreshTitleBar();

		createMenu(mainFrame);

		createMainPanel(mainFrame);

		// do not call super.show, as we are doing things a little bit
		// differently around here (including restoring from previous
		// position...)
		// super.show();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Stage everything to be shown
				mainFrame.pack();

				// Actually display the whole jazz
				mainFrame.setVisible(true);

				minimize();
			}
		});

		songCtrl.randomize();

		regenerateSongList();

		startPlaying();
	}

	private JMenuBar createMenu(JFrame parent) {

		JMenuBar menu = new JMenuBar();

		JMenu songs = new JMenu("Songs");
		menu.add(songs);

		JMenuItem startPlaying = new JMenuItem("Start Playing");
		startPlaying.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				playNextSong();
			}
		});
		songs.add(startPlaying);

		JMenuItem stopPlaying = new JMenuItem("Stop Playing");
		stopPlaying.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopPlaying();
			}
		});
		songs.add(stopPlaying);

		songs.addSeparator();

		JMenuItem jumpToPlaying = new JMenuItem("Jump to Playing Song");
		jumpToPlaying.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jumpToSong(currentlyPlayedSong);
			}
		});
		songs.add(jumpToPlaying);

		JMenuItem jumpToSelected = new JMenuItem("Jump to Selected Song");
		jumpToSelected.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jumpToSong(currentlySelectedSong);
			}
		});
		songs.add(jumpToSelected);

		songs.addSeparator();

		JMenuItem randomize = new JMenuItem("Randomize");
		randomize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.randomize();
				regenerateSongList();
			}
		});
		songs.add(randomize);

		JMenuItem sortArtist = new JMenuItem("Sort by Artist");
		sortArtist.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.sort(SortCriterion.ARTIST);
				regenerateSongList();
			}
		});
		songs.add(sortArtist);

		JMenuItem sortTitle = new JMenuItem("Sort by Song Title");
		sortTitle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.sort(SortCriterion.TITLE);
				regenerateSongList();
			}
		});
		songs.add(sortTitle);

		JMenuItem sortRating = new JMenuItem("Sort by Rating");
		sortRating.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.sort(SortCriterion.RATING);
				regenerateSongList();
			}
		});
		songs.add(sortRating);

		songs.addSeparator();

		JMenuItem importSongs = new JMenuItem("Import Songs");
		importSongs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				importSongs();
			}
		});
		songs.add(importSongs);

		JMenuItem importLegacyPlaylist = new JMenuItem("Import Songs from Legacy Playlist");
		importLegacyPlaylist.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				importLegacyPlaylist();
			}
		});
		songs.add(importLegacyPlaylist);

		JMenuItem cullMultiples = new JMenuItem("Cull Multiples");
		cullMultiples.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.cullMultiples();
				songCtrl.save();
				regenerateSongList();
			}
		});
		songs.add(cullMultiples);

		songs.addSeparator();

		JMenuItem save = new JMenuItem("Save");
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.save();
			}
		});
		songs.add(save);

		JMenu artists = new JMenu("Select Artist");
		menu.add(artists);

		JMenuItem allArtists = new JMenuItem("All Artists");
		allArtists.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.selectAllSongs();
				songCtrl.randomize();
				regenerateSongList();
			}
		});
		artists.add(allArtists);

		// actually, the artists that we have a lot of songs of are not necessarily the ones that
		// we want to see in the list, so load this from configuration by default:
		List<String> artistNames = configuration.getList(CONFIG_KEY_MAIN_ARTISTS);
		// and only get the top artists if there is no such configuration:
		if ((artistNames == null) || (artistNames.size() == 0)) {
			artistNames = songCtrl.getTopArtists(32);
		}

		for (String artistName : artistNames) {
			JMenuItem artistItem = new JMenuItem(artistName);
			artistItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					songCtrl.selectSongsOfArtist(artistName);
					songCtrl.randomize();
					regenerateSongList();
				}
			});
			artists.add(artistItem);
		}

		JMenu playlists = new JMenu("Select Playlist");
		menu.add(playlists);

		JMenuItem allSongs = new JMenuItem("All Songs");
		allSongs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.selectAllSongs();
				songCtrl.randomize();
				regenerateSongList();
			}
		});
		playlists.add(allSongs);

		List<Record> playlistRecords = configuration.getAllContents().getArray(CONFIG_KEY_PLAYLISTS);

		for (Record playlistRecord : playlistRecords) {
			JMenuItem playlistItem = new JMenuItem(playlistRecord.getString("name"));
			playlistItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					songCtrl.selectPlaylist(playlistRecord);
					songCtrl.randomize();
					regenerateSongList();
				}
			});
			playlists.add(playlistItem);
		}

		menu.add(new MenuItemForMainMenu("|"));

		AbstractButton prev = new MenuItemForMainMenu("Previous");
		prev.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				playPreviousSong();
			}
		});
		menu.add(prev);

		pauseItem = new MenuItemForMainMenu("Pause");
		pauseItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pauseItem.getText().equals("Pause")) {
					pauseCurSong();
					pauseItem.setText("Continue");
				} else {
					continueCurSong();
					pauseItem.setText("Pause");
				}
			}
		});
		menu.add(pauseItem);

		AbstractButton next = new MenuItemForMainMenu("Next");
		next.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				playNextSong();
			}
		});
		menu.add(next);

		menu.add(new MenuItemForMainMenu("|"));

		AbstractButton songIsOver = new MenuItemForMainMenu("Song is Over");
		songIsOver.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				curSongIsOver();
			}
		});
		menu.add(songIsOver);

		AbstractButton resetLength = new MenuItemForMainMenu("Reset Length");
		resetLength.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resetCurSongLength();
			}
		});
		menu.add(resetLength);

		menu.add(new MenuItemForMainMenu("|"));

		minimizeMaximize = new MenuItemForMainMenu("Maximize");
		minimizeMaximize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (minimizeMaximize.getText().equals("Maximize")) {
					minimizeMaximize.setText("Minimize");
					maximize();
				} else {
					minimizeMaximize.setText("Maximize");
					minimize();
				}
			}
		});
		menu.add(minimizeMaximize);

		AbstractButton close = new MenuItemForMainMenu("Close");
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				timingCtrl.close();
				System.exit(0);
			}
		});
		menu.add(close);

		JMenu huh = new JMenu("?");

		JMenuItem openConfigPath = new JMenuItem("Open Config Path");
		openConfigPath.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().open(configuration.getParentDirectory().getJavaFile());
				} catch (IOException ex) {
					// do nothing
				}
			}
		});
		huh.add(openConfigPath);

		JMenuItem about = new JMenuItem("About");
		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String aboutMessage = "This is the " + Main.PROGRAM_TITLE + ".\n" +
					"Version: " + Main.VERSION_NUMBER + " (" + Main.VERSION_DATE + ")\n" +
					"Brought to you by: A Softer Space";
				JOptionPane.showMessageDialog(mainFrame, aboutMessage, "About", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		huh.add(about);
		menu.add(huh);

		menu.add(new MenuItemForMainMenu("|"));

		ratingItem = new BarMenuItemForMainMenu();
		ratingItem.setMaximum(100);
		ratingItem.addBarListener(new BarListener() {
			@Override
			public void onBarMove(int position) {
				if (currentlyPlayedSong != null) {
					currentlyPlayedSong.setRating(position);
					songCtrl.save();
				}
			}
		});
		menu.add(ratingItem);

		songItem = new JMenuItem("");
		songItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentlyPlayedSong != null) {
					StringSelection selection = new StringSelection(currentlyPlayedSong.getClipboardText());
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(selection, selection);
				}
			}
		});
		menu.add(songItem);

		timeRemainingItem = new MenuItemForMainMenu("");
		menu.add(timeRemainingItem);

		parent.setJMenuBar(menu);

		return menu;
	}

	private JPanel createMainPanel(JFrame parent) {

		JPanel mainPanel = new JPanel();
		mainPanel.setPreferredSize(new Dimension(800, 500));
		GridBagLayout mainPanelLayout = new GridBagLayout();
		mainPanel.setLayout(mainPanelLayout);

		JPanel mainPanelRightOuter = new JPanel();
		GridBagLayout mainPanelRightOuterLayout = new GridBagLayout();
		mainPanelRightOuter.setLayout(mainPanelRightOuterLayout);

		mainPanelRight = new JPanel();
		mainPanelRight.setLayout(new CardLayout());
		mainPanelRight.setPreferredSize(new Dimension(8, 8));

		JPanel gapPanel = new JPanel();
		gapPanel.setPreferredSize(new Dimension(8, 8));

		String[] songList = new String[0];
		songListComponent = new JList<String>(songList);

		songListComponent.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				showSelectedTab();

				// on double-click...
				if (e.getClickCount() == 2) {
					// ... actually play the song!
					playSong(currentlySelectedSong);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				showPopupAndSelectedTab(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				showPopupAndSelectedTab(e);
			}

			private void showPopupAndSelectedTab(MouseEvent e) {
				/*
				if (e.isPopupTrigger()) {
					songListComponent.setSelectedIndex(songListComponent.locationToIndex(e.getPoint()));
					songListPopup.show(songListComponent, e.getX(), e.getY());
				}
				*/
				showSelectedTab();
			}
		});

		songListComponent.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_UP:
					case KeyEvent.VK_DOWN:
						showSelectedTab();
						break;
				}
			}
		});

		songListScroller = new JScrollPane(songListComponent);
		songListScroller.setPreferredSize(new Dimension(8, 8));
		songListScroller.setBorder(BorderFactory.createEmptyBorder());

		searchPanel = new JPanel();
		searchPanel.setLayout(new GridBagLayout());
		searchPanel.setVisible(false);

		searchField = new JTextField();

		// listen to text updates
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				search();
			}
			public void removeUpdate(DocumentEvent e) {
				search();
			}
			public void insertUpdate(DocumentEvent e) {
				search();
			}
			private void search() {
				String searchFor = searchField.getText();

				// TODO :: actually search for the song ;)
			}
		});

		// listen to the enter key being pressed (which does not create text updates)
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String searchFor = searchField.getText();

				// TODO :: actually search for the song ;)
			}
		});

		searchPanel.add(searchField, new Arrangement(0, 0, 1.0, 1.0));

		mainPanelRightOuter.add(mainPanelRight, new Arrangement(0, 0, 1.0, 1.0));

		mainPanelRightOuter.add(searchPanel, new Arrangement(0, 1, 1.0, 0.0));

		mainPanel.add(songListScroller, new Arrangement(0, 0, 0.2, 1.0));

		mainPanel.add(gapPanel, new Arrangement(2, 0, 0.0, 0.0));

		mainPanel.add(mainPanelRightOuter, new Arrangement(3, 0, 1.0, 1.0));

		parent.add(mainPanel, BorderLayout.CENTER);

		return mainPanel;
	}

	private void showSearchBar() {

		searchPanel.setVisible(true);

		searchField.requestFocus();
	}

	private void minimize() {

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		int width = (int) screenSize.getWidth() + 20;
		int height = (int) screenSize.getHeight() - 73;

		mainFrame.setSize(width, height);

		mainFrame.setPreferredSize(new Dimension(width, height));

		mainFrame.setLocation(new Point(-8, (int) screenSize.getHeight() - 53));
	}

	private void maximize() {

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		int width = (int) screenSize.getWidth() - 166;
		int height = (int) screenSize.getHeight() - 73;

		mainFrame.setSize(width, height);

		mainFrame.setPreferredSize(new Dimension(width, height));

		mainFrame.setLocation(new Point(83, 26));
	}

	/**
	 * Regenerate the file list on the left hand side based on the songCtrl.getSongs() list,
	 * and (if at least one file exists), select and open the current tab or, if it
	 * is null, the lastly added one
	 */
	public void regenerateSongList() {

		/*
		// if there is no last shown tab...
		if (currentlyPlayedSong == null) {
			// ... show some random tab explicitly - this is fun, and the tabbed layout otherwise shows it anyway, so may as well...
			if (songCtrl.getSongs().size() > 0) {
				setCurrentlyPlayedSong(songCtrl.getSongs().get(0));
			}
		}
		*/

		/*
		Collections.sort(songCtrl.getSongs(), new Comparator<Song>() {
			public int compare(Song a, Song b) {
				return a.getFilePath().toLowerCase().compareTo(b.getFilePath().toLowerCase());
			}
		});
		*/

		strSongs = new String[songCtrl.getSongs().size()];

		int i = 0;

		for (Song song : songCtrl.getSongs()) {
			strSongs[i] = song.toString();
			if (song.equals(currentlyPlayedSong)) {
				strSongs[i] = ">> " + strSongs[i] + " <<";
			}
			i++;
		}

		songListComponent.setListData(strSongs);

		// if there still is no last shown tab (e.g. we just deleted the very last one)...
		if (currentlyPlayedSong == null) {
			// ... then we do not need to show or highlight any ;)
			return;
		}

		/*
		// show the last shown tab
		showTab(currentlyPlayedSong);

		highlightTabInLeftListOrTree(currentlyPlayedSong);
		*/

		refreshTitleBar();
	}

	/*
	public void highlightTabInLeftListOrTree(Song song) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// highlight tab the list
				int i = 0;
				for (Song cur : songCtrl.getSongs()) {
					if (song.equals(cur)) {
						songListComponent.setSelectedIndex(i);
						break;
					}
					i++;
				}
			}
		});
	}
	*/

	private void refreshTitleBar() {
		mainFrame.setTitle(Main.PROGRAM_TITLE + " - " + songCtrl.getSongAmount() + " songs loaded");
	}

	private void stopPlaying() {

		timingCtrl.stopPlaying();

		currentlyPlayedSong = null;
		songItem.setText("");

		regenerateSongList();
	}

	private void playSong(Song song) {

		if (song == null) {
			return;
		}

		String player = playerCtrl.getPlayerForSong(song);

		if (player == null) {
			return;
		}

		currentlyPlayedSong = song;
		songItem.setText(song.toString());
		ratingItem.setBarPosition(song.getRating());

		timingCtrl.stopPlaying();

		try {
			Process process = ProcessUtils.startProcess(player, song.getPath());

			pauseItem.setText("Pause");

			SongEndTask songEndTask = new SongEndTask(this, process);
			timingCtrl.startPlaying(songEndTask, song.getLength());

			regenerateSongList();

		} catch (IOException ex) {
			System.err.println("Could not start playing the song " + song.getPath() + " with player " + player + " due to: " + ex);
		}
	}

	public void playNextSong() {

		playSong(songCtrl.getNextSong(currentlyPlayedSong));
	}

	private void playPreviousSong() {

		playSong(songCtrl.getPreviousSong(currentlyPlayedSong));
	}

	private void startPlaying() {

		List<Song> songs = songCtrl.getSongs();

		if (songs.size() > 0) {
			playSong(songs.get(0));
		}
	}

	private void importSongs() {

		JFileChooser filePicker;

		// use the last-used directory
		String lastDirectory = configuration.getValue(CONFIG_KEY_LAST_SONG_DIRECTORY);

		if ((lastDirectory != null) && !"".equals(lastDirectory)) {
			filePicker = new JFileChooser(new java.io.File(lastDirectory));
		} else {
			filePicker = new JFileChooser();
		}

		filePicker.setDialogTitle("Open Songs or Directories to Import");
		filePicker.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		filePicker.setMultiSelectionEnabled(true);

		int result = filePicker.showOpenDialog(mainFrame);

		switch (result) {

			case JFileChooser.APPROVE_OPTION:

				// load the files
				configuration.set(CONFIG_KEY_LAST_SONG_DIRECTORY, filePicker.getCurrentDirectory().getAbsolutePath());

				for (java.io.File curFile : filePicker.getSelectedFiles()) {
					importSongsRecursively(curFile);
				}

				songCtrl.save();
				regenerateSongList();

				break;

			case JFileChooser.CANCEL_OPTION:
				// cancel was pressed... do nothing for now
				break;
		}
	}

	private void importSongsRecursively(java.io.File parent) {

		if (parent.isDirectory()) {
			java.io.File[] curFiles = parent.listFiles();

			for (java.io.File curFile : curFiles) {
				importSongsRecursively(curFile);
			}
		} else {
			songCtrl.addUnlessAlreadyPresent(new Song(parent));
		}
	}

	private void importLegacyPlaylist() {

		JFileChooser filePicker;

		// use the last-used directory
		String lastDirectory = configuration.getValue(CONFIG_KEY_LAST_LEGACY_DIRECTORY);

		if ((lastDirectory != null) && !"".equals(lastDirectory)) {
			filePicker = new JFileChooser(new java.io.File(lastDirectory));
		} else {
			filePicker = new JFileChooser();
		}

		filePicker.setDialogTitle("Open a Legacy Playlist to Import");
		filePicker.setFileSelectionMode(JFileChooser.FILES_ONLY);
		filePicker.setMultiSelectionEnabled(false);

		int result = filePicker.showOpenDialog(mainFrame);

		switch (result) {

			case JFileChooser.APPROVE_OPTION:

				// load the files
				configuration.set(CONFIG_KEY_LAST_LEGACY_DIRECTORY, filePicker.getCurrentDirectory().getAbsolutePath());

				java.io.File curFile = filePicker.getSelectedFile();

				String mppPath;
				String intPath;
				String selectedPath = curFile.getAbsolutePath();

				// we assume that either music.mpp or music_int.mpp were selected
				if (selectedPath.contains("music.mpp")) {
					mppPath = selectedPath;
					intPath = selectedPath.replace("music.mpp", "music_int.mpp");
				} else {
					if (selectedPath.contains("music_int.mpp")) {
						mppPath = selectedPath.replace("music_int.mpp", "music.mpp");
						intPath = selectedPath;
					} else {
						System.err.println("You selected neither a file called music.mpp, nor one called music_int.mpp!");
						return;
					}
				}
				SimpleFile mppFile = new SimpleFile(mppPath);
				SimpleFile intFile = new SimpleFile(intPath);
				mppFile.setEncoding(TextEncoding.ISO_LATIN_1);
				intFile.setEncoding(TextEncoding.ISO_LATIN_1);
				List<String> mppContents = mppFile.getContents();
				List<String> intContents = intFile.getContents();
				for (int i = 0; i < mppContents.size(); i++) {
					Song song = new Song(intContents.get(i*2), mppContents.get(i));
					String lengthAndRating = intContents.get((i*2)+1);
					String[] lengthAndRatings = lengthAndRating.split("\\*");
					song.setStrLength(lengthAndRatings[0]);
					if (lengthAndRatings.length > 1) {
						song.setRating(lengthAndRatings[1]);
					}
					songCtrl.add(song);
				}

				songCtrl.save();
				regenerateSongList();

				break;

			case JFileChooser.CANCEL_OPTION:
				// cancel was pressed... do nothing for now
				break;
		}
	}

	private void resetCurSongLength() {
		if (currentlyPlayedSong != null) {
			currentlyPlayedSong.setLength(null);
			songCtrl.save();
		}
		timingCtrl.resetSongLength();
	}

	private void pauseCurSong() {
		timingCtrl.pauseSong();
	}

	private void continueCurSong() {
		timingCtrl.continueSong();
	}

	private void curSongIsOver() {
		if (currentlyPlayedSong != null) {
			Long newLength = timingCtrl.getElapsedTimeSinceLastSongStart();
			if (newLength == null) {
				currentlyPlayedSong.setLength(null);
			} else {
				currentlyPlayedSong.setLength(newLength.intValue());
			}
			songCtrl.save();
		}
		playNextSong();
	}

	public void setRemainingTime(Long remainingTime) {

		if (timeRemainingItem == null) {
			return;
		}

		if (remainingTime == null) {
			timeRemainingItem.setText("");
			return;
		}

		if (remainingTime < 0) {
			remainingTime = 0l;
		}

		remainingTime = remainingTime / 1000;

		String minutes = "" + ((int) Math.floor(remainingTime / 60));
		String seconds = "" + (remainingTime % 60);
		if (seconds.length() < 2) {
			seconds = "0" + seconds;
		}

		timeRemainingItem.setText(minutes + ":" + seconds);
	}

	private void showSelectedTab() {

		Integer selectedItem = songListComponent.getSelectedIndex();

		currentlySelectedSong = songCtrl.getSong(selectedItem);
	}

	private void jumpToSong(Song song) {
		// find the song in the songCtrl
		int songPos = songCtrl.getSongPosition(song);
		int songAmount = songCtrl.getSongAmount();

		// let the songListComponent jump to it
		JScrollBar bar = songListScroller.getVerticalScrollBar();
		bar.setValue((bar.getMaximum() * songPos) / songAmount);
		songListScroller.repaint();
	}

}
