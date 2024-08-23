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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public final class QrSegment {

  public static QrSegment makeBytes(byte[] data) {
    Objects.requireNonNull(data);
    if (data.length * 8L > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Data too long");
    int[] bits = new int[(data.length + 3) / 4];
    for (int i = 0; i < data.length; i++)
      bits[i >>> 2] |= (data[i] & 0xFF) << (~i << 3);
    return new QrSegment(Mode.BYTE, data.length, bits, data.length * 8);
  }

  public static QrSegment makeNumeric(String digits) {
    Objects.requireNonNull(digits);
    BitBuffer bb = new BitBuffer();
    int accumData = 0;
    int accumCount = 0;
    for (int i = 0; i < digits.length(); i++) {
      char c = digits.charAt(i);
      if (c < '0' || c > '9')
        throw new IllegalArgumentException("String contains non-numeric characters");
      accumData = accumData * 10 + (c - '0');
      accumCount++;
      if (accumCount == 3) {
        bb.appendBits(accumData, 10);
        accumData = 0;
        accumCount = 0;
      }
    }
    if (accumCount > 0)
      bb.appendBits(accumData, accumCount * 3 + 1);
    return new QrSegment(Mode.NUMERIC, digits.length(), bb.data, bb.bitLength);
  }

  public static QrSegment makeAlphanumeric(String text) {
    Objects.requireNonNull(text);
    BitBuffer bb = new BitBuffer();
    int accumData = 0;
    int accumCount = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= ALPHANUMERIC_MAP.length || ALPHANUMERIC_MAP[c] == -1)
        throw new IllegalArgumentException("String contains unencodable characters in alphanumeric mode");
      accumData = accumData * 45 + ALPHANUMERIC_MAP[c];
      accumCount++;
      if (accumCount == 2) {
        bb.appendBits(accumData, 11);
        accumData = 0;
        accumCount = 0;
      }
    }
    if (accumCount > 0)
      bb.appendBits(accumData, 6);
    return new QrSegment(Mode.ALPHANUMERIC, text.length(), bb.data, bb.bitLength);
  }

  public static List<QrSegment> makeSegments(String text) {
    Objects.requireNonNull(text);


    List<QrSegment> result = new ArrayList<>();
    /*if (text.equals("")) ;
    else*/
    if (isNumeric(text))
      result.add(makeNumeric(text));
    else if (isAlphanumeric(text))
      result.add(makeAlphanumeric(text));
    else
      result.add(makeBytes(text.getBytes(StandardCharsets.UTF_8)));
    return result;
  }

  public static boolean isNumeric(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c < '0' || c > '9')
        return false;
    }
    return true;
  }

  public static boolean isAlphanumeric(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= ALPHANUMERIC_MAP.length || ALPHANUMERIC_MAP[c] == -1)
        return false;
    }
    return true;
  }

  public final Mode mode;
  public final int numChars;
  final int[] data;
  final int bitLength;


  public QrSegment(Mode md, int numCh, int[] data, int bitLen) {
    mode = Objects.requireNonNull(md);
    this.data = Objects.requireNonNull(data);
    if (numCh < 0 || bitLen < 0 || bitLen > data.length * 32L)
      throw new IllegalArgumentException("Invalid value");
    numChars = numCh;
    bitLength = bitLen;
  }

  static int getTotalBits(List<QrSegment> segs, int version) {
    Objects.requireNonNull(segs);
    long result = 0;
    for (QrSegment seg : segs) {
      Objects.requireNonNull(seg);
      int ccbits = seg.mode.numCharCountBits(version);
      if (seg.numChars >= (1 << ccbits))
        return -1;
      result += 4L + ccbits + seg.bitLength;
      if (result > Integer.MAX_VALUE)
        return -1;
    }
    return (int) result;
  }


  static final int[] ALPHANUMERIC_MAP;

  static {
    final String ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
    int maxCh = -1;
    for (int i = 0; i < ALPHANUMERIC_CHARSET.length(); i++)
      maxCh = Math.max(ALPHANUMERIC_CHARSET.charAt(i), maxCh);
    ALPHANUMERIC_MAP = new int[maxCh + 1];
    Arrays.fill(ALPHANUMERIC_MAP, -1);
    for (int i = 0; i < ALPHANUMERIC_CHARSET.length(); i++)
      ALPHANUMERIC_MAP[ALPHANUMERIC_CHARSET.charAt(i)] = i;
  }


  public enum Mode {
    NUMERIC(0x1, 10, 12, 14),
    ALPHANUMERIC(0x2, 9, 11, 13),
    BYTE(0x4, 8, 16, 16),
    KANJI(0x8, 8, 10, 12),
    ECI(0x7, 0, 0, 0);

    final int modeBits;

    private final int[] numBitsCharCount;

    Mode(int mode, int... ccbits) {
      modeBits = mode;
      numBitsCharCount = ccbits;
    }

    int numCharCountBits(int ver) {
      assert QrCode.MIN_VERSION <= ver && ver <= QrCode.MAX_VERSION;
      return numBitsCharCount[(ver + 7) / 17];
    }

  }

}
