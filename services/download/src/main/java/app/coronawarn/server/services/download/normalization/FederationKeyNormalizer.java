/*-
 * ---license-start
 * Corona-Warn-App
 * ---
 * Copyright (C) 2020 SAP SE and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package app.coronawarn.server.services.download.normalization;


import app.coronawarn.server.common.persistence.domain.normalization.DiagnosisKeyNormalizer;
import app.coronawarn.server.common.persistence.domain.normalization.NormalizableFields;
import java.util.Map;

/**
 * This class is used to derive days since onset of symptoms.
 */
public class FederationKeyNormalizer implements DiagnosisKeyNormalizer {

  private final Map<Integer, Integer> dsosAndTrl;

  /**
   * Constructor for this class.
   *
   * @param dsosAndTrl A map containing integer key-pair values of days since onset of symptoms and transmission risk
   *                   level.
   */
  public FederationKeyNormalizer(Map<Integer, Integer> dsosAndTrl) {
    this.dsosAndTrl = dsosAndTrl;
  }

  @Override
  public NormalizableFields normalize(NormalizableFields fieldsAndValues) {
    validateNormalizableFields(fieldsAndValues);
    int trl = dsosAndTrl.getOrDefault(fieldsAndValues.getDaysSinceOnsetOfSymptoms(), 1);
    return NormalizableFields.of(trl, fieldsAndValues.getDaysSinceOnsetOfSymptoms());
  }

  private void validateNormalizableFields(NormalizableFields fieldsAndValues) {
    if (fieldsAndValues.getDaysSinceOnsetOfSymptoms() == null) {
      throw new IllegalArgumentException("Days since onset of symptoms is missing!");
    }
  }

}
