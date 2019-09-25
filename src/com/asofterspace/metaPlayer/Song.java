/**
 * Unlicensed code created by A Softer Space, 2019
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.metaPlayer;

import com.asofterspace.toolbox.io.Record;


public class Song {

	private String artist;
	private String title;
	private String path;
	private Integer length;
	private Integer rating;


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

	@Override
	public String toString() {
		return artist + " - " + title;
	}

}
