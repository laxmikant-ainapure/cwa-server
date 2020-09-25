

package app.coronawarn.server.services.submission.normalization;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import app.coronawarn.server.common.persistence.domain.normalization.NormalizableFields;
import app.coronawarn.server.services.submission.config.SubmissionServiceConfig;
import app.coronawarn.server.services.submission.config.SubmissionServiceConfig.TekFieldDerivations;

class SubmissionKeyNormalizerTest {

  @ParameterizedTest
  @MethodSource("dsosFromTrlParameters")
  void testDsosIsCorrectlyDerived(int inputTrlValue, int expectedDsosValue) {
    SubmissionServiceConfig mockedConfig = mock(SubmissionServiceConfig.class);
    TekFieldDerivations mockedDerivationRules = mock(TekFieldDerivations.class);
    when(mockedConfig.getTekFieldDerivations()).thenReturn(mockedDerivationRules);
    when(mockedDerivationRules.deriveDsosFromTrl(inputTrlValue)).thenReturn(expectedDsosValue);

    SubmissionKeyNormalizer normalizer = new SubmissionKeyNormalizer(mockedConfig);
    NormalizableFields result = normalizer.normalize(NormalizableFields.of(inputTrlValue, null));
    Assertions.assertThat(result.getDaysSinceOnsetOfSymptoms()).isEqualTo(expectedDsosValue);

    result = normalizer.normalize(NormalizableFields.of(inputTrlValue - 1, null));
    Assertions.assertThat(result.getDaysSinceOnsetOfSymptoms()).isNotEqualTo(expectedDsosValue);
  }

  @Test
  void testErrorIsThrownWhenAllRequiredFieldsForNormalizationAreMissing() {
    SubmissionServiceConfig mockedConfig = mock(SubmissionServiceConfig.class);
    TekFieldDerivations mockedDerivationRules = mock(TekFieldDerivations.class);
    when(mockedConfig.getTekFieldDerivations()).thenReturn(mockedDerivationRules);
    when(mockedDerivationRules.deriveDsosFromTrl(1)).thenReturn(2);

    SubmissionKeyNormalizer normalizer = new SubmissionKeyNormalizer(mockedConfig);
    Assertions.assertThatThrownBy(() -> {
      normalizer.normalize(NormalizableFields.of(null, null));
    }).isOfAnyClassIn(IllegalArgumentException.class);
  }

  private static Stream<Arguments> dsosFromTrlParameters() {
    return Stream.of(
        Arguments.of(1, 14),
        Arguments.of(3, 10),
        Arguments.of(6, 8)
    );
  }
}
