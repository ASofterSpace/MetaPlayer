/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

public class Artist {

	private String name;
	private int songs;


	public Artist(String name) {
		this.name = name;
		this.songs = 1;
	}

	public void addSong() {
		songs = songs + 1;
	}

	public int getSongAmount() {
		return songs;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof Artist) {
			Artist otherArtist = (Artist) other;
			if (name == null) {
				return otherArtist.name == null;
			}
			return name.equals(otherArtist.name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (name == null) {
			return 0;
		}
		return name.hashCode();
	}
}
