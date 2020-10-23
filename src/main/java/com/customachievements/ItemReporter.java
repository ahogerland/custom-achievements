/*
 * Copyright (c) 2020, Alec Hogerland <https://github.com/ahogerland>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.customachievements;

import com.customachievements.events.ItemsValidated;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class ItemReporter
{
	private final Client client;
	private final EventBus eventBus;
	private final ItemManager itemManager;

	private final CustomAchievementsConfig config;

	@Inject
	public ItemReporter(
			final Client client,
			final EventBus eventBus,
			final ItemManager itemManager,
			final CustomAchievementsConfig config)
	{
		this.client = client;
		this.eventBus = eventBus;
		this.itemManager = itemManager;
		this.config = config;
	}

	@Subscribe
	public void onLootReceived(final LootReceived lootReceived)
	{
		if (config.ironmanModeEnabled() && lootReceived.getType() == LootRecordType.PLAYER)
		{
			return;
		}

		final ItemSource source = lootReceived.getType() == LootRecordType.PLAYER ?
				ItemSource.PLAYER_LOOT :
				ItemSource.LOOT;

		final Collection<NamedItem> items = lootReceived.getItems().stream()
				.map(itemStack -> createNamedItem(itemStack.getId(), itemStack.getQuantity()))
				.collect(Collectors.toList());

		eventBus.post(new ItemsValidated(source, items));
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged itemContainerChanged)
	{
		if (itemContainerChanged.getContainerId() == InventoryID.INVENTORY.getId())
		{
			final Collection<NamedItem> items = Arrays.stream(itemContainerChanged.getItemContainer().getItems())
					.map(item -> createNamedItem(item.getId(), item.getQuantity()))
					.collect(Collectors.toList());

			eventBus.post(new ItemsValidated(ItemSource.INVENTORY, items));
		}
	}

	public void refresh()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);

			if (container != null)
			{
				eventBus.post(new ItemContainerChanged(InventoryID.INVENTORY.getId(), container));
			}
		}
	}

	private NamedItem createNamedItem(int id, int quantity)
	{
		return new NamedItem(id, itemManager.getItemComposition(id).getName(), quantity);
	}
}
