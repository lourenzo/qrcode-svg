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
import java.util.Objects;

final class ReedSolomonGenerator {

  public static final Memoizer<Integer, ReedSolomonGenerator> MEMOIZER
    = new Memoizer<>(ReedSolomonGenerator::new);

  private final byte[][] polynomialMultiply;

  private ReedSolomonGenerator(int degree) {
    if (degree < 1 || degree > 255)
      throw new IllegalArgumentException("Degree out of range");

    byte[] coefficients = new byte[degree];
    coefficients[degree - 1] = 1;

    int root = 1;
    for (int i = 0; i < degree; i++) {

      for (int j = 0; j < coefficients.length; j++) {
        coefficients[j] = (byte) multiply(coefficients[j] & 0xFF, root);
        if (j + 1 < coefficients.length)
          coefficients[j] ^= coefficients[j + 1];
      }
      root = multiply(root, 0x02);
    }

    polynomialMultiply = new byte[256][degree];
    for (int i = 0; i < polynomialMultiply.length; i++) {
      for (int j = 0; j < degree; j++)
        polynomialMultiply[i][j] = (byte) multiply(i, coefficients[j] & 0xFF);
    }
  }

  public void getRemainder(byte[] data, int dataOff, int dataLen, byte[] result) {
    Objects.requireNonNull(data);
    Objects.requireNonNull(result);
    int degree = polynomialMultiply[0].length;
    assert result.length == degree;

    Arrays.fill(result, (byte) 0);
    for (int i = dataOff, dataEnd = dataOff + dataLen; i < dataEnd; i++) {
      byte[] table = polynomialMultiply[(data[i] ^ result[0]) & 0xFF];
      for (int j = 0; j < degree - 1; j++)
        result[j] = (byte) (result[j + 1] ^ table[j]);
      result[degree - 1] = table[degree - 1];
    }
  }

  private static int multiply(int x, int y) {
    assert x >> 8 == 0 && y >> 8 == 0;

    int z = 0;
    for (int i = 7; i >= 0; i--) {
      z = (z << 1) ^ ((z >>> 7) * 0x11D);
      z ^= ((y >>> i) & 1) * x;
    }
    assert z >>> 8 == 0;
    return z;
  }

}
