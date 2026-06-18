package br.com.lourenzo.qrcode.demo;

import br.com.lourenzo.qrcode.SvgQrCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Demo {

    public static void main(String[] args) {
        String data = "https://github.com/lourenzo/qrcode-svg";
        Path outputDir = Paths.get("demo_output");

        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            // Example 1: Default
            SvgQrCode defaultQr = SvgQrCode.builder().build();
            Files.writeString(outputDir.resolve("demo1_default.svg"), defaultQr.generateSvgFile(data));

            // Example 2: No custom finder patterns (Standard QR)
            SvgQrCode standardQr = SvgQrCode.builder()
                    .useCustomFinderPatterns(false)
                    .build();
            Files.writeString(outputDir.resolve("demo2_standard.svg"), standardQr.generateSvgFile(data));

            // Example 3: Custom colors with background
            SvgQrCode colorfulQr = SvgQrCode.builder()
                    .useBackground(true)
                    .backgroundColor("#2b2b2b")
                    .foregroundColor("#ffcc00")
                    .build();
            Files.writeString(outputDir.resolve("demo3_colorful.svg"), colorfulQr.generateSvgFile(data));

            // Example 4: Large scale and custom border
            SvgQrCode largeQr = SvgQrCode.builder()
                    .scale(20)
                    .border(8)
                    .build();
            Files.writeString(outputDir.resolve("demo4_large.svg"), largeQr.generateSvgFile(data));

            System.out.println("Demo SVG files generated successfully in: " + outputDir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error generating demo files: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
