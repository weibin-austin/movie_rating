package com.antra.movie_rating;

import com.antra.movie_rating.dao.MovieAverageScoreRepository;
import com.antra.movie_rating.dao.MovieCharctDAO;
import com.antra.movie_rating.dao.MovieDAO;
import com.antra.movie_rating.dao.UserRepository;
import com.antra.movie_rating.domain.Movie;
import com.antra.movie_rating.domain.MovieCharact;
import com.antra.movie_rating.domain.MovieRating;
import com.antra.movie_rating.domain.MovieScore;
import com.antra.movie_rating.domain.User;
import com.antra.movie_rating.exception.RatingNotExistException;
import com.antra.movie_rating.service.MovieRatingService;
import com.antra.movie_rating.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RatingFlowIntegrationTests {

	@Autowired
	MovieDAO movieDAO;
	@Autowired
	MovieCharctDAO charctDAO;
	@Autowired
	UserRepository userRepository;
	@Autowired
	MovieRatingService ratingService;
	@Autowired
	MovieService movieService;
	@Autowired
	MovieAverageScoreRepository avgScoreDAO;

	private Movie movie;
	private User user;
	private MovieCharact charact;

	@BeforeEach
	public void setUp() {
		movie = movieDAO.save(Movie.builder().title("Test Movie").imdbId("tt7654321").genre("Action, Drama").build());
		charact = charctDAO.save(MovieCharact.builder().id(1).name("Plot").build());
		user = userRepository.save(new User("Test User", "tester" + System.nanoTime(),
				System.nanoTime() + "@test.com", "secret", new java.util.HashSet<>()));
	}

	private MovieRating buildRating(int score) {
		MovieRating rating = new MovieRating();
		rating.setComment("score " + score);
		rating.setUser(user);
		rating.setMovie(movie);
		List<MovieScore> scores = new ArrayList<>();
		MovieScore s = new MovieScore();
		s.setCharact(charact);
		s.setScore(score);
		s.setRating(rating);
		scores.add(s);
		rating.setScores(scores);
		return rating;
	}

	@Test
	public void rateEditDeleteFlow() {
		// rate: average appears
		ratingService.saveRating(buildRating(2));
		assertEquals(2f, ratingService.getAverageScore(movie.getId()));

		// edit: same user's rating is replaced, average follows
		ratingService.updateRating(buildRating(5));
		assertEquals(5f, ratingService.getAverageScore(movie.getId()));
		assertEquals(1, ratingService.getRatingByUserId(user.getId(), 10, 0).size());

		// delete: rating and average are gone
		ratingService.deleteRating(movie.getId(), user.getId());
		assertNull(ratingService.getAverageScore(movie.getId()));
		assertTrue(ratingService.getIfUserCanRateMovie(movie.getId(), user.getId()));

		// deleting or editing a missing rating is a 404-mapped error
		assertThrows(RatingNotExistException.class, () -> ratingService.deleteRating(movie.getId(), user.getId()));
		assertThrows(RatingNotExistException.class, () -> ratingService.updateRating(buildRating(3)));
	}

	@Test
	public void ratingTotalReflectsUserRatings() {
		// a fresh user has no ratings
		assertEquals(0L, ratingService.getRatingTotalByUserId(user.getId()));

		// one rating -> count 1
		ratingService.saveRating(buildRating(4));
		assertEquals(1L, ratingService.getRatingTotalByUserId(user.getId()));

		// a rating on a second movie -> count 2
		Movie other = movieDAO.save(Movie.builder().title("Second Movie").imdbId("tt7654399").genre("Comedy").build());
		MovieRating secondRating = buildRating(3);
		secondRating.setMovie(other);
		ratingService.saveRating(secondRating);
		assertEquals(2L, ratingService.getRatingTotalByUserId(user.getId()));

		// the count is per-user: a different user still has 0
		User otherUser = userRepository.save(new User("Other User", "other" + System.nanoTime(),
				System.nanoTime() + "@test.com", "secret", new java.util.HashSet<>()));
		assertEquals(0L, ratingService.getRatingTotalByUserId(otherUser.getId()));

		// deleting one rating brings the count back down
		ratingService.deleteRating(movie.getId(), user.getId());
		assertEquals(1L, ratingService.getRatingTotalByUserId(user.getId()));
	}

	@Test
	public void popularMoviesComeFromStoredAverages() {
		Movie other = movieDAO.save(Movie.builder().title("Other Movie").imdbId("tt7654322").genre("Animation").build());
		ratingService.saveRating(buildRating(3));
		MovieRating otherRating = buildRating(5);
		otherRating.setMovie(other);
		otherRating.getScores().get(0).setRating(otherRating);
		ratingService.saveRating(otherRating);

		List<Movie> popular = movieService.searchMovie("popular");
		assertTrue(popular.size() >= 2);
		// ranked by average score descending
		assertEquals(other.getId(), popular.get(0).getId());

		List<Movie> cartoons = movieService.searchMovie("cartoon");
		assertTrue(cartoons.stream().allMatch(m -> m.getGenre().contains("Animation")));
		assertTrue(cartoons.stream().anyMatch(m -> m.getId().equals(other.getId())));

		List<Movie> action = movieService.searchMovie("action");
		assertTrue(action.stream().anyMatch(m -> m.getId().equals(movie.getId())));
	}
}
