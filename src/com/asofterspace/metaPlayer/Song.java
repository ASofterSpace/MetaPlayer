/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.Record;

import java.io.File;


public class Song {

	private String artist;
	private String title;
	private String path;
	private Integer length;
	private Integer rating;


	public Song() {}

	public Song(Record record) {
		this.artist = record.getString("artist");
		this.title = record.getString("title");
		this.path = record.getString("path");
		this.length = record.getInteger("length");
		this.rating = record.getInteger("rating");
	}

	public Record toRecord() {
		Record result = new Record();
		result.set("artist", new Record(artist));
		result.set("title", new Record(title));
		result.set("path", new Record(path));
		result.set("length", new Record(length));
		result.set("rating", new Record(rating));
		return result;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	public void setLength(String length) {
		try {
			this.length = Integer.parseInt(length);
		} catch (NumberFormatException e) {}
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public void setRating(String rating) {
		try {
			this.rating = Integer.parseInt(rating);
		} catch (NumberFormatException e) {}
	}

	@Override
	public String toString() {
		if (artist == null) {
			return title;
		}
		return artist + " - " + title;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof Song) {
			Song otherSong = (Song) other;
			if (otherSong.path == null) {
				if (path == null) {
					return true;
				} else {
					return false;
				}
			}
			return otherSong.path.equals(path);
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (path == null) {
			return 0;
		}
		return path.hashCode();
	}

}
