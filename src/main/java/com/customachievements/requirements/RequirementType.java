package com.customachievements.requirements;

import lombok.Getter;

public enum RequirementType
{
	UNKNOWN("Select a requirement type...", ""),
	ABSTRACT("Abstract", "An abstract requirement must be marked as completed manually."),
	SKILL("Skill", "Require a skill level or XP amount."),
	ITEM("Item", "Collect an item."),
	QUEST("Quest", "Require quest completion."),
	CHUNK("Chunk Unlock", "Require a chunk (area restricted game-modes).");

	@Getter
	private final String name;

	@Getter
	private final String description;

	RequirementType(String name, String description)
	{
		this.name = name;
		this.description = description;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
