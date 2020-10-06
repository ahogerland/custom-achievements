package com.customachievements.requirements;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;

@Getter
@Setter
public class AbstractRequirement extends Requirement
{
	private String name;

	public AbstractRequirement(final String name)
	{
		this(name, false);
	}

	public AbstractRequirement(final String name, final boolean complete)
	{
		super(RequirementType.ABSTRACT, complete);
		this.name = name;
	}

	@Override
	public Requirement deepCopy()
	{
		return new AbstractRequirement(name, complete);
	}

	@Override
	public void forceUpdate(Client client) {}

	@Override
	public String toString()
	{
		return name;
	}
}
