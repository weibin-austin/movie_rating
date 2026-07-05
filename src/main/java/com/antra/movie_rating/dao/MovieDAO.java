package com.antra.movie_rating.dao;


import com.antra.movie_rating.domain.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieDAO extends JpaRepository<Movie, Integer> {

	List<Movie> findByTitleIgnoreCase(String title);

	Movie findByImdbIdIgnoreCase(String imdbId);

	@Query("select s.movie from movie_average s order by s.averageScore desc")
	List<Movie> findPopular(Pageable pageable);

	@Query("select s.movie from movie_average s where lower(s.movie.genre) like lower(concat('%', :genre, '%')) order by s.averageScore desc")
	List<Movie> findPopularByGenre(@Param("genre") String genre, Pageable pageable);
}
