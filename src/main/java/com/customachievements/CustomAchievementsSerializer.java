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

import com.customachievements.requirements.AbstractRequirement;
import com.customachievements.requirements.ItemRequirement;
import com.customachievements.requirements.QuestRequirement;
import com.customachievements.requirements.Requirement;
import com.customachievements.requirements.RequirementType;
import com.customachievements.requirements.SkillRequirement;
import com.customachievements.requirements.SlayRequirement;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.List;

@Slf4j
public class CustomAchievementsSerializer
{
	private final GsonBuilder builder;
	private final Type listType;

	public CustomAchievementsSerializer()
	{
		this.builder = new GsonBuilder();
		this.listType = new TypeToken<List<AchievementElement>>() {}.getType();

		builder.registerTypeAdapter(AchievementElement.class, new AchievementElementTypeAdapter());
	}

	public CustomAchievementsSerializer setPrettyPrinting()
	{
		builder.setPrettyPrinting();
		return this;
	}

	public String toJson(List<AchievementElement> entries)
	{
		return builder.create().toJson(entries);
	}

	public List<AchievementElement> fromJson(String json)
	{
		try
		{
			return builder.create().fromJson(json, listType);
		}
		catch (JsonParseException e)
		{
			log.error(e.getMessage());
			return null;
		}
	}

	private static class AchievementElementTypeAdapter implements JsonSerializer<AchievementElement>, JsonDeserializer<AchievementElement>
	{
		@Override
		public JsonElement serialize(AchievementElement src, Type typeOfSrc, JsonSerializationContext context)
		{
			return context.serialize(src);
		}

		@Override
		public AchievementElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
		{
			final JsonObject jsonObject = json.getAsJsonObject();

			if (jsonObject.get("type") != null)
			{
				return deserializeRequirement(json, context);
			}
			else
			{
				return context.deserialize(json, Achievement.class);
			}
		}

		private Requirement deserializeRequirement(JsonElement json, JsonDeserializationContext context) throws JsonParseException
		{
			RequirementType type = context.deserialize(json.getAsJsonObject().get("type"), RequirementType.class);

			switch (type)
			{
				case SKILL:
					return context.deserialize(json, SkillRequirement.class);
				case ITEM:
					return context.deserialize(json, ItemRequirement.class);
				case SLAY:
					return context.deserialize(json, SlayRequirement.class);
				case QUEST:
					return context.deserialize(json, QuestRequirement.class);
				case ABSTRACT:
				default:
					return context.deserialize(json, AbstractRequirement.class);
			}
		}
	}
}
