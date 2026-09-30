/*
 * Copyright (c) 2020, Zoinkwiz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.projectx.questhelper.steps.choice;

import net.runelite.client.plugins.projectx.questhelper.QuestHelperConfig;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.util.text.Rs2TextSanitizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WidgetChoiceStep
{
	protected final QuestHelperConfig config;

	@Getter
	private final String choice;

	@Setter
	String expectedTextInWidget;

	private Pattern pattern;

	@Getter
	protected List<String> excludedStrings;
	protected int excludedGroupId;
	protected int excludedChildId;

	private final int choiceById;

	@Getter
	protected final int groupId;
	protected final int childId;

	protected final int varbitId;
	protected int varbitValue = -1;
	protected final Map<Integer, String> varbitValueToAnswer;

	protected boolean shouldNumber = false;

	/** The "[2] " that {@link #highlightText} writes in front of an option it has marked. */
	private static final Pattern HIGHLIGHT_NUMBER = Pattern.compile("^\\[\\d+]\\s*");

	@Setter
	@Getter
	private int groupIdForChecking;

	public WidgetChoiceStep(QuestHelperConfig config, String choice, int groupId, int childId)
	{
		this.config = config;
		this.choice = choice;
		this.choiceById = -1;
		this.groupId = groupId;
		this.groupIdForChecking = groupId;
		this.childId = childId;
		this.pattern = null;
		this.varbitId = -1;
		this.varbitValueToAnswer = null;
	}

	public WidgetChoiceStep(QuestHelperConfig config, int groupId, int childId)
	{
		this.config = config;
		this.choice = null;
		this.choiceById = -1;
		this.groupId = groupId;
		this.groupIdForChecking = groupId;
		this.childId = childId;
		this.pattern = null;
		this.varbitId = -1;
		this.varbitValueToAnswer = null;
	}

	public WidgetChoiceStep(QuestHelperConfig config, Pattern pattern, int groupId, int childId)
	{
		this.config = config;
		this.choice = null;
		this.choiceById = -1;
		this.groupId = groupId;
		this.groupIdForChecking = groupId;
		this.childId = childId;
		this.pattern = pattern;
		this.varbitId = -1;
		this.varbitValueToAnswer = null;
	}

	public WidgetChoiceStep(QuestHelperConfig config, int choiceId, int groupId, int childId)
	{
		this.config = config;
		this.choice = null;
		this.choiceById = choiceId;
		this.groupId = groupId;
		this.groupIdForChecking = groupId;
		this.childId = childId;
		this.varbitId = -1;
		this.varbitValueToAnswer = null;
	}

	public WidgetChoiceStep(QuestHelperConfig config, int choiceId, String choice, int groupId, int childId)
	{
		this.config = config;
		this.choice = choice;
		this.choiceById = choiceId;
		this.groupId = groupId;
		this.groupIdForChecking = groupId;
		this.childId = childId;
		this.varbitId = -1;
		this.varbitValueToAnswer = null;
	}

	public WidgetChoiceStep(QuestHelperConfig config, int choiceId, Pattern pattern, int groupId, int childId)
	{
		this.config = config;
		this.choice = null;
		this.choiceById = choiceId;
		this.groupId = groupId;
		this.groupIdForChecking = groupId;
		this.childId = childId;
		this.pattern = pattern;
		this.varbitId = -1;
		this.varbitValueToAnswer = null;
	}

	public WidgetChoiceStep(QuestHelperConfig config, int groupId, int childId, int varbitId, Map<Integer, String> varbitValueToAnswer)
	{
		this.config = config;
		this.choice = null;
		this.choiceById = -1;
		this.groupId = groupId;
		this.groupIdForChecking = groupId;
		this.childId = childId;
		this.pattern = null;
		this.varbitId = varbitId;
		this.varbitValueToAnswer = varbitValueToAnswer;
	}

	public void addExclusion(int excludedGroupId, int excludedChildId, String excludedString)
	{
		this.excludedStrings = Collections.singletonList(excludedString);
		this.excludedGroupId = excludedGroupId;
		this.excludedChildId = excludedChildId;
	}

	public void addExclusions(int excludedGroupId, int excludedChildId, String... excludedStrings)
	{
		this.excludedStrings = Arrays.asList(excludedStrings);
		this.excludedGroupId = excludedGroupId;
		this.excludedChildId = excludedChildId;
	}

	public void highlightChoice(Client client)
	{
		Widget exclusionDialogChoice = client.getWidget(excludedGroupId, excludedChildId);
		if (exclusionDialogChoice != null)
		{
			Widget[] exclusionChoices = exclusionDialogChoice.getChildren();
			if (exclusionChoices != null)
			{
				for (Widget currentExclusionChoice : exclusionChoices)
				{
					for (String excludedString : excludedStrings)
					{
						if (currentExclusionChoice.getText().contains(excludedString))
						{
							return;
						}
					}
				}
			}
		}
		Widget dialogChoice = client.getWidget(groupId, childId);

		if (dialogChoice == null)
		{
			return;
		}
		if(varbitId != -1){
			varbitValue = client.getVarbitValue(varbitId);
		}

		Widget[] choices = dialogChoice.getChildren();
		checkWidgets(choices);
		Widget[] nestedChildren = dialogChoice.getNestedChildren();
		checkWidgets(nestedChildren);
	}

	/**
	 * Whether a dialogue option's on-screen text is the one this choice is looking for.
	 *
	 * <p>The single place that answers that question, so the option highlighted for the player and
	 * the option a script presses cannot disagree. Comparison goes through
	 * {@link Rs2TextSanitizer#sanitizeForParsing} rather than {@code String.equals}: the widget
	 * text arrives carrying colour tags, HTML entities and typographic apostrophes that the quest
	 * was not written with. It also tolerates the {@code [n] } this class prefixes onto an option
	 * it has highlighted, so an option stays matchable after being marked once.
	 *
	 * <p>False for the index- and varbit-based choices, which name an option by position rather
	 * than by wording and cannot be resolved from its text.
	 */
	public boolean matches(String widgetText)
	{
		if (widgetText == null)
		{
			return false;
		}
		String text = stripHighlightNumber(widgetText);
		if (pattern != null)
		{
			// Patterns are authored against the game's own wording, markup and all.
			return pattern.matcher(text).find();
		}
		return choice != null && sameText(text, choice);
	}

	private static boolean sameText(String widgetText, String expected)
	{
		return widgetText != null && expected != null
			&& Rs2TextSanitizer.sanitizeForParsing(widgetText)
			.equals(Rs2TextSanitizer.sanitizeForParsing(expected));
	}

	private static String stripHighlightNumber(String text)
	{
		return text == null ? null : HIGHLIGHT_NUMBER.matcher(text).replaceFirst("");
	}

	protected void checkWidgets(Widget[] choices)
	{
		if (choices != null && choices.length > 0)
		{
			// Bounds-checked: choiceById indexes the option list the quest was written against,
			// and a dialogue offering fewer options than that read past the end of the array.
			if (choiceById != -1 && choiceById < choices.length && choices[choiceById] != null)
			{
				if (matches(choices[choiceById].getText()) || (choice == null && pattern == null))
				{
					highlightText(choices[choiceById], choiceById);
				}
			}
			else if (varbitId != -1 && varbitValue != -1 && varbitValueToAnswer != null)
			{
				String answer = varbitValueToAnswer.get(varbitValue);
				for (int i = 0; i < choices.length; i++)
				{
					if (sameText(stripHighlightNumber(choices[i].getText()), answer))
					{
						highlightText(choices[i], i);
						return;
					}
				}
			}
			else
			{
				for (int i = 0; i < choices.length; i++)
				{
					if (matches(choices[i].getText()))
					{
						highlightText(choices[i], i);
						return;
					}
				}
			}
		}
	}

	protected void highlightText(Widget text, int option)
	{
		if (!config.showTextHighlight())
		{
			return;
		}

		if (shouldNumber && !HIGHLIGHT_NUMBER.matcher(text.getText()).find())
		{
			text.setText("[" + option + "] " + text.getText());
		}

		text.setTextColor(config.textHighlightColor().getRGB());
		text.setOnMouseLeaveListener((JavaScriptCallback) ev -> text.setTextColor(config.textHighlightColor().getRGB()));
	}
}
