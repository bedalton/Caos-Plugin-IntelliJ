# CaosPlugin
This plugin aims to provide simple CAOS and agent editing for a variety of Creatures variants<br/>

## Supports:
- Creatures 1
- Creatures 2
- Creatures Village (Mostly)
- Creatures 3
- Docking Station

## Implemented
- Syntax Highlighting
- Basic completion
- Definition reference
- Parameter hints
- Known value completion from list. (ie. Drives or Chemicals by name)
- SPR, C16 and S16 viewering

## Type hints
![Shows CAOS commands with typehints marking paramater name and value type](https://github.com/bedalton/Caos-Plugin-IntelliJ/blob/assets/assets/type-hints.png)

Plugin adds type-hints to show both parameter name and rvalue type. If known, type hints are added to describe composite bitflag values for things like BUMP and ATTR, and also for english names for int values such as drive and chemical names

## Code folding
![Shows workflow of adding setting a bitflag value for ATTR using a editor panel](https://github.com/bedalton/Caos-Plugin-IntelliJ/blob/assets/assets/code-folding.png)

Some code can be folded to a more human readable descriptive text.

## BitFlag builders
![Shows workflow of adding setting a bitflag value for ATTR using a editor panel](https://github.com/bedalton/Caos-Plugin-IntelliJ/blob/assets/assets/set-bitflags.png)

An editor panel is available to help generate a bitflag value for things like ATTR. The panel is opened by using the autocomplete actions list (made visible with CTRL+SPACE) if there is no value, and the QuickFix/Intention Actions panel opened with ALT+ENTER, if there is already a value in place.

## CLAS value generator
![Shows workflow of adding setting a bitflag value for ATTR using a editor panel](https://github.com/bedalton/Caos-Plugin-IntelliJ/blob/assets/assets/generate-clas.png)

To aid in generating C1 CLAS values, a generator panel is available within the editor. The panel is opened using the autocomplete action (made visible with CTRL+SPACE) if there is no value, and the QuickFix/Intention Actions panel opened with ALT+ENTER, if there is already a value in place.

## Values list completion
![Shows workflow of adding setting a bitflag value for ATTR using a editor panel](https://github.com/bedalton/Caos-Plugin-IntelliJ/blob/assets/assets/values-list-completion.png)

Autocomplete allows the use of english to try to find the corresponding int value in a given context such as CHEM, DRIV, EMIT, etc.

## ATT editor
![Shows workflow of adding setting a bitflag value for ATTR using a editor panel](https://github.com/bedalton/Caos-Plugin-IntelliJ/blob/assets/assets/att-editor.png)

An ATT editor is built into the project, and will try to match an ATT file with its respective sprite within the project. If found, a visual editor is shown which allows setting points in the file by clicking on each sprite in the sequence. 
- Points are selected with a radio button at the top of the editor. 
- Should the editor not guess the correct body part or Creatures variant, these options can be selected manually. 
- Descriptive labels can be hidden by unchecking the labels checkbox.
- Sprite scaling can be adjusted to better find points on the sprite.

## COB viewer
![Shows workflow of adding setting a bitflag value for ATTR using a editor panel](https://github.com/bedalton/Caos-Plugin-IntelliJ/blob/assets/assets/cob-view.png)

Cob viewer allows previewing the contents of a sprite without fully decompiling it. Internal script viewing is available, along with the ability to preview embedded sprites in C2 COBs




## Todo:
- COB compiler
- Pray File editing/Decompiler
- Sprite compilers
