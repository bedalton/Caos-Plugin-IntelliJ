# IntelliJ CAOS Plugin 

The CAOS and Agenteering plugin for IntelliJ adds CAOS, PRAY, and ATT file editing capabilities as well as Creatures sprite viewers.

## Installation

1. Install JetBrain's IntelliJ IDE, **Community (it's free)** version   
`https://www.jetbrains.com/idea/download/`

2. Open IntelliJ and Select plugins on the left side menu.
3. In the search bar, type CAOS and click "Creatures CAOS and Agenteering"
4. Click install in the panel on the right, and restart IntelliJ when prompted.

## Project / Module

A project in IntelliJ is essentially a container for Modules. A module groups files and functionality.

Though CAOS plugin features can mostly work in any kind of module, it is recommended to create a CAOS Module. CAOS modules are game variant specific. C1 will not work properly with C2 or C3 files, etc. 

\* Scripts in a C3 or DS module can be loaded as the other on a file by file basis.

### Single Module Project

If you are only developing for one game variant or just keep your projects lean, you can create a single module project.

1. Select "New Project"
2. Select "CAOS Script" from the left side panel (not "CAOS") and click next
3. Fill out the module details  
   - Project Name
   - Project Location 
   - CAOS Variant - This can be changed later, but may cause problems.
4. Click Create


### Multi-Module project

To create a project that contains multiple variants:
1. Select "New Project" 
2. Select "Empty Project" from the left side panel of options.
3. Choose the project name (Cannot have spaces), and the parent folder for this project.
4. When the project loads choose File -> New -> Module
5. Select "CAOS Script" from the left side panel (not "CAOS") and click next
6. Fill out the module details
    - Project Name
    - Project Location
    - CAOS Variant - This can be changed later, but may cause problems.
7. Repeat for other variants you care about
8. (Optional) Remove root project node
	1. File -> Project Structure
	2. Select the main project folder (one without egg icon)
	3. Click the minus( - ) in the toolbar above it.
	4. Click okay when project informs you that no files will be deleted


## IntelliJ Basics

**AutoComplete** `CTRL+SPACE` - This will trigger the auto complete window to pop up  
Examples of Autocomplete  
- Command names
- GAME/NAME keys
- File names
- Integer values by name, so you can type `hun` after `chem` to have IntelliJ suggest the number for Hungry for protein. Pressing enter will insert the number of the highlighted chemical

**Context Action** `Alt+Enter` (Windows), `Option+Enter` (MacOS) - Pops up a list of available actions for the code where your cursor is at.   
Examples of actions:
- Generate `CLAS` value in C1
- Put text on separate lines when viewing single line C1/C2 code
- Generate Bit-Flag values for things like ATTR or BHVR
- Replace `"` with `[]` in C1 or `[]` with `"` for C2e
- Replace `va09` with `var9` in C1

**Follow Link** Clicking a text item while holding `CTRL` (Windows) or `CMD` (MacOS), can navigate you to the item it references.  
In CAOS this could be the command to view documentation, a subroutine name, or a file name in code.

**View Documentation** Holding `CTRL` (Windows) or `CMD` (MacOS) while hovering will provide a quick popup of documentation for an item if it is available

**Reformat** CAOS scripts can be re-formatted by pressing
- `CTRL+ALT+L` (windows)
- `CMD+OPTION+L` (MacOS)


**Disable Inspections** If the plugin is marking code wrong that is not wrong, you can disable the inspection.
1. Press `ALT+ENTER` (Windows), `Option+Enter`
2. Use arrows to highlight the inspection
3. Press Right Arrow Key
4. Highlight Disable inspection
5. Press enter



## CAOS2Pray

You can compile CAOS2Pray files by clicking on the hammer while viewing the CAOS script. There is some autocompletion available

**Current Problems** IntelliJ reports an error if you use C3-NAME and DS-NAME instead of AGNT-Name or DSAG-Name

- You can Link or Attach multiple files by starting to type Link or Attach, then pressing CTRL+SPACE. This *should* show you an option to attach/link many. Files are split by type and folder. You can filter the files to only include those matching a regex expression.


## ATT Editor

**The ATT editor expects a valid full ATT file before you can use the visual editor**

- A panel must first be clicked on before it can be edited
- Pressing a number 1 - 6 will change which point you are editing when you click. A program exception will occur if that point is not allowed

## Project views


## Theming and Syntax Coloring

The CAOS plugin uses system theming, so installing a style theme will apply the coloring to the CAOS and PRAY files.

To set colors manually:
1. Open Settings/Preferences
    - Windows: File -> Settings 
    - MacOS: IntelliJ Idea -> Preferences
2. In the left hand panel go: `Editor` -> `Color Scheme`  -> then either `PRAY` or `CAOS Script` depending on what you want to edit
3. Clicking on an element in the top section will highlight the code in the bottom view that will be affected.
4. You must uncheck "inherit values from:" on the right side panel to enable editing of the colors and features.


## Inlay Hints

If you find the inlay hints distracting, you can disable them. To do so open 
1. Open Setting/Preferences
   - Windows: File -> Settings
   - MacOS: IntelliJ Idea -> Preferences
2. Expand Editor 
3. Click "Inlay Hints"
4. Expand Parameter names
5. Expand CreaturesPRAY or CaosScript
6. Uncheck any inlay hints you do not want

