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

package br.com.lourenzo.qrcode;

final class QrTemplate {

  public static final Memoizer<Integer, QrTemplate> MEMOIZER
    = new Memoizer<>(QrTemplate::new);

  private final int version;
  private final int size;
  final int[] template;
  final int[][] masks;
  final int[] dataOutputBitIndexes;

  private int[] isFunction;

  private QrTemplate(int ver) {
    if (ver < QrCode.MIN_VERSION || ver > QrCode.MAX_VERSION)
      throw new IllegalArgumentException("Version out of range");
    version = ver;
    size = version * 4 + 17;
    template = new int[(size * size + 31) / 32];
    isFunction = new int[template.length];

    drawFunctionPatterns();
    masks = generateMasks();
    dataOutputBitIndexes = generateZigzagScan();
    isFunction = null;
  }

  private void drawFunctionPatterns() {

    for (int i = 0; i < size; i++) {
      darkenFunctionModule(6, i, ~i & 1);
      darkenFunctionModule(i, 6, ~i & 1);
    }

    drawFinderPattern(3, 3);
    drawFinderPattern(size - 4, 3);
    drawFinderPattern(3, size - 4);

    int[] alignPatPos = getAlignmentPatternPositions();
    int numAlign = alignPatPos.length;
    for (int i = 0; i < numAlign; i++) {
      for (int j = 0; j < numAlign; j++) {

        if (!(i == 0 && j == 0 || i == 0 && j == numAlign - 1 || i == numAlign - 1 && j == 0))
          drawAlignmentPattern(alignPatPos[i], alignPatPos[j]);
      }
    }

    drawDummyFormatBits();
    drawVersion();
  }

  private void drawDummyFormatBits() {

    for (int i = 0; i <= 5; i++)
      darkenFunctionModule(8, i, 0);
    darkenFunctionModule(8, 7, 0);
    darkenFunctionModule(8, 8, 0);
    darkenFunctionModule(7, 8, 0);
    for (int i = 9; i < 15; i++)
      darkenFunctionModule(14 - i, 8, 0);


    for (int i = 0; i < 8; i++)
      darkenFunctionModule(size - 1 - i, 8, 0);
    for (int i = 8; i < 15; i++)
      darkenFunctionModule(8, size - 15 + i, 0);
    darkenFunctionModule(8, size - 8, 1);
  }

  private void drawVersion() {
    if (version < 7)
      return;

    int rem = version;
    for (int i = 0; i < 12; i++)
      rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
    int bits = version << 12 | rem;
    assert bits >>> 18 == 0;

    for (int i = 0; i < 18; i++) {
      int bit = QrCode.getBit(bits, i);
      int a = size - 11 + i % 3;
      int b = i / 3;
      darkenFunctionModule(a, b, bit);
      darkenFunctionModule(b, a, bit);
    }
  }

  private void drawFinderPattern(int x, int y) {
    for (int dy = -4; dy <= 4; dy++) {
      for (int dx = -4; dx <= 4; dx++) {
        int dist = Math.max(Math.abs(dx), Math.abs(dy));
        int xx = x + dx, yy = y + dy;
        if (0 <= xx && xx < size && 0 <= yy && yy < size)
          darkenFunctionModule(xx, yy, (dist != 2 && dist != 4) ? 1 : 0);
      }
    }
  }

  private void drawAlignmentPattern(int x, int y) {
    for (int dy = -2; dy <= 2; dy++) {
      for (int dx = -2; dx <= 2; dx++)
        darkenFunctionModule(x + dx, y + dy, Math.abs(Math.max(Math.abs(dx), Math.abs(dy)) - 1));
    }
  }

  private int[][] generateMasks() {
    int[][] result = new int[8][template.length];
    for (int mask = 0; mask < result.length; mask++) {
      int[] maskModules = result[mask];
      for (int y = 0, i = 0; y < size; y++) {
        for (int x = 0; x < size; x++, i++) {
          boolean invert = switch (mask) {
            case 0 -> (x + y) % 2 == 0;
            case 1 -> y % 2 == 0;
            case 2 -> x % 3 == 0;
            case 3 -> (x + y) % 3 == 0;
            case 4 -> (x / 3 + y / 2) % 2 == 0;
            case 5 -> x * y % 2 + x * y % 3 == 0;
            case 6 -> (x * y % 2 + x * y % 3) % 2 == 0;
            case 7 -> ((x + y) % 2 + x * y % 3) % 2 == 0;
            default -> throw new AssertionError();
          };
          int bit = (invert ? 1 : 0) & ~getModule(isFunction, x, y);
          maskModules[i >>> 5] |= bit << i;
        }
      }
    }
    return result;
  }

  private int[] generateZigzagScan() {
    int[] result = new int[getNumRawDataModules(version) / 8 * 8];
    int i = 0;
    for (int right = size - 1; right >= 1; right -= 2) {
      if (right == 6)
        right = 5;
      for (int vert = 0; vert < size; vert++) {
        for (int j = 0; j < 2; j++) {
          int x = right - j;
          boolean upward = ((right + 1) & 2) == 0;
          int y = upward ? size - 1 - vert : vert;
          if (getModule(isFunction, x, y) == 0 && i < result.length) {
            result[i] = y * size + x;
            i++;
          }
        }
      }
    }
    assert i == result.length;
    return result;
  }

  private int getModule(int[] grid, int x, int y) {
    assert 0 <= x && x < size;
    assert 0 <= y && y < size;
    int i = y * size + x;
    return QrCode.getBit(grid[i >>> 5], i);
  }

  private void darkenFunctionModule(int x, int y, int enable) {
    assert 0 <= x && x < size;
    assert 0 <= y && y < size;
    assert enable == 0 || enable == 1;
    int i = y * size + x;
    template[i >>> 5] |= enable << i;
    isFunction[i >>> 5] |= 1 << i;
  }

  private int[] getAlignmentPatternPositions() {
    if (version == 1)
      return new int[]{};
    else {
      int numAlign = version / 7 + 2;
      int step = (version == 32) ? 26 :
        (version * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2) * 2;
      int[] result = new int[numAlign];
      result[0] = 6;
      for (int i = result.length - 1, pos = size - 7; i >= 1; i--, pos -= step)
        result[i] = pos;
      return result;
    }
  }

  static int getNumRawDataModules(int ver) {
    if (ver < QrCode.MIN_VERSION || ver > QrCode.MAX_VERSION)
      throw new IllegalArgumentException("Version number out of range");
    int result = (16 * ver + 128) * ver + 64;
    if (ver >= 2) {
      int numAlign = ver / 7 + 2;
      result -= (25 * numAlign - 10) * numAlign - 55;
      if (ver >= 7)
        result -= 36;
    }
    return result;
  }

}
