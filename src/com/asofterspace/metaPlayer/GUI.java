/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.configuration.ConfigFile;
import com.asofterspace.toolbox.gui.Arrangement;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.gui.MainWindow;
import com.asofterspace.toolbox.gui.MenuItemForMainMenu;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.utils.Callback;
import com.asofterspace.toolbox.Utils;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;


public class GUI extends MainWindow {

	private final static String CONFIG_KEY_WIDTH = "mainFrameWidth";
	private final static String CONFIG_KEY_HEIGHT = "mainFrameHeight";
	private final static String CONFIG_KEY_LEFT = "mainFrameLeft";
	private final static String CONFIG_KEY_TOP = "mainFrameTop";
	private final static String CONFIG_KEY_LAST_SONG_DIRECTORY = "songDir";
	private final static String CONFIG_KEY_LAST_LEGACY_DIRECTORY = "legacyDir";

	private TimingCtrl timingCtrl;
	private PlayerCtrl playerCtrl;
	private SongCtrl songCtrl;

	private Song currentlyPlayedSong;

	private JPanel mainPanelRight;

	private JPanel searchPanel;
	private JTextField searchField;

	private JMenuItem songItem;
	private AbstractButton pauseItem;

	private ConfigFile configuration;
	private JList<String> songListComponent;
	private JPopupMenu songListPopup;
	private String[] strSongs;
	private JScrollPane songListScroller;


	public GUI(TimingCtrl timingCtrl, PlayerCtrl playerCtrl, SongCtrl songCtrl, ConfigFile config) {

		this.timingCtrl = timingCtrl;

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

		final Integer lastWidth = configuration.getInteger(CONFIG_KEY_WIDTH, -1);
		final Integer lastHeight = configuration.getInteger(CONFIG_KEY_HEIGHT, -1);
		final Integer lastLeft = configuration.getInteger(CONFIG_KEY_LEFT, -1);
		final Integer lastTop = configuration.getInteger(CONFIG_KEY_TOP, -1);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Stage everything to be shown
				mainFrame.pack();

				// Actually display the whole jazz
				mainFrame.setVisible(true);

				if ((lastWidth < 1) || (lastHeight < 1)) {
					GuiUtils.maximizeWindow(mainFrame);
				} else {
					mainFrame.setSize(lastWidth, lastHeight);

					mainFrame.setPreferredSize(new Dimension(lastWidth, lastHeight));

					mainFrame.setLocation(new Point(lastLeft, lastTop));
				}

				mainFrame.addComponentListener(new ComponentAdapter() {
					public void componentResized(ComponentEvent componentEvent) {
						configuration.set(CONFIG_KEY_WIDTH, mainFrame.getWidth());
						configuration.set(CONFIG_KEY_HEIGHT, mainFrame.getHeight());
					}

					public void componentMoved(ComponentEvent componentEvent) {
						configuration.set(CONFIG_KEY_LEFT, mainFrame.getLocation().x);
						configuration.set(CONFIG_KEY_TOP, mainFrame.getLocation().y);
					}
				});
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

		JMenuItem randomize = new JMenuItem("Randomize");
		randomize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.randomize();
				regenerateSongList();
			}
		});
		songs.add(randomize);

		JMenuItem sort = new JMenuItem("Sort");
		sort.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.sort();
				regenerateSongList();
			}
		});
		songs.add(sort);

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
				if (pauseItem.getName().equals("Pause")) {
					pauseCurSong();
					pauseItem.setName("Continue");
				} else {
					continueCurSong();
					pauseItem.setName("Pause");
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

		songItem = new JMenuItem("");
		songItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO :: copy the path of the currently played song to the clipboard
			}
		});
		menu.add(songItem);

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

		/*
		songListComponent.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				showSelectedTab();
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
				if (e.isPopupTrigger()) {
					songListComponent.setSelectedIndex(songListComponent.locationToIndex(e.getPoint()));
					songListPopup.show(songListComponent, e.getX(), e.getY());
				}

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
		*/

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
		*/

		highlightTabInLeftListOrTree(currentlyPlayedSong);

		refreshTitleBar();
	}

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
		songItem.setText(song.getPath());

		try {
			timingCtrl.stopPlaying();

			Process process = new ProcessBuilder(player, song.getPath()).start();

			pauseItem.setName("Pause");

			if (song.getLength() != null) {
				SongEndTask songEndTask = new SongEndTask(this, process);
				timingCtrl.schedule(songEndTask, song.getLength());
			}

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
				mppFile.useCharset(StandardCharsets.ISO_8859_1);
				intFile.useCharset(StandardCharsets.ISO_8859_1);
				List<String> mppContents = mppFile.getContents();
				List<String> intContents = intFile.getContents();
				for (int i = 0; i < mppContents.size(); i++) {
					Song song = new Song(intContents.get(i*2), mppContents.get(i));
					String lengthAndRating = intContents.get((i*2)+1);
					String[] lengthAndRatings = lengthAndRating.split("\\*");
					song.setLength(lengthAndRatings[0]);
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
		// TODO
	}

	private void pauseCurSong() {
		timingCtrl.pauseSong();
	}

	private void continueCurSong() {
		timingCtrl.continueSong();
	}

	private void curSongIsOver() {
		// TODO
	}

}
