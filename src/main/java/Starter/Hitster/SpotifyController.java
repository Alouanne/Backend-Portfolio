package Starter.Hitster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
@RestController
public class SpotifyController {

  @GetMapping("/auth/spotify")
  public ResponseEntity<Void> redirectToSpotify(@RequestParam String state) {
    String clientId = SecretDate.getClientId();
    String redirectUri = "http://localhost:8080/callback";

    String scope = "user-read-playback-state user-modify-playback-state playlist-read-private streaming";
    String authUrl = "https://accounts.spotify.com/authorize" +
            "?client_id=" + clientId +
            "&response_type=code" +
            "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
            "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
            "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
  }
  @GetMapping("/callback")
  public ResponseEntity<String> handleSpotifyCallback(
          @RequestParam String code,
          @RequestParam String state) {

    String clientId = SecretDate.getClientId();
    String clientSecret = SecretDate.getClientSecret();
    String redirectUri = "http://localhost:8080/callback";

    String credentials = clientId + ":" + clientSecret;
    String encodedCredentials = Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set("Authorization", "Basic " + encodedCredentials);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("code", code);
    params.add("redirect_uri", redirectUri);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(
              "https://accounts.spotify.com/api/token",
              request,
              Map.class);
      if (response.getStatusCode() == HttpStatus.OK) {
        String accessToken = (String) response.getBody().get("access_token");

        // Redirect to your HTML page with the access token as a URL param (or hash)
        String redirectUrl = "http://localhost:63342/alouanne.github.io/OnlineHitster/Hitster.html?access_token=" + accessToken + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
      }
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error during token request: " + e.getMessage());
    }
  }

  @GetMapping("/getPlaylist")
  public ResponseEntity<String> getPlaylist(@RequestParam String playlistId, @RequestParam String accessToken) {
    List<JsonNode> allTracks = new ArrayList<>();
    String nextUrl = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks?offset=0&limit=100";

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    ObjectMapper mapper = new ObjectMapper();

    try {
      do {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(nextUrl, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
          JsonNode data = mapper.readTree(response.getBody());
          JsonNode items = data.has("items") ? data.get("items") : data.get("tracks").get("items");
          if (items != null && items.isArray()) {
            items.forEach(allTracks::add);
          }

          JsonNode next = data.has("next") ? data.get("next") : data.get("tracks").get("next");
          nextUrl = next != null && !next.isNull() ? next.asText() : null;
        } else {
          return ResponseEntity.status(response.getStatusCode()).body("Failed to fetch playlist: " + response.getStatusCode());
        }

      } while (nextUrl != null);

      String resultJson = mapper.writeValueAsString(allTracks);
      return ResponseEntity.ok(resultJson);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching playlist: " + e.getMessage());
    }

  }

}

