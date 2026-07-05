package com.antra.movie_rating.service;

import com.antra.movie_rating.api.response.RateScoreVO;
import com.antra.movie_rating.api.response.RatingCommentResponseVO;
import com.antra.movie_rating.dao.MovieAverageScoreCustomRepo;
import com.antra.movie_rating.dao.MovieAverageScoreRepository;
import com.antra.movie_rating.dao.MovieCharctDAO;
import com.antra.movie_rating.dao.MovieRatingDAO;
import com.antra.movie_rating.domain.MovieAverageScore;
import com.antra.movie_rating.domain.MovieCharact;
import com.antra.movie_rating.domain.MovieRating;
import com.antra.movie_rating.domain.MovieScore;
import com.antra.movie_rating.exception.RatingNotExistException;
import com.antra.movie_rating.utility.RatingConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MovieRatingServiceImpl implements MovieRatingService {

	@Autowired
	MovieCharctDAO charctDAO;

	@Autowired
	MovieRatingDAO ratingDAO;

	@Autowired
	MovieAverageScoreRepository avgScoreDAO;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value="charactCache")
	public List<MovieCharact> getAllMovieCharact() {
		return charctDAO.findAll();
	}

	@Override
	@Transactional
	public MovieRating saveRating(MovieRating rating) {
		rating = ratingDAO.save(rating);
		avgScoreDAO.updateAverage(rating.getMovie().getId());
		return rating;
	}

	@Override
	@Transactional
	public MovieRating updateRating(MovieRating updated) {
		List<MovieRating> existing = ratingDAO.findByUserIdAndMovieId(
				updated.getUser().getId(), updated.getMovie().getId());
		if (existing == null || existing.isEmpty()) {
			throw new RatingNotExistException("User has not rated this movie yet");
		}
		MovieRating rating = existing.get(0);
		rating.setComment(updated.getComment());
		rating.getScores().clear();
		for (MovieScore score : updated.getScores()) {
			score.setRating(rating);
			rating.getScores().add(score);
		}
		rating = ratingDAO.save(rating);
		avgScoreDAO.updateAverage(rating.getMovie().getId());
		return rating;
	}

	@Override
	@Transactional
	public void deleteRating(int movieId, int userId) {
		List<MovieRating> existing = ratingDAO.findByUserIdAndMovieId(userId, movieId);
		if (existing == null || existing.isEmpty()) {
			throw new RatingNotExistException("User has not rated this movie");
		}
		ratingDAO.deleteAll(existing);
		avgScoreDAO.updateAverage(movieId);
	}

	@Override
	@Transactional(readOnly = true)
	public Float getAverageScore(int movieId) {
		MovieAverageScore avg = avgScoreDAO.findByMovieId(movieId);
		return avg == null ? null : avg.getAverageScore();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean getIfUserCanRateMovie(int movieId, int userId) {
		List<MovieRating> result = ratingDAO.findByUserIdAndMovieId(userId, movieId);
		return result == null || result.size() == 0;
	}

	@Override
	@Transactional(readOnly = true)
	public List<RatingCommentResponseVO> getRatingCommentInfo(int movieId, int page, int recordNo, Date fromTime) {
		Pageable pageCriteria = PageRequest.of(page, recordNo, Sort.by(Sort.Direction.DESC, "createdDate"));
		List<MovieRating> ratingList = ratingDAO.findByCreatedDateBeforeAndMovieId(fromTime, movieId, pageCriteria);
		return RatingConverter.convertRatingListToResponseVO(ratingList);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RatingCommentResponseVO> getRatingByUserId(int userId,int recordNo, int page) {
		Pageable pageCriteria = PageRequest.of(page, recordNo, Sort.by(Sort.Direction.DESC, "createdDate"));
		List<MovieRating> ratingList = ratingDAO.findByUserIdOrderByCreatedDateDesc(userId, pageCriteria);

		return RatingConverter.convertRatingListToResponseVO(ratingList);
	}

}
