// SelectionView.java
//
// Copyright 2018 by Jack Boyce (jboyce@gmail.com) and others

/*
    This file is part of Juggling Lab.

    Juggling Lab is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Juggling Lab is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Juggling Lab; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package jugglinglab.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import jugglinglab.core.*;
import jugglinglab.jml.*;
import jugglinglab.util.*;


public class SelectionView extends View {
    protected static final int rows = 3;
    protected static final int columns = 3;
    protected static final int count = rows * columns;
    protected static final int center = (count - 1) / 2;

    protected AnimationPanel[] ja;
    protected JLayeredPane layered;
    protected Mutator mutator;


    public SelectionView(Dimension dim) {
        this.ja = new AnimationPanel[count];
        for (int i = 0; i < count; i++)
            this.ja[i] = new AnimationPanel();

        // JLayeredPane on the left so we can show a grid of animations with
        // an overlay drawn on top
        this.layered = makeLayeredPane(dim, makeAnimationGrid(), makeOverlay());

        this.mutator = new Mutator();
        JPanel controls = mutator.getControlPanel();

        GridBagLayout gb = new GridBagLayout();
        this.setLayout(gb);

        this.add(layered);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = gbc.weighty = 1.0;
        gb.setConstraints(layered, gbc);

        this.add(controls);
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridheight = gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gb.setConstraints(controls, gbc);
    }

    protected JPanel makeAnimationGrid() {
        JPanel pgrid = new JPanel();

        pgrid.setLayout(new GridLayout(rows, columns));
        for (int i = 0; i < count; i++)
            pgrid.add(ja[i]);

        pgrid.addMouseListener(new MouseAdapter() {
            // will only receive mouseReleased events here when one of the
            // AnimationPanel objects dispatches it to us in its
            // mouseReleased() method.
            @Override
            public void mouseReleased(MouseEvent me) {
                Component c = me.getComponent();
                int num;
                for (num = 0; num < count; num++) {
                    if (c == SelectionView.this.ja[num])
                        break;
                }
                if (num == count)
                    return;
                try {
                    SelectionView.this.restartView(ja[num].getPattern(), null);
                } catch (JuggleExceptionUser jeu) {
                    new ErrorDialog(parent, jeu.getMessage());
                } catch (JuggleExceptionInternal jei) {
                    ErrorDialog.handleFatalException(jei);
                }
            }
        });

        pgrid.addMouseMotionListener(new MouseMotionAdapter() {
            // Dispatched here from one of the AnimationPanels when the
            // user drags the mouse for a camera angle change. Copy to the
            // other animations.
            @Override
            public void mouseDragged(MouseEvent me) {
                Component c = me.getComponent();
                int num;
                for (num = 0; num < count; num++) {
                    if (c == SelectionView.this.ja[num])
                        break;
                }
                if (num == count)
                    return;
                double[] ca = ja[num].getCameraAngle();
                for (int i = 0; i < count; i++) {
                    if (i != num)
                        ja[i].setCameraAngle(ca);
                }
            }
        });
        return pgrid;
    }

    protected JPanel makeOverlay() {
        JPanel poverlay = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                Dimension d = getSize();
                int xleft = (d.width * ((columns - 1) / 2)) / columns;
                int ytop = (d.height * ((rows - 1) / 2)) / rows;
                int width = d.width / columns;
                int height = d.height / rows;

                Graphics2D g2 = (Graphics2D)g.create();
                Stroke stroke = new BasicStroke(2, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_BEVEL, 0);
                g2.setStroke(stroke);
                g2.setColor(Color.lightGray);
                g2.drawRect(xleft, ytop, width, height);
                g2.dispose();
            }
        };
        poverlay.setOpaque(false);
        return poverlay;
    }

    protected JLayeredPane makeLayeredPane(Dimension d, JPanel grid, JPanel overlay) {
        JLayeredPane layered = new JLayeredPane();

        // ensure the entire grid fits on the screen, rescaling if needed
        int pref_width = columns * d.width;
        int pref_height = rows * d.height;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int max_width = screenSize.width - 200;    // allocation for controls
        int max_height = screenSize.height - 50;

        if (pref_width > max_width || pref_height > max_height) {
            double scale = Math.min((double)max_width / (double)pref_width,
                                    (double)max_height / (double)pref_height);
            pref_width = (int)(scale * pref_width);
            pref_height = (int)(scale * pref_height);
        }
        layered.setPreferredSize(new Dimension(pref_width, pref_height));

        layered.add(grid, JLayeredPane.DEFAULT_LAYER);
        layered.add(overlay, JLayeredPane.PALETTE_LAYER);

        layered.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension d = layered.getSize();
                grid.setBounds(0, 0, d.width, d.height);
                overlay.setBounds(0, 0, d.width, d.height);
            }
        });
        return layered;
    }


    @Override
    public void restartView() throws JuggleExceptionUser, JuggleExceptionInternal {
        for (int i = 0; i < count; i++)
            ja[i].restartJuggle();
    }

    @Override
    public void restartView(JMLPattern p, AnimationPrefs c) throws
                        JuggleExceptionUser, JuggleExceptionInternal {
        ja[center].restartJuggle(p, c);
        for (int i = 0; i < count; i++) {
            if (i != center) {
                JMLPattern newp = (p == null ? null : mutator.mutatePattern(p));
                ja[i].restartJuggle(newp, c);
            }
        }
    }

    @Override
    public void setAnimationPanelPreferredSize(Dimension d) {
        // This works differently for this view since the JLayeredPane has no
        // layout manager, so preferred size info can't propagate up from the
        // individual animation panels. So we go the other direction: set a
        // preferred size for the overall JLayeredPane, which gets propagated to
        // the grid (and the individual animations) by the ComponentAdapter above.
        int width = columns * d.width;
        int height = rows * d.height;
        layered.setPreferredSize(new Dimension(width, height));
    }

    @Override
    public Dimension getAnimationPanelSize() {
        return ja[center].getSize(new Dimension());
    }

    @Override
    public void disposeView() {
        for (int i = 0; i < count; i++)
            ja[i].disposeAnimation();
    }

    @Override
    public JMLPattern getPattern()              { return ja[center].getPattern(); }

    @Override
    public AnimationPrefs getAnimationPrefs()   { return ja[center].getAnimationPrefs(); }

    @Override
    public boolean getPaused()                  { return ja[center].getPaused(); }

    @Override
    public void setPaused(boolean pause) {
        if (ja[center].message == null)
            for (int i = 0; i < count; i++)
                ja[i].setPaused(pause);
    }

    @Override
    public void writeGIF() {
        for (int i = 0; i < count; i++)
            ja[i].writingGIF = true;
        boolean origpause = getPaused();
        setPaused(true);

        Runnable cleanup = new Runnable() {
            @Override
            public void run() {
                setPaused(origpause);
                for (int i = 0; i < count; i++)
                    ja[i].writingGIF = false;
            }
        };

        new View.GIFWriter(ja[center], cleanup);
    }
}
