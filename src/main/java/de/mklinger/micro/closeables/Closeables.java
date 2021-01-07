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

import java.util.Collection;

/**
 * Utility methods to close multiple instances of {@link AutoCloseable} at once.
 *
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class Closeables {
	/** No instantiation */
	private Closeables() {}

	/**
	 * Close the given closeables, throwing an unchecked exception in case of an
	 * error. If more than one exception is thrown during closing, the second and
	 * following exceptions are added as suppressed exceptions.
	 * <p>
	 * If one of the exceptions thrown is an instance of
	 * {@link InterruptedException}, the interrupted state of the current thread
	 * will be set.
	 * </p>
	 *
	 * @param closeables The closeable instances to close. {@code Null} elements in
	 *        the list are ignored.
	 * @throws UncheckedCloseException in case of an error
	 */
	public static void closeUnchecked(final Collection<? extends AutoCloseable> closeables) {
		closeUnchecked(closeables.toArray(new AutoCloseable[closeables.size()]));
	}

	/**
	 * Close the given closeables, throwing an unchecked exception in case of an
	 * error. If more than one exception is thrown during closing, the second and
	 * following exceptions are added as suppressed exceptions.
	 * <p>
	 * If one of the exceptions thrown is an instance of
	 * {@link InterruptedException}, the interrupted state of the current thread
	 * will be set.
	 * </p>
	 *
	 * @param closeables The closeable instances to close. {@code Null} elements in
	 *        the array are ignored.
	 * @throws UncheckedCloseException in case of an error
	 */
	public static void closeUnchecked(final AutoCloseable... closeables) {
		try {
			close(closeables);
		} catch (final Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			} else {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				throw new UncheckedCloseException("Error on close", e);
			}
		}
	}

	/**
	 * Close the given closeables, throwing the first original exception in case of
	 * an error. If more than one exception is thrown during closing, the second and
	 * following exceptions are added as suppressed exceptions.
	 * <p>
	 * If one of the suppressed exceptions is an instance of
	 * {@link InterruptedException}, the interrupted state of the current thread
	 * will be set.
	 * </p>
	 *
	 * @param closeables The closeable instances to close. {@code Null} elements in
	 *        the list are ignored.
	 * @throws Exception in case of an error
	 */
	public static void close(final Collection<? extends AutoCloseable> closeables) throws Exception {
		close(closeables.toArray(new AutoCloseable[closeables.size()]));
	}

	/**
	 * Close the given closeables, throwing the first original exception in case of
	 * an error. If more than one exception is thrown during closing, the second and
	 * following exceptions are added as suppressed exceptions.
	 * <p>
	 * If one of the suppressed exceptions is an instance of
	 * {@link InterruptedException}, the interrupted state of the current thread
	 * will be set.
	 * </p>
	 *
	 * @param closeables The closeable instances to close. {@code Null} elements in
	 *        the array are ignored.
	 * @throws Exception in case of an error
	 */
	public static void close(final AutoCloseable... closeables) throws Exception {
		Exception error = null;
		for (final AutoCloseable closeable : closeables) {
			try {
				if (closeable != null) {
					closeable.close();
				}
			} catch (final Exception e) {
				if (error == null) {
					error = e;
				} else {
					if (e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					error.addSuppressed(e);
				}
			}
		}
		if (error != null) {
			throw error;
		}
	}

	/**
	 * Unchecked wrapper for checked exceptions thrown during closing a closeable
	 * instance.
	 */
	public static class UncheckedCloseException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public UncheckedCloseException(final String message, final Exception cause) {
			super(message, cause);
		}
	}
}
