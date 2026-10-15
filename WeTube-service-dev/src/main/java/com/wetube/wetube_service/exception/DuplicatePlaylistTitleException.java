package com.wetube.wetube_service.exception;

public class DuplicatePlaylistTitleException extends Throwable {
    public DuplicatePlaylistTitleException(String title) {
        super("Playlist with title '" + title + "' has already been added.");
    }
}
