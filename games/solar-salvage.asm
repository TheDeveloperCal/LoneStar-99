* SOLAR SALVAGE 99 — an original deterministic grid puzzle cartridge.
* Copyright (C) 2026 DeveloperCal. SPDX-License-Identifier: GPL-3.0-only
* Controls: 1 left, 2 up, 3 right, 4 down. Collect the cell and reach the exit.
* The displayed maze and progress come from TMS9900 state on the emulated TI.
        IDT  'SOLAR SALVAGE'
POSX    EQU  >8320
POSY    EQU  >8322
ENERGY  EQU  >8324
CELLS   EQU  >8326
MODE    EQU  >8328
CANDX   EQU  >832A
CANDY   EQU  >832C

START   LIMI 0
        LWPI >8300
        BL   @INITVDP
        BL   @INITFONT
        BL   @TITLE
TITLELP BL   @ANYKEY
        CI   R0,0
        JEQ  TITLELP
        BL   @WAITUP
STARTGAME
        CLR  @POSX
        LI   R0,1
        MOV  R0,@POSY
        LI   R0,8
        MOV  R0,@ENERGY
        CLR  @CELLS
        CLR  @MODE
        BL   @DRAW
GAMELOOP
        BL   @READACT
        CI   R0,0
        JEQ  GAMELOOP
        MOV  R0,R8
        BL   @WAITUP
        MOV  R8,R0
        BL   @ACTION
        BL   @DRAW
        MOV  @MODE,R0
        CI   R0,0
        JEQ  GAMELOOP
OVERLP  BL   @ANYKEY
        CI   R0,0
        JEQ  OVERLP
        BL   @WAITUP
        B    @STARTGAME

TITLE   MOV  R11,R15
        BL   @CLEAR
        LI   R1,>0061
        LI   R4,STITLE
        BL   @PRINT
        LI   R1,>00A2
        LI   R4,INTRO
        BL   @PRINT
        LI   R1,>00E2
        LI   R4,HELP1
        BL   @PRINT
        LI   R1,>0102
        LI   R4,HELP2
        BL   @PRINT
        LI   R1,>01E4
        LI   R4,SPRESS
        BL   @PRINT
        B    *R15

ACTION  MOV  R11,R10
        MOV  @POSX,R1
        MOV  @POSY,R2
        MOV  R1,@CANDX
        MOV  R2,@CANDY
        CI   R0,1
        JNE  MOVEUP
        CI   R1,0
        JEQ  TRYMOVE
        DEC  R1
        JMP  TRYMOVE
MOVEUP  CI   R0,2
        JNE  MOVERIGHT
        CI   R2,0
        JEQ  TRYMOVE
        DEC  R2
        JMP  TRYMOVE
MOVERIGHT
        CI   R0,3
        JNE  MOVEDOWN
        CI   R1,4
        JHE  TRYMOVE
        INC  R1
        JMP  TRYMOVE
MOVEDOWN
        CI   R0,4
        JNE  ACTDONE
        CI   R2,2
        JHE  TRYMOVE
        INC  R2
TRYMOVE MOV  R1,@CANDX
        MOV  R2,@CANDY
* Wall at (1,0) and (3,1); a blocked move costs one energy.
        CI   R1,1
        JNE  WALL2
        CI   R2,0
        JEQ  BLOCKED
WALL2   CI   R1,3
        JNE  COMMIT
        CI   R2,1
        JEQ  BLOCKED
COMMIT  MOV  R1,@POSX
        MOV  R2,@POSY
        DEC  @ENERGY
* The cell is at (2,1); collecting it restores two energy units.
        CI   R1,2
        JNE  CHECKEXIT
        CI   R2,1
        JNE  CHECKEXIT
        MOV  @CELLS,R3
        CI   R3,0
        JNE  CHECKEXIT
        LI   R3,1
        MOV  R3,@CELLS
        MOV  @ENERGY,R3
        AI   R3,2
        MOV  R3,@ENERGY
CHECKEXIT
* Extraction is at (4,2), and requires the energy cell.
        CI   R1,4
        JNE  CHECKENERGY
        CI   R2,2
        JNE  CHECKENERGY
        MOV  @CELLS,R3
        CI   R3,0
        JEQ  CHECKENERGY
        LI   R3,1
        MOV  R3,@MODE
        JMP  ACTDONE
BLOCKED DEC  @ENERGY
CHECKENERGY
        MOV  @ENERGY,R3
        CI   R3,0
        JGT  ACTDONE
        LI   R3,2
        MOV  R3,@MODE
ACTDONE B    *R10

DRAW    MOV  R11,R15
        BL   @CLEAR
        LI   R1,>0025
        LI   R4,STITLE
        BL   @PRINT
        LI   R1,>0063
        LI   R4,MAPTXT
        BL   @PRINT
        LI   R2,0
        LI   R6,>00A7
        BL   @DRAWROW
        LI   R2,1
        LI   R6,>00C7
        BL   @DRAWROW
        LI   R2,2
        LI   R6,>00E7
        BL   @DRAWROW
        LI   R1,>0122
        LI   R4,XTXT
        BL   @PRINT
        MOV  @POSX,R0
        LI   R5,>0128
        BL   @PUTNUM
        LI   R1,>0142
        LI   R4,YTXT
        BL   @PRINT
        MOV  @POSY,R0
        LI   R5,>0148
        BL   @PUTNUM
        LI   R1,>0162
        LI   R4,ENERGYTXT
        BL   @PRINT
        MOV  @ENERGY,R0
        LI   R5,>016A
        BL   @PUTNUM
        LI   R1,>0182
        LI   R4,CELLTXT
        BL   @PRINT
        MOV  @CELLS,R0
        LI   R5,>0188
        BL   @PUTNUM
        MOV  @MODE,R0
        CI   R0,0
        JEQ  DRAWHINT
        CI   R0,1
        JNE  DRAWLOSS
        LI   R1,>01A4
        LI   R4,WINMSG
        BL   @PRINT
        JMP  DRAWDONE
DRAWLOSS
        LI   R1,>01A5
        LI   R4,LOSEMSG
        BL   @PRINT
        JMP  DRAWDONE
DRAWHINT
        LI   R1,>01E2
        LI   R4,HELP1
        BL   @PRINT
        LI   R1,>0202
        LI   R4,HELP2
        BL   @PRINT
DRAWDONE B    *R15

DRAWROW MOV  R11,R9
        CLR  R3
CELLLOOP
* P=player, W=wall, C=cell, E=exit, S=start, .=open floor.
        MOV  @POSX,R0
        C    R0,R3
        JNE  CELLWALL
        MOV  @POSY,R0
        C    R0,R2
        JEQ  CELLPLAYER
CELLWALL
        CI   R3,1
        JNE  WALLB
        CI   R2,0
        JEQ  CELLW
WALLB   CI   R3,3
        JNE  CELLENERGY
        CI   R2,1
        JEQ  CELLW
CELLENERGY
        CI   R3,2
        JNE  CELLEXIT
        CI   R2,1
        JNE  CELLEXIT
        MOV  @CELLS,R0
        CI   R0,0
        JEQ  CELLC
CELLEXIT
        CI   R3,4
        JNE  CELLSTART
        CI   R2,2
        JEQ  CELLE
CELLSTART
        CI   R3,0
        JNE  CELLDOT
        CI   R2,1
        JEQ  CELLSTARTMARK
CELLDOT LI   R4,DOTCHAR
        JMP  CELLPRINT
CELLPLAYER LI R4,PCHAR
        JMP  CELLPRINT
CELLW   LI   R4,WCHAR
        JMP  CELLPRINT
CELLC   LI   R4,CCHAR
        JMP  CELLPRINT
CELLE   LI   R4,ECHAR
        JMP  CELLPRINT
CELLSTARTMARK LI R4,SCHAR
CELLPRINT
        MOV  R6,R1
        A    R3,R1
        BL   @PRINT
        INC  R3
        CI   R3,5
        JLT  CELLLOOP
        B    *R9

STITLE  TEXT 'SOLAR SALVAGE 99'
        BYTE 0
INTRO   TEXT 'FIND THE CELL. REACH THE EXIT.'
        BYTE 0
HELP1   TEXT '1 LEFT  2 UP  3 RIGHT'
        BYTE 0
HELP2   TEXT '4 DOWN. A WALL COSTS ENERGY.'
        BYTE 0
SPRESS  TEXT 'PRESS ANY KEY TO EXPLORE'
        BYTE 0
MAPTXT  TEXT 'S AND E MARK START AND EXIT'
        BYTE 0
XTXT    TEXT 'POSITION X:'
        BYTE 0
YTXT    TEXT 'POSITION Y:'
        BYTE 0
ENERGYTXT TEXT 'ENERGY:'
        BYTE 0
CELLTXT TEXT 'CELLS:'
        BYTE 0
WINMSG  TEXT 'SALVAGE RECOVERED!'
        BYTE 0
LOSEMSG TEXT 'ENERGY DEPLETED'
        BYTE 0
PCHAR   TEXT 'P'
        BYTE 0
WCHAR   TEXT 'W'
        BYTE 0
CCHAR   TEXT 'C'
        BYTE 0
ECHAR   TEXT 'E'
        BYTE 0
SCHAR   TEXT 'S'
        BYTE 0
DOTCHAR TEXT '.'
        BYTE 0
* @HOMEBREW_RUNTIME
