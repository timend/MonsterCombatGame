package de.enderling.monstercombat.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import de.enderling.monstercombat.AnotherGame;
import de.enderling.monstercombat.MonsterCombatGame;

import java.awt.*;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1024;
		config.height = 768;
		config.fullscreen = false;
		new LwjglApplication(new AnotherGame(), config);
	}
}
