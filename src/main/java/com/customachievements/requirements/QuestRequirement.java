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

import com.customachievements.events.QuestStateChanged;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.eventbus.Subscribe;

import static com.customachievements.AchievementState.*;

@Getter
@Setter
public class QuestRequirement extends Requirement
{
	private Quest quest;

	public QuestRequirement(Quest quest)
	{
		super(RequirementType.QUEST);
		this.quest = quest;
	}

	public QuestRequirement(QuestRequirement other)
	{
		super(other);
		this.quest = other.quest;
	}

	@Subscribe
	public void onQuestStateChanged(final QuestStateChanged questStateChanged)
	{
		if (questStateChanged.getQuest() == quest)
		{
			updateState(questStateChanged.getState());
			refresh();
		}
	}

	@Override
	public void forceUpdate(Client client)
	{
		updateState(quest.getState(client));
	}

	@Override
	public Requirement deepCopy()
	{
		return new QuestRequirement(this);
	}

	@Override
	public String toString()
	{
		return String.format("Complete %s", quest.getName());
	}

	private void updateState(QuestState state)
	{
		switch (state)
		{
			case FINISHED:
				setProgress(COMPLETE);
				break;
			case IN_PROGRESS:
				setProgress(IN_PROGRESS);
				break;
			default:
				setProgress(INCOMPLETE);
		}
	}
}
