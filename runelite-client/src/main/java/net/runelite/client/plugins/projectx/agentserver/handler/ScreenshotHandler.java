package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ScreenshotHandler extends AgentHandler {

	private final Client client;
	private final DrawManager drawManager;

	public ScreenshotHandler(Gson gson, Client client, DrawManager drawManager) {
		super(gson);
		this.client = client;
		this.drawManager = drawManager;
	}

	@Override
	public String getPath() {
		return "/screenshot";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		try {
			requireGet(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, String> params = parseQuery(exchange.getRequestURI());
		boolean save = "true".equalsIgnoreCase(params.getOrDefault("save", "false"));

		BufferedImage screenshot = captureFrame();
		if (screenshot == null) {
			sendJson(exchange, 500, errorResponse("Failed to capture screenshot - no frame available"));
			return;
		}

		if (save) {
			String dir = params.getOrDefault("dir", System.getProperty("user.home") + "/.runelite/test-results/screenshots");
			String label = params.getOrDefault("label", "screenshot");
			File outDir = new File(dir);
			outDir.mkdirs();
			String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
			File outFile = new File(outDir, label + "-" + timestamp + ".png");
			ImageIO.write(screenshot, "PNG", outFile);

			Map<String, Object> result = new LinkedHashMap<>();
			result.put("success", true);
			result.put("path", outFile.getAbsolutePath());
			result.put("width", screenshot.getWidth());
			result.put("height", screenshot.getHeight());
			sendJson(exchange, 200, result);
			return;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(screenshot, "PNG", baos);
		byte[] bytes = baos.toByteArray();
		exchange.getResponseHeaders().set("Content-Type", "image/png");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	private BufferedImage captureFrame() {
		final BufferedImage[] result = new BufferedImage[1];
		CountDownLatch latch = new CountDownLatch(1);

		drawManager.requestNextFrameListener(img -> {
			try {
				result[0] = ImageUtil.bufferedImageFromImage(img);
			} catch (Exception e) {
				log.error("Error converting frame image", e);
			} finally {
				latch.countDown();
			}
		});

		try {
			if (!latch.await(5, TimeUnit.SECONDS)) {
				log.error("Timed out waiting for frame capture");
				return null;
			}
		} catch (InterruptedException e) {
			log.error("Frame capture interrupted", e);
			Thread.currentThread().interrupt();
			return null;
		}

		return result[0];
	}
}
