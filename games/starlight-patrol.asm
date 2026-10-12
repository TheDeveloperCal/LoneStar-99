* STARLIGHT PATROL — an original lane-based space arcade cartridge.
* Copyright (C) 2026 DeveloperCal. SPDX-License-Identifier: GPL-3.0-only
* Controls: 1 left, 2 fire (scores 10), 3 right, 4 advance the threat.
* The player, threat, score and lives are stored in emulated console RAM.
        IDT  'STAR PATROL'
LANE    EQU  >8320
THREAT  EQU  >8322
SCORE   EQU  >8324
LIVES   EQU  >8326
MODE    EQU  >8328

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
        LI   R0,1
        MOV  R0,@LANE
        MOV  R0,@THREAT
        LI   R0,3
        MOV  R0,@LIVES
        CLR  @SCORE
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
        LI   R1,>0065
        LI   R4,STITLE
        BL   @PRINT
        LI   R1,>00C2
        LI   R4,SINTRO
        BL   @PRINT
        LI   R1,>0102
        LI   R4,SHELP1
        BL   @PRINT
        LI   R1,>0122
        LI   R4,SHELP2
        BL   @PRINT
        LI   R1,>01E4
        LI   R4,SPRESS
        BL   @PRINT
        B    *R15

ACTION  MOV  R11,R10
        CI   R0,1
        JNE  ACT2
        MOV  @LANE,R1
        CI   R1,0
        JEQ  ACTDONE
        DEC  @LANE
        JMP  ACTDONE
ACT2    CI   R0,2
        JNE  ACT3
        MOV  @SCORE,R1
        AI   R1,10
        MOV  R1,@SCORE
        MOV  @THREAT,R1
        INC  R1
        CI   R1,3
        JLT  THRSAVE
        CLR  R1
THRSAVE MOV  R1,@THREAT
        JMP  ACTDONE
ACT3    CI   R0,3
        JNE  ACT4
        MOV  @LANE,R1
        CI   R1,2
        JHE  ACTDONE
        INC  @LANE
        JMP  ACTDONE
ACT4    CI   R0,4
        JNE  ACTDONE
        MOV  @LANE,R1
        C    R1,@THREAT
        JNE  ADVANCE
        DEC  @LIVES
ADVANCE MOV  @THREAT,R1
        INC  R1
        CI   R1,3
        JLT  THRSAVE2
        CLR  R1
THRSAVE2 MOV R1,@THREAT
        MOV  @LIVES,R1
        CI   R1,0
        JGT  ACTDONE
        LI   R1,2
        MOV  R1,@MODE
ACTDONE B    *R10

DRAW    MOV  R11,R15
        BL   @CLEAR
        LI   R1,>0025
        LI   R4,STITLE
        BL   @PRINT
        LI   R1,>0062
        LI   R4,LANETXT
        BL   @PRINT
        MOV  @LANE,R0
        LI   R5,>0069
        BL   @PUTNUM
        LI   R1,>0082
        LI   R4,THRTXT
        BL   @PRINT
        MOV  @THREAT,R0
        LI   R5,>0089
        BL   @PUTNUM
        LI   R1,>00C2
        LI   R4,SCORETXT
        BL   @PRINT
        MOV  @SCORE,R0
        LI   R5,>00CA
        BL   @PUTNUM
        LI   R1,>00E2
        LI   R4,LIVESTXT
        BL   @PRINT
        MOV  @LIVES,R0
        LI   R5,>00EA
        BL   @PUTNUM
        LI   R1,>0109
        LI   R4,LANES
        BL   @PRINT
        LI   R1,>0122
        LI   R4,SHIPLBL
        BL   @PRINT
        MOV  @LANE,R0
        MOV  R0,R2
        SLA  R0,2
        A    R2,R0
        LI   R1,>0129
        A    R0,R1
        LI   R4,SHIPCHAR
        BL   @PRINT
        LI   R1,>0142
        LI   R4,THREATLBL
        BL   @PRINT
        MOV  @THREAT,R0
        MOV  R0,R2
        SLA  R0,2
        A    R2,R0
        LI   R1,>0149
        A    R0,R1
        LI   R4,ASTCHAR
        BL   @PRINT
        MOV  @MODE,R0
        CI   R0,2
        JNE  DRAWHINT
        LI   R1,>0185
        LI   R4,GAMEOVER
        BL   @PRINT
        LI   R1,>01E3
        LI   R4,RESTART
        BL   @PRINT
        JMP  DRAWDONE
DRAWHINT
        LI   R1,>0182
        LI   R4,SHELP1
        BL   @PRINT
        LI   R1,>01A2
        LI   R4,SHELP2
        BL   @PRINT
DRAWDONE B   *R15

STITLE  TEXT 'STARLIGHT PATROL'
        BYTE 0
SINTRO  TEXT 'DEFEND THE THREE LANES'
        BYTE 0
SHELP1  TEXT '1 LEFT  2 FIRE  3 RIGHT'
        BYTE 0
SHELP2  TEXT '4 ADVANCE THE ASTEROID'
        BYTE 0
SPRESS  TEXT 'PRESS ANY KEY TO LAUNCH'
        BYTE 0
LANETXT TEXT 'PLAYER LANE:'
        BYTE 0
THRTXT  TEXT 'THREAT LANE:'
        BYTE 0
SCORETXT TEXT 'SCORE:'
        BYTE 0
LIVESTXT TEXT 'LIVES:'
        BYTE 0
LANES   TEXT '0   1   2'
        BYTE 0
SHIPLBL TEXT 'SHIP:'
        BYTE 0
THREATLBL TEXT 'THREAT:'
        BYTE 0
SHIPCHAR TEXT 'P'
        BYTE 0
ASTCHAR TEXT 'A'
        BYTE 0
GAMEOVER TEXT 'PATROL OVER'
        BYTE 0
RESTART TEXT 'PRESS ANY KEY TO RESTART'
        BYTE 0
* @HOMEBREW_RUNTIME
