package com.antra.movie_rating.api.response;

import lombok.Data;

@Data
public class JwtAuthenticationResponse {
	private String accessToken;
	private String tokenType = "Bearer";
	private String name;
	private String username;
	private String email;

	public JwtAuthenticationResponse(String accessToken) {
		this.accessToken = accessToken;
	}

	public JwtAuthenticationResponse(String accessToken, String name, String username, String email) {
		this.accessToken = accessToken;
		this.name = name;
		this.username = username;
		this.email = email;
	}
}
