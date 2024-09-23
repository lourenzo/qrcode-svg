package br.com.lourenzo.qrcode;

import lombok.Builder;

import static java.util.stream.IntStream.range;

@Builder
public class SvgQrCode {

  @Builder.Default
  private String backgroundColor = "#FFFFFF";

  @Builder.Default
  private String foregroundColor = "#000000";

  @Builder.Default
  private Integer omitRadius = 4;

  @Builder.Default
  private Integer scale = 10;

  @Builder.Default
  private Integer border = 4;

  @Builder.Default
  private Boolean useBackground = false;

  @Builder.Default
  private Boolean useCustomFinderPatterns = true;

  private QrCode qr;

  public String generateSvg(String url) {
    qr = QrCode.encodeText(url, QrCode.Ecc.HIGH);

    StringBuilder svg = new StringBuilder();

    if (useBackground) {
      svg
        .append("\t<rect width=\"100%\" height=\"100%\" fill=\"")
        .append(backgroundColor)
        .append("\"/>\n");
    }

    range(0, qr.size)
      .forEach(y -> range(0, qr.size)
        .filter(x -> qr.getModule(x, y))
        .filter(x -> !isFinderPattern(x, y, qr.size))
        .filter(x -> !isInOmissionZone(x, y, (qr.size - 1) / 2f, (qr.size - 1) / 2f, omitRadius))
        .forEach(x -> svg.append("\t<circle cx=\"").append((x + border) * scale + scale / 2.0)
          .append("\" cy=\"").append((y + border) * scale + scale / 2.0)
          .append("\" r=\"").append(scale / 2.2)
          .append("\" fill=\"").append(foregroundColor).append("\"/>\n")));

    if (useCustomFinderPatterns) {
      svg
        .append(buildLogo())
        .append(buildFinderPatterns());
    }

    return svg.toString();
  }

  public String generateSvgFile(String url) {
    String contents = generateSvg(url);
    int totalSize = qr.size * scale + (2 * border * scale);

    return """
      <?xml version="1.0" encoding="UTF-8"?>
      <svg xmlns="http://www.w3.org/2000/svg" width="%s" height="%s" viewBox="0 0 %s %s">
      %sd
      </svg>
      """.formatted(totalSize, totalSize, totalSize, totalSize, contents);
  }

  // TODO: allow logo customization
  private String buildLogo() {
    double a = (0.01 * scale);
    double b = 0;
    double c = 0;
    double d = (0.01 * scale);
    double e = ((qr.size - 6) / 2d * scale) + border * scale;
    double f = ((qr.size - 8) / 2d * scale) + border * scale;

    String background = (useBackground)
      ? "<rect x=\"50\" y=\"170\" fill=\"" + backgroundColor + "\" width=\"500\" height=\"500\" />"
      : "";

    return """
      <style type="text/css">
        .logoMarker{fill:url(#logoGradientPattern);}
        .logoGray{fill:#7D8082;}
      </style>
      <g transform="matrix(%s,%s,%s,%s,%s,%s)">
      %s
      <linearGradient id="logoGradientPattern">
        <stop offset="0" style="stop-color:#EF3F40" />
        <stop offset="100%%" style="stop-color:#483165" />
      </linearGradient>
      <path class="logoMarker" d="M261.5,477.4l13.7,10.1l13.9-9.8C421.6,384.1,412.2,299,402,266.8c-16.1-51-67-85.3-126.5-85.4l0,0
      c-57.7,0.1-105.6,32.2-121.9,82C131.3,330.9,171.6,410.9,261.5,477.4z M198.2,278.1c9.9-30.1,40.2-49.6,77.2-49.6
      c38.9,0.1,71.7,21.2,81.7,52.5c13.8,43.6-16.5,98.2-81.3,148C212.5,378,183.7,322.3,198.2,278.1z" />
      <path class="logoGray" d="M237.1 293.6a38.3 37.2 0 1 0 76.6 0 38.3 37.2 0 1 0-76.6 0M431.6 569.7l3.6-5.5s17-19.9
      29.5-44.6c-14.2-12.3-21.6-18.7-37.7-32.3-15.9 29.7-31.9 47.2-31.9 47.2-.3.5-1.1.6-1.5.2l-64-58.7c-9.8 8.2-20.5 16.4-32.2 24.6l-9.5 6.7
      72.8 66.3c.5.4.5 1.2 0 1.6-27.1 23.2-57.3 35.1-90.4 35.1-27.4 0-50.5-7.3-68.2-21.9-17.7-14.6-26.8-32.3-26.8-53.6 0-27.6
      13.2-49.6 39.1-66-12.8-11.7-24.2-23.7-34.5-35.9-36.8 22.9-59.4 58.9-59.4 102.5 0 37.1 13.4 67 40.8 90.1s63.9 34.7 108.3
      34.7c49.8 0 92.9-17.6 128.8-52.1.1-.1.4-.1.5 0l47.5 43.3c.2.2.4.3.7.3h71.3c.9 0 1.3-1.1.7-1.7l-87.5-80.3z"/>
      </g>""".formatted(a, b, c, d, e, f, background);
  }

  private String buildFinderPatterns() {
    int actualBorder = border * scale;

    return """
      <rect class="logoMarker" x="%d" y="%d" rx="%d" width="%d" height="%d" />
      <rect x="%d" y="%d" rx="%d" fill="%s" width="%d" height="%d" />
      <rect x="%d" y="%d" rx="%d" fill="%s" width="%d" height="%d" />

      <rect class="logoMarker" x="%d" y="%d" rx="%d" width="%d" height="%d" />
      <rect x="%d" y="%d" rx="%d" fill="%s" width="%d" height="%d" />
      <rect x="%d" y="%d" rx="%d" fill="%s" width="%d" height="%d" />

      <rect class="logoMarker" x="%d" y="%d" rx="%d" width="%d" height="%d" />
      <rect x="%d" y="%d" rx="%d" fill="%s" width="%d" height="%d" />
      <rect x="%d" y="%d" rx="%d" fill="%s" width="%d" height="%d" />""".formatted(
      actualBorder, actualBorder, scale, 7 * scale, 7 * scale,
      actualBorder + scale, actualBorder + scale, scale, "#FFFFFF", 5 * scale, 5 * scale,
      actualBorder + 2 * scale, actualBorder + 2 * scale, scale, "#7D8082", 3 * scale, 3 * scale,
      actualBorder + (qr.size - 7) * scale, actualBorder, scale, 7 * scale, 7 * scale,
      actualBorder + (qr.size - 6) * scale, actualBorder + scale, scale, "#FFFFFF", 5 * scale, 5 * scale,
      actualBorder + (qr.size - 5) * scale, actualBorder + 2 * scale, scale, "#7D8082", 3 * scale, 3 * scale,
      actualBorder, actualBorder + (qr.size - 7) * scale, scale, 7 * scale, 7 * scale,
      actualBorder + scale, actualBorder + (qr.size - 6) * scale, scale, "#FFFFFF", 5 * scale, 5 * scale,
      actualBorder + 2 * scale, actualBorder + (qr.size - 5) * scale, scale, "#7D8082", 3 * scale, 3 * scale
    );
  }

  private boolean isFinderPattern(int x, int y, int size) {
    if (!useCustomFinderPatterns) return false;
    return (x <= 6 && y <= 6) || (x >= size - 7 && y <= 6) || (x <= 6 && y >= size - 7);
  }

  private boolean isInOmissionZone(int x, int y, double centerX, double centerY, int radius) {
    if (!useCustomFinderPatterns) return false;
    double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
    return distance <= radius;
  }
}
