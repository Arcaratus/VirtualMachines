package com.arcaratus.virtualmachines.proxy;

import com.arcaratus.virtualmachines.init.VMTextures;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EventHandlerClient
{
    public static final EventHandlerClient INSTANCE = new EventHandlerClient();

    @SubscribeEvent
    public void handleTextureStitchPreEvent(TextureStitchEvent.Pre event)
    {
        VMTextures.registerTextures(event.getMap());
    }
}
