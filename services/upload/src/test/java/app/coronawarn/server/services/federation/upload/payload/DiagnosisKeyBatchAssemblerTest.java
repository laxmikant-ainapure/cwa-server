

package app.coronawarn.server.services.federation.upload.payload;

import static app.coronawarn.server.services.federation.upload.utils.MockData.*;
import static java.util.Collections.emptyList;

import app.coronawarn.server.common.persistence.domain.DiagnosisKey;
import app.coronawarn.server.common.persistence.domain.FederationUploadKey;
import app.coronawarn.server.services.federation.upload.config.UploadServiceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.List;
import java.util.stream.Stream;

@EnableConfigurationProperties(value = UploadServiceConfig.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DiagnosisKeyBatchAssembler.class}, initializers = ConfigFileApplicationContextInitializer.class)
class DiagnosisKeyBatchAssemblerTest {

  private static volatile int minKeyThreshold;
  private static volatile int maxKeyCount;

  @Autowired
  DiagnosisKeyBatchAssembler diagnosisKeyBatchAssembler;

  @Autowired
  UploadServiceConfig uploadServiceConfig;

  @BeforeEach
  public void setup() {
    minKeyThreshold = uploadServiceConfig.getMinBatchKeyCount();
    maxKeyCount = uploadServiceConfig.getMaxBatchKeyCount();
  }

  private void assertKeysAreEqual(DiagnosisKey persistenceKey,
      app.coronawarn.server.common.protocols.external.exposurenotification.DiagnosisKey exportKey) {
    Assertions.assertArrayEquals(persistenceKey.getKeyData(), exportKey.getKeyData().toByteArray(),
        "Key Data should be the same");
    Assertions.assertArrayEquals(persistenceKey.getVisitedCountries().toArray(),
        exportKey.getVisitedCountriesList().toArray(),
        "Visited countries should be the same");
    Assertions.assertEquals(persistenceKey.getRollingPeriod(), exportKey.getRollingPeriod(),
        "Rolling Period should be the same");
    Assertions.assertEquals(persistenceKey.getReportType(), exportKey.getReportType(),
        "Verification Type should be the same");
    Assertions.assertEquals(persistenceKey.getTransmissionRiskLevel(), exportKey.getTransmissionRiskLevel(),
        "Transmission Risk Level should be the same");
    Assertions.assertEquals(persistenceKey.getOriginCountry(), exportKey.getOrigin(),
        "Origin Country should be the same");
  }

  @Test
  void shouldReturnEmptyListIfNoKeysGiven() {
    var result = diagnosisKeyBatchAssembler.assembleDiagnosisKeyBatch(emptyList());
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnEmptyListIfLessThenThresholdKeysGiven() {
    var result = diagnosisKeyBatchAssembler.assembleDiagnosisKeyBatch(generateRandomUploadKeys(true, minKeyThreshold - 1));
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void packagedKeysShouldContainInitialInformation() {
    var fakeKeys = generateRandomUploadKeys(true, minKeyThreshold);
    var result = diagnosisKeyBatchAssembler.assembleDiagnosisKeyBatch(fakeKeys);
    var firstBatch = result.keySet().iterator().next();
    Assertions.assertEquals(fakeKeys.size(), firstBatch.getKeysCount());
    // as keys are created equal we need to compare just the first two elements of each list
    assertKeysAreEqual(fakeKeys.get(0), firstBatch.getKeys(0));
  }

  @Test
  void shouldNotPackageKeysIfConsentFlagIsNotSet() {
    var dataset = generateRandomUploadKeys(true, minKeyThreshold);
    dataset.add(generateRandomUploadKey(false));
    var result = diagnosisKeyBatchAssembler.assembleDiagnosisKeyBatch(dataset);
    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals(minKeyThreshold, result.keySet().iterator().next().getKeysCount());
  }

  @ParameterizedTest
  @MethodSource("keysToPartitionAndBatchNumberExpectations")
  void shouldGenerateCorrectNumberOfBatches(List<FederationUploadKey> dataset, Integer expectedBatches) {
    var result = diagnosisKeyBatchAssembler.assembleDiagnosisKeyBatch(dataset);
    Assertions.assertEquals(expectedBatches, result.size());
  }

  /**
   * @return A stream of tuples which represents the dataset together with the
   * expectation required to test batch key partioning.
   */
  private static Stream<Arguments> keysToPartitionAndBatchNumberExpectations() {
    return Stream.of(
        Arguments.of(generateRandomUploadKeys(true, minKeyThreshold - 1), 0),
        Arguments.of(generateRandomUploadKeys(true, minKeyThreshold), 1),
        Arguments.of(generateRandomUploadKeys(true, maxKeyCount), 1),
        Arguments.of(generateRandomUploadKeys(true, maxKeyCount / 2), 1),
        Arguments.of(generateRandomUploadKeys(true, maxKeyCount - 1), 1),
        Arguments.of(generateRandomUploadKeys(true, maxKeyCount + 1), 2),
        Arguments.of(generateRandomUploadKeys(true, 2 * maxKeyCount), 2),
        Arguments.of(generateRandomUploadKeys(true, 3 * maxKeyCount), 3),
        Arguments.of(generateRandomUploadKeys(true, 4 * maxKeyCount), 4),
        Arguments.of(generateRandomUploadKeys(true, 2 * maxKeyCount + 1), 3),
        Arguments.of(generateRandomUploadKeys(true, 2 * maxKeyCount + maxKeyCount / 2), 3),
        Arguments.of(generateRandomUploadKeys(true, 2 * maxKeyCount - maxKeyCount / 2), 2)
    );
  }
}
