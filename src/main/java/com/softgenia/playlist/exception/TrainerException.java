package com.softgenia.playlist.exception;

import org.springframework.data.crossstore.ChangeSetPersister;

public class TrainerException extends ChangeSetPersister.NotFoundException {
    @Override
    public String getMessage() {
        return "Trainer does not exists";
    }
}
