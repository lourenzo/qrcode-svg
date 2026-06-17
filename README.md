# qrcode-svg

Generate Customized SVG-based QR Codes in Java.

* With options for customizing finder patterns, with detection of edges for arbitrary manipulation;
* Detection of alignment patterns;
* With settings for colors and shapes of modules (data elements forming the code);
* Control the size of the logo override;
* Full control of string composition, proportion and QRCode features;

## Usage

This library allows you to easily generate an SVG QR code using a Builder pattern.

```java
import br.com.lourenzo.qrcode.SvgQrCode;

public class Example {
    public static void main(String[] args) {
        // Build the generator with customized options
        SvgQrCode qrCode = SvgQrCode.builder()
            .useCustomFinderPatterns(true)
            .useBackground(true)
            .backgroundColor("#F0F0F0")
            .foregroundColor("#FF5733")
            .build();

        // Generate a complete SVG file string
        String svgOutput = qrCode.generateSvgFile("https://example.com");
        System.out.println(svgOutput);
    }
}
```

## Configuration Options

You can configure the following properties using the `SvgQrCode.builder()`:

* **backgroundColor** (`String`, default: `"#FFFFFF"`): The background color of the QR code (if `useBackground` is true). Needs to be a valid SVG color.
* **foregroundColor** (`String`, default: `"#000000"`): The color of the modules (the actual data dots). Needs to be a valid SVG color.
* **omitRadius** (`Integer`, default: `4`): The radius for an omission zone in the center of the code, creating space for a custom logo. Only applies if `useCustomFinderPatterns` is true.
* **scale** (`Integer`, default: `10`): Scaling factor for drawing the SVG shapes.
* **border** (`Integer`, default: `4`): The size of the border (quiet zone) around the QR code, measured in modules.
* **useBackground** (`Boolean`, default: `false`): If `true`, a background `<rect>` with the specified `backgroundColor` will be drawn behind the modules.
* **useCustomFinderPatterns** (`Boolean`, default: `true`): If `true`, uses customized styles for finder patterns and allows the center omission zone. If `false`, standard module drawing is used for everything.

Under active development, reach out for news soon.
