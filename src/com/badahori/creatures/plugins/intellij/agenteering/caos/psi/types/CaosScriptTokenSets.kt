package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes.CaosDef_HASH_TAG
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.*
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.tree.TokenSet.create

class CaosScriptTokenSets {

    companion object {

        val BLOCK_START_AND_ENDS = create(
                CaosScript_C_ENDM,
                CaosScript_C_DOIF,
                CaosScript_C_ELIF,
                CaosScript_C_ELSE,
                CaosScript_C_ENDI,
                CaosScript_C_RETN,
                CaosScript_C_ENUM,
                CaosScript_C_NEXT,
                CaosScript_C_ESCN,
                CaosScript_C_NSCN,
                CaosScript_C_REPS,
                CaosScript_C_REPE,
                CaosScript_C_LOOP,
                CaosScript_C_UNTL,
                CaosScript_C_EVER,
                CaosScript_C_SCRP,
                CaosScript_C_SUBR
        )

        /**
         * Eq op tokens. Symbols are grouped by old ('eq'|'ne') and new style ('='|'!=')
         */
        @JvmStatic
        val EQ_OPS = create(
                CaosScript_EQ_OP,
                CaosScript_EQ_OP_OLD,
                CaosScript_EQ_OP_OLD_,
                CaosScript_EQ_OP_NEW,
                CaosScript_EQ_OP_NEW_
        )

        @JvmStatic
        val COMMENTS = create(
                CaosScript_COMMENT_LITERAL,
                CaosScript_COMMENT_START,
                CaosScript_COMMENT_TEXT,
                CaosScript_COMMENT,
                CaosScript_COMMENT_BODY
        )

        @JvmStatic
        val WHITE_SPACE_LIKE_WITH_COMMENT = create(
                CaosScript_SPACE_,
                CaosScript_COMMENT_LITERAL,
                TokenType.WHITE_SPACE
        )

        @JvmStatic
        val WHITESPACES = create(
                CaosScript_SPACE_,
                CaosScript_SPACE_LIKE,
                TokenType.WHITE_SPACE,
                CaosScript_SPACE,
                CaosScript_NEWLINE,
                CaosScript_NEW_LINE,
                CaosScript_NEW_LINE_LIKE
        )
        @JvmStatic
        val NUMBER_LITERALS = create(
                CaosScript_INT,
                CaosScript_DECIMAL,
                CaosScript_PLUS,
                CaosScript_BINARY
        )

        @JvmStatic
        val STRING_LIKE = create(
                CaosScript_ANIMATION_STRING,
                CaosScript_BYTE_STRING,
                CaosScript_ANIM_R,
                CaosScript_DOUBLE_QUOTE,
                CaosScript_STRING_TEXT,
                CaosScript_QUOTE_STRING_LITERAL,
                CaosScript_TEXT_LITERAL,
                CaosScript_C_1_STRING,
                CaosScript_OPEN_BRACKET,
                CaosScript_CLOSE_BRACKET,
                CaosScript_CHARACTER,
                CaosScript_DOUBLE_QUOTE,
                CaosScript_STRING_CHAR,
                CaosScript_STRING_ESCAPE_CHAR,
                CaosScript_SINGLE_QUOTE,
                CaosScript_CHAR_CHAR
        )

        val LITERALS: TokenSet = create(
                CaosScript_INT,
                CaosScript_DECIMAL,
                CaosScript_NUMBER,
                CaosScript_QUOTE_STRING_LITERAL,
                CaosScript_C_1_STRING,
                CaosScript_BYTE_STRING,
                CaosScript_ANIMATION_STRING,
                CaosScript_QUOTE_STRING_LITERAL,
                CaosScript_CHARACTER
        )

        @JvmStatic
        val KEYWORDS = create(
                CaosScript_K_INST,
                CaosScript_K_ISCR,
                CaosScript_K_RSCR,
                CaosScript_K_DOIF,
                CaosScript_K_ELIF,
                CaosScript_K_ELSE,
                CaosScript_K_ENDI,
                CaosScript_K_ENDM,
                CaosScript_K_ENUM,
                CaosScript_K_ESCN,
                CaosScript_K_EPAS,
                CaosScript_K_ETCH,
                CaosScript_K_ESEE,
                CaosScript_K_EVER,
                CaosScript_K_LOOP,
                CaosScript_K_NEXT,
                CaosScript_K_NSCN,
                CaosScript_K_REPS,
                CaosScript_K_REPE,
                CaosScript_K_RETN,
                CaosScript_K_SUBR,
                CaosScript_K_SCRP,
                CaosScript_K_UNTL,
                CaosScript_K_CRETN,
                CaosScript_K_ECON,
                CaosScript_K_AND,
                CaosScript_K_OR
        )

        @JvmStatic
        val Variables = create(
                CaosScript_VAR_X,
                CaosScript_VA_XX,
                CaosScript_OBV_X,
                CaosScript_OV_XX,
                CaosScript_MV_XX
        )

        @JvmStatic
        val ANIMATION_STRING = create(
                CaosScript_ANIMATION_STRING,
                CaosScript_BYTE_STRING,
                CaosScript_ANIM_R
        )

        @JvmStatic
        val ScriptTerminators = create(
                CaosScript_K_SCRP,
                CaosScript_K_ENDM
        )


        @JvmStatic
        val ALL_CAOS_COMMAND_LIKE_TOKENS = create(
                CaosScript_K_CD_,
                CaosScript_K_EJCT,
                CaosScript_K_INIT,
                CaosScript_K_SHUT,
                CaosScript_K_STOP,
                CaosScript_K_BANG,
                CaosScript_K_CALC,
                CaosScript_K_UCLN,
                CaosScript_K_PAWS,
                CaosScript_K_BUZZ,
                CaosScript_K_CATO,
                CaosScript_K_DCOR,
                CaosScript_K_DOIN,
                CaosScript_K_DSEE,
                CaosScript_K_MIND,
                CaosScript_K_MOTR,
                CaosScript_K_STEP,
                CaosScript_K_PLAY,
                CaosScript_K_CALG,
                CaosScript_K_SOUL,
                CaosScript_K_NAMN,
                CaosScript_K_NOTV,
                CaosScript_K_ADMP,
                CaosScript_K_AVEL,
                CaosScript_K_FDMP,
                CaosScript_K_FVEL,
                CaosScript_K_SDMP,
                CaosScript_K_SPIN,
                CaosScript_K_SVEL,
                CaosScript_K_VARC,
                CaosScript_K_PLMD,
                CaosScript_K_PLMU,
                CaosScript_K_ADIN,
                CaosScript_K_DELE,
                CaosScript_K_WEBB,
                CaosScript_K_BOOT,
                CaosScript_K_MMSC,
                CaosScript_K_CALL,
                CaosScript_K_CORE,
                CaosScript_K_DELN,
                CaosScript_K_ROTN,
                CaosScript_K_JECT,
                CaosScript_K_PRT_COL,
                CaosScript_K_JOIN,
                CaosScript_K_NEW_COL,
                CaosScript_K_COMP,
                CaosScript_K_CRAG,
                CaosScript_K_TINO,
                CaosScript_K_PAT_COL,
                CaosScript_K_MOVE,
                CaosScript_K_INEW,
                CaosScript_K_ONEW,
                CaosScript_K_SEND,
                CaosScript_K_APPR,
                CaosScript_K_DONE,
                CaosScript_K_DROP,
                CaosScript_K_ELSE,
                CaosScript_K_ENDI,
                CaosScript_K_EVER,
                CaosScript_K_FADE,
                CaosScript_K_INST,
                CaosScript_K_LOOP,
                CaosScript_K_MATE,
                CaosScript_K_NEXT,
                CaosScript_K_OVER,
                CaosScript_K_REPE,
                CaosScript_K_RETN,
                CaosScript_K_SAYN,
                CaosScript_K_STPC,
                CaosScript_K_TOUC,
                CaosScript_K_WALK,
                CaosScript_K_ASLP,
                CaosScript_K_BASE,
                CaosScript_K_DREA,
                CaosScript_K_PART,
                CaosScript_K_REPS,
                CaosScript_K_TICK,
                CaosScript_K_WAIT,
                CaosScript_K_NEGV,
                CaosScript_K_ANDV,
                CaosScript_K_MODV,
                CaosScript_K_ORRV,
                CaosScript_K_ANIM,
                CaosScript_K_ENUM,
                CaosScript_K_LTCY,
                CaosScript_K_RTAR,
                CaosScript_K_CABN,
                CaosScript_K_SCRX,
                CaosScript_K_UNTL,
                CaosScript_K_KILL,
                CaosScript_K_TARG,
                CaosScript_K_MVBY,
                CaosScript_K_MVTO,
                CaosScript_K_MESG,
                CaosScript_K_WRIT,
                CaosScript_K_GSUB,
                CaosScript_K_SPAS,
                CaosScript_K_ASEA,
                CaosScript_K_CBRG,
                CaosScript_K_DDE_COL,
                CaosScript_K_GETB,
                CaosScript_K_BIOC,
                CaosScript_K_NEWV,
                CaosScript_K_ORGN,
                CaosScript_K_RRCT,
                CaosScript_K_GIDS,
                CaosScript_K_ROOT,
                CaosScript_K_NACT,
                CaosScript_K_ISCR,
                CaosScript_K_LNG_PLUS,
                CaosScript_K_NSCN,
                CaosScript_K_RCLR,
                CaosScript_K_RSCR,
                CaosScript_K_SYS_COL,
                CaosScript_K_DMAP,
                CaosScript_K_TECO,
                CaosScript_K_BBD_COL,
                CaosScript_K_EMIT,
                CaosScript_K_ALLR,
                CaosScript_K_EMTR,
                CaosScript_K_RCTN,
                CaosScript_K_RPTY,
                CaosScript_K_FMLY,
                CaosScript_K_LNEU,
                CaosScript_K_DPS2,
                CaosScript_K_VCB1,
                CaosScript_K_BBTX,
                CaosScript_K_CBRX,
                CaosScript_K_GNUS,
                CaosScript_K_RAIN,
                CaosScript_K_CMRP,
                CaosScript_K_BBT2,
                CaosScript_K_VOCB,
                CaosScript_K_LVOB,
                CaosScript_K_SPCS,
                CaosScript_K_ESCN,
                CaosScript_K_KMSG,
                CaosScript_K_PIC2,
                CaosScript_K_BBFD,
                CaosScript_K_LCUS,
                CaosScript_K_BBLE,
                CaosScript_K_CBUB,
                CaosScript_K_ROOM,
                CaosScript_K_CONV,
                CaosScript_K_BUMP,
                CaosScript_K_CAGE,
                CaosScript_K_CLAS,
                CaosScript_K_PICT,
                CaosScript_K_GND_NUM,
                CaosScript_K_GNDW,
                CaosScript_K_RMS_NUM,
                CaosScript_K_TEMP,
                CaosScript_K_WIND,
                CaosScript_K_EDIT,
                CaosScript_K_GRND,
                CaosScript_K_CMND,
                CaosScript_K_SNDV,
                CaosScript_K_TOTL,
                CaosScript_K_SNDF,
                CaosScript_K_TOOL,
                CaosScript_K_BORN,
                CaosScript_K_BRN_COL,
                CaosScript_K_DMPB,
                CaosScript_K_DBG_COL,
                CaosScript_K_CPRO,
                CaosScript_K_FLSH,
                CaosScript_K_POLL,
                CaosScript_K_PROF,
                CaosScript_K_TOCK,
                CaosScript_K_FCUS,
                CaosScript_K_FILE,
                CaosScript_K_ICLO,
                CaosScript_K_OCLO,
                CaosScript_K_OFLU,
                CaosScript_K_FRSH,
                CaosScript_K_HELP,
                CaosScript_K_MAPK,
                CaosScript_K_MEMX,
                CaosScript_K_NOHH,
                CaosScript_K_NUDE,
                CaosScript_K_PRAY,
                CaosScript_K_REFR,
                CaosScript_K_QUIT,
                CaosScript_K_REAF,
                CaosScript_K_RGAM,
                CaosScript_K_SAVE,
                CaosScript_K_STPT,
                CaosScript_K_SUBR,
                CaosScript_K_WDOW,
                CaosScript_K_AERO,
                CaosScript_K_AGES,
                CaosScript_K_ATTR,
                CaosScript_K_BHVR,
                CaosScript_K_DMPL,
                CaosScript_K_DMPT,
                CaosScript_K_CABP,
                CaosScript_K_CABV,
                CaosScript_K_CABW,
                CaosScript_K_CLAC,
                CaosScript_K_CMRT,
                CaosScript_K_HTML,
                CaosScript_K_WTIK,
                CaosScript_K_DELM,
                CaosScript_K_DIRN,
                CaosScript_K_DOCA,
                CaosScript_K_ELAS,
                CaosScript_K_FACE,
                CaosScript_K_FRAT,
                CaosScript_K_FRIC,
                CaosScript_K_GAIT,
                CaosScript_K_HAIR,
                CaosScript_K_IMSK,
                CaosScript_K_MIRA,
                CaosScript_K_MOUS,
                CaosScript_K_PAUS,
                CaosScript_K_PERM,
                CaosScript_K_PLNE,
                CaosScript_K_GARB,
                CaosScript_K_IZAP,
                CaosScript_K_OZAP,
                CaosScript_K_PURE,
                CaosScript_K_SHOW,
                CaosScript_K_TNTW,
                CaosScript_K_UNCS,
                CaosScript_K_WPAU,
                CaosScript_K_ZOMB,
                CaosScript_K_ALPH,
                CaosScript_K_BODY,
                CaosScript_K_BRMI,
                CaosScript_K_DMPD,
                CaosScript_K_DMPN,
                CaosScript_K_MAPD,
                CaosScript_K_MCLR,
                CaosScript_K_RTYP,
                CaosScript_K_TRAN,
                CaosScript_K_VOLM,
                CaosScript_K_ABSV,
                CaosScript_K_ACCG,
                CaosScript_K_RNGE,
                CaosScript_K_ADDB,
                CaosScript_K_GLOB,
                CaosScript_K_IOPE,
                CaosScript_K_JDEL,
                CaosScript_K_STRK,
                CaosScript_K_ADDS,
                CaosScript_K_SETS,
                CaosScript_K_ADDV,
                CaosScript_K_DIVV,
                CaosScript_K_MULV,
                CaosScript_K_SETV,
                CaosScript_K_SUBV,
                CaosScript_K_ALTR,
                CaosScript_K_SETL,
                CaosScript_K_SETT,
                CaosScript_K_PROP,
                CaosScript_K_ANMS,
                CaosScript_K_APRO,
                CaosScript_K_OUTS,
                CaosScript_K_DELG,
                CaosScript_K_DELW,
                CaosScript_K_HAND,
                CaosScript_K_HIST,
                CaosScript_K_WIPE,
                CaosScript_K_LOAD,
                CaosScript_K_MANN,
                CaosScript_K_MIDI,
                CaosScript_K_ORDR,
                CaosScript_K_SHOU,
                CaosScript_K_SIGN,
                CaosScript_K_TACT,
                CaosScript_K_OUTX,
                CaosScript_K_PSWD,
                CaosScript_K_PTXT,
                CaosScript_K_SEZZ,
                CaosScript_K_SNDC,
                CaosScript_K_SNDE,
                CaosScript_K_SNDL,
                CaosScript_K_VOIS,
                CaosScript_K_WRLD,
                CaosScript_K_CLIK,
                CaosScript_K_CMRA,
                CaosScript_K_DOOR,
                CaosScript_K_DPAS,
                CaosScript_K_EPAS,
                CaosScript_K_LINK,
                CaosScript_K_PUHL,
                CaosScript_K_PUPT,
                CaosScript_K_TTAR,
                CaosScript_K_VOIC,
                CaosScript_K_WEAR,
                CaosScript_K_ZOOM,
                CaosScript_K_CACL,
                CaosScript_K_GPAS,
                CaosScript_K_META,
                CaosScript_K_BKGD,
                CaosScript_K_OOPE,
                CaosScript_K_SETD,
                CaosScript_K_SETN,
                CaosScript_K_CHAR,
                CaosScript_K_CHEM,
                CaosScript_K_DRIV,
                CaosScript_K_GRPV,
                CaosScript_K_STIM,
                CaosScript_K_ASRT,
                CaosScript_K_DOIF,
                CaosScript_K_ELIF,
                CaosScript_K_OUTV,
                CaosScript_K_TACK,
                CaosScript_K_ECON,
                CaosScript_K_FORF,
                CaosScript_K_FREL,
                CaosScript_K_LIKE,
                CaosScript_K_NORN,
                CaosScript_K_FLTO,
                CaosScript_K_MVFT,
                CaosScript_K_MVSF,
                CaosScript_K_VELO,
                CaosScript_K_FRMT,
                CaosScript_K_GALL,
                CaosScript_K_SNDQ,
                CaosScript_K_GENE,
                CaosScript_K_CLON,
                CaosScript_K_CROS,
                CaosScript_K_SCAM,
                CaosScript_K_GOTO,
                CaosScript_K_GRPL,
                CaosScript_K_EVNT,
                CaosScript_K_FOTO,
                CaosScript_K_UTXT,
                CaosScript_K_NAME,
                CaosScript_K_LINE,
                CaosScript_K_LOCI,
                CaosScript_K_SIMP,
                CaosScript_K_VHCL,
                CaosScript_K_CREA,
                CaosScript_K_NEWC,
                CaosScript_K_TINT,
                CaosScript_K_BUTT,
                CaosScript_K_DULL,
                CaosScript_K_FIXD,
                CaosScript_K_GRPH,
                CaosScript_K_TEXT,
                CaosScript_K_KRAK,
                CaosScript_K_RATE,
                CaosScript_K_WTNT,
                CaosScript_K_RPAS,
                CaosScript_K_SETA,
                CaosScript_K_SNAP,
                CaosScript_K_SPNL,
                CaosScript_K_SWAY,
                CaosScript_K_TRCK,
                CaosScript_K_URGE,
                CaosScript_K_CAMN,
                CaosScript_K_DIED,
                CaosScript_K_CNAM,
                CaosScript_K_CTIM,
                CaosScript_K_DATA,
                CaosScript_K_MONK,
                CaosScript_K_OVVD,
                CaosScript_K_HATC,
                CaosScript_K_LIVE,
                CaosScript_K_LOBE,
                CaosScript_K_NEGG,
                CaosScript_K_PANC,
                CaosScript_K_ENDM,
                CaosScript_K_POIN,
                CaosScript_K_SLIM,
                CaosScript_K_SNEZ,
                CaosScript_K_ABRT,
                CaosScript_K_CAMT,
                CaosScript_K_WTOP,
                CaosScript_K_AIM_COL,
                CaosScript_K_WORD,
                CaosScript_K_IMPT,
                CaosScript_K_POSE,
                CaosScript_K_SAY_NUM,
                CaosScript_K_STM_NUM,
                CaosScript_K_VRSN,
                CaosScript_K_KNOB,
                CaosScript_K_MCRT,
                CaosScript_K_TELE,
                CaosScript_K_PRLD,
                CaosScript_K_DBGM,
                CaosScript_K_PUTS,
                CaosScript_K_SAY_DOL,
                CaosScript_K_CELL,
                CaosScript_K_FIRE,
                CaosScript_K_TRIG,
                CaosScript_K_SCRP,
                CaosScript_K_WPOS,
                CaosScript_K_RNDV,
                CaosScript_K_RMEV,
                CaosScript_K_DBGV,
                CaosScript_K_DBUG,
                CaosScript_K_PUTV,
                CaosScript_K_PUTB,
                CaosScript_K_PLDS,
                CaosScript_K_BKBD,
                CaosScript_K_CBTN,
                CaosScript_K_SCEN,
                CaosScript_K_LIFT,
                CaosScript_K_SPOT,
                CaosScript_K_DEAD,
                CaosScript_K_LOCK,
                CaosScript_K_SLOW,
                CaosScript_K_UNLK,
                CaosScript_K_DELR,
                CaosScript_K_INJR,
                CaosScript_K_ESEE,
                CaosScript_K_ETCH,
                CaosScript_K_STAR,
                CaosScript_K_WRT_PLUS,
                CaosScript_K_NET_COL,
                CaosScript_K_HEAD,
                CaosScript_K_WHOD,
                CaosScript_K_WHOZ,
                CaosScript_K_RUSO,
                CaosScript_K_HEAR,
                CaosScript_K_WHOF,
                CaosScript_K_WHON,
                CaosScript_K_PASS,
                CaosScript_K_STAT,
                CaosScript_K_UNIK,
                CaosScript_K_RSET,
                CaosScript_K_STRE,
                CaosScript_K_PRNT,
                CaosScript_K_SCRL,
                CaosScript_K_EXPR,
                CaosScript_K_MIRR,
                CaosScript_K_BMPS,
                CaosScript_K_EXEC,
                CaosScript_K_IMGE,
                CaosScript_K_SWAP,
                CaosScript_K_TNTC,
                CaosScript_K_DYED,
                CaosScript_K_TNTO,
                CaosScript_K_SCLE,
                CaosScript_K_RMSC,
                CaosScript_K_FRQH,
                CaosScript_K_FRQL,
                CaosScript_K_FRQM,
                CaosScript_K_ABBA,
                CaosScript_K_CATA,
                CaosScript_K_HOTP,
                CaosScript_K_MODU,
                CaosScript_K_UFOS,
                CaosScript_K_VECX,
                CaosScript_K_VECY,
                CaosScript_K_ADDM,
                CaosScript_K_PNXT,
                CaosScript_K_SEEN,
                CaosScript_K_ANGL,
                CaosScript_K_NETU,
                CaosScript_K_MAME,
                CaosScript_K_EAME,
                CaosScript_K_EAMN,
                CaosScript_K_WVET,
                CaosScript_K_LOWA,
                CaosScript_K_UPPA,
                CaosScript_K_MON2,
                CaosScript_K_LIMB,
                CaosScript_K_BACK,
                CaosScript_K_FORE,
                CaosScript_K_AGTI,
                CaosScript_K_SINS,
                CaosScript_K_TCOR,
                CaosScript_K_IT_,
                CaosScript_K_CARR,
                CaosScript_K_FROM,
                CaosScript_K_HGHT,
                CaosScript_K_MOVS,
                CaosScript_K_OWNR,
                CaosScript_K_PNTR,
                CaosScript_K_POSB,
                CaosScript_K_POSL,
                CaosScript_K_POSR,
                CaosScript_K_POST,
                CaosScript_K_WDTH,
                CaosScript_K_P1_,
                CaosScript_K_P2_,
                CaosScript_K_CMRX,
                CaosScript_K_CMRY,
                CaosScript_K_INS_NUM,
                CaosScript_K_POSX,
                CaosScript_K_POSY,
                CaosScript_K_SEAN,
                CaosScript_K_UNID,
                CaosScript_K_VELX,
                CaosScript_K_VELY,
                CaosScript_K_WALL,
                CaosScript_K_YEAR,
                CaosScript_K_OBST,
                CaosScript_K_UP_,
                CaosScript_K_BVAR,
                CaosScript_K_BYIT,
                CaosScript_K_CABB,
                CaosScript_K_CABL,
                CaosScript_K_CABR,
                CaosScript_K_CABT,
                CaosScript_K_CODE,
                CaosScript_K_CODF,
                CaosScript_K_CODG,
                CaosScript_K_CODP,
                CaosScript_K_CODS,
                CaosScript_K_DATE,
                CaosScript_K_DAYT,
                CaosScript_K_DECN,
                CaosScript_K_DFTX,
                CaosScript_K_DFTY,
                CaosScript_K_DOWN,
                CaosScript_K_EMID,
                CaosScript_K_ETIK,
                CaosScript_K_FALL,
                CaosScript_K_FLTX,
                CaosScript_K_FLTY,
                CaosScript_K_GNAM,
                CaosScript_K_HELD,
                CaosScript_K_HHLD,
                CaosScript_K_HOTS,
                CaosScript_K_IITT,
                CaosScript_K_INNF,
                CaosScript_K_INNI,
                CaosScript_K_INNL,
                CaosScript_K_INOK,
                CaosScript_K_LEFT,
                CaosScript_K_MAPH,
                CaosScript_K_MAPW,
                CaosScript_K_MONT,
                CaosScript_K_MOPX,
                CaosScript_K_MOPY,
                CaosScript_K_MOVX,
                CaosScript_K_MOVY,
                CaosScript_K_MOWS,
                CaosScript_K_MSEC,
                CaosScript_K_MTHX,
                CaosScript_K_MTHY,
                CaosScript_K_NPGS,
                CaosScript_K_NULL,
                CaosScript_K_NWLD,
                CaosScript_K_PACE,
                CaosScript_K_PAGE,
                CaosScript_K_ITOT,
                CaosScript_K_OTOT,
                CaosScript_K_RACE,
                CaosScript_K_RGHT,
                CaosScript_K_RTIM,
                CaosScript_K_TAGE,
                CaosScript_K_TIME,
                CaosScript_K_UFTX,
                CaosScript_K_UFTY,
                CaosScript_K_VMJR,
                CaosScript_K_VMNR,
                CaosScript_K_WNAM,
                CaosScript_K_WNDB,
                CaosScript_K_WNDH,
                CaosScript_K_WNDL,
                CaosScript_K_WNDR,
                CaosScript_K_WNDT,
                CaosScript_K_WNDW,
                CaosScript_K_WUID,
                CaosScript_K_ACOS,
                CaosScript_K_ASIN,
                CaosScript_K_ATAN,
                CaosScript_K_COS_,
                CaosScript_K_FTOI,
                CaosScript_K_SIN_,
                CaosScript_K_SQRT,
                CaosScript_K_TAN_,
                CaosScript_K_ADDR,
                CaosScript_K_AGNT,
                CaosScript_K_BKDS,
                CaosScript_K_CATX,
                CaosScript_K_DBG_NUM,
                CaosScript_K_DBGA,
                CaosScript_K_ERID,
                CaosScript_K_GTOS,
                CaosScript_K_HEAP,
                CaosScript_K_ITOF,
                CaosScript_K_KEYD,
                CaosScript_K_MLOC,
                CaosScript_K_FRMA,
                CaosScript_K_RLOC,
                CaosScript_K_TORX,
                CaosScript_K_TORY,
                CaosScript_K_VISI,
                CaosScript_K_GMAP,
                CaosScript_K_GRAP,
                CaosScript_K_TMVB,
                CaosScript_K_TMVF,
                CaosScript_K_TMVT,
                CaosScript_K_AVAR,
                CaosScript_K_GRID,
                CaosScript_K_TWIN,
                CaosScript_K_CAOS,
                CaosScript_K_CATI,
                CaosScript_K_HIRP,
                CaosScript_K_LORP,
                CaosScript_K_TYPE,
                CaosScript_K_DEPS,
                CaosScript_K_READ,
                CaosScript_K_DISQ,
                CaosScript_K_MUTE,
                CaosScript_K_ORGF,
                CaosScript_K_ORGI,
                CaosScript_K_RAND,
                CaosScript_K_WOLF,
                CaosScript_K_FVWM,
                CaosScript_K_GAME,
                CaosScript_K_GAMN,
                CaosScript_K_COUN,
                CaosScript_K_GEND,
                CaosScript_K_PREV,
                CaosScript_K_VARI,
                CaosScript_K_LOFT,
                CaosScript_K_MTOA,
                CaosScript_K_MTOC,
                CaosScript_K_OOWW,
                CaosScript_K_EXPO,
                CaosScript_K_TEST,
                CaosScript_K_REAN,
                CaosScript_K_REAQ,
                CaosScript_K_SNAX,
                CaosScript_K_STOF,
                CaosScript_K_STOI,
                CaosScript_K_STRL,
                CaosScript_K_WNTI,
                CaosScript_K_FIND,
                CaosScript_K_FINR,
                CaosScript_K_IMPO,
                CaosScript_K_SUBS,
                CaosScript_K_RTIF,
                CaosScript_K_SORC,
                CaosScript_K_SORQ,
                CaosScript_K_NCLS,
                CaosScript_K_PCLS,
                CaosScript_K_AGTS,
                CaosScript_K_INJT,
                CaosScript_K_MAKE,
                CaosScript_K_RELX,
                CaosScript_K_RELY,
                CaosScript_K_SEEE,
                CaosScript_K_SCOL,
                CaosScript_K_VTOS,
                CaosScript_K_WILD,
                CaosScript_K_ACTV,
                CaosScript_K_BABY,
                CaosScript_K_DRV_EXC,
                CaosScript_K_HOUR,
                CaosScript_K_LIML,
                CaosScript_K_LIMR,
                CaosScript_K_LIMT,
                CaosScript_K_MINS,
                CaosScript_K_NEID,
                CaosScript_K_OBJP,
                CaosScript_K_TCAR,
                CaosScript_K_XVEC,
                CaosScript_K_YVEC,
                CaosScript_K_TOKN,
                CaosScript_K_ATTN,
                CaosScript_K_EGGL,
                CaosScript_K_FLOR,
                CaosScript_K_FRZN,
                CaosScript_K_GRAV,
                CaosScript_K_HATL,
                CaosScript_K_HSRC,
                CaosScript_K_INTR,
                CaosScript_K_LACB,
                CaosScript_K_LITE,
                CaosScript_K_LSRC,
                CaosScript_K_ONTR,
                CaosScript_K_PRES,
                CaosScript_K_PSRC,
                CaosScript_K_RADN,
                CaosScript_K_REST,
                CaosScript_K_RMNO,
                CaosScript_K_RNDR,
                CaosScript_K_RSRC,
                CaosScript_K_SEAV,
                CaosScript_K_SIZE,
                CaosScript_K_THRT,
                CaosScript_K_TMOD,
                CaosScript_K_WLDH,
                CaosScript_K_WLDW,
                CaosScript_K_WNDX,
                CaosScript_K_WNDY,
                CaosScript_K_ISAR,
                CaosScript_K_MUSC,
                CaosScript_K_OBDT,
                CaosScript_K_OBSV,
                CaosScript_K_SCOR,
                CaosScript_K_RMND,
                CaosScript_K_RMNR,
                CaosScript_K_RMN_NUM,
                CaosScript_K_BRED,
                CaosScript_K_HEDX,
                CaosScript_K_HEDY,
                CaosScript_K_ONTV,
                CaosScript_K_SNDS,
                CaosScript_K_WINH,
                CaosScript_K_WINW,
                CaosScript_K_LANG,
                CaosScript_K_ERRA,
                CaosScript_K_HOST,
                CaosScript_K_RAWE,
                CaosScript_K_USER,
                CaosScript_K_WHAT,
                CaosScript_K_ULIN,
                CaosScript_K_MON1,
                CaosScript_K_CLS2,
                CaosScript_K_WORD,
                CaosScript_K_XIST
        )

        @JvmStatic
        val ALL_COMMANDS: TokenSet by lazy {
            val tokens = ALL_CAOS_COMMAND_LIKE_TOKENS
                    .types
                    .filterNot { KEYWORDS.contains(it) }
                    .toSet()
                    .toTypedArray()
            create(*tokens)
        }

        @JvmStatic
        val ALL_FIND_USAGES_TOKENS: TokenSet by lazy {
            val otherTokens = listOf(
                    CaosScript_N_VAR,
                    CaosScript_N_CONST,
                    CaosScript_VAR_X,
                    CaosScript_VA_XX,
                    CaosScript_OBV_X,
                    CaosScript_OV_XX,
                    CaosScript_MV_XX,
                    CaosScript_VAR_TOKEN,
                    CaosDef_HASH_TAG,
                    CaosScript_INT
            )
            val tokens = (ALL_CAOS_COMMAND_LIKE_TOKENS
                    .types + otherTokens)
                    .toSet()
                    .toTypedArray()
            create(*tokens)
        }

    }
}