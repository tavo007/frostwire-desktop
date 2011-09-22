package com.frostwire.gui.library;

import java.io.File;
import java.util.Map;

import org.limewire.util.FilenameUtils;
import org.limewire.util.StringUtils;

import com.frostwire.mp3.ID3v2;
import com.frostwire.mp3.Mp3File;
import com.frostwire.mplayer.MPlayer;

public class AudioMetaData {

    private String title;
    private float durationInSecs;
    private String artist;
    private String album;
    private String bitrate;
    private String comment;
    private String genre;
    private String track;
    private String year;

    public AudioMetaData(File file) {
        readUsingMPlayer(file);
        if (file.getName().endsWith("mp3")) {
            readUsingMP3Tags(file);
        }

        sanitizeData(file);
    }

    public String getTitle() {
        return title;
    }

    public float getDurationInSecs() {
        return durationInSecs;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getBitrate() {
        return bitrate;
    }

    public String getComment() {
        return comment;
    }

    public String getGenre() {
        return genre;
    }

    public String getTrack() {
        return track;
    }

    public String getYear() {
        return year;
    }

    private void readUsingMPlayer(File file) {
        MPlayer mplayer = new MPlayer();
        Map<String, String> properties = mplayer.getProperties(file.getAbsolutePath());

        title = properties.get("Title");
        durationInSecs = parseDurationInSecs(properties.get("ID_LENGTH"));
        artist = properties.get("Artist");
        album = properties.get("Album");
        bitrate = parseBitrate(properties.get("ID_AUDIO_BITRATE"));
        comment = properties.get("Comment");
        genre = properties.get("Genre");
        track = properties.get("Track");
        year = properties.get("Year");
    }

    private void readUsingMP3Tags(File file) {
        try {
            Mp3File mp3 = new Mp3File(file.getAbsolutePath());
            if (mp3.hasId3v2Tag()) {
                ID3v2 tag = mp3.getId3v2Tag();
                if (StringUtils.isNullOrEmpty(title, true)) {
                    title = tag.getTitle();
                }
                if (StringUtils.isNullOrEmpty(artist, true)) {
                    artist = tag.getArtist();
                }
                if (StringUtils.isNullOrEmpty(album, true)) {
                    album = tag.getAlbum();
                }
                if (StringUtils.isNullOrEmpty(comment, true)) {
                    comment = tag.getComment();
                    if (comment != null && (comment.startsWith("0") || comment.startsWith(" 0"))) {
                        comment = tag.getItunesComment();
                        comment = comment.substring(50);
                    }
                }
                if (StringUtils.isNullOrEmpty(genre, true) || genre.trim().equals("Unknown")) {
                    genre = tag.getGenreDescription();
                }
                if (StringUtils.isNullOrEmpty(track, true)) {
                    track = tag.getTrack();
                }
                if (StringUtils.isNullOrEmpty(year, true)) {
                    year = tag.getYear();
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void sanitizeData(File file) {
        if (StringUtils.isNullOrEmpty(title, true)) {
            title = FilenameUtils.getBaseName(file.getAbsolutePath());
        }
        if (durationInSecs < 0) {
            durationInSecs = 0;
        }
        if (artist == null) {
            artist = "";
        }
        if (album == null) {
            album = "";
        }
        if (bitrate == null) {
            bitrate = "";
        }
        if (comment == null) {
            comment = "";
        }

        if (genre == null) {
            genre = "";
        }

        if (track == null) {
            track = "";
        } else {
            int index = -1;
            index = track.indexOf('/');
            if (index != -1) {
                track = track.substring(0, index);
            }
        }

        if (year == null) {
            year = "";
        }
    }

    private String parseBitrate(String bitrate) {
        if (bitrate == null) {
            return "";
        }
        try {
            return (Integer.parseInt(bitrate) / 1000) + " kbps";
        } catch (Exception e) {
            return bitrate;
        }
    }

    public float parseDurationInSecs(String durationInSecs) {
        try {
            return Float.parseFloat(durationInSecs);
        } catch (Exception e) {
            return 0;
        }
    }
}