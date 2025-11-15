package com.softgenia.playlist.exception;

import org.springframework.data.crossstore.ChangeSetPersister;

public class WorkoutException extends ChangeSetPersister.NotFoundException {
    @Override
    public String getMessage() {
        return "Workout does not exists";
    }
}
