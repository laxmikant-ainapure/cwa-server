

package app.coronawarn.server.services.submission.controller;

import static java.time.ZoneOffset.UTC;

import app.coronawarn.server.common.protocols.external.exposurenotification.ReportType;
import app.coronawarn.server.common.protocols.external.exposurenotification.TemporaryExposureKey;
import app.coronawarn.server.common.protocols.internal.SubmissionPayload;
import com.google.protobuf.ByteString;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * RequestExecutor executes requests against the diagnosis key submission endpoint and holds a various methods for test
 * request generation.
 */
@Component
public class RequestExecutor {

  public static final String VALID_KEY_DATA_1 = "testKey111111111";
  public static final String VALID_KEY_DATA_2 = "testKey222222222";
  public static final String VALID_KEY_DATA_3 = "testKey333333333";
  private static final URI SUBMISSION_URL = URI.create("/version/v1/diagnosis-keys");

  private final TestRestTemplate testRestTemplate;

  public RequestExecutor(TestRestTemplate testRestTemplate) {
    this.testRestTemplate = testRestTemplate;
  }

  public ResponseEntity<Void> execute(HttpMethod method, RequestEntity<SubmissionPayload> requestEntity) {
    return testRestTemplate.exchange(SUBMISSION_URL, method, requestEntity, Void.class);
  }

  public ResponseEntity<Void> executePost(Collection<TemporaryExposureKey> keys, HttpHeaders headers) {
    SubmissionPayload body = SubmissionPayload.newBuilder()
        .setOrigin("DE")
        .addAllVisitedCountries(List.of("DE"))
        .addAllKeys(keys).build();
    return executePost(body, headers);
  }

  public ResponseEntity<Void> executePost(SubmissionPayload body, HttpHeaders headers) {
    return execute(HttpMethod.POST, new RequestEntity<>(body, headers, HttpMethod.POST, SUBMISSION_URL));
  }

  public ResponseEntity<Void> executePost(SubmissionPayload body) {
    return executePost(body, buildDefaultHeader());
  }

  public ResponseEntity<Void> executePost(Collection<TemporaryExposureKey> keys) {
    return executePost(keys, buildDefaultHeader());
  }

  private HttpHeaders buildDefaultHeader() {
    return HttpHeaderBuilder.builder()
        .contentTypeProtoBuf()
        .cwaAuth()
        .withoutCwaFake()
        .build();
  }

  public static TemporaryExposureKey buildTemporaryExposureKey(
      String keyData, int rollingStartIntervalNumber, int transmissionRiskLevel, ReportType reportType, int daysSinceOnsetOfSymptoms){
    return TemporaryExposureKey.newBuilder()
        .setKeyData(ByteString.copyFromUtf8(keyData))
        .setRollingStartIntervalNumber(rollingStartIntervalNumber)
        .setTransmissionRiskLevel(transmissionRiskLevel)
        .setReportType(reportType)
        .setDaysSinceOnsetOfSymptoms(daysSinceOnsetOfSymptoms)
        .build();
  }

  public static TemporaryExposureKey buildTemporaryExposureKeyWithFlexibleRollingPeriod(
      String keyData, int rollingStartIntervalNumber, int transmissionRiskLevel, int rollingPeriod) {
    return TemporaryExposureKey.newBuilder()
        .setKeyData(ByteString.copyFromUtf8(keyData))
        .setRollingStartIntervalNumber(rollingStartIntervalNumber)
        .setTransmissionRiskLevel(transmissionRiskLevel)
        .setRollingPeriod(rollingPeriod).build();
  }

  public static int createRollingStartIntervalNumber(Integer daysAgo) {
    return Math.toIntExact(LocalDate
        .ofInstant(Instant.now(), UTC)
        .minusDays(daysAgo).atStartOfDay()
        .toEpochSecond(UTC) / (60 * 10));
  }

  public static Collection<TemporaryExposureKey> buildPayloadWithOneKey() {
    return Collections.singleton(buildTemporaryExposureKey(VALID_KEY_DATA_1, createRollingStartIntervalNumber(1), 3,ReportType.CONFIRMED_CLINICAL_DIAGNOSIS,1));
  }
}
