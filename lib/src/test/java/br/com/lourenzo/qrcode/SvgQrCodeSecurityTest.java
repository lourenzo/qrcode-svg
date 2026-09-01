package br.com.lourenzo.qrcode;

import org.junit.Test;
import static org.junit.Assert.*;

public class SvgQrCodeSecurityTest {

    @Test
    public void testSvgInjectionIsPrevented() {
        // Attack payload mimicking an SVG injection attempt
        String maliciousColor = "\"><script>alert('XSS')</script><rect fill=\"#000000";

        SvgQrCode qrCode = SvgQrCode.builder()
                .backgroundColor(maliciousColor)
                .foregroundColor(maliciousColor)
                .useBackground(true)
                .build();

        String svg = qrCode.generateSvg("https://example.com");

        // Ensure the payload is escaped and does not contain raw script tags or injection attributes
        assertFalse(svg.contains("\"><script>alert('XSS')</script><rect fill=\"#000000"));
        assertFalse(svg.contains("<script>"));
        assertTrue(svg.contains("&quot;&gt;&lt;script&gt;alert(&apos;XSS&apos;)&lt;/script&gt;&lt;rect fill=&quot;#000000"));
    }
}
