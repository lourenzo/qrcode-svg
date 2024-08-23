package br.com.lourenzo.qrcode

public class SvgQrCode {

  public static String generateSvg(String url) {
    QrCode qr = QrCode.encodeText(url, QrCode.Ecc.HIGH);
    int scale = 10;
    int border = 4;

    int omitRadius = 0;

    double centerX = (qr.size - 1) / 2.0;
    double centerY = (qr.size - 1) / 2.0;

    StringBuilder svg = new StringBuilder();

    svg.append("\t<rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\"/>\n");

    for (int y = 0; y < qr.size; y++) {
      for (int x = 0; x < qr.size; x++) {
        if (qr.getModule(x, y)) {

          if (!isFinderPattern(x, y, qr.size) && !isInOmissionZone(x, y, centerX, centerY, omitRadius)) {

            svg.append("\t<circle cx=\"").append((x + border) * scale + scale / 2.0)
              .append("\" cy=\"").append((y + border) * scale + scale / 2.0)
              .append("\" r=\"").append(scale / 2.2)
              .append("\" fill=\"#000000\"/>\n");
          }
        }
      }
    }

    svg.append(logoExcerpt);
    svg.append(finderPatterns);

    return svg.toString();
  }

  public static String generateSvgFile(String url) {
    int totalSize = 410;
    String contents = generateSvg(url);

    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
      "width=\"" + totalSize + "\" height=\"" + totalSize + "\" " +
      "viewBox=\"0 0 " + totalSize + " " + totalSize + "\">\n" +
      contents +
      "\n</svg>\n";
  }

  private static final String finderPatterns = """
    <style type="text/css">
      .gradientFill{fill:url(#gradientCarmineToJacarta);}
      .greyFill{fill:#7D8082;}
    </style>
    <linearGradient id="gradientCarmineToJacarta">
      <stop offset="0" style="stop-color:#EF3F40" />
      <stop offset="100%" style="stop-color:#483165" />
    </linearGradient>
    <rect class="gradientFill" x="40" y="40" rx="10" width="70" height="70" />
    <rect x="50" y="50" rx="10" fill="#FFFFFF" width="50" height="50" />
    <rect x="60" y="60" rx="10" fill="#7D8082" width="30" height="30" />

    <rect class="gradientFill" x="300" y="40" rx="10" width="70" height="70" />
    <rect x="310" y="50" rx="10" fill="#FFFFFF" width="50" height="50" />
    <rect x="320" y="60" rx="10" fill="#7D8082" width="30" height="30" />

    <rect class="gradientFill" x="40" y="300" rx="10" width="70" height="70" />
    <rect x="50" y="310" rx="10" fill="#FFFFFF" width="50" height="50" />
    <rect x="60" y="320" rx="10" fill="#7D8082" width="30" height="30" />""";

  private static boolean isFinderPattern(int x, int y, int size) {
    return (x <= 6 && y <= 6) || (x >= size - 7 && y <= 6) || (x <= 6 && y >= size - 7);
  }

  private static boolean isInOmissionZone(int x, int y, double centerX, double centerY, int radius) {
    double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
    return distance <= radius;
  }
}
