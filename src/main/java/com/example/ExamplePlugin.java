package com.example;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.*;


@Slf4j
@PluginDescriptor(
	name = "Hans finder"
)
public class ExamplePlugin extends Plugin
{
	@Inject
	private Client client;


	@Inject
	private OverlayManager overlayManager;


	private final Overlay hansOverlay = new Overlay() {

		{
			setPosition(OverlayPosition.DYNAMIC);
			setLayer(OverlayLayer.ABOVE_SCENE);
		}

		@Override
		public Dimension render(Graphics2D graphics) {
			for (NPC npc : client.getNpcs()) {
				if ("Hans".equals(npc.getName())) {
					graphics.setColor(Color.CYAN);
					graphics.draw(npc.getConvexHull());

					var textLocation = npc.getCanvasTextLocation(
							graphics,
							npc.getName(),
							npc.getLogicalHeight()
					);

					if (textLocation != null) {
						graphics.drawString(
								npc.getName(),
								textLocation.getX(),
								textLocation.getY()
						);
					}
				}
			}
			return null;
		}
	};

	private final Overlay hansMinimapOverlay = new Overlay()
	{
		{
			setPosition(OverlayPosition.DYNAMIC);
			setLayer(OverlayLayer.ABOVE_WIDGETS);
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			for (NPC npc : client.getNpcs())
			{
				if ("Hans".equals(npc.getName()))
				{
					LocalPoint localPoint = npc.getLocalLocation();
					if (localPoint == null)
					{
						continue;
					}

					var minimapPoint = Perspective.localToMinimap(client, localPoint);

					if (minimapPoint != null)
					{

						OverlayUtil.renderMinimapLocation(graphics, minimapPoint, Color.CYAN);


						String name = npc.getName();
						int x = minimapPoint.getX();
						int y = minimapPoint.getY() - 5; // slightly above the dot


						graphics.setColor(Color.BLACK);
						graphics.drawString(name, x + 1, y + 1);

						graphics.setColor(Color.CYAN);
						graphics.drawString(name, x, y);
					}
				}
			}
			return null;
		}
	};


	@Override
	protected void startUp()
	{
		overlayManager.add(hansOverlay);
		overlayManager.add(hansMinimapOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(hansOverlay);
		overlayManager.remove(hansMinimapOverlay);
	}



}
