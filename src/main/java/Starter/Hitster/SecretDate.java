package Starter.Hitster;

public class SecretDate {
    public static String getClientSecret() {
      return System.getenv("CLIENT_SECRET");
    }

    public static String getClientId() {
      return System.getenv("CLIENT_ID");
    }
    public static String getRedirectUri() {
      return System.getenv("REDIRECT_URL  ");

    }
}
