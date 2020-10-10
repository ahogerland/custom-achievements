# Custom Achievements
Create custom achievements to keep track of personal goals and account progress.

## Features
Achievements may have a variety of different requirements. Each requirement has its own unqiue behaviour and together cover a wide range of different tasks.
- **Skill Requirement**: Track skill levels and XP milestones.
- **Item Requirement**: Collect/Create items. May be restricted to monster drops or simply aquiring items in your inventory.
- **Slay Requirement**: Track monster kills.
- **Quest Requirement**: Track quest progress/completion.
- **Abstract Requirement**: Put anything here, but it must be marked as completed manually. Useful if none of the above fit the task.

## Usage
- [Achievements](#achievements)
- [Requirements](#requirements)
- [Achievement Editor](#achievement-editor)
- [Edit Mode](#edit-mode)

### Achievements
Add a new achievement using by pressing the "Add New Achievement" button in the action menu. This will take you to the [Achievement Editor](#achievement-editor) where you can add/remove requirements. Additionally, you can turn off auto-completion if there is some implicit step that must be done before you'd like to mark the achievement as complete.

Achievements may be in any of the following states:
- **Incomplete**: There are still incomplete requirements.
- **In Progress**: One or more requirements are in progress.
- **Complete**: All requirements have been completed.
- **Complete (Forced)**: The achievement has been marked as complete even though requirements are still incomplete (these requirements will be marked as "skipped").

Requirements are listed below the achievement by default, but may be collapsed by pressing the arrow to the left of the achievement name.

Clicking on the name of an achievement will mark it as "Complete (Forced)" indicated with a star beside the name. All incomplete child requirements will be marked as skipped, but progress will still be recorded normally (with notifications muted). Clicking again will revert it back to its true status.

### Requirements
Requirements are added through the [Achievement Editor](#achievement-editor), which will be opened immediately after adding a new achievement or can be accessed via [Edit Mode](#edit-mode).

Requirements may be in any of the following states:
- **Incomplete**: Requirement has not been met.
- **In Progress**: Requirement is partially complete.
- **Complete**: Requirement has been met.
- **Skipped**: Special case for when the parent achievement has been marked as "Complete (Forced)" and the requirement has not yet been completed.

Clicking the name of a requirement will mark it as complete or, if already completed, will reset its progress (*except for Quest requirements!*).

### Achievement Editor
The Achievement Editor is used to add/modify the name, requirements, and settings for an achievement. Below are descriptions for each requirement attribute.

*It's important to note that progress made toward a requirement will NOT be overwritten ONLY for quantity attributes.*

#### Skill Requirement
| Attribute   | Description                                |
|-------------|--------------------------------------------|
| Skill       | An enumeration of every skill.             |
| Target Type | Either LEVEL or XP.                        |
| Target      | Target value for the selected Target Type. |

#### Item Requirement
| Attribute       | Description                                             |
|-----------------|---------------------------------------------------------|
| Item            | The exact name of the item required (case insensitive). |
| Quantity        | The amount of items to collect.                         |
| Tracking Option | Dropped: Recieve as a drop from monsters or activities.<br> Inventory: Aquire in inventory (ie. item creation or pickups). |

#### Slay Requirement
| Attribute   | Description                                               |
|-------------|-----------------------------------------------------------|
| Target      | The exact name of the monster to slay (case insensitive). |
| Quantity    | The amount of monsters to slay.                           |

#### Quest Requirement
| Attribute   | Description                    |
|-------------|--------------------------------|
| Quest       | An enumeration of every quest. |

#### Abstract Requirement
| Attribute   | Description                  |
|-------------|------------------------------|
| Name        | Anything your heart desires. |

### Edit Mode
Edit Mode can be toggled via the "Toggle Edit Mode" button in the action menu. This enables achievement/requirement reordering, editing, and removal through each respective button provided to the right of each entry.

Clicking on the edit button for a requirement will open the Achievement Editor for the parent achievement - from which the requirement can be edited.
