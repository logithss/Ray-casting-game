package com.company;

import javafx.geometry.Point2D;
import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Lenovo on 10.07.2017.
 */
public class Camera extends JPanel {
    private final int resX, resY, wallHeight, weaponX, weaponY, floorSize = 4, ceilingSize = 4, halfResY, visibility = 8, fogRGB = Color.black.getRGB();
            // floorSize and ceilingSize are in tiles

    private BufferedImage rendered;

    private Hero hero;

    private int[][] map;

    private LinkedList<NPC> NPCs;

    public Camera(int resX, int resY, Hero hero, int[][] map, LinkedList<NPC> NPCs) {
        this.resX = resX;
        this.resY = resY;
        wallHeight = resY;
        halfResY = resY / 2;
        weaponX = resX - 1000;
        weaponY = resY - 500;
        this.hero = hero;
        this.map = map;
        this.NPCs = NPCs;

        rendered = new BufferedImage(resX, resY, BufferedImage.TYPE_INT_RGB);
    }

    public void paint(Graphics g) {
        render(g);
        drawWeapon(g);
        drawViewfinder(g);
    }

    private void drawViewfinder(Graphics g) {
        BufferedImage viewfinder = Textures.getSprites().get(Sprite.Sprites.VIEWFINDER).getImage();
        g.drawImage(viewfinder, (resX - viewfinder.getWidth()) / 2, (resY - viewfinder.getHeight()) / 2, null);
    }

    private void drawWeapon(Graphics g) {
        BufferedImage img = Textures.getSprites().get(Textures.getWeapons().get(hero.getWeapon())).getImage();
        if (img != null) {
            AffineTransform at = new AffineTransform();
            at.translate(img.getWidth() / 2, img.getHeight() / 2);
            at.rotate(hero.getWeaponAngle(), img.getWidth() / 5, img.getHeight() / 2);
            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);

            g.drawImage(op.filter(img, null), weaponX, weaponY, null);
        }
    }

    private LinkedList<Pair<NPC, Integer>> checkNPCs() {
        LinkedList<Pair<NPC, Integer>> NPCsToDraw = new LinkedList<>();

        for (NPC i : NPCs) {
            Point2D vec = i.getPos().subtract(hero.getPos()), dir = hero.getDir(), zero = new Point2D(1, 0), perp = new Point2D(-vec.getY(), vec.getX());
            double vecAngle = vec.angle(zero), dirAngle = dir.angle(zero);
            vecAngle = vec.getY() < 0 ? 360 - vecAngle : vecAngle;
            dirAngle = dir.getY() < 0 ? 360 - dirAngle : dirAngle;
            perp = perp.multiply(1 / vec.magnitude() * Textures.getSprites().get(Textures.getNPCs().get(i.getType()).get(i.getPosition())).getImage().getWidth() /
                    Textures.getSprites().get(Textures.getBlocks().get(1)).getImage().getWidth());
            vec = vec.add(perp.multiply(vecAngle < dirAngle ? 1 : -1));

            // TODO CALCULATE X WHERE NPC SHOULD BE DRAWN

            if (vec.angle(dir) < hero.getFov() * 90 / Math.PI)
                NPCsToDraw.add(new Pair<>(i, 2137));
        }

        NPCsToDraw.sort(new Comparator<Pair<NPC, Integer>>() {
            @Override
            public int compare(Pair<NPC, Integer> o1, Pair<NPC, Integer> o2) {
                return o1.getValue() < o2.getValue() ? -1 : 1;
            }
        });

        return NPCsToDraw;
    }

    private void render(Graphics g) {       // TODO DRAW NPCS HERE
        int wallCenterZ = (int) (halfResY * hero.getzDir());
        double fovRatio = hero.getDefaultFov() / hero.getFov();
        Point2D dir = hero.getDir(), plane = new Point2D(-dir.getY(), dir.getX()).multiply(Math.tan(hero.getFov() / 2) * dir.magnitude()), vec = dir.add(plane),
                deltaPlane = plane.multiply((double) 2 / resX), pos = hero.getPos();

        for (int i = 0; i < resX; i++, vec = vec.subtract(deltaPlane)) {        // TODO DRAW DIFFERENT HEIGHT BLOCKS
            Iterator<Pair<Point2D, Boolean>> iterator = hero.collisionInfo(vec).iterator();     // TODO CHECK ALSO WHERE BLOCKS ENDS NOT ONLY WHERE START (TO DRAW IT LIKE FLOOR OK)

            Pair<Point2D, Boolean> collisionInfo = hero.collisionInfo(vec).getFirst();
            Point2D collisionPoint = collisionInfo.getKey();

            int j = 0, h = (int) (wallHeight * fovRatio * vec.magnitude() / pos.distance(collisionPoint)), emptyH = wallCenterZ - h / 2;

            BufferedImage img = Textures.getSprites().get(Textures.getBlocks().get(hero.block(vec, collisionPoint))).getImage();
            int x = (int) (((collisionInfo.getValue() ? collisionPoint.getY() : collisionPoint.getX()) % 1) * img.getWidth());

            float fogRatio = (float) pos.distance(collisionPoint);
            fogRatio /= fogRatio < visibility ? visibility : 1;

            for (; j < emptyH; j++) {
                double d = halfResY * vec.magnitude() / (wallCenterZ - j) * fovRatio;
                Point2D p = hero.getPos().add(vec.multiply(d / vec.magnitude()));
                int tile = map[(int) p.getY()][(int) p.getX()];

                if (d < visibility) {
                    BufferedImage ceiling = Textures.getSprites().get(Textures.getCeilings().getOrDefault(tile, Sprite.Sprites.CEILING0)).getImage();
                    rendered.setRGB(i, j, mix(ceiling.getRGB((int) ((p.getX() % ceilingSize) / ceilingSize * ceiling.getWidth()),
                            (int) ((p.getY() % ceilingSize) / ceilingSize * ceiling.getHeight())), fogRGB, (float) d / visibility));
                }
                else
                    rendered.setRGB(i, j, fogRGB);
            }
            for (; j < resY && j < emptyH + h; j++)
                rendered.setRGB(i, j, fogRatio < 1 ? mix(img.getRGB(x, (j - emptyH) * img.getHeight() / h), fogRGB, fogRatio) : fogRGB);
            for (; j < resY; j++) {
                double d = halfResY * vec.magnitude() / (j - wallCenterZ) * fovRatio;
                Point2D p = hero.getPos().add(vec.multiply(d / vec.magnitude()));
                int tile = map[(int) p.getY()][(int) p.getX()];

                if (d < visibility) {
                    BufferedImage floor = Textures.getSprites().get(Textures.getFloors().getOrDefault(tile, Sprite.Sprites.FLOOR0)).getImage();
                    rendered.setRGB(i, j, mix(floor.getRGB((int) ((p.getX() % floorSize) / floorSize * floor.getWidth()),
                            (int) ((p.getY() % floorSize) / floorSize * floor.getHeight())), fogRGB, (float) d / visibility));
                }
                else
                    rendered.setRGB(i, j, fogRGB);
            }
        }

        g.drawImage(rendered, 0, 0, null);
    }

    private int mix (int c0, int c1, float ratio) {     // ratio of (c1 - all) to all
        ratio = ratio < 0f ? 0f : ratio > 1f ? 1f : ratio;
        float iRatio = 1.0f - ratio;

        int a = (int) ((c0 >> 24 & 0xff) * iRatio + (c1 >> 24 & 0xff) * ratio);
        int r = (int) (((c0 & 0xff0000) >> 16) * iRatio + ((c1 & 0xff0000) >> 16) * ratio);
        int g = (int) (((c0 & 0xff00) >> 8) * iRatio + ((c1 & 0xff00) >> 8) * ratio);
        int b = (int) ((c0 & 0xff) * iRatio + (c1 & 0xff) * ratio);

        return a << 24 | r << 16 | g << 8 | b;
    }
}
