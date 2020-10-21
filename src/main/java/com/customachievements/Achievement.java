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

import lombok.Getter;
import lombok.Setter;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;

import java.awt.Color;

import static com.customachievements.AchievementState.COMPLETE;

@Getter
@Setter
public class Achievement extends AchievementElement
{
	private String name;

	public Achievement(String name)
	{
		super();
		this.name = name;
	}

	public Achievement(Achievement other)
	{
		super(other);
		this.name = other.name;
	}

	@Override
	public void refresh()
	{
		AchievementState childrenState = getChildrenState();

		if (isForceComplete())
		{
			if (childrenState == COMPLETE)
			{
				setForceComplete(false);
			}

			setState(COMPLETE);
		}
		else
		{
			setState(childrenState);
		}
	}

	@Override
	public AchievementElement deepCopy()
	{
		return new Achievement(this);
	}

	@Override
	public String completionChatMessage(CustomAchievementsConfig config)
	{
		final Color notificationsColor = config.notificationsColor();

		return new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append(notificationsColor, "Congratulations! You have completed ")
				.append(notificationsColor, name)
				.append(notificationsColor, ". Your Achievements have been updated.")
				.build();
	}

	@Override
	public String toString()
	{
		return name;
	}
}
