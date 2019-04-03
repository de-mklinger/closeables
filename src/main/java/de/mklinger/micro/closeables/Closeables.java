package de.mklinger.micro.closeables;

import java.util.Collection;

/**
 * @author Marc Klinger - mklinger[at]mklinger[dot]de
 */
public class Closeables {
	/** No instantiation */
	private Closeables() {}

	public static void closeUnchecked(final Collection<? extends AutoCloseable> closeables) {
		closeUnchecked(closeables.toArray(new AutoCloseable[closeables.size()]));
	}

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

	public static void close(final Collection<? extends AutoCloseable> closeables) throws Exception {
		close(closeables.toArray(new AutoCloseable[closeables.size()]));
	}

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

	public static class UncheckedCloseException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public UncheckedCloseException(final String message, final Exception cause) {
			super(message, cause);
		}
	}
}
