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

import java.util.Set;

import com.customachievements.AchievementState;
import com.customachievements.CustomAchievementsConfig;
import com.customachievements.AchievementElement;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;

import static com.customachievements.AchievementState.*;

@Getter
@Setter
public abstract class Requirement extends AchievementElement
{
	protected static final Set<Character> VOWELS = ImmutableSet.of('a', 'e', 'i', 'o', 'u');
	protected static final String NAME_UNKNOWN = "???";

	private RequirementType type;
	private AchievementState progress;

	public Requirement(RequirementType type)
	{
		super();
		this.type = type;
		this.progress = INCOMPLETE;
	}

	public Requirement(Requirement other)
	{
		super(other);
		this.type = other.type;
		this.progress = other.progress;
	}

	public abstract void forceUpdate(Client client);
	public abstract AchievementElement deepCopy();
	public abstract String toString();

	@Override
	public void reset()
	{
		super.reset();
		progress = INCOMPLETE;
	}

	@Override
	public void refresh()
	{
		AchievementState childrenState = getChildrenState();

		if (isForceComplete())
		{
			setState(COMPLETE);

			if (progress == COMPLETE && childrenState == COMPLETE)
			{
				setForceComplete(false);
			}
		}
		else if (!getChildren().isEmpty() &&
					((progress == COMPLETE && childrenState != COMPLETE) ||
						(progress == INCOMPLETE && childrenState != INCOMPLETE)))
		{
			setState(IN_PROGRESS);
		}
		else
		{
			setState(progress);
		}
	}

	@Override
	public String completionChatMessage(CustomAchievementsConfig config)
	{
		return new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append("Achievement Requirement complete: ")
				.append(config.notificationsColor(), toString())
				.build();
	}
}
