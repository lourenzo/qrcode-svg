/*
 * QR Code Generator with custom SVG rendering of modules, finder patterns,
 *  alignment patterns and central logo supporting.
 * Based on Nayuki's Fast QR Code Generator
 *
 * Copyright (c) Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/fast-qr-code-generator-library
 *
 * Copyright (c) Lourenzo Ferreira. (MIT Licence)
 * https://github.com/lourenzo/qrcode-svg
 */

package br.com.comprealugueagora.qrcode;

import java.util.Arrays;
import java.util.Objects;

final class BitBuffer {

  int[] data;
  int bitLength;

  public BitBuffer() {
    data = new int[64];
    bitLength = 0;
  }

  public byte[] getBytes() {
    if (bitLength % 8 != 0)
      throw new IllegalStateException("Data is not a whole number of bytes");
    byte[] result = new byte[bitLength / 8];
    for (int i = 0; i < result.length; i++)
      result[i] = (byte) (data[i >>> 2] >>> (~i << 3));
    return result;
  }

  public void appendBits(int val, int len) {
    if (len < 0 || len > 31 || val >>> len != 0)
      throw new IllegalArgumentException("Value out of range");
    if (len > Integer.MAX_VALUE - bitLength)
      throw new IllegalStateException("Maximum length reached");

    if (bitLength + len + 1 > data.length << 5)
      data = Arrays.copyOf(data, data.length * 2);
    assert bitLength + len <= data.length << 5;

    int remain = 32 - (bitLength & 0x1F);
    if (remain < len) {
      data[bitLength >>> 5] |= val >>> (len - remain);
      bitLength += remain;
      assert (bitLength & 0x1F) == 0;
      len -= remain;
      val &= (1 << len) - 1;
      remain = 32;
    }
    data[bitLength >>> 5] |= val << (remain - len);
    bitLength += len;
  }

  public void appendBits(int[] vals, int len) {
    Objects.requireNonNull(vals);
    if (len == 0)
      return;
    if (len < 0 || len > vals.length * 32L)
      throw new IllegalArgumentException("Value out of range");
    int wholeWords = len / 32;
    int tailBits = len % 32;
    if (tailBits > 0 && vals[wholeWords] << tailBits != 0)
      throw new IllegalArgumentException("Last word must have low bits clear");
    if (len > Integer.MAX_VALUE - bitLength)
      throw new IllegalStateException("Maximum length reached");

    while (bitLength + len > data.length * 32)
      data = Arrays.copyOf(data, data.length * 2);

    int shift = bitLength % 32;
    if (shift == 0) {
      System.arraycopy(vals, 0, data, bitLength / 32, (len + 31) / 32);
      bitLength += len;
    } else {
      for (int i = 0; i < wholeWords; i++) {
        int word = vals[i];
        data[bitLength >>> 5] |= word >>> shift;
        bitLength += 32;
        data[bitLength >>> 5] = word << (32 - shift);
      }
      if (tailBits > 0)
        appendBits(vals[wholeWords] >>> (32 - tailBits), tailBits);
    }
  }

}
