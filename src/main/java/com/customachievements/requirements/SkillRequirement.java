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

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;

import static com.customachievements.AchievementState.COMPLETE;

@Getter
@Setter
public class SkillRequirement extends Requirement
{
	private Skill skill;
	private SkillTargetType targetType;
	private int target;

	public SkillRequirement(Skill skill, SkillTargetType targetType, int target)
	{
		super(RequirementType.SKILL);
		this.skill = skill;
		this.targetType = targetType;
		this.target = target;
	}

	public SkillRequirement(SkillRequirement other)
	{
		super(other);
		this.skill = other.skill;
		this.targetType = other.targetType;
		this.target = other.target;
	}

	@Subscribe
	public void onStatChanged(final StatChanged statChanged)
	{
		if (getProgress() != COMPLETE && skill.equals(statChanged.getSkill()))
		{
			if ((targetType == SkillTargetType.LEVEL && statChanged.getLevel() >= target) ||
				(targetType == SkillTargetType.XP && statChanged.getXp() >= target))
			{
				setProgress(COMPLETE);
				refresh();
			}
		}
	}

	@Override
	public void forceUpdate(Client client)
	{
		if ((targetType == SkillTargetType.LEVEL && client.getRealSkillLevel(skill) >= target) ||
			(targetType == SkillTargetType.XP && client.getSkillExperience(skill) >= target))
		{
			setProgress(COMPLETE);
		}
	}

	@Override
	public Requirement deepCopy()
	{
		return new SkillRequirement(this);
	}

	@Override
	public String toString()
	{
		if (targetType == SkillTargetType.LEVEL)
		{
			return String.format("%d %s", target, skill.getName());
		}
		else
		{
			return String.format("%d %s XP", target, skill.getName());
		}
	}
}
