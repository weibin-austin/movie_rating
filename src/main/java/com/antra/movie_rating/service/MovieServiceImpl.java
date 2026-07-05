package com.antra.movie_rating.service;

import com.antra.movie_rating.api.request.MovieCriteria;
import com.antra.movie_rating.dao.MovieAverageScoreRepository;
import com.antra.movie_rating.dao.MovieDAO;
import com.antra.movie_rating.domain.Movie;
import com.antra.movie_rating.domain.MovieAverageScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class MovieServiceImpl implements MovieService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MovieServiceImpl.class);

	@Autowired
	MovieDAO movieDAO;

	@Autowired
	MovieAverageScoreRepository avgScoreDAO;

	// OMDB IMDB ids look like tt0120338; the same search field accepts either an id or a title
	private static final Pattern IMDB_ID_PATTERN = Pattern.compile("(?i)tt\\d{6,10}");

	@Override
	@Cacheable(key="#criteria.title", value="movieCache", sync = true)
	@Transactional
	public Movie searchMovie(MovieCriteria criteria) {
		String search = criteria.getTitle() == null ? "" : criteria.getTitle().trim();
		boolean byImdbId = IMDB_ID_PATTERN.matcher(search).matches();

		if (byImdbId) {
			Movie found = movieDAO.findByImdbIdIgnoreCase(search);
			if (found != null) {
				return found;
			}
		} else {
			List<Movie> res = movieDAO.findByTitleIgnoreCase(search);
			if (res.size() > 0) {
				return res.get(0);
			}
		}

		String url = "http://www.omdbapi.com";
		RestTemplate rt = new RestTemplate();

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
				.queryParam("apikey", "27c4caaf")
				.queryParam(byImdbId ? "i" : "t", search)
				.queryParam("plot", "full");

		RequestEntity<Void> request = RequestEntity
					.get(uriBuilder.build().toUri()).accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<Movie> movie = rt.exchange(request, Movie.class);

		LOGGER.info(movie.toString());
		if (movie.getBody().getImdbId() != null
				&& movieDAO.findByImdbIdIgnoreCase(movie.getBody().getImdbId()) == null) {
			movieDAO.save(movie.getBody());
		}

		return movie.getBody();
	}

	private static final int POPULAR_LIMIT = 20;

	@Override
	@Cacheable(value="popularMovieCache", key="#type", sync = true)
	@Transactional(readOnly = true)
	public List<Movie> searchMovie(String type) {
		Pageable top = PageRequest.of(0, POPULAR_LIMIT);
		switch (type == null ? "" : type.toLowerCase()) {
			case "popular":
				return movieDAO.findPopular(top);
			case "cartoon":
				// OMDB tags cartoons with the "Animation" genre
				return movieDAO.findPopularByGenre("Animation", top);
			default:
				// any other type ("action", "drama", ...) is treated as a genre
				return movieDAO.findPopularByGenre(type, top);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public MovieAverageScore getMovieAverageScoreById(Integer id) {
		return avgScoreDAO.findByMovieId(id);
	}

	@Override
	public List<Movie> searchAllMovies() {
		return movieDAO.findAll();
	}

}
