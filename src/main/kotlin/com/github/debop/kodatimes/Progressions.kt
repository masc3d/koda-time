package com.github.debop.kodatimes

import org.joda.time.*
import org.joda.time.base.AbstractInstant
import org.joda.time.base.BaseDateTime
import java.util.*
import kotlin.NoSuchElementException

// a mod b (in arithmetical sense)
private fun mod(a: Int, b: Int): Int {
  val mod = a % b
  return if (mod >= 0) mod else mod + b
}

private fun mod(a: Long, b: Long): Long {
  val mod = a % b
  return if (mod >= 0) mod else mod + b
}

// (a - b) mod c
private fun differenceModulo(a: Int, b: Int, c: Int): Int {
  return mod(mod(a, c) - mod(b, c), c)
}

private fun differenceModulo(a: Long, b: Long, c: Long): Long {
  return mod(mod(a, c) - mod(b, c), c)
}

/**
 * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
 * from [start] to [end] in case of a positive [step], or from [end] to [start] in case of a negative
 * [step].
 *
 * No validation on passed parameters is performed. The given parameters should satisfy the condition: either
 * `step > 0` and `start >= end`, or `step < 0` and`start >= end`.
 * @param start first element of the progression
 * @param end ending bound for the progression
 * @param step increment, or difference of successive elements in the progression
 * @return the final element of the progression
 * @suppress
 */
internal fun getProgressionLastElement(start: DateTime, end: DateTime, step: Period): DateTime {
  return when {
    step.millis > 0 -> end - differenceModulo(end.millis, start.millis, step.millis.toLong())
    step.millis < 0 -> end + differenceModulo(start.millis, end.millis, -step.millis.toLong())
    else            -> throw IllegalArgumentException("Step is zero.")
  }
}

internal fun getProgressionLastElement(start: Instant, end: Instant, step: Period): Instant {
  return when {
    step.millis > 0 -> end - differenceModulo(end.millis, start.millis, step.millis.toLong())
    step.millis < 0 -> end + differenceModulo(start.millis, end.millis, -step.millis.toLong())
    else            -> throw IllegalArgumentException("Step is zero.")
  }
}

/**
 * A progression of value of `ReadableInstant`
 *
 * @property first first value of progression
 * @property last  last value of progression
 * @property step  progression step
 */
abstract class JodaTimeProgression<T : ReadableInstant>(start: T, endInclusive: T, val step: Period) : Iterable<T> {
  init {
    check(step.millis != 0) { "step must be non-zero" }
  }

  val first: T = start
  abstract val last: T

  open fun isEmpty(): Boolean = if (step.millis > 0) first > last else first < last

  override fun equals(other: Any?): Boolean =
      other is JodaTimeProgression<*> &&
      ((isEmpty() && other.isEmpty()) ||
       (first == other.first && last == other.last && step == other.step))

  override fun hashCode(): Int =
      if (isEmpty()) -1
      else Objects.hash(first, last, step)

  override fun toString(): String =
      if (step.millis > 0) "$first..$last step $step"
      else "$first downTo $last step ${-step}"
}


/**
 * A progression of value of `DateTime` type
 */
open class DateTimeProgression internal constructor(start: DateTime, endInclusive: DateTime, step: Period)
  : JodaTimeProgression<DateTime>(start, endInclusive, step), Iterable<DateTime> {

  override val last: DateTime = getProgressionLastElement(start, endInclusive, step)

  override fun iterator(): Iterator<DateTime> = DateTimeProgressionIterator(first, last, step)

  companion object {
    @JvmStatic
    fun fromClosedRange(rangeStart: DateTime, rangeEnd: DateTime, step: Period): DateTimeProgression =
        DateTimeProgression(rangeStart, rangeEnd, step)
  }
}

/**
 * A progression of value of `Instant` type
 */
open class InstantProgression internal constructor(start: Instant, endInclusive: Instant, step: Period)
  : JodaTimeProgression<Instant>(start, endInclusive, step), Iterable<Instant> {

  override val last: Instant = getProgressionLastElement(start, endInclusive, step)

  override fun iterator(): Iterator<Instant> = InstantProgressionIterator(first, last, step)

  companion object {
    @JvmStatic
    fun fromClosedRange(rangeStart: Instant, rangeEnd: Instant, step: Period): InstantProgression =
        InstantProgression(rangeStart, rangeEnd, step)
  }
}


/**
 * An iterator over a progression of values of type `DateTime`.
 *
 * @property step the number by which the value is incremented on each step.
 */
internal class DateTimeProgressionIterator(first: DateTime, last: DateTime, val step: Period) : JodaTimeIterator<DateTime>() {

  private val finalElement = last
  private var hasNext: Boolean = if (step.millis > 0) first <= last else first >= last
  private var next = if (hasNext) first else finalElement

  override fun hasNext(): Boolean = hasNext

  override fun nextJodaTime(): DateTime {
    val value = next
    if (value == finalElement) {
      if (!hasNext) throw NoSuchElementException()
      hasNext = false
    } else {
      next += step
    }
    return value
  }
}

/**
 * An iterator over a progression of values of type `Instant`.
 *
 * @property step the number by which the value is incremented on each step.
 */
internal class InstantProgressionIterator(first: Instant, last: Instant, val step: Period) : JodaTimeIterator<Instant>() {

  private val finalElement = last
  private var hasNext: Boolean = if (step.millis > 0) first <= last else first >= last
  private var next = if (hasNext) first else finalElement

  override fun hasNext(): Boolean = hasNext

  override fun nextJodaTime(): Instant {
    val value = next
    if (value == finalElement) {
      if (!hasNext) throw NoSuchElementException()
      hasNext = false
    } else {
      next += step
    }
    return value
  }
}


