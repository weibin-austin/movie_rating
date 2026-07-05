package com.antra.movie_rating.exception;

public class RatingNotExistException extends RuntimeException {
	public RatingNotExistException() {
	}

	public RatingNotExistException(String message) {
		super(message);
	}
}
