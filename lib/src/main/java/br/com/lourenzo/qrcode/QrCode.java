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

package br.com.lourenzo.qrcode

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class QrCode {

  public static QrCode encodeText(String text, Ecc ecl) {
    Objects.requireNonNull(text);
    Objects.requireNonNull(ecl);
    List<QrSegment> segs = QrSegment.makeSegments(text);
    return encodeSegments(segs, ecl);
  }

  public static QrCode encodeSegments(List<QrSegment> segs, Ecc ecl) {
    return encodeSegments(segs, ecl, MIN_VERSION, MAX_VERSION, -1, true);
  }

  public static QrCode encodeSegments(List<QrSegment> segs, Ecc ecl, int minVersion, int maxVersion, int mask, boolean boostEcl) {
    Objects.requireNonNull(segs);
    Objects.requireNonNull(ecl);
    if (!(MIN_VERSION <= minVersion && minVersion <= maxVersion && maxVersion <= MAX_VERSION) || mask < -1 || mask > 7)
      throw new IllegalArgumentException("Invalid value");


    int version, dataUsedBits;
    for (version = minVersion; ; version++) {
      int dataCapacityBits = getNumDataCodewords(version, ecl) * 8;
      dataUsedBits = QrSegment.getTotalBits(segs, version);
      if (dataUsedBits != -1 && dataUsedBits <= dataCapacityBits)
        break;
      if (version >= maxVersion) {
        String msg = "Segment too long";
        if (dataUsedBits != -1)
          msg = String.format("Data length = %d bits, Max capacity = %d bits", dataUsedBits, dataCapacityBits);
        throw new DataTooLongException(msg);
      }
    }


    for (Ecc newEcl : Ecc.values()) {
      if (boostEcl && dataUsedBits <= getNumDataCodewords(version, newEcl) * 8)
        ecl = newEcl;
    }


    BitBuffer bb = new BitBuffer();
    for (QrSegment seg : segs) {
      bb.appendBits(seg.mode.modeBits, 4);
      bb.appendBits(seg.numChars, seg.mode.numCharCountBits(version));
      bb.appendBits(seg.data, seg.bitLength);
    }
    assert bb.bitLength == dataUsedBits;


    int dataCapacityBits = getNumDataCodewords(version, ecl) * 8;
    assert bb.bitLength <= dataCapacityBits;
    bb.appendBits(0, Math.min(4, dataCapacityBits - bb.bitLength));
    bb.appendBits(0, (8 - bb.bitLength % 8) % 8);
    assert bb.bitLength % 8 == 0;


    for (int padByte = 0xEC; bb.bitLength < dataCapacityBits; padByte ^= 0xEC ^ 0x11)
      bb.appendBits(padByte, 8);


    return new QrCode(version, ecl, bb.getBytes(), mask);
  }

  public final int version;
  public final int size;
  public final Ecc errorCorrectionLevel;
  public final int mask;
  private final int[] modules;


  public QrCode(int ver, Ecc ecl, byte[] dataCodewords, int msk) {

    if (ver < MIN_VERSION || ver > MAX_VERSION)
      throw new IllegalArgumentException("Version value out of range");
    if (msk < -1 || msk > 7)
      throw new IllegalArgumentException("Mask value out of range");
    version = ver;
    size = ver * 4 + 17;
    errorCorrectionLevel = Objects.requireNonNull(ecl);
    Objects.requireNonNull(dataCodewords);

    QrTemplate tpl = QrTemplate.MEMOIZER.get(ver);
    modules = tpl.template.clone();


    byte[] allCodewords = addEccAndInterleave(dataCodewords);
    drawCodewords(tpl.dataOutputBitIndexes, allCodewords);
    mask = handleConstructorMasking(tpl.masks, msk);
  }


  public boolean getModule(int x, int y) {
    if (0 <= x && x < size && 0 <= y && y < size) {
      int i = y * size + x;
      return getBit(modules[i >>> 5], i) != 0;
    } else
      return false;
  }

  private void drawFormatBits(int msk) {

    int data = errorCorrectionLevel.formatBits << 3 | msk;
    int rem = data;
    for (int i = 0; i < 10; i++)
      rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
    int bits = (data << 10 | rem) ^ 0x5412;
    assert bits >>> 15 == 0;


    for (int i = 0; i <= 5; i++)
      setModule(8, i, getBit(bits, i));
    setModule(8, 7, getBit(bits, 6));
    setModule(8, 8, getBit(bits, 7));
    setModule(7, 8, getBit(bits, 8));
    for (int i = 9; i < 15; i++)
      setModule(14 - i, 8, getBit(bits, i));


    for (int i = 0; i < 8; i++)
      setModule(size - 1 - i, 8, getBit(bits, i));
    for (int i = 8; i < 15; i++)
      setModule(8, size - 15 + i, getBit(bits, i));
    setModule(8, size - 8, 1);
  }

  private void setModule(int x, int y, int dark) {
    assert 0 <= x && x < size;
    assert 0 <= y && y < size;
    assert dark == 0 || dark == 1;
    int i = y * size + x;
    modules[i >>> 5] &= ~(1 << i);
    modules[i >>> 5] |= dark << i;
  }

  private byte[] addEccAndInterleave(byte[] data) {
    Objects.requireNonNull(data);
    if (data.length != getNumDataCodewords(version, errorCorrectionLevel))
      throw new IllegalArgumentException();


    int numBlocks = NUM_ERROR_CORRECTION_BLOCKS[errorCorrectionLevel.ordinal()][version];
    int blockEccLen = ECC_CODEWORDS_PER_BLOCK[errorCorrectionLevel.ordinal()][version];
    int rawCodewords = QrTemplate.getNumRawDataModules(version) / 8;
    int numShortBlocks = numBlocks - rawCodewords % numBlocks;
    int shortBlockDataLen = rawCodewords / numBlocks - blockEccLen;

    byte[] result = new byte[rawCodewords];
    ReedSolomonGenerator rs = ReedSolomonGenerator.MEMOIZER.get(blockEccLen);
    byte[] ecc = new byte[blockEccLen];
    for (int i = 0, k = 0; i < numBlocks; i++) {
      int datLen = shortBlockDataLen + (i < numShortBlocks ? 0 : 1);
      rs.getRemainder(data, k, datLen, ecc);
      for (int j = 0, l = i; j < datLen; j++, k++, l += numBlocks) {
        if (j == shortBlockDataLen)
          l -= numShortBlocks;
        result[l] = data[k];
      }
      for (int j = 0, l = data.length + i; j < blockEccLen; j++, l += numBlocks)
        result[l] = ecc[j];
    }
    return result;
  }

  private void drawCodewords(int[] dataOutputBitIndexes, byte[] allCodewords) {
    Objects.requireNonNull(dataOutputBitIndexes);
    Objects.requireNonNull(allCodewords);
    if (allCodewords.length * 8 != dataOutputBitIndexes.length)
      throw new IllegalArgumentException();
    for (int i = 0; i < dataOutputBitIndexes.length; i++) {
      int j = dataOutputBitIndexes[i];
      int bit = getBit(allCodewords[i >>> 3], ~i & 7);
      modules[j >>> 5] |= bit << j;
    }
  }

  private void applyMask(int[] msk) {
    if (msk.length != modules.length)
      throw new IllegalArgumentException();
    for (int i = 0; i < msk.length; i++)
      modules[i] ^= msk[i];
  }

  private int handleConstructorMasking(int[][] masks, int msk) {
    if (msk == -1) {
      int minPenalty = Integer.MAX_VALUE;
      for (int i = 0; i < 8; i++) {
        applyMask(masks[i]);
        drawFormatBits(i);
        int penalty = getPenaltyScore();
        if (penalty < minPenalty) {
          msk = i;
          minPenalty = penalty;
        }
        applyMask(masks[i]);
      }
    }
    assert 0 <= msk && msk <= 7;
    applyMask(masks[msk]);
    drawFormatBits(msk);
    return msk;
  }

  private int getPenaltyScore() {
    int result = 0;
    int dark = 0;
    int[] runHistory = new int[7];


    for (int index = 0, downIndex = size, end = size * size; index < end; ) {
      int runColor = 0;
      int runX = 0;
      Arrays.fill(runHistory, 0);
      int curRow = 0;
      int nextRow = 0;
      for (int x = 0; x < size; x++, index++, downIndex++) {
        int c = getBit(modules[index >>> 5], index);
        if (c == runColor) {
          runX++;
          if (runX == 5)
            result += PENALTY_N1;
          else if (runX > 5)
            result++;
        } else {
          finderPenaltyAddHistory(runX, runHistory);
          if (runColor == 0)
            result += finderPenaltyCountPatterns(runHistory) * PENALTY_N3;
          runColor = c;
          runX = 1;
        }
        dark += c;
        if (downIndex < end) {
          curRow = ((curRow << 1) | c) & 3;
          nextRow = ((nextRow << 1) | getBit(modules[downIndex >>> 5], downIndex)) & 3;

          if (x >= 1 && (curRow == 0 || curRow == 3) && curRow == nextRow)
            result += PENALTY_N2;
        }
      }
      result += finderPenaltyTerminateAndCount(runColor, runX, runHistory) * PENALTY_N3;
    }


    for (int x = 0; x < size; x++) {
      int runColor = 0;
      int runY = 0;
      Arrays.fill(runHistory, 0);
      for (int y = 0, index = x; y < size; y++, index += size) {
        int c = getBit(modules[index >>> 5], index);
        if (c == runColor) {
          runY++;
          if (runY == 5)
            result += PENALTY_N1;
          else if (runY > 5)
            result++;
        } else {
          finderPenaltyAddHistory(runY, runHistory);
          if (runColor == 0)
            result += finderPenaltyCountPatterns(runHistory) * PENALTY_N3;
          runColor = c;
          runY = 1;
        }
      }
      result += finderPenaltyTerminateAndCount(runColor, runY, runHistory) * PENALTY_N3;
    }


    int total = size * size;

    int k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1;
    result += k * PENALTY_N4;
    return result;
  }

  static int getNumDataCodewords(int ver, Ecc ecl) {
    return QrTemplate.getNumRawDataModules(ver) / 8
      - ECC_CODEWORDS_PER_BLOCK[ecl.ordinal()][ver]
      * NUM_ERROR_CORRECTION_BLOCKS[ecl.ordinal()][ver];
  }

  private int finderPenaltyCountPatterns(int[] runHistory) {
    int n = runHistory[1];
    assert n <= size * 3;
    boolean core = n > 0 && runHistory[2] == n && runHistory[3] == n * 3 && runHistory[4] == n && runHistory[5] == n;
    return (core && runHistory[0] >= n * 4 && runHistory[6] >= n ? 1 : 0)
      + (core && runHistory[6] >= n * 4 && runHistory[0] >= n ? 1 : 0);
  }

  private int finderPenaltyTerminateAndCount(int currentRunColor, int currentRunLength, int[] runHistory) {
    if (currentRunColor == 1) {
      finderPenaltyAddHistory(currentRunLength, runHistory);
      currentRunLength = 0;
    }
    currentRunLength += size;
    finderPenaltyAddHistory(currentRunLength, runHistory);
    return finderPenaltyCountPatterns(runHistory);
  }

  private void finderPenaltyAddHistory(int currentRunLength, int[] runHistory) {
    if (runHistory[0] == 0)
      currentRunLength += size;
    System.arraycopy(runHistory, 0, runHistory, 1, runHistory.length - 1);
    runHistory[0] = currentRunLength;
  }

  static int getBit(int x, int i) {
    return (x >>> i) & 1;
  }

  public static final int MIN_VERSION = 1;

  public static final int MAX_VERSION = 40;

  private static final int PENALTY_N1 = 3;
  private static final int PENALTY_N2 = 3;
  private static final int PENALTY_N3 = 40;
  private static final int PENALTY_N4 = 10;


  private static final byte[][] ECC_CODEWORDS_PER_BLOCK = {


    {-1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
    {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28},
    {-1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
    {-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},
  };

  private static final byte[][] NUM_ERROR_CORRECTION_BLOCKS = {
    {-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25},
    {-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49},
    {-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68},
    {-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81},
  };

  public enum Ecc {
    LOW(1),
    MEDIUM(0),
    QUARTILE(3),
    HIGH(2);

    final int formatBits;

    Ecc(int fb) {
      formatBits = fb;
    }
  }

}
