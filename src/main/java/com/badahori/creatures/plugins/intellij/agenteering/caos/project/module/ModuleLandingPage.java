package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module;

import com.au.id.mcc.adapted.swing.SVGIcon;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.JBImageIcon;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
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
            Logger.getAnonymousLogger().severe("Error loading egg image from resource stream");
            e.printStackTrace();
            return null;
        }
    }

    private Icon createEggIconSVG(final int width, final int height) {
        try {
            final URL resource = this.getClass().getResource("/icons/_White-Egg.svg");
            final String path = resource != null ? resource.toExternalForm() : null;
            if (path == null) {
                return null;
            }
            return new SVGIcon(path, width, height);
        } catch (Exception e) {
            Logger.getAnonymousLogger().severe("Error loading egg image from resource");
            e.printStackTrace();
            return null;
        }
    }
}
