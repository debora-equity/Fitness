package com.softgenia.playlist.exception;

import org.springframework.data.crossstore.ChangeSetPersister;

public class VideoException extends ChangeSetPersister.NotFoundException {
    @Override
    public String getMessage() {
        return "Video does not exists";
    }
}
