# Movie Rating backend

## Development 
Using Spring Boot main class to start the app.
* Lombok is used. Make sure you have the plugin of Lombok in IntelliJ or eclipse.
### DB 
#### 1. MySQL
Before running the main app, run DB scripts to generate all the tables.
Make sure the datasource is changed in `application.properties` file.

#### 2. H2 - Embedded DB
The branch `h2_as_db` is using embedded database instead of MySQL.
No scripts need to be run.

## Security
Spring Security, DB based user authentication and JWT are used.
### User sign up
basic information of users are saved in DB. Password is bcrypted.
### User log in
Using username or email  +  password to log in. JWT token will be returned for the valid combination.
After that, all API calls needs authorization header.

## Front-end is the project listed in [`rating`](https://github.com/AntraJava/movie_rating_front_end)

## API documents

[Swagger](http://localhost:8080/swagger-ui.html)

## To Do
### No UI changes
* [x] Search by `IMDB` or title: the existing search field (`GET /movie?title=`) now detects IMDB ids (`tt0120338`) and searches by id, in the DB first and then OMDB.
* [x] Popular movies (`GET /movie/popular?type=popular`) are no longer hard coded: they are the top movies ranked by stored average rating.
* [x] Popular Action / Cartoon movies (`type=action` / `type=cartoon`) filter that ranking by genre (`cartoon` maps to OMDB's `Animation` genre; any other type is treated as a genre name).
* [x] Cache expiration: caches use Caffeine with `expireAfterWrite=10m` (see `spring.cache.*` in `application.properties`).

### UI changes (backend part done in this repo)
* [x] Show user's name on the upper right: `POST /api/auth/signin` now returns `name`, `username` and `email` alongside the token (the JWT also carries a `name` claim). Front-end still needs to display it.
* [x] Delete rating: `DELETE /rating?movieId={id}` removes the logged-in user's rating for that movie and recalculates the average. Front-end needs a delete button.
* [x] Edit rating: `PUT /rating` with the same body as the create call replaces the logged-in user's rating and recalculates the average. Front-end needs an edit flow.
