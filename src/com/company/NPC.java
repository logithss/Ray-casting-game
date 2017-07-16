package com.company;

import javafx.geometry.Point2D;

import java.util.LinkedList;

/**
 * Created by Lenovo on 14.07.2017.
 */
public class NPC extends Character {
    enum Position {
        STANDING, BACK, LEFT, RIGHT, FALLING, CASTING, ATTACKING
    }

    enum NPCs {
        BALDRIC, KNIGHT, ORC        // TODO MAGE
    }

    enum Attitude {
        GOOD, NEUTRAL, EVIL
    }

    private Attitude attitude;
    private NPCs type;
    private Position position = Position.STANDING;

    public NPC(double speed, double sprintSpeed, int health, int mana, int stamina, int maxStamina, Point2D pos, Point2D dir, LinkedList<Weapon.Weapons> weapons, Attitude attitude,
               NPCs type) {
        super(speed, sprintSpeed, health, mana, stamina, maxStamina, pos, dir, weapons);
        this.attitude = attitude;
        this.type = type;
    }

    void update() {

    }

    public NPCs getType() {
        return type;
    }

    public Position getPosition() {
        return position;
    }
}
