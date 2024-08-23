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

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

final class Memoizer<T, R> {

  private final Function<T, R> function;
  Map<T, SoftReference<R>> cache = new ConcurrentHashMap<>();
  private final Set<T> pending = new HashSet<>();

  public Memoizer(Function<T, R> func) {
    function = func;
  }

  public R get(T arg) {

    {
      SoftReference<R> ref = cache.get(arg);
      if (ref != null) {
        R result = ref.get();
        if (result != null) return result;
      }
    }


    while (true) {
      synchronized (this) {
        SoftReference<R> ref = cache.get(arg);
        if (ref != null) {
          R result = ref.get();
          if (result != null) return result;
          cache.remove(arg);
        }
        assert !cache.containsKey(arg);

        if (pending.add(arg)) break;

        try {
          this.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    try {
      R result = function.apply(arg);
      cache.put(arg, new SoftReference<>(result));
      return result;
    } finally {
      synchronized (this) {
        pending.remove(arg);
        this.notifyAll();
      }
    }
  }

}
