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


public class DataTooLongException extends IllegalArgumentException {

  public DataTooLongException(String msg) {
    super(msg);
  }

}
