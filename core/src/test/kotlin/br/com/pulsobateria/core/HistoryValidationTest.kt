package br.com.pulsobateria.core

import org.junit.Assert.*
import org.junit.Test

class HistoryValidationTest {
 @Test fun observedPeriodAndShortPauseAreValid() {
  assertTrue(HistoryValidation.validSession(1000,121000,120000,5,4,140.0))
  assertTrue(HistoryValidation.validSession(1000,121000,60000,5,4,140.0))
 }
 @Test fun negativeDurationIsRejected() { assertFalse(HistoryValidation.validSession(1000,2000,-1,5,4,140.0)) }
 @Test fun civilClockDriftDoesNotInvalidateMonotonicDuration() { assertTrue(HistoryValidation.validSession(1000,2000,3000,5,4,140.0)) }
 @Test fun moreTemperaturesThanSamplesIsRejected() { assertFalse(HistoryValidation.validSession(1000,2000,1000,2,4,140.0)) }
 @Test fun impossibleSumAndNonFiniteAreRejected() {
  assertFalse(HistoryValidation.validSession(1000,2000,1000,2,1,101.0))
  assertFalse(HistoryValidation.validSession(1000,2000,1000,2,0,1.0))
  assertFalse(HistoryValidation.validSession(1000,2000,1000,2,1,Double.NaN))
 }
 @Test fun negativeAndReversedTimestampsAreRejected() {
  assertFalse(HistoryValidation.validSession(-1,2000,1000,2,1,30.0))
  assertFalse(HistoryValidation.validSession(2000,1000,1000,2,1,30.0))
 }
 @Test fun pointsMustBelongToRecordedPeriod() {
  assertTrue(HistoryValidation.sampleWithinSession(1500,1000,2000))
  assertFalse(HistoryValidation.sampleWithinSession(999,1000,2000))
  assertFalse(HistoryValidation.sampleWithinSession(2001,1000,2000))
 }
}
