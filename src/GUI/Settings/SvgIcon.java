package GUI.Settings;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SvgIcon {

    private static final Pattern PATH_D = Pattern.compile("<path\\s+d=\"([^\"]+)\"");
    private static final Pattern VIEWABLE = Pattern.compile("viewBox=\"0 0 ([\\d.]+) ([\\d.]+)\"");

    public static Group load(String resourcePath, double size, String fillHex) {
        Group group = new Group();
        populate(group, resourcePath, size, fillHex);
        return group;
    }

    public static void setContent(Group group, String resourcePath, double size, String fillHex) {
        populate(group, resourcePath, size, fillHex);
    }

    private static void populate(Group group, String resourcePath, double size, String fillHex) {
        group.getChildren().clear();
        String svg = readResource(resourcePath);

        double vbW = 48, vbH = 48;
        Matcher vb = VIEWABLE.matcher(svg);
        if (vb.find()) {
            vbW = Double.parseDouble(vb.group(1));
            vbH = Double.parseDouble(vb.group(2));
        }

        double scale = size / Math.max(vbW, vbH);

        Matcher m = PATH_D.matcher(svg);
        while (m.find()) {
            SVGPath path = new SVGPath();
            path.setContent(m.group(1));
            path.setFill(Color.web(fillHex));
            path.getTransforms().add(new Scale(scale, scale, 0, 0));
            group.getChildren().add(path);
        }
    }

    private static String readResource(String resourcePath) {
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

        try (InputStream is = SvgIcon.class.getClassLoader().getResourceAsStream(normalized)) {
            if (is == null) throw new IOException("Not found: " + normalized);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}