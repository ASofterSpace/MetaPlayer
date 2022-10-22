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
import com.asofterspace.toolbox.gui.OpenFileDialog;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.utils.CallbackWithStatus;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JCheckBoxMenuItem;
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

	private final static Integer MAX_ARTISTS_PER_BUCKET = 32;

	private TimingCtrl timingCtrl;
	private PlayerCtrl playerCtrl;
	private SongCtrl songCtrl;

	private Song currentlyPlayedSong = null;
	private Song currentlySelectedSong = null;

	private JPanel mainPanelRight;

	private JPanel searchPanel;
	private JTextField searchField;

	private JMenuItem songItem;
	private MenuItemForMainMenu pauseItem;
	private MenuItemForMainMenu timeRemainingItem;
	private MenuItemForMainMenu minimizeMaximize;
	private BarMenuItemForMainMenu ratingItem;

	private ConfigFile configuration;
	private JList<String> songListComponent;
	private JPopupMenu songListPopup;
	private String[] strSongs;
	private JScrollPane songListScroller;
	private List<Record> allPlaylistRecords = new ArrayList<>();

	private JCheckBoxMenuItem skipWithDuration;
	private JCheckBoxMenuItem skipWithoutDuration;
	private JCheckBoxMenuItem skipWithRating;
	private JCheckBoxMenuItem skipBelow95;
	private JCheckBoxMenuItem skipBelow90;
	private JCheckBoxMenuItem skipBelow80;
	private JCheckBoxMenuItem skipBelow70;
	private JCheckBoxMenuItem skipBelow60;
	private JCheckBoxMenuItem skipBelow50;
	private JCheckBoxMenuItem skipBelow45;
	private JCheckBoxMenuItem skipWithoutRating;


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


		// automatically play pre-selected song after startup
		String afterStartupPlaySong = MetaPlayer.getAfterStartupPlaySong();
		Song forceThisSong = null;

		if (afterStartupPlaySong != null) {
			afterStartupPlaySong = afterStartupPlaySong.trim().toLowerCase();
			List<Song> songs = songCtrl.getSongs();
			for (Song song : songs) {
				if (afterStartupPlaySong.equals(song.toString().trim().toLowerCase())) {
					forceThisSong = song;
					songCtrl.selectSongsOfArtists(forceThisSong.getArtists());
					break;
				}
			}
		}

		// automatically play pre-selected playlist after startup
		String afterStartupPlayPlaylist = MetaPlayer.getAfterStartupPlayPlaylist();

		if (afterStartupPlayPlaylist != null) {
			afterStartupPlayPlaylist = afterStartupPlayPlaylist.trim().toLowerCase();

			for (Record pl : allPlaylistRecords) {
				if (afterStartupPlayPlaylist.equals(pl.getString(SongCtrl.PLAYLIST_NAME_KEY).toLowerCase())) {
					songCtrl.selectPlaylist(pl, allPlaylistRecords);
					break;
				}
			}
		}

		songCtrl.randomize();

		regenerateSongList();

		// play the first song
		if (forceThisSong == null) {
			playNextSong();
		} else {
			playSong(forceThisSong);
		}
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

		JMenuItem invertOrder = new JMenuItem("Invert Current Order");
		invertOrder.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.invertOrder();
				regenerateSongList();
			}
		});
		songs.add(invertOrder);

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
				configuration.create();
			}
		});
		songs.add(save);

		JMenu artists = new JMenu("Artists");
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

		JMenuItem artistsOfCurlyPlayedSong = new JMenuItem("Artists of Currently Played Song");
		artistsOfCurlyPlayedSong.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentlyPlayedSong == null) {
					songCtrl.selectSongsOfArtists(null);
				} else {
					songCtrl.selectSongsOfArtists(currentlyPlayedSong.getArtists());
				}
				songCtrl.randomize();
				regenerateSongList();
			}
		});
		artists.add(artistsOfCurlyPlayedSong);

		JMenuItem songsWithSameName = new JMenuItem("Songs with the Same Name (by Any Artist)");
		songsWithSameName.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentlyPlayedSong == null) {
					songCtrl.selectSongsByName(null);
				} else {
					songCtrl.selectSongsByName(currentlyPlayedSong.getTitle());
				}
				songCtrl.randomize();
				regenerateSongList();
			}
		});
		artists.add(songsWithSameName);

		JMenu artistsByName = new JMenu("Artists by Name...");
		artists.add(artistsByName);

		for (char c = 'A'; c <= 'Z'; c++) {
			addArtistsByNameSubMenu(artistsByName, ""+c);
		}
		addArtistsByNameSubMenu(artistsByName, "Other");

		artists.addSeparator();

		// actually, the artists that we have a lot of songs of are not necessarily the ones that
		// we want to see in the list, so load this from configuration by default:
		List<String> artistNames = configuration.getList(CONFIG_KEY_MAIN_ARTISTS);
		// and only get the top artists if there is no such configuration:
		if ((artistNames == null) || (artistNames.size() == 0)) {
			artistNames = songCtrl.getTopArtists(MAX_ARTISTS_PER_BUCKET);
		}

		for (final String artistName : artistNames) {
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

		JMenu playlists = new JMenu("Playlists");
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

		JMenuItem songsOfAllPlaylistsWithCurSong = new JMenuItem("Songs of All Playlists Including Current Song");
		songsOfAllPlaylistsWithCurSong.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				songCtrl.selectSongsOfPlaylistsWithCurSong(currentlyPlayedSong, allPlaylistRecords);
				songCtrl.randomize();
				regenerateSongList();
			}
		});
		playlists.add(songsOfAllPlaylistsWithCurSong);

		playlists.addSeparator();

		List<Record> playlistRecords = configuration.getAllContents().getArray(CONFIG_KEY_PLAYLISTS);

		allPlaylistRecords = new ArrayList<>();
		addPlaylistsBasedOnRecords(playlistRecords, playlists);

		JMenu skip = new JMenu("Skip");
		menu.add(skip);

		ActionListener skipClickListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveSkipState();
			}
		};

		skipWithDuration = new JCheckBoxMenuItem("Skip Songs With Duration");
		skipWithDuration.setSelected(configuration.getBoolean("skipSongsWithDuration", false));
		skipWithDuration.addActionListener(skipClickListener);
		skip.add(skipWithDuration);

		skipWithoutDuration = new JCheckBoxMenuItem("Skip Songs Without Duration");
		skipWithoutDuration.setSelected(configuration.getBoolean("skipSongsWithoutDuration", false));
		skipWithoutDuration.addActionListener(skipClickListener);
		skip.add(skipWithoutDuration);

		skip.addSeparator();

		skipWithRating = new JCheckBoxMenuItem("Skip Songs With Rating");
		skipWithRating.setSelected(configuration.getBoolean("skipSongsWithRating", false));
		skipWithRating.addActionListener(skipClickListener);
		skip.add(skipWithRating);

		skipBelow95 = new JCheckBoxMenuItem("Skip Songs With Rating Below 95%");
		skipBelow95.setSelected(configuration.getBoolean("skipSongsBelow95", false));
		skipBelow95.addActionListener(skipClickListener);
		skip.add(skipBelow95);

		skipBelow90 = new JCheckBoxMenuItem("Skip Songs With Rating Below 90%");
		skipBelow90.setSelected(configuration.getBoolean("skipSongsBelow90", false));
		skipBelow90.addActionListener(skipClickListener);
		skip.add(skipBelow90);

		skipBelow80 = new JCheckBoxMenuItem("Skip Songs With Rating Below 80%");
		skipBelow80.setSelected(configuration.getBoolean("skipSongsBelow80", false));
		skipBelow80.addActionListener(skipClickListener);
		skip.add(skipBelow80);

		skipBelow70 = new JCheckBoxMenuItem("Skip Songs With Rating Below 70%");
		skipBelow70.setSelected(configuration.getBoolean("skipSongsBelow70", false));
		skipBelow70.addActionListener(skipClickListener);
		skip.add(skipBelow70);

		skipBelow60 = new JCheckBoxMenuItem("Skip Songs With Rating Below 60%");
		skipBelow60.setSelected(configuration.getBoolean("skipSongsBelow60", false));
		skipBelow60.addActionListener(skipClickListener);
		skip.add(skipBelow60);

		skipBelow50 = new JCheckBoxMenuItem("Skip Songs With Rating Below 50%");
		skipBelow50.setSelected(configuration.getBoolean("skipSongsBelow50", false));
		skipBelow50.addActionListener(skipClickListener);
		skip.add(skipBelow50);

		skipBelow45 = new JCheckBoxMenuItem("Skip Songs With Rating Below 45%");
		skipBelow45.setSelected(configuration.getBoolean("skipSongsBelow45", false));
		skipBelow45.addActionListener(skipClickListener);
		skip.add(skipBelow45);

		skipWithoutRating = new JCheckBoxMenuItem("Skip Songs Without Rating");
		skipWithoutRating.setSelected(configuration.getBoolean("skipSongsWithoutRating", false));
		skipWithoutRating.addActionListener(skipClickListener);
		skip.add(skipWithoutRating);

		skip.addSeparator();

		JMenuItem skipNone = new JMenuItem("Play All");
		skipNone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				skipWithDuration.setSelected(false);
				skipWithoutDuration.setSelected(false);
				skipWithRating.setSelected(false);
				skipBelow95.setSelected(false);
				skipBelow90.setSelected(false);
				skipBelow80.setSelected(false);
				skipBelow70.setSelected(false);
				skipBelow60.setSelected(false);
				skipBelow50.setSelected(false);
				skipBelow45.setSelected(false);
				skipWithoutRating.setSelected(false);
				saveSkipState();
			}
		});
		skip.add(skipNone);

		JMenuItem discoverNew = new JMenuItem("Discover New Songs");
		discoverNew.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				skipWithDuration.setSelected(true);
				skipWithoutDuration.setSelected(false);
				skipWithRating.setSelected(true);
				skipBelow95.setSelected(false);
				skipBelow90.setSelected(false);
				skipBelow80.setSelected(false);
				skipBelow70.setSelected(false);
				skipBelow60.setSelected(false);
				skipBelow50.setSelected(false);
				skipBelow45.setSelected(false);
				skipWithoutRating.setSelected(false);
				saveSkipState();
			}
		});
		skip.add(discoverNew);

		JMenuItem defaultSkipping = new JMenuItem("Default Skipping");
		defaultSkipping.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				skipWithDuration.setSelected(false);
				skipWithoutDuration.setSelected(false);
				skipWithRating.setSelected(false);
				skipBelow95.setSelected(false);
				skipBelow90.setSelected(false);
				skipBelow80.setSelected(false);
				skipBelow70.setSelected(false);
				skipBelow60.setSelected(false);
				skipBelow50.setSelected(false);
				skipBelow45.setSelected(true);
				skipWithoutRating.setSelected(false);
				saveSkipState();
			}
		});
		skip.add(defaultSkipping);

		menu.add(new MenuItemForMainMenu("|"));

		MenuItemForMainMenu prev = new MenuItemForMainMenu("Previous");
		prev.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				playPreviousSong();
			}
		});
		menu.add(prev);

		pauseItem = new MenuItemForMainMenu("Pause");
		pauseItem.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (pauseItem.getTextContent().equals("Pause")) {
					pauseCurSong();
					pauseItem.setText("Continue");
				} else {
					continueCurSong();
					pauseItem.setText("Pause");
				}
			}
		});
		menu.add(pauseItem);

		MenuItemForMainMenu next = new MenuItemForMainMenu("Next");
		next.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				playNextSong();
			}
		});
		menu.add(next);

		menu.add(new MenuItemForMainMenu("|"));

		MenuItemForMainMenu songIsOver = new MenuItemForMainMenu("Song is Over");
		songIsOver.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				curSongIsOver();
			}
		});
		menu.add(songIsOver);

		MenuItemForMainMenu resetLength = new MenuItemForMainMenu("Reset Length");
		resetLength.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				resetCurSongLength();
			}
		});
		menu.add(resetLength);

		menu.add(new MenuItemForMainMenu("|"));

		minimizeMaximize = new MenuItemForMainMenu("Maximize");
		minimizeMaximize.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (minimizeMaximize.getTextContent().trim().equals("Maximize")) {
					minimizeMaximize.setText("Minimize");
					maximize();
				} else {
					minimizeMaximize.setText("Maximize");
					minimize();
				}
			}
		});
		menu.add(minimizeMaximize);

		MenuItemForMainMenu close = new MenuItemForMainMenu("Close");
		close.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
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
				String aboutMessage = "This is the " + MetaPlayer.PROGRAM_TITLE + ".\n" +
					"Version: " + MetaPlayer.VERSION_NUMBER + " (" + MetaPlayer.VERSION_DATE + ")\n" +
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
			public void onBarMove(Integer position) {
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
					StringSelection selection = new StringSelection(
						currentlyPlayedSong.getClipboardText(songCtrl, allPlaylistRecords));
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

	private void addPlaylistsBasedOnRecords(List<Record> playlistRecords, JMenu parentElement) {

		for (final Record playlistRecord : playlistRecords) {

			List<Record> sublists = playlistRecord.getValues(SongCtrl.PLAYLIST_SUBLISTS_KEY);

			if ((sublists != null) && (sublists.size() > 0)) {

				JMenu submenu = new JMenu(playlistRecord.getString(SongCtrl.PLAYLIST_NAME_KEY));

				addPlaylistsBasedOnRecords(sublists, submenu);

				parentElement.add(submenu);

			} else {

				// sort and remove duplicates
				List<String> songList = playlistRecord.getArrayAsStringList(SongCtrl.PLAYLIST_SONGS_KEY);
				songList = StrUtils.sortAndRemoveDuplicates(songList);
				playlistRecord.set(SongCtrl.PLAYLIST_SONGS_KEY, songList);

				List<String> artistList = playlistRecord.getArrayAsStringList(SongCtrl.PLAYLIST_ARTISTS_KEY);
				artistList = StrUtils.sortAndRemoveDuplicates(artistList);
				playlistRecord.set(SongCtrl.PLAYLIST_ARTISTS_KEY, artistList);

				allPlaylistRecords.add(playlistRecord);

				JMenuItem playlistItem = new JMenuItem(playlistRecord.getString(SongCtrl.PLAYLIST_NAME_KEY));
				playlistItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						songCtrl.selectPlaylist(playlistRecord, allPlaylistRecords);
						songCtrl.randomize();
						regenerateSongList();
					}
				});
				parentElement.add(playlistItem);
			}
		}
	}

	private void saveSkipState() {
		configuration.set("skipSongsWithDuration", skipWithDuration.isSelected());
		configuration.set("skipSongsWithoutDuration", skipWithoutDuration.isSelected());
		configuration.set("skipSongsWithRating", skipWithRating.isSelected());
		configuration.set("skipSongsBelow95", skipBelow95.isSelected());
		configuration.set("skipSongsBelow90", skipBelow90.isSelected());
		configuration.set("skipSongsBelow80", skipBelow80.isSelected());
		configuration.set("skipSongsBelow70", skipBelow70.isSelected());
		configuration.set("skipSongsBelow60", skipBelow60.isSelected());
		configuration.set("skipSongsBelow50", skipBelow50.isSelected());
		configuration.set("skipSongsBelow45", skipBelow45.isSelected());
		configuration.set("skipSongsWithoutRating", skipWithoutRating.isSelected());
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

		mainFrame.setLocation(new Point(-8, (int) screenSize.getHeight() - 50));
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
		mainFrame.setTitle(MetaPlayer.PROGRAM_TITLE + " - " + songCtrl.getSongAmount() + " songs loaded");
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

		boolean notifyListeners = false;
		ratingItem.setBarPosition(song.getRating(), notifyListeners);

		// explicitly continue a paused song (if there is any), such that it can be stopped
		// in the next line
		timingCtrl.continueSong();

		// stop playing the current song as we do not want to play two at the same time
		timingCtrl.stopPlaying();

		try {
			Process process = IoUtils.executeAsync(player, song.getPath());

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

	private void importSongs() {

		OpenFileDialog filePicker = new OpenFileDialog();

		// use the last-used directory
		String lastDirectory = configuration.getValue(CONFIG_KEY_LAST_SONG_DIRECTORY);

		if ((lastDirectory != null) && !"".equals(lastDirectory)) {
			filePicker.setCurrentDirectory(new Directory(lastDirectory));
		}

		filePicker.setDialogTitle("Open Songs or Directories to Import");
		filePicker.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		filePicker.setMultiSelectionEnabled(true);

		filePicker.showOpenDialog(new CallbackWithStatus() {

			public void call(int status) {

				switch (status) {

					case OpenFileDialog.APPROVE_OPTION:

						// load the files
						configuration.set(CONFIG_KEY_LAST_SONG_DIRECTORY, filePicker.getCurrentDirectory().getAbsoluteDirname());

						for (File curFile : filePicker.getSelectedFiles()) {
							importSong(curFile);
						}
						for (Directory curFolder : filePicker.getSelectedDirectories()) {
							importSongsRecursively(curFolder);
						}

						songCtrl.save();
						regenerateSongList();

						break;

					case OpenFileDialog.CANCEL_OPTION:
						// cancel was pressed... do nothing for now
						break;
				}
			}
		});
	}

	private void importSong(File file) {
		String lowFilename = file.getFilename().toLowerCase();
		if (!(lowFilename.endsWith(".jpg") ||
			  lowFilename.endsWith(".png") ||
			  lowFilename.endsWith(".bmp") ||
			  lowFilename.endsWith(".txt") ||
			  lowFilename.endsWith(".lnk") ||
			  lowFilename.endsWith(".srt"))) {
			songCtrl.addUnlessAlreadyPresent(new Song(file));
		}
	}

	private void importSongsRecursively(Directory parent) {

		boolean recursive = true;

		List<File> curFiles = parent.getAllFiles(recursive);

		for (File curFile : curFiles) {
			importSong(curFile);
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

		timeRemainingItem.setText(minutes + ":" + seconds + "  ");
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

	/**
	 * Are we skipping this song?
	 */
	public boolean skippingSong(Song song) {

		if (skipWithDuration.isSelected() && song.hasLength()) {
			return true;
		}

		if (skipWithoutDuration.isSelected() && !song.hasLength()) {
			return true;
		}

		if (skipWithRating.isSelected() && song.hasRating()) {
			return true;
		}

		if (song.hasRating()) {
			int rating = song.getRatingInt();
			if (skipBelow95.isSelected() && (rating < 95)) {
				return true;
			}
			if (skipBelow90.isSelected() && (rating < 90)) {
				return true;
			}
			if (skipBelow80.isSelected() && (rating < 80)) {
				return true;
			}
			if (skipBelow70.isSelected() && (rating < 70)) {
				return true;
			}
			if (skipBelow60.isSelected() && (rating < 60)) {
				return true;
			}
			if (skipBelow50.isSelected() && (rating < 50)) {
				return true;
			}
			if (skipBelow45.isSelected() && (rating < 45)) {
				return true;
			}
		}

		if (skipWithoutRating.isSelected() && !song.hasRating()) {
			return true;
		}

		return false;
	}

	private void addArtistsByNameSubMenu(JMenu artistsByName, String bucketName) {

		JMenu bucketMenu = new JMenu(bucketName + "...");
		artistsByName.add(bucketMenu);

		// add the top 32 artists for each bucket
		List<String> artistNames = songCtrl.getTopArtists(MAX_ARTISTS_PER_BUCKET, bucketName);

		for (final String artistName : artistNames) {
			JMenuItem artistItem = new JMenuItem(artistName);
			artistItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					songCtrl.selectSongsOfArtist(artistName);
					songCtrl.randomize();
					regenerateSongList();
				}
			});
			bucketMenu.add(artistItem);
		}
	}

}
