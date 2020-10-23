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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.customachievements.requirements.AbstractRequirement;
import com.customachievements.requirements.ChunkRequirement;
import com.customachievements.requirements.ItemRequirement;
import com.customachievements.requirements.ItemTrackingOption;
import com.customachievements.requirements.QuestRequirement;
import com.customachievements.requirements.Requirement;
import com.customachievements.requirements.RequirementType;
import com.customachievements.requirements.SkillRequirement;
import com.customachievements.requirements.SkillTargetType;
import com.customachievements.requirements.SlayRequirement;
import lombok.NonNull;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import static com.customachievements.CustomAchievementsPanel.LIST_ENTRY_HEIGHT;
import static com.customachievements.CustomAchievementsPanel.BUTTON_WIDTH;
import static com.customachievements.CustomAchievementsPanel.BORDER_OFFSET;
import static com.customachievements.CustomAchievementsPanel.LIST_SEPARATOR_REGEX;

public class EditAchievementPanel extends FixedWidthPanel
{
	public static final String ACTION_UPDATE = "ACTION_UPDATE";
	public static final String ACTION_CANCEL = "ACTION_CANCEL";

	private static final ImageIcon ADD_ICON;
	private static final ImageIcon REMOVE_ICON;
	private static final ImageIcon REMOVE_ICON_FADED;

	private final List<ActionListener> listeners = new ArrayList<>();

	private final Skill[] orderedSkills;
	private final Comparator<Skill> skillComparator;

	private final CustomAchievementsPlugin plugin;

	private AchievementElement parent;
	private AchievementElement target;
	private int targetIndex;

	private RequirementType selectedRequirementType = RequirementType.NONE;

	static
	{
		final BufferedImage addImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "add_icon.png");
		final BufferedImage removeImage = ImageUtil.getResourceStreamFromClass(CustomAchievementsPlugin.class, "mini_remove_icon.png");

		ADD_ICON = new ImageIcon(addImage);

		REMOVE_ICON = new ImageIcon(removeImage);
		REMOVE_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(removeImage, 0.2f));
	}

	EditAchievementPanel(CustomAchievementsPlugin plugin)
	{
		this.plugin = plugin;

		parent = null;
		target = new Achievement("");
		targetIndex = 0;

		orderedSkills = Skill.values().clone();
		skillComparator = Comparator.comparing(Skill::getName);
		Arrays.sort(orderedSkills, skillComparator);

		setLayout(new GridBagLayout());

		refresh();
	}

	public void setTarget(int targetIndex, AchievementElement parent, AchievementElement target)
	{
		this.parent = parent;
		this.target = target.deepCopy();
		this.targetIndex = targetIndex;

		refresh();
	}

	public void updateTarget()
	{
		if (parent == null)
		{
			if (targetIndex < plugin.getElements().size())
			{
				plugin.set(targetIndex, target);
			}
			else
			{
				plugin.add(target);
			}
		}
		else
		{
			if (targetIndex < parent.getChildren().size())
			{
				plugin.set(targetIndex, parent, target);
			}
			else
			{
				plugin.add(parent, target);
			}
		}

		// Update Achievement status
		plugin.globalRefresh();
		plugin.updateConfig();

		notifyListeners(ACTION_UPDATE);
	}

	public void refresh()
	{
		removeAll();

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 0;

		add(createTargetPanel(target), gbc);
		gbc.gridy++;

		add(createKeywordPanel(target), gbc);
		gbc.gridy++;

		add(createSeparator(ColorScheme.DARK_GRAY_COLOR), gbc);
		gbc.gridy++;

		for (AchievementElement child : target.getChildren())
		{
			if (child instanceof Requirement)
			{
				add(createRequirementPanel((Requirement) child, parent != null), gbc);
				gbc.gridy++;
			}
		}

		if (!target.getChildren().isEmpty())
		{
			add(createSeparator(ColorScheme.DARK_GRAY_COLOR), gbc);
			gbc.gridy++;
		}

		add(createAddRequirementPanel(parent != null), gbc);
		gbc.gridy++;

		add(createConfirmationPanel(), gbc);
		gbc.gridy++;

		add(createSeparator(ColorScheme.DARKER_GRAY_COLOR), gbc);

		revalidate();
		repaint();
	}

	public void addActionListener(@NonNull final ActionListener listener)
	{
		listeners.add(listener);
	}

	private void notifyListeners(final String command)
	{
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);

		for (ActionListener listener : listeners)
		{
			listener.actionPerformed(event);
		}
	}

	private JLabel createSeparator(Color color)
	{
		final JLabel separator = new JLabel();

		separator.setBorder(BorderFactory.createMatteBorder(4, 0, 4, 0, color));
		return separator;
	}

	private JPanel createTargetPanel(final AchievementElement element)
	{
		if (element instanceof Achievement)
		{
			return createAchievementPanel((Achievement) element);
		}
		else if (element instanceof Requirement)
		{
			return createRequirementPanel((Requirement) element, false);
		}
		else
		{
			return new JPanel();
		}
	}

	private JPanel createKeywordPanel(final AchievementElement element)
	{
		final JPanel wrapper = new JPanel(new GridLayout(2, 1));
		wrapper.setBorder(BorderFactory.createEmptyBorder(0, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JLabel nameLabel = new JLabel("Keywords");
		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());

		final FlatTextField keywordInput = new FlatTextField();
		keywordInput.getTextField().setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		keywordInput.setText(String.join(", ", element.getKeywords()));
		keywordInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
		keywordInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		keywordInput.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e)
			{
				String[] keywords = keywordInput.getText().toLowerCase().split(LIST_SEPARATOR_REGEX);
				element.setKeywords(Arrays.asList(keywords));
			}
		});

		wrapper.add(nameLabel);
		wrapper.add(keywordInput);

		return wrapper;
	}

	private JPanel createAchievementPanel(final Achievement achievement)
	{
		final JPanel wrapper = new JPanel(new GridLayout(2, 1));
		wrapper.setBorder(BorderFactory.createEmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JLabel nameLabel = new JLabel("Achievement Name");
		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());

		final FlatTextField nameInput = new FlatTextField();
		nameInput.getTextField().setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		nameInput.setText(achievement.getName());
		nameInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nameInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		nameInput.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e)
			{
				achievement.setName(nameInput.getText());
			}
		});

		wrapper.add(nameLabel);
		wrapper.add(nameInput);

		return wrapper;
	}

	private JPanel createRequirementPanel(final Requirement requirement, boolean sub)
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JPanel titleWrapper = new JPanel(new BorderLayout());
		titleWrapper.setOpaque(false);

		final JLabel nameLabel = new JLabel(String.format("%s %s",
				requirement.getType(),
				sub ? "Sub-Requirement" : "Requirement"));
		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());

		JButton removeButton = new JButton(REMOVE_ICON_FADED);
		SwingUtil.removeButtonDecorations(removeButton);
		removeButton.setPreferredSize(new Dimension(BUTTON_WIDTH, LIST_ENTRY_HEIGHT));
		removeButton.setRolloverIcon(REMOVE_ICON);
		removeButton.setToolTipText("Remove");
		removeButton.addActionListener(e -> {
			target.getChildren().remove(requirement);
			refresh();
		});

		JPanel requirementPanel;

		switch (requirement.getType())
		{
			case SKILL:
				requirementPanel = createSkillRequirementPanel((SkillRequirement) requirement);
				break;
			case ITEM:
				requirementPanel = createItemRequirementPanel((ItemRequirement) requirement);
				break;
			case SLAY:
				requirementPanel = createSlayRequirementPanel((SlayRequirement) requirement);
				break;
			case QUEST:
				requirementPanel = createQuestRequirementPanel((QuestRequirement) requirement);
				break;
			case CHUNK:
				requirementPanel = createChunkRequirementPanel((ChunkRequirement) requirement);
				break;
			case ABSTRACT:
			default:
				requirementPanel = createAbstractRequirementPanel((AbstractRequirement) requirement);
		}

		titleWrapper.add(nameLabel, BorderLayout.WEST);
		wrapper.add(titleWrapper, BorderLayout.NORTH);
		wrapper.add(requirementPanel, BorderLayout.CENTER);

		if (requirement != target)
		{
			titleWrapper.add(removeButton, BorderLayout.EAST);
		}

		return wrapper;
	}

	private JPanel createSkillRequirementPanel(final SkillRequirement requirement)
	{
		final JPanel wrapper = new JPanel(new GridLayout(2, 1));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JPanel optionsPanel = new JPanel(new GridLayout(1, 2));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JComboBox<Skill> skillComboBox = new JComboBox<>(orderedSkills);
		skillComboBox.setForeground(Color.WHITE);
		skillComboBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		skillComboBox.setSelectedIndex(Arrays.binarySearch(orderedSkills, requirement.getSkill(), skillComparator));
		skillComboBox.setToolTipText("Skill");
		skillComboBox.addActionListener(e -> {
			requirement.setSkill((Skill) skillComboBox.getSelectedItem());
			requirement.reset();
			refresh();
		});

		final JComboBox<SkillTargetType> targetTypeComboBox = new JComboBox<>(SkillTargetType.values());
		targetTypeComboBox.setForeground(Color.WHITE);
		targetTypeComboBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		targetTypeComboBox.setSelectedIndex(requirement.getTargetType().ordinal());
		targetTypeComboBox.setToolTipText("Target Type");
		targetTypeComboBox.addActionListener(e -> {
			requirement.setTargetType((SkillTargetType) targetTypeComboBox.getSelectedItem());
			requirement.reset();
			refresh();
		});

		final JSpinner valueSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		valueSpinner.setBackground(ColorScheme.DARK_GRAY_COLOR);
		valueSpinner.setValue(requirement.getTarget());
		valueSpinner.setToolTipText("Target Value");
		valueSpinner.addChangeListener(e -> {
			requirement.setTarget((int) valueSpinner.getValue());
			requirement.reset();
			refresh();
		});

		optionsPanel.add(skillComboBox);
		optionsPanel.add(targetTypeComboBox);
		wrapper.add(optionsPanel);
		wrapper.add(valueSpinner);

		return wrapper;
	}

	private JPanel createItemRequirementPanel(final ItemRequirement requirement)
	{
		final JPanel wrapper = new JPanel(new GridLayout(3, 2));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JLabel nameLabel = new JLabel("Item");
		nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final FlatTextField nameInput = new FlatTextField();
		nameInput.setText(requirement.getName());
		nameInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nameInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		nameInput.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e)
			{
				requirement.setName(nameInput.getText());
				requirement.reset();
			}
		});

		final JLabel quantityLabel = new JLabel("Quantity");
		quantityLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		quantitySpinner.setBackground(ColorScheme.DARK_GRAY_COLOR);
		quantitySpinner.setValue(requirement.getQuantity());
		quantitySpinner.setToolTipText("Quantity");
		quantitySpinner.addChangeListener(e -> {
			requirement.setQuantity((int) quantitySpinner.getValue());
			requirement.setProgress(AchievementState.INCOMPLETE);
			refresh();
		});

		final JLabel trackingLabel = new JLabel("Tracking Option");
		quantityLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JComboBox<ItemTrackingOption> trackingComboBox = new JComboBox<>(ItemTrackingOption.values());
		trackingComboBox.setForeground(Color.WHITE);
		trackingComboBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		trackingComboBox.setSelectedIndex(requirement.getTrackingOption().ordinal());
		trackingComboBox.setRenderer(new ItemTrackingOptionComboBoxRenderer());
		trackingComboBox.addActionListener(e -> {
			requirement.setTrackingOption((ItemTrackingOption) trackingComboBox.getSelectedItem());
			requirement.reset();
			refresh();
		});

		wrapper.add(nameLabel);
		wrapper.add(nameInput);
		wrapper.add(quantityLabel);
		wrapper.add(quantitySpinner);
		wrapper.add(trackingLabel);
		wrapper.add(trackingComboBox);

		return wrapper;
	}

	private JPanel createSlayRequirementPanel(final SlayRequirement requirement)
	{
		final JPanel wrapper = new JPanel(new GridLayout(3, 2));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JLabel nameLabel = new JLabel("Target");
		nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final FlatTextField nameInput = new FlatTextField();
		nameInput.setText(requirement.getName());
		nameInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nameInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		nameInput.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e)
			{
				requirement.setName(nameInput.getText());
				requirement.reset();
			}
		});

		final JLabel quantityLabel = new JLabel("Quantity");
		quantityLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		quantitySpinner.setBackground(ColorScheme.DARK_GRAY_COLOR);
		quantitySpinner.setValue(requirement.getQuantity());
		quantitySpinner.setToolTipText("Quantity");
		quantitySpinner.addChangeListener(e -> {
			requirement.setQuantity((int) quantitySpinner.getValue());
			requirement.setProgress(AchievementState.INCOMPLETE);
			refresh();
		});

		final JLabel properNounLabel = new JLabel("Proper Noun");
		properNounLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JCheckBox properNounCheckBox = new JCheckBox();
		properNounCheckBox.setSelected(requirement.isProperNoun());
		properNounCheckBox.addActionListener(e -> requirement.setProperNoun(properNounCheckBox.isSelected()));

		wrapper.add(nameLabel);
		wrapper.add(nameInput);
		wrapper.add(quantityLabel);
		wrapper.add(quantitySpinner);
		wrapper.add(properNounLabel);
		wrapper.add(properNounCheckBox);

		return wrapper;
	}

	private JPanel createQuestRequirementPanel(final QuestRequirement requirement)
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JComboBox<Quest> questComboBox = new JComboBox<>(Quest.values());
		questComboBox.setForeground(Color.WHITE);
		questComboBox.setBackground(ColorScheme.DARK_GRAY_COLOR);
		questComboBox.setSelectedIndex(requirement.getQuest().ordinal());
		questComboBox.setRenderer(new QuestComboBoxRenderer());
		questComboBox.addActionListener(e -> {
			requirement.setQuest((Quest) questComboBox.getSelectedItem());
			requirement.reset();
			refresh();
		});

		wrapper.add(questComboBox, BorderLayout.CENTER);

		return wrapper;
	}

	private JPanel createChunkRequirementPanel(final ChunkRequirement requirement)
	{
		final JPanel wrapper = new JPanel(new GridLayout(2, 2));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JLabel regionLabel = new JLabel("Chunk ID");
		regionLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JSpinner regionSpinner = new JSpinner();
		regionSpinner.setEditor(new JSpinner.NumberEditor(regionSpinner, "#"));
		regionSpinner.setBackground(ColorScheme.DARK_GRAY_COLOR);
		regionSpinner.setValue(requirement.getRegionId());
		regionSpinner.setToolTipText("Chunk ID");
		regionSpinner.addChangeListener(e -> {
			requirement.setRegionId((int) regionSpinner.getValue());
			requirement.reset();
			refresh();
		});

		final JLabel nicknameLabel = new JLabel("Nickname");
		nicknameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final FlatTextField nicknameInput = new FlatTextField();
		nicknameInput.setText(requirement.getNickname());
		nicknameInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nicknameInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		nicknameInput.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e)
			{
				requirement.setNickname(nicknameInput.getText());
				requirement.reset();
			}
		});

		wrapper.add(regionLabel);
		wrapper.add(regionSpinner);
		wrapper.add(nicknameLabel);
		wrapper.add(nicknameInput);

		return wrapper;
	}

	private JPanel createAbstractRequirementPanel(final AbstractRequirement requirement)
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final FlatTextField nameInput = new FlatTextField();
		nameInput.setText(requirement.getName());
		nameInput.setBackground(ColorScheme.DARK_GRAY_COLOR);
		nameInput.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		nameInput.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e)
			{
				requirement.setName(nameInput.getText());
				requirement.reset();
			}
		});

		wrapper.add(nameInput, BorderLayout.CENTER);

		return wrapper;
	}

	private JPanel createAddRequirementPanel(boolean sub)
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JComboBox<RequirementType> dropdown = new JComboBox<>(RequirementType.values());
		dropdown.setBackground(ColorScheme.DARK_GRAY_COLOR);
		dropdown.setSelectedItem(selectedRequirementType);
		dropdown.setRenderer(new RequirementTypeComboBoxRenderer());
		dropdown.addActionListener(e -> {
			selectedRequirementType = (RequirementType) dropdown.getSelectedItem();
			refresh();
		});

		final JButton submitButton = new JButton(sub ? "Add Sub-Requirement" : "Add Requirement", ADD_ICON);
		submitButton.setEnabled(selectedRequirementType != RequirementType.NONE);
		submitButton.addActionListener(e -> {
			if (dropdown.getSelectedItem() != null)
			{
				Requirement requirement = plugin.createRequirement((RequirementType) dropdown.getSelectedItem());
				target.getChildren().add(requirement);
			}

			refresh();
		});

		wrapper.add(submitButton, BorderLayout.CENTER);
		wrapper.add(dropdown, BorderLayout.SOUTH);

		return wrapper;
	}

	private JPanel createConfirmationPanel()
	{
		final JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(BorderFactory.createEmptyBorder(20, 14, 14, 14));
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		final JButton confirmButton = new JButton("Confirm");
		confirmButton.addActionListener(e -> updateTarget());

		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> notifyListeners(ACTION_CANCEL));

		wrapper.add(confirmButton, BorderLayout.WEST);
		wrapper.add(cancelButton, BorderLayout.EAST);

		return wrapper;
	}

	private static class QuestComboBoxRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value instanceof Quest)
			{
				Quest quest = (Quest) value;
				label.setText(quest.getName());
			}

			return label;
		}
	}

	private static class ItemTrackingOptionComboBoxRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value instanceof ItemTrackingOption)
			{
				ItemTrackingOption option = (ItemTrackingOption) value;
				label.setToolTipText(option.getDescription());
			}

			return label;
		}
	}

	private static class RequirementTypeComboBoxRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			label.setForeground(index == 0 ? ColorScheme.LIGHT_GRAY_COLOR : ColorScheme.BRAND_ORANGE);

			if (value instanceof RequirementType)
			{
				RequirementType type = (RequirementType) value;
				label.setToolTipText(type.getDescription());
			}

			return label;
		}
	}
}
