package com.customachievements.requirements;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;

@Getter
@Setter
public class SkillRequirement extends Requirement
{
	private Skill skill;
	private SkillTargetType targetType;
	private int target;

	public SkillRequirement(final Skill skill,
							final SkillTargetType targetType,
							final int target)
	{
		this(skill, targetType, target, false);
	}

	public SkillRequirement(final Skill skill,
							final SkillTargetType targetType,
							final int target,
							final boolean complete)
	{
		super(RequirementType.SKILL, complete);
		this.skill = skill;
		this.targetType = targetType;
		this.target = target;
	}

	@Override
	public Requirement deepCopy()
	{
		return new SkillRequirement(skill, targetType, target, complete);
	}

	@Override
	public void forceUpdate(Client client)
	{
		if (!complete)
		{
			if ((targetType == SkillTargetType.LEVEL && client.getRealSkillLevel(skill) >= target) ||
				(targetType == SkillTargetType.XP && client.getSkillExperience(skill) >= target))
			{
				complete = true;
				notifyListener();
			}
		}
	}

	@Subscribe
	public void onStatChanged(final StatChanged statChanged)
	{
		if (!complete && skill.equals(statChanged.getSkill()))
		{
			if ((targetType == SkillTargetType.LEVEL && statChanged.getLevel() >= target) ||
				(targetType == SkillTargetType.XP && statChanged.getXp() >= target))
			{
				complete = true;
				notifyListener();
			}
		}
	}

	@Override
	public String toString()
	{
		switch (targetType)
		{
			case LEVEL:
				return String.format("%d %s", target, skill.getName());
			case XP:
				return String.format("%d %s XP", target, skill.getName());
			default:
				return "Unknown Skill Requirement";
		}
	}
}
