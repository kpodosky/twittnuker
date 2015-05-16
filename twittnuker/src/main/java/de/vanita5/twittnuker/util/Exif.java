/*
 * Copyright (C) 2011 The Android Open Source Project
 *
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
 */

package de.vanita5.twittnuker.util;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.vanita5.twittnuker.BuildConfig;

public class Exif {
	private static final String TAG = "Exif";

	public static int getOrientation(final File file) {
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			return getOrientation(is);
		} catch (final IOException e) {
			return 0;
		} finally {
			Utils.closeSilently(is);
		}
	}

	public static int getOrientation(final FileDescriptor fd) {
		InputStream is = null;
		try {
			is = new FileInputStream(fd);
			return getOrientation(is);
		} finally {
			Utils.closeSilently(is);
		}
	}

	public static int getOrientation(final InputStream is) {
		if (is == null) return 0;

		final byte[] buf = new byte[8];
		int length = 0;

		// ISO/IEC 10918-1:1993(E)
		while (read(is, buf, 2) && (buf[0] & 0xFF) == 0xFF) {
			final int marker = buf[1] & 0xFF;

			// Check if the marker is a padding.
			if (marker == 0xFF) {
				continue;
			}

			// Check if the marker is SOI or TEM.
			if (marker == 0xD8 || marker == 0x01) {
				continue;
			}
			// Check if the marker is EOI or SOS.
			if (marker == 0xD9 || marker == 0xDA) return 0;

			// Get the length and check if it is reasonable.
			if (!read(is, buf, 2)) return 0;
			length = pack(buf, 0, 2, false);
			if (length < 2) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Invalid length");
				return 0;
			}
			length -= 2;

			// Break if the marker is EXIF in APP1.
			if (marker == 0xE1 && length >= 6) {
				if (!read(is, buf, 6)) return 0;
				length -= 6;
				if (pack(buf, 0, 4, false) == 0x45786966 && pack(buf, 4, 2, false) == 0) {
					break;
				}
			}

			// Skip other markers.
			try {
				is.skip(length);
			} catch (final IOException ex) {
				return 0;
			}
			length = 0;
		}

		// JEITA CP-3451 Exif Version 2.2
		if (length > 8) {
			int offset = 0;
			final byte[] jpeg = new byte[length];
			if (!read(is, jpeg, length)) return 0;

			// Identify the byte order.
			int tag = pack(jpeg, offset, 4, false);
			if (tag != 0x49492A00 && tag != 0x4D4D002A) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Invalid byte order");
				return 0;
			}
			final boolean littleEndian = tag == 0x49492A00;

			// Get the offset and check if it is reasonable.
			int count = pack(jpeg, offset + 4, 4, littleEndian) + 2;
			if (count < 10 || count > length) {
				Log.e(TAG, "Invalid offset");
				return 0;
			}
			offset += count;
			length -= count;

			// Get the count and go through all the elements.
			count = pack(jpeg, offset - 2, 2, littleEndian);
			while (count-- > 0 && length >= 12) {
				// Get the tag and check if it is orientation.
				tag = pack(jpeg, offset, 2, littleEndian);
				if (tag == 0x0112) {
					// We do not really care about type and count, do we?
					final int orientation = pack(jpeg, offset + 8, 2, littleEndian);
					switch (orientation) {
						case 1:
							return 0;
						case 3:
							return 180;
						case 6:
							return 90;
						case 8:
							return 270;
					}
					if (BuildConfig.DEBUG) Log.i(TAG, "Unsupported orientation");
					return 0;
				}
				offset += 12;
				length -= 12;
			}
		}

		if (BuildConfig.DEBUG) Log.i(TAG, "Orientation not found");
		return 0;
	}

	public static int getOrientation(final String file) {
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			return getOrientation(is);
		} catch (final IOException e) {
			return 0;
		} finally {
			Utils.closeSilently(is);
		}
	}

	private static int pack(final byte[] bytes, int offset, int length, final boolean littleEndian) {
		int step = 1;
		if (littleEndian) {
			offset += length - 1;
			step = -1;
		}

		int value = 0;
		while (length-- > 0) {
			value = value << 8 | bytes[offset] & 0xFF;
			offset += step;
		}
		return value;
	}

	private static boolean read(final InputStream is, final byte[] buf, final int length) {
		try {
			return is.read(buf, 0, length) == length;
		} catch (final IOException ex) {
			return false;
		}
	}
}