# CAOS2Cob
CAOS2Cob aims to provide a simple way to compile C1 and C2 agents
for the Creatures Games. CAOS2Cob script is a standard CAOS file with special comment markup.

```CAOS
**Caos2Cob
*# C1-Name "Island Carrot Vendor"
*# Cob File = "Island Carrot Vendor.cob"
*# expiry = 9999-12-31
*# Thumbnail = "disp[0].spr"
*# Quantity Used = 0
*# RSCR = "2 8 3 Removal.cos"

iscr
    inst
    enum 2 8 3
        setv actv 0
        setv var0 attr
        orrv var0 4
        bhvr 1 1
    next
endm

scrp 2 8 3 1
    snde vend
    anim [123456789] over
    ....
endm
```


## Usage
CAOS2Cob comments begin with ```*#```, followed by either a property or a command.
- Properties are formatted with the property name followed by an equal sign, and finally the value
- Commands are formatted with a command word, followed by a list or space delimited values or arguments

**Required:** Every CAOS2Cob script must have the Agent name marked by a **C1-Name** or **C2-Name** command followed by the name of the agent.<br/>
Example: `C1-Name "Carrot Vendor"`

Any CAOS2Cob arguments or values that contain a space, must be enclosed in double quotes.
Any quotes in the values must be escaped with a leading slash

#### String values
| Desired Value | What to write   |
| ------------- | --------------- |
| Hello         | `Hello` or `"Hello"`|
| Fastest Car   | `"Fastest Car"`   |
| The "One"     | ``"The \"One\""``   |

#### Date Values
Dates are formatted in the format `YYYY-MM-DD`, where `YYYY` is a four digit year, `MM` is a two-digit month, and `DD` is the two-digit day of the month
- Example: `2021-01-16` = January 16, 2021

#### File name values
All file names are relative to the CAOS2Cob CAOS script's enclosing folder. 

*\*File names must be in quotes if they contain a space*

#### Install and Removal Scripts
Install blocks should be marked start with `iscr` and end with `endm`. In C1, each ISCR block is added to the cob separately.
In C2, they are combined in the order they appear in the document.

Removal blocks should be marked start with `rscr` and end with `endm`.

*\* code blocks not starting in `ISCR`, `RSCR` or `SCRP` are ignored by the compiler.*

It is also possible to define your install or removal scripts in external files. 
- **ISCR** Install Script command - To include a script as an install script, use the CAOS2Cob command value `INST`, and assign it provide the install file's name. (ie. `*# Inst "Install Script.cos"`)<br/>
  *\*Multiple install scripts will be added individually for C1, but in C2 they will be combined into a single install script*<br/>
- **RSCR** Removal script command - To include an external script as a removal script, use the CAOS2Cob property value `RSCR`, and assign it the removal file's name. (ie. `*# Rscr "Removal Script.cos"`)<br/>
*\*Multiple removal scripts will be combined into one removal script before compiling.*

#### Thumbnail
CAOS2Cob scripts can provide a thumbnail image to show in the Agent Injector in the Creatures games.
The image files can be JPEG, GIF, PNG, BMP or even Creature's sprite files.

- If using a Creature's sprite file, an array accessor should be used to specify the frame number to use. (ie. `file[7].spr`).<br>*\*\*If no array index is supplied, frame \[0\] is used.*

### Agent Properties

#### Both C1 and C2

- **Cob Name** (String) - The output file name **required**
- **Quantity Available** (String) - *C1: `255`, C2: `-1` (infinite)*
- **Thumbnail** (File Name) - The thumbnail to show in the agent injector. Can be JPEG, GIF, PNG, BMP, or a Creatures Sprite
- **Expiry Date** (Date) - default: 9999-12-31
- **ISCR** (File name) - The name of the removal script file
- **RSCR** (file name) - The name of the removal script file

#### C1
- **Quantity Used** (Int) - The number of injections used so far.
- **Remover Name** (File Name) - The name of the remover COB if a removal script is defined. ***Default**: COB file name with the `.rcb` extension*

#### C2
- **AgentDescription** (String)
- **Last Usage Date** (Date)
- **Reuse Interval** (Int)
- **Creation Time** (Date)
- **Version** (Int)
- **Revision** (Int)
- **Author Name** (String)
- **Author Email** (String)
- **Author URL** (String)
- **Author Comments** (String)

### Agent Commands

Uses a command name followed by a space delimited list of file names

#### Both C1 and C2
- **Rscr** (...CAOS file) - The removal script(s) to use
- **Inst** (...CAOS file) - The install script(s) to use
- **Link** (...CAOS file) - This command will include event scripts from other CAOS files into this agent. **_Non-Event scripts are ignored_** 

#### C2
- **Depend** (...S16 and WAV files) - List of files required to run this COB.
- **Attach** (...S16 and WAV files) - Files to include in the compiled build and also to mark as dependencies
- **Include** (...S16 and WAV files) - Files to include in the COB build, but to not automatically add to the dependencies list

*\**Depends files are not included in the build and are not required to compile.
They simply tell the Creatures 2 runtime that they must already exist to inject this COB*
