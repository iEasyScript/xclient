package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

public class BankHandler extends AgentHandler {

	public BankHandler(Gson gson) {
		super(gson);
	}

	@Override
	public String getPath() {
		return "/bank";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		String sub = getSubPath(exchange, "/bank");

		switch (sub) {
			case "":
			case "/":
				handleStatus(exchange);
				break;
			case "/open":
				handleOpen(exchange);
				break;
			case "/close":
				handleClose(exchange);
				break;
			case "/deposit":
				handleDeposit(exchange);
				break;
			case "/withdraw":
				handleWithdraw(exchange);
				break;
			default:
				sendJson(exchange, 404, errorResponse("Unknown path: /bank" + sub));
		}
	}

	private void handleStatus(HttpExchange exchange) throws IOException {
		try {
			requireGet(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		boolean open = Rs2Bank.isOpen();
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("open", open);

		if (open) {
			List<Rs2ItemModel> bankItems = Rs2Bank.bankItems();
			if (bankItems != null) {
				List<Map<String, Object>> items = bankItems.stream()
						.map(this::serializeItem)
						.collect(Collectors.toList());
				response.put("count", items.size());
				response.put("items", items);
			}
		}

		sendJson(exchange, 200, response);
	}

	private void handleOpen(HttpExchange exchange) throws IOException {
		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		boolean result = Rs2Bank.openBank();
		if (result) {
			sleepUntil(Rs2Bank::isOpen, 5000);
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("opened", Rs2Bank.isOpen());
		sendJson(exchange, 200, response);
	}

	private void handleClose(HttpExchange exchange) throws IOException {
		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		boolean result = Rs2Bank.closeBank();
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("closed", result);
		sendJson(exchange, 200, response);
	}

	private void handleDeposit(HttpExchange exchange) throws IOException {
		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> body;
		try {
			body = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		Boolean all = (Boolean) body.get("all");
		String name = (String) body.get("name");

		boolean success;
		if (Boolean.TRUE.equals(all) && (name == null || name.isEmpty())) {
			success = Rs2Bank.depositAll();
		} else if (name != null && !name.isEmpty()) {
			success = Rs2Bank.depositAll(name);
		} else {
			sendJson(exchange, 400, errorResponse("Provide name or {all: true}"));
			return;
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("success", success);
		if (name != null) response.put("name", name);
		sendJson(exchange, 200, response);
	}

	private void handleWithdraw(HttpExchange exchange) throws IOException {
		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> body;
		try {
			body = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		String name = (String) body.get("name");
		Number quantityNum = (Number) body.get("quantity");
		Number idNum = (Number) body.get("id");

		if (name == null && idNum == null) {
			sendJson(exchange, 400, errorResponse("Provide either name or id"));
			return;
		}

		boolean success;
		int quantity = quantityNum != null ? quantityNum.intValue() : 1;

		if (name != null && !name.isEmpty()) {
			if (quantity <= 0) {
				success = Rs2Bank.withdrawAll(name);
			} else {
				success = Rs2Bank.withdrawX(name, quantity);
			}
		} else {
			if (quantity <= 0) {
				success = Rs2Bank.withdrawAll(idNum.intValue());
			} else {
				success = Rs2Bank.withdrawX(idNum.intValue(), quantity);
			}
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("success", success);
		if (name != null) response.put("name", name);
		if (idNum != null) response.put("id", idNum.intValue());
		response.put("quantity", quantity);
		sendJson(exchange, 200, response);
	}

	private Map<String, Object> serializeItem(Rs2ItemModel item) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", item.getId());
		map.put("name", item.getName());
		map.put("quantity", item.getQuantity());
		return map;
	}
}
