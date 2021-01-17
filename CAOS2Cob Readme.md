# CAOS2Cob
CAOS2Cob aims to provide a simple way to compile C1 and C2 agents
for the Creatures Games. CAOS2Cob a standard CAOS file with special comment markup.

```CAOS
**Caos2Cob
*# Agent Name = "Island Carrot Vendor"
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


##Usage
Every CAOS2Cob must begin with ```**CAOS2COB``` if the compiler knows your variant 
or ```**CAOS2COB C1``` or ```**CAOS2COB C2``` if the compiler does not

CAOS2Cob comments begin with ```*#```, followed by either a property or a command.
- Properties are formatted with the property name followed by an equal sign, and finally the value
- Commands are formatted with a command word, followed by a list or space delimeted values or arguments

Any CAOS2Cob arguments or values that contain a space, must be enclosed in double quotes. Any quotes in the values must be escaped with a leading slash
#### String values
| Desired Value | What to write   |
| ------------- | --------------- |
| Hello         | `Hello` or `"Hello"`|
| Fastest Car   | `"Fastest Car"`   |
| The "One"     | ``"The \"One\""``   |

#### Date Values
Dates are formatted in the format `YYYY-MM-DD`, where `YYYY` is the year, `MM` is the month, and `DD` is the day of the month
- Example: `2021-01-16` = January 16, 2021

#### File name values
All file names are relative to the CAOS2Cob CAOS script's enclosing folder. 

*\*File names must be quotes if they contain a space*

#### Install and Removal Scripts
Install blocks should be marked start with `iscr` and end with `endm`. In C1, each ISCR block is added to the cob separately.
In C2, they are combined in the order they appear in the document.

Removal blocks should be marked start with `rscr` and end with `endm`.

*\* code blocks not starting in `ISCR`, `RSCR` or `SCRP` are ignored by the compiler.*

It is also possible to define your install or removal scripts in external files. 
- **ISCR** Install Script property - To include a script as an install script, use the CAOS2Cob property value `INST`, and assign it the install file's name. (ie. `*# inst = "Install Script.cos"`)
- **RSCR** Removal script property - To include an external script as a removal script, use the CAOS2Cob property value `RSCR`, and assign it the removal file's name. (ie. `*# rscr = "Removal Script.cos"`)

#### Thumbnail
CAOS2Cob scripts can provide a thumbnail image to show in the Agent Injector in the Creatures games.
The image files can be JPEG, GIF, PNG, BMP or even Creature's sprite files.

- If using a Creature's sprite file, an array accessor should be used to specify the frame number to use. (ie. `file[7].spr`).<br>*\*\*If no array index is supplied, frame \[0\] is used.*

### Agent Properties

#### Both C1 and C2

- **Agent Name** (String) **required**
- **Cob Name** (String) - The output file name **required**
- **Quantity Available** (String) - *C1: `255`, C2: `-1` (infinite)*
- **Thumbnail** (File Name) - The thumbnail to show in the agent injector. Can be JPEG, GIF, PNG, BMP, or a Creatures Sprite
- **Expiry Date** (Date) - default: 9999-12-31
- **ISCR** (File name) - The name of the removal script file
- **RSCR** (file name) - The name of the removal script file

#### C1
- **Quantity Used** (Int) = The nu
- **Remover Name** (File Name) - The name of the remover COB if a removal script is defined. Default: is the COB file name with the `.rcb` extension

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
- **Link** (CAOS file) - This command will include event scripts from other CAOS files into this agent. **_Non-Event scripts are ignored_** 

#### C2
- **Depends** (S16 and WAV files) - List of files required to run this COB. <br/>
- **Attach** (S16 and WAV files) - Files to include in the compiled build and also to mark as dependencies
- **Include** (S16 and WAV files) - Files to include in the COB build, but not to automatically add to the dependencies list

*\**Depends files are not included in the build and are not required to compile.
They simply tell the Creatures 2 runtime that they must already exist to inject this COB*
