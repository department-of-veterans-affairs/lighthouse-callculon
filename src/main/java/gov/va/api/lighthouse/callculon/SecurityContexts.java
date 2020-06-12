package gov.va.api.lighthouse.callculon;

import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.SneakyThrows;

public class SecurityContexts {

  /**
   * Provide an SSL context that will accept the VA certs that can get in the way of the load
   * balancer.
   */
  @SneakyThrows
  public static SSLContext relaxed() {
    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            private void check(X509Certificate[] certs)
                throws CertificateExpiredException, CertificateNotYetValidException {
              for (var cert : certs) {
                cert.checkValidity();
              }
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
              check(certs);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
              check(certs);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return Optional.<X509Certificate[]>empty().orElse(null); // hush.
            }
          }
        };

    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    return sc;
    // HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  }
}
