* OILFIELD OUTRUN — an original three-lane Texas driving challenge.
* Copyright (C) 2026 DeveloperCal. SPDX-License-Identifier: GPL-3.0-only
* Controls: 1 steer left, 3 steer right, 2 accelerate past the next hazard.
* Avoid the rotating oilfield hazard. Distance, score, lane and lives are emulated.
        IDT  'OILFIELD OUTRUN'
LANE    EQU  >8320
HAZARD  EQU  >8322
DIST    EQU  >8324
SCORE   EQU  >8326
LIVES   EQU  >8328
MODE    EQU  >832A

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
        LI   R0,2
        MOV  R0,@HAZARD
        LI   R0,3
        MOV  R0,@LIVES
        CLR  @DIST
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
        LI   R4,OTITLE
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
        CI   R0,1
        JNE  DRIVE2
        MOV  @LANE,R1
        CI   R1,0
        JEQ  ACTDONE
        DEC  @LANE
        JMP  ACTDONE
DRIVE2  CI   R0,3
        JNE  ACCEL
        MOV  @LANE,R1
        CI   R1,2
        JHE  ACTDONE
        INC  @LANE
        JMP  ACTDONE
ACCEL  CI   R0,2
        JNE  ACTDONE
        MOV  @DIST,R1
        AI   R1,10
        MOV  R1,@DIST
        MOV  @LANE,R1
        C    R1,@HAZARD
        JNE  SAFE
        DEC  @LIVES
        JMP  NEXTHAZ
SAFE    MOV  @SCORE,R1
        AI   R1,10
        MOV  R1,@SCORE
NEXTHAZ MOV  @HAZARD,R1
        INC  R1
        CI   R1,3
        JLT  HAZSAVE
        CLR  R1
HAZSAVE MOV  R1,@HAZARD
        MOV  @LIVES,R1
        CI   R1,0
        JGT  ACTDONE
        LI   R1,2
        MOV  R1,@MODE
ACTDONE B    *R10

DRAW    MOV  R11,R15
        BL   @CLEAR
        LI   R1,>0026
        LI   R4,OTITLE
        BL   @PRINT
        LI   R1,>0062
        LI   R4,DISTTXT
        BL   @PRINT
        MOV  @DIST,R0
        LI   R5,>0069
        BL   @PUTNUM
        LI   R1,>0082
        LI   R4,SCORETXT
        BL   @PRINT
        MOV  @SCORE,R0
        LI   R5,>0089
        BL   @PUTNUM
        LI   R1,>00A2
        LI   R4,LIVESTXT
        BL   @PRINT
        MOV  @LIVES,R0
        LI   R5,>00AA
        BL   @PUTNUM
        LI   R1,>00E9
        LI   R4,ROADTXT
        BL   @PRINT
        LI   R1,>0102
        LI   R4,CARLBL
        BL   @PRINT
        MOV  @LANE,R0
        MOV  R0,R2
        SLA  R0,2
        A    R2,R0
        LI   R1,>0108
        A    R0,R1
        LI   R4,CARCHAR
        BL   @PRINT
        LI   R1,>0122
        LI   R4,OILLLBL
        BL   @PRINT
        MOV  @HAZARD,R0
        MOV  R0,R2
        SLA  R0,2
        A    R2,R0
        LI   R1,>0128
        A    R0,R1
        LI   R4,OILCHAR
        BL   @PRINT
        MOV  @MODE,R0
        CI   R0,2
        JNE  DRAWHINT
        LI   R1,>0185
        LI   R4,CRASHMSG
        BL   @PRINT
        LI   R1,>01E3
        LI   R4,RESTART
        BL   @PRINT
        JMP  DRAWDONE
DRAWHINT
        LI   R1,>0182
        LI   R4,HELP1
        BL   @PRINT
        LI   R1,>01A2
        LI   R4,HELP2
        BL   @PRINT
DRAWDONE B    *R15

OTITLE  TEXT 'OILFIELD OUTRUN'
        BYTE 0
INTRO   TEXT 'DODGE HAZARDS IN WEST TEXAS'
        BYTE 0
HELP1   TEXT '1 LEFT  2 DRIVE  3 RIGHT'
        BYTE 0
HELP2   TEXT 'A CLEAR LANE EARNS 10 POINTS'
        BYTE 0
SPRESS  TEXT 'PRESS ANY KEY TO START'
        BYTE 0
DISTTXT TEXT 'DISTANCE:'
        BYTE 0
SCORETXT TEXT 'SCORE:'
        BYTE 0
LIVESTXT TEXT 'LIVES:'
        BYTE 0
LANETXT TEXT 'YOUR LANE:'
        BYTE 0
HAZTXT  TEXT 'HAZARD LANE:'
        BYTE 0
ROADTXT TEXT '0   1   2'
        BYTE 0
CARLBL  TEXT 'CAR:'
        BYTE 0
OILLLBL TEXT 'OIL:'
        BYTE 0
CARCHAR TEXT 'P'
        BYTE 0
OILCHAR TEXT 'O'
        BYTE 0
CRASHMSG TEXT 'OUT OF LIVES - CRASH!'
        BYTE 0
RESTART TEXT 'PRESS ANY KEY TO RESTART'
        BYTE 0
* @HOMEBREW_RUNTIME
