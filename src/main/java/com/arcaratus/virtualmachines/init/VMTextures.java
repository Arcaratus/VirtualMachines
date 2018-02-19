package com.arcaratus.virtualmachines.init;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

public class VMTextures
{
    public static TextureAtlasSprite[] MACHINE_FACE;
    public static TextureAtlasSprite MACHINE_FACE_FARM;
    public static TextureAtlasSprite MACHINE_FACE_FISHERY;
    public static TextureAtlasSprite MACHINE_FACE_DARK_ROOM;
    public static TextureAtlasSprite MACHINE_FACE_ANIMAL_FARM;

    public static TextureAtlasSprite[] MACHINE_ACTIVE;
    public static TextureAtlasSprite MACHINE_ACTIVE_FARM;
    public static TextureAtlasSprite MACHINE_ACTIVE_FISHERY;
    public static TextureAtlasSprite MACHINE_ACTIVE_DARK_ROOM;
    public static TextureAtlasSprite MACHINE_ACTIVE_ANIMAL_FARM;

    private static TextureMap textureMap;

    private VMTextures() {}

    public static void registerTextures(TextureMap map)
    {
        textureMap = map;

        MACHINE_FACE_FARM = register(MACHINE_FACE_ + "farm");
        MACHINE_FACE_FISHERY = register(MACHINE_FACE_ + "fishery");
        MACHINE_FACE_DARK_ROOM = register(MACHINE_FACE_ + "dark_room");
        MACHINE_FACE_ANIMAL_FARM = register(MACHINE_FACE_ + "animal_farm");

        MACHINE_FACE = new TextureAtlasSprite[] { MACHINE_FACE_FARM, MACHINE_FACE_FISHERY, MACHINE_FACE_DARK_ROOM, MACHINE_FACE_ANIMAL_FARM };

        MACHINE_ACTIVE_FARM = register(MACHINE_ACTIVE_ + "farm");
        MACHINE_ACTIVE_FISHERY = register(MACHINE_ACTIVE_ + "fishery");
        MACHINE_ACTIVE_DARK_ROOM = register(MACHINE_ACTIVE_ + "dark_room");
        MACHINE_ACTIVE_ANIMAL_FARM = register(MACHINE_ACTIVE_ + "animal_farm");

        MACHINE_ACTIVE = new TextureAtlasSprite[] { MACHINE_ACTIVE_FARM, MACHINE_ACTIVE_FISHERY, MACHINE_ACTIVE_DARK_ROOM, MACHINE_ACTIVE_ANIMAL_FARM };
    }

    private static TextureAtlasSprite register(String sprite)
    {
        return textureMap.registerSprite(new ResourceLocation(sprite));
    }

    private static final String BLOCKS = "virtualmachines:blocks/";

    private static final String MACHINE_ = BLOCKS + "machine/machine_";
    private static final String MACHINE_FACE_ = MACHINE_ + "face_";
    private static final String MACHINE_ACTIVE_ = MACHINE_ + "active_";
}
