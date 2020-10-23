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
package com.customachievements.requirements;

import com.customachievements.NamedItem;
import com.customachievements.ItemSource;
import com.customachievements.events.ItemsValidated;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.eventbus.Subscribe;

import java.util.Collection;

import static com.customachievements.AchievementState.*;

@Getter
@Setter
public class ItemRequirement extends Requirement
{
	private String name;
	private int quantity;
	private int count;
	private ItemTrackingOption trackingOption;

	public ItemRequirement(String name, int quantity)
	{
		super(RequirementType.ITEM);
		this.name = name;
		this.quantity = quantity;
		this.count = 0;
		this.trackingOption = ItemTrackingOption.INVENTORY;
	}

	public ItemRequirement(ItemRequirement other)
	{
		super(other);
		this.name = other.name;
		this.quantity = other.quantity;
		this.count = other.count;
		this.trackingOption = other.trackingOption;
	}

	@Subscribe
	public void onItemsValidated(final ItemsValidated itemsValidated)
	{
		if (getProgress() != COMPLETE)
		{
			if (trackingOption == ItemTrackingOption.DROPPED &&
				itemsValidated.getSource() != ItemSource.INVENTORY)
			{
				countItems(itemsValidated.getItems());
			}
			else if (trackingOption == ItemTrackingOption.INVENTORY &&
					itemsValidated.getSource() == ItemSource.INVENTORY)
			{
				count = 0;
				countItems(itemsValidated.getItems());
			}
		}
	}

	@Override
	public void forceUpdate(Client client)
	{
		updateState();
	}

	@Override
	public Requirement deepCopy()
	{
		return new ItemRequirement(this);
	}

	@Override
	public void reset()
	{
		super.reset();
		count = 0;
	}

	@Override
	public String toString()
	{
		if (name.isEmpty())
		{
			return NAME_UNKNOWN;
		}
		else
		{
			final int done = getState() == COMPLETE ? quantity : Math.min(count, quantity);
			return String.format("%s (%d/%d)", name, done, quantity);
		}
	}

	private void countItems(Collection<NamedItem> items)
	{
		for (NamedItem item : items)
		{
			if (item.getName().equalsIgnoreCase(name))
			{
				count += item.getQuantity();
			}
		}

		updateState();
		broadcastState();
	}

	private void updateState()
	{
		if (count >= quantity)
		{
			setProgress(COMPLETE);
		}
		else if (count > 0)
		{
			setProgress(IN_PROGRESS);
		}
		else
		{
			setProgress(INCOMPLETE);
		}
	}
}
