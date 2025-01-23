package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.JBImageIcon;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;

public class ModuleLandingPage {
    private JPanel imageContainer;
    private JPanel main;

    private static final Logger LOGGER = Logger.getLogger('#' + ModuleLandingPage.class.getSimpleName());


    JPanel getPanel() {
        return main;
    }

    private void createUIComponents() {
        initEggIcon();
    }

    private void initEggIcon() {
        imageContainer = new JPanel();
        final Dimension size = new Dimension(80, 102);
        final float scale = 0.83f; // 80w box with 67px egg
//        final Icon eggIcon = createEggIconSVG((int) Math.floor(size.width * scale), (int) Math.floor(size.height * scale));
        final Icon eggIcon = createEggIconPNG((int) Math.floor(size.width * scale), (int) Math.floor(size.height * scale));
        if (eggIcon == null) {
            imageContainer.setVisible(false);
            return;
        }
        final JLabel label = new JLabel(eggIcon);
        imageContainer.add(label);
        imageContainer.setPreferredSize(size);
        imageContainer.setMinimumSize(size);

    }

    private Icon createEggIconPNG(final int width, final int height) {
        try {
            final URL resource = this.getClass().getResource("/icons/White-Egg.png");
            if (resource == null) {
                LOGGER.severe("Failed to locate egg icon resource");
                return null;
            }
            Image image = ImageLoader.loadFromResource("/icons/White-Egg.png", getClass());

            if (image == null) {
                LOGGER.severe("Failed to load egg image from resource");
                return null;
            }
            return new JBImageIcon(image);
        } catch (Exception e) {
            if (e instanceof ProcessCanceledException) {
                throw (ProcessCanceledException)e;
            }
            if (e instanceof CancellationException) {
                throw (CancellationException)e;
            }
            Logger.getAnonymousLogger().severe("Error loading egg image from resource stream");
            e.printStackTrace();
            return null;
        }
    }

    private Icon createEggIconSVG(final int width, final int height) {
        try {

            final BufferedImage image1x = readIconImage("/icons/_White-Egg.png");
            final BufferedImage image2x = readIconImage("/icons/_White-Egg@2x.png");
            final BufferedImage image3x = readIconImage("/icons/_White-Egg@3x.png");
            final Image image = new BaseMultiResolutionImage(new BufferedImage[] { image1x, image2x, image3x });
            return new JBImageIcon(image);
        } catch (Exception e) {
            if (e instanceof ProcessCanceledException) {
                throw (ProcessCanceledException)e;
            }
            if (e instanceof CancellationException) {
                throw (CancellationException)e;
            }
            Logger.getAnonymousLogger().severe("Error loading egg image from resource");
            e.printStackTrace();
            return null;
        }
    }

    @NotNull
    private BufferedImage readIconImage(final String name) throws IOException {
        try(final InputStream stream = this.getClass().getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("Failed to get stream for icon: " + name);
            }
            return ImageIO.read(stream);
        }
    }
}
