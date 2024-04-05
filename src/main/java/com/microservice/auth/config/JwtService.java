package com.microservice.auth.config;

import com.microservice.auth.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Data
public class JwtService {

	@Value("${application.security.jwt.secret-key}")
	private String secretKey;
	@Value("${application.security.jwt.expiration}")
	private long jwtExpiration;
	@Value("${application.security.jwt.refresh-token.expiration}")
	private long refreshExpiration;

	public boolean isRefreshTokenValid(String refreshToken, UserDetails userDetails) {
		final String username = extractPhone(refreshToken);
		return (username.equals(userDetails.getUsername())) && !isTokenExpired(refreshToken);
	}

	public String extractPhone(String token) {
		return extractClaims(token, Claims::getSubject);
	}

	public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public String extractUserId(String token) {
		return extractClaims(token, claims -> claims.get("id", String.class));
	}

	public String generateToken(User userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	public String generateToken(
			Map<String, Object> extraClaims,
			User userDetails
	) {
		extraClaims.put("lang", userDetails.getLocale());
		extraClaims.put("roles", userDetails.getRoles());
		return buildToken(extraClaims, userDetails, jwtExpiration);
	}

	public String generateRefreshToken(User userDetails) {
		return generateRefreshToken(userDetails, new HashMap<>());
	}

	public String generateRefreshToken(User userDetails, Map<String, Object> extraClaims) {
		extraClaims.put("id", userDetails.getId());
		return buildToken(extraClaims, userDetails, refreshExpiration);
	}

	private String buildToken(
			Map<String, Object> extraClaims,
			UserDetails userDetails,
			long expiration
	) {
		return Jwts
				.builder()
				.setClaims(extraClaims)
				.setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getSignInKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractPhone(token);
		return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
	}


	boolean isTokenExpired(String token) {
		boolean before;
		try {
			before = extractExpiration(token).before(new Date());
		} catch (ExpiredJwtException e) {
			before = true;
		}

		return before;
	}

	private Date extractExpiration(String token) {
		return extractClaims(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		return Jwts
				.parserBuilder()
				.setSigningKey(getSignInKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	private Key getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
