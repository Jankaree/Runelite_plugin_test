package com.example;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.runelite.api.NPC;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import okhttp3.*;

import static net.runelite.http.api.RuneLiteAPI.GSON;


@Slf4j
@PluginDescriptor(
	name = "Hans finder"
)
public class HansFinderPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private HansFinderConfig hansFinderConfig;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private DrawManager drawManager;


	@Provides
	HansFinderConfig hansFinderConfig(ConfigManager configManager){
		return configManager.getConfig(HansFinderConfig.class);
	}

	private final Overlay hansOverlay = new Overlay() {

		{
			setPosition(OverlayPosition.DYNAMIC);
			setLayer(OverlayLayer.ABOVE_SCENE);
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			for (NPC npc : client.getNpcs())
			{
				if (!"Hans".equals(npc.getName()))
					continue;

				var poly = npc.getCanvasTilePoly();
				if (poly != null)
				{
					graphics.setColor(Color.CYAN);
					graphics.draw(poly);
				}

				var hull = npc.getConvexHull();
				if (hull != null)
				{
					graphics.draw(hull);
				}

				var textLocation = npc.getCanvasTextLocation(graphics, npc.getName(), npc.getLogicalHeight());
				if (textLocation != null)
				{
					graphics.drawString(npc.getName(), textLocation.getX(), textLocation.getY());
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
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getType() != MenuAction.NPC_FIRST_OPTION.getId())
		{
			return;
		}

		String target = Text.removeTags(event.getTarget());
		if (!"Hans".equals(target))
		{
			return;
		}

		client.createMenuEntry(-1)
				.setOption("Broadcast")
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.setIdentifier(event.getIdentifier())
				.setParam0(event.getActionParam0())
				.setParam1(event.getActionParam1());
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() != MenuAction.RUNELITE)
			return;

		if (!"Broadcast".equals(event.getMenuOption()))
			return;

		String target = Text.removeTags(event.getMenuTarget());
		if (!"Hans".equals(target))
			return;

		log.info("Broadcast clicked for {}", target);
		sendHansWebhook();
	}


	private void sendHansWebhook()
	{
		String webhook = hansFinderConfig.webhook();

		if (webhook == null || webhook.isBlank())
		{
			log.warn("Webhook URL is empty");
			return;
		}

		HttpUrl url = HttpUrl.parse(webhook);
		if (url == null)
		{
			log.warn("Invalid webhook URL: {}", webhook);
			return;
		}

		log.info("Sending Hans webhook to {}", url);

		String content = "Hans spotted in world " + client.getWorld();

		MultipartBody.Builder builder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart(
						"payload_json",
						GSON.toJson(new DiscordWebhookBody())
				);

		drawManager.requestNextFrameListener(image ->
		{
			try
			{
				byte[] imageBytes = convertImageToByteArray((BufferedImage) image);

				builder.addFormDataPart(
						"file",
						"hans.png",
						RequestBody.create(MediaType.parse("image/png"), imageBytes)
				);

				Request request = new Request.Builder()
						.url(url)
						.post(builder.build())
						.build();

				sendRequest(request);
			}
			catch (Exception e)
			{
				log.error("Failed to send Hans webhook", e);
			}
		});
	}


	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to send webhook", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}


	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(hansOverlay);
		overlayManager.add(hansMinimapOverlay);

		log.info("HansFinderPlugin started!");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(hansOverlay);
		overlayManager.remove(hansMinimapOverlay);
	}



}
