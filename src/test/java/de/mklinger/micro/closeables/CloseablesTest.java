/*
 * Copyright mklinger GmbH - https://www.mklinger.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mklinger.micro.closeables;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class CloseablesTest {
	private static final AutoCloseable noneThrowing = () -> {};
	private static final AutoCloseable checkedThrowing = () -> { throw new Exception(); };
	private static final AutoCloseable uncheckedThrowing = () -> { throw new RuntimeException(); };
	private static final AutoCloseable interruptedThrowing = () -> { throw new InterruptedException(); };

	@Test
	public void testCloseWithNoneThrowing() throws Exception {
		Closeables.close(noneThrowing, noneThrowing);
	}

	@Test
	public void testCloseWithNoneThrowingAndNull() throws Exception {
		Closeables.close(noneThrowing, null, noneThrowing, null);
	}

	@Test
	public void testCloseListWithNoneThrowing() throws Exception {
		Closeables.close(asList(noneThrowing, noneThrowing));
	}

	@Test
	public void testCloseListWithNoneThrowingAndNull() throws Exception {
		Closeables.close(asList(noneThrowing, null, noneThrowing, null));
	}

	@Test
	public void testCloseWithCheckedThrowing() throws Exception {
		try {
			Closeables.close(noneThrowing, noneThrowing, checkedThrowing, uncheckedThrowing);
			fail("Expected exception was not thrown");
		} catch (final Exception e) {
			assertThat(e, isChecked());
			assertThat(e, hasSuppressed(1));
		}
	}

	@Test
	public void testCloseWithUncheckedThrowing() throws Exception {
		try {
			Closeables.close(noneThrowing, noneThrowing, uncheckedThrowing, checkedThrowing);
			fail("Expected exception was not thrown");
		} catch (final Exception e) {
			assertThat(e, isUnchecked());
			assertThat(e, hasSuppressed(1));
		}
	}

	@Test
	public void testCloseWithInterruptedThrowing() throws Throwable {
		runInThread(() -> {
			try {
				Closeables.close(noneThrowing, noneThrowing, checkedThrowing, uncheckedThrowing, interruptedThrowing);
				fail("Expected exception was not thrown");
			} catch (final Exception e) {
				assertThat(e, isChecked());
				assertThat(e, hasSuppressed(2));
				assertThat(Thread.currentThread().isInterrupted(), equalTo(true));
			}
		});
	}

	// ------

	@Test
	public void testCloseUncheckedWithNoneThrowing() throws Exception {
		Closeables.closeUnchecked(noneThrowing, noneThrowing);
	}

	@Test
	public void testCloseUncheckedWithNoneThrowingAndNull() throws Exception {
		Closeables.closeUnchecked(noneThrowing, null, noneThrowing, null);
	}

	@Test
	public void testCloseUncheckedListWithNoneThrowing() throws Exception {
		Closeables.closeUnchecked(asList(noneThrowing, noneThrowing));
	}

	@Test
	public void testCloseUncheckedListWithNoneThrowingAndNull() throws Exception {
		Closeables.closeUnchecked(asList(noneThrowing, null, noneThrowing, null));
	}

	@Test
	public void testCloseUncheckedWithCheckedThrowing() throws Exception {
		try {
			Closeables.closeUnchecked(noneThrowing, noneThrowing, checkedThrowing, uncheckedThrowing);
			fail("Expected exception was not thrown");
		} catch (final Exception e) {
			assertThat(e, isUnchecked());
			final Throwable cause = e.getCause();
			assertThat(cause, notNullValue());
			assertThat(cause, hasSuppressed(1));
		}
	}

	@Test
	public void testCloseUncheckedWithUncheckedThrowing() throws Exception {
		try {
			Closeables.closeUnchecked(noneThrowing, noneThrowing, uncheckedThrowing, checkedThrowing);
			fail("Expected exception was not thrown");
		} catch (final Exception e) {
			assertThat(e, isUnchecked());
			assertThat(e, hasSuppressed(1));
		}
	}

	@Test
	public void testCloseUncheckedWithInterruptedThrowing() throws Throwable {
		runInThread(() -> {
			try {
				Closeables.closeUnchecked(interruptedThrowing, noneThrowing, noneThrowing, checkedThrowing, uncheckedThrowing);
				fail("Expected exception was not thrown");
			} catch (final Exception e) {
				assertThat(e, isUnchecked());
				final Throwable cause = e.getCause();
				assertThat(cause, notNullValue());
				assertThat(cause, hasSuppressed(2));
				assertThat(Thread.currentThread().isInterrupted(), equalTo(true));
			}
		});
	}

	private Matcher<Throwable> isChecked() {
		return allOf(isException(), not(isUnchecked()));
	}

	private Matcher<Throwable> isException() {
		return instanceOf(Exception.class);
	}

	private Matcher<Throwable> isUnchecked() {
		return instanceOf(RuntimeException.class);
	}

	private Matcher<Throwable> hasSuppressed(final int size) {
		return Matchers.hasProperty("suppressed", Matchers.arrayWithSize(size));
	}

	private void runInThread(final Runnable r) throws Throwable {
		final AtomicReference<Throwable> executionError = new AtomicReference<>();

		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					r.run();
				} catch (final Throwable e) {
					executionError.set(e);
				}
			}
		};

		t.setDaemon(true);
		t.start();
		try {
			t.join(1000);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TestInterruptedException(e);
		}

		if (executionError.get() != null) {
			throw executionError.get();
		}
	}

	private static class TestInterruptedException extends Exception {
		private static final long serialVersionUID = 1L;

		public TestInterruptedException(final InterruptedException cause) {
			super(cause);
		}
	}
}
