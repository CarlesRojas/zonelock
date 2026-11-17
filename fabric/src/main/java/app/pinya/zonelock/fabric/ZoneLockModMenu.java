package app.pinya.zonelock.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import app.pinya.zonelock.client.screen.ZoneLockConfigScreen;

/** Mod Menu (optional): the mod's settings screen. Only loaded when Mod Menu is installed. */
public class ZoneLockModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ZoneLockConfigScreen::new;
    }
}
