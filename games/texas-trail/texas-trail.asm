* THE TEXAS TRAIL — an original TI-99/4A frontier resource game.
* Copyright (C) 2026 DeveloperCal.
* SPDX-License-Identifier: GPL-3.0-only
* At build time, the separate upstream-licensed font table is inserted at the
* marker below. The resulting cartridge contains material under both licenses.
* Written for the TI-99/4A TMS9900 and 9918A. No Android game logic.
* Controls: first key opens help; next key starts the journey. During the journey,
* 1 travels, 2 rests, 3 repairs the wagon, and 4 trades for supplies.
* At a travel fork, 1 chooses the prairie road and 2 the dry western cut.
* The numeric keypad and touch keys both address the TI keyboard matrix.
        IDT  'TEXTRAIL'
VDPWD   EQU  >8C00
VDPWA   EQU  >8C02
SOUND   EQU  >8400
FOOD    EQU  >8320
WATER   EQU  >8322
CASH    EQU  >8324
HEALTH  EQU  >8326
WAGON   EQU  >8328
MILES   EQU  >832A
DAY     EQU  >832C
RAND    EQU  >832E
MODE    EQU  >8330
EVENT   EQU  >8332


START   LIMI 0
        LWPI >8300
        LI   R1,REGTAB
        LI   R2,16
RGL     MOVB *R1+,@VDPWA
        DEC  R2
        JNE  RGL
        LI   R1,>0780
        BL   @SETWR
        LI   R0,>D000
        MOVB R0,@VDPWD
        BL   @INITFONT
        BL   @TITLE
TITLELP BL   @ANYKEY
        CI   R0,0
        JEQ  TITLELP
        BL   @WAITUP
        BL   @HELPBOX
HLPWAIT BL   @ANYKEY
        CI   R0,0
        JEQ  HLPWAIT
        BL   @WAITUP
        B    @NEWRUN

NEWRUN  LI   R0,70
        MOV  R0,@FOOD
        MOV  R0,@WATER
        LI   R0,40
        MOV  R0,@CASH
        LI   R0,100
        MOV  R0,@HEALTH
        MOV  R0,@WAGON
        CLR  @MILES
        LI   R0,1
        MOV  R0,@DAY
        LI   R0,>ACE1
        MOV  R0,@RAND
        CLR  @MODE
        LI   R0,EVENT0
        MOV  R0,@EVENT
        BL   @DRAWMAP

MAIN    BL   @READACT
        CI   R0,0
        JEQ  MAIN
        MOV  R0,R8
        BL   @WAITUP
        CI   R8,1
        JEQ  TRAVEL
        CI   R8,2
        JNE  CHK3
        B    @REST
CHK3    CI   R8,3
        JNE  CHK4
        B    @REPAIR
CHK4    CI   R8,4
        JNE  MAIN
        B    @TRADE
        JMP  MAIN

TRAVEL  BL   @ROUTEBOX
ROUTELP BL   @READACT
        CI   R0,0
        JEQ  ROUTELP
        MOV  R0,R8
        BL   @WAITUP
        INC  @DAY
        MOV  @RAND,R0
        MOV  R0,R1
        ANDI R1,1
        SRL  R0,1
        CI   R1,0
        JEQ  RNDOK
        LI   R1,>B400
        XOR  R1,R0
RNDOK   MOV  R0,@RAND
        ANDI R0,>003F
        CI   R8,1
        JNE  CUT
        AI   R0,60
        A    R0,@MILES
        LI   R0,5
        BL   @USEWATER
        LI   R0,6
        BL   @USEFOOD
        LI   R0,EVENT1
        MOV  R0,@EVENT
        JMP  WEATHER
CUT     CI   R8,2
        JNE  ROUTELP
        AI   R0,95
        A    R0,@MILES
        LI   R0,11
        BL   @USEWATER
        LI   R0,7
        BL   @USEFOOD
        LI   R0,EVENT2
        MOV  R0,@EVENT
        DEC  @WAGON
        DEC  @WAGON
        DEC  @HEALTH
WEATHER MOV  @RAND,R0
        ANDI R0,7
        CI   R0,0
        JNE  WTHR1
        LI   R0,EVENT3
        MOV  R0,@EVENT
        MOV  @WATER,R0
        AI   R0,-9
        MOV  R0,@WATER
        MOV  @WAGON,R0
        AI   R0,-10
        MOV  R0,@WAGON
        MOV  @HEALTH,R0
        AI   R0,-4
        MOV  R0,@HEALTH
        JMP  CHECK
WTHR1   CI   R0,1
        JNE  CHECK
        LI   R0,EVENT4
        MOV  R0,@EVENT
        MOV  @WATER,R0
        AI   R0,8
        CI   R0,100
        JLE  WTRSAVE
        LI   R0,100
WTRSAVE MOV  R0,@WATER
        JMP  CHECK

REST    INC  @DAY
        MOV  @FOOD,R0
        AI   R0,-4
        MOV  R0,@FOOD
        MOV  @WATER,R0
        AI   R0,-4
        MOV  R0,@WATER
        MOV  @HEALTH,R0
        AI   R0,12
        CI   R0,100
        JLE  RESTOK
        LI   R0,100
        MOV  R0,@HEALTH
RESTOK  LI   R0,EVENT5
        MOV  R0,@EVENT
        JMP  CHECK

REPAIR  MOV  @CASH,R0
        CI   R0,8
        JLT  POOR
        MOV  @CASH,R0
        AI   R0,-8
        MOV  R0,@CASH
        MOV  @WAGON,R0
        AI   R0,25
        CI   R0,100
        JLE  REPOK
        LI   R0,100
        MOV  R0,@WAGON
REPOK   LI   R0,EVENT6
        MOV  R0,@EVENT
        JMP  CHECK

TRADE   MOV  @CASH,R0
        CI   R0,10
        JLT  POOR
        MOV  @CASH,R0
        AI   R0,-10
        MOV  R0,@CASH
        MOV  @FOOD,R0
        AI   R0,16
        MOV  R0,@FOOD
        MOV  @WATER,R0
        AI   R0,16
        MOV  R0,@WATER
        CI   R0,100
        JLE  TRDW
        LI   R0,100
        MOV  R0,@FOOD
TRDW    MOV  @WATER,R0
        CI   R0,100
        JLE  TRDOK
        LI   R0,100
        MOV  R0,@WATER
TRDOK   LI   R0,EVENT7
        MOV  R0,@EVENT
        JMP  CHECK
POOR    LI   R0,EVENT8
        MOV  R0,@EVENT

CHECK   MOV  @MILES,R0
        CI   R0,320
        JLT  CHECKFOOD
        LI   R0,320
        MOV  R0,@MILES
        LI   R0,1
        MOV  R0,@MODE
        LI   R0,EVENT9
        MOV  R0,@EVENT
        JMP  DRAW
CHECKFOOD
        MOV  @FOOD,R0
        CI   R0,0
        JGT  CHECKWATER
        LI   R0,2
        MOV  R0,@MODE
        LI   R0,EVENT10
        MOV  R0,@EVENT
        JMP  DRAW
CHECKWATER
        MOV  @WATER,R0
        CI   R0,0
        JGT  CHECKHEALTH
        LI   R0,2
        MOV  R0,@MODE
        LI   R0,EVENT11
        MOV  R0,@EVENT
        JMP  DRAW
CHECKHEALTH
        MOV  @HEALTH,R0
        CI   R0,0
        JGT  CHECKWAGON
        LI   R0,2
        MOV  R0,@MODE
        LI   R0,EVENT12
        MOV  R0,@EVENT
        JMP  DRAW
CHECKWAGON
        MOV  @WAGON,R0
        CI   R0,0
        JGT  DRAW
        LI   R0,2
        MOV  R0,@MODE
        LI   R0,EVENT13
        MOV  R0,@EVENT

DRAW    BL   @DRAWMAP
        MOV  @MODE,R0
        CI   R0,0
        JNE  ENDED
        B    @MAIN
ENDED   BL   @WAITUP
        BL   @ANYKEY
ENDLP   CI   R0,0
        JNE  ENDLP
        BL   @READACT
        CI   R0,0
        JEQ  ENDLP
        BL   @WAITUP
        B    @NEWRUN

INITFONT MOV R11,R14
        LI   R1,>0300
        BL   @SETWR
        LI   R2,32
        LI   R0,>1700
FCOL    MOVB R0,@VDPWD
        DEC  R2
        JNE  FCOL
        LI   R4,FONT
FLP     MOVB *R4+,R0
        ANDI R0,>FF00
        JEQ  FLPD
        SRL  R0,8
        SLA  R0,3
        AI   R0,>0800
        MOV  R0,R1
        BL   @SETWR
        LI   R2,8
FLPB    MOVB *R4+,@VDPWD
        DEC  R2
        JNE  FLPB
        JMP  FLP
FLPD    B    *R14

TITLE   LI   R5,TITLETAB
        B    @DRAWSTATIC
HELPBOX LI   R5,HELPTAB
        B    @DRAWSTATIC
ROUTEBOX LI R5,ROUTETAB
        B    @DRAWSTATIC

DRAWSTATIC
        MOV  R11,R15
        BL   @CLEAR
DSLOOP  MOV  *R5+,R1
        CI   R1,-1
        JEQ  DSDONE
        MOV  *R5+,R4
        BL   @PRINT
        JMP  DSLOOP
DSDONE  B    *R15

DRAWMAP MOV  R11,R15
        BL   @CLEAR
        MOV  @MODE,R0
        CI   R0,1
        JNE  NOTWIN
        B    @VICTORY
NOTWIN  MOV  @MODE,R0
        CI   R0,2
        JNE  NOTLOSS
        B    @DEFEAT
NOTLOSS
        LI   R1,>0025
        LI   R4,MAPTITLE
        BL   @PRINT
        LI   R1,>0062
        LI   R4,DAYTXT
        BL   @PRINT
        MOV  @DAY,R0
        LI   R5,>0067
        BL   @PUTNUM
        LI   R1,>0082
        LI   R4,MILETXT
        BL   @PRINT
        MOV  @MILES,R0
        LI   R5,>0089
        BL   @PUTNUM
        LI   R1,>00C2
        LI   R4,FOODTXT
        BL   @PRINT
        MOV  @FOOD,R0
        LI   R5,>00C8
        BL   @PUTNUM
        LI   R1,>00E2
        LI   R4,WATERTXT
        BL   @PRINT
        MOV  @WATER,R0
        LI   R5,>00E9
        BL   @PUTNUM
        LI   R1,>0102
        LI   R4,CASHTXT
        BL   @PRINT
        MOV  @CASH,R0
        LI   R5,>0108
        BL   @PUTNUM
        LI   R1,>0122
        LI   R4,HEALTHTXT
        BL   @PRINT
        MOV  @HEALTH,R0
        LI   R5,>0129
        BL   @PUTNUM
        LI   R1,>0142
        LI   R4,WAGONTXT
        BL   @PRINT
        MOV  @WAGON,R0
        LI   R5,>0149
        BL   @PUTNUM
        LI   R1,>0182
        LI   R4,REG0
        MOV  @MILES,R0
        CI   R0,100
        JLT  REGION
        LI   R4,REG1
        MOV  @MILES,R0
        CI   R0,180
        JLT  REGION
        LI   R4,REG2
        MOV  @MILES,R0
        CI   R0,260
        JLT  REGION
        LI   R4,REG3
REGION  BL   @PRINT
        LI   R1,>01C2
        MOV  @EVENT,R4
        BL   @PRINT
        LI   R1,>0222
        LI   R4,ACTTXT1
        BL   @PRINT
        LI   R1,>0242
        LI   R4,ACTTXT2
        BL   @PRINT
        LI   R1,>02A2
        LI   R4,ROWRULE
        BL   @PRINT
        B    *R15

VICTORY LI   R1,>0047
        LI   R4,WIN1
        BL   @PRINT
        LI   R1,>00A5
        LI   R4,WIN2
        BL   @PRINT
        LI   R1,>0122
        LI   R4,ENDSTAT
        BL   @PRINT
        MOV  @DAY,R0
        LI   R5,>0129
        BL   @PUTNUM
        LI   R1,>01E5
        LI   R4,AGAIN
        BL   @PRINT
        B    *R15

DEFEAT  LI   R1,>0046
        LI   R4,LOSE1
        BL   @PRINT
        LI   R1,>00A4
        LI   R4,LOSE2
        BL   @PRINT
        LI   R1,>0122
        LI   R4,ENDSTAT
        BL   @PRINT
        MOV  @DAY,R0
        LI   R5,>0129
        BL   @PUTNUM
        LI   R1,>01E5
        LI   R4,AGAIN
        BL   @PRINT
        B    *R15

CLEAR   MOV  R11,R14
        LI   R1,>0000
        BL   @SETWR
        LI   R2,768
        LI   R0,>2000
CLLOOP  MOVB R0,@VDPWD
        DEC  R2
        JNE  CLLOOP
        B    *R14

PRINT   MOV  R11,R14
        BL   @SETWR
PRLOOP  MOVB *R4+,R0
        ANDI R0,>FF00
        CI   R0,0
        JEQ  PRDONE
        MOVB R0,@VDPWD
        JMP  PRLOOP
PRDONE  B    *R14

PUTNUM  MOV  R11,R14
        MOV  R0,R2
        MOV  R5,R1
        BL   @SETWR
        LI   R3,100
        BL   @DIGIT
        LI   R3,10
        BL   @DIGIT
        LI   R3,1
        BL   @DIGIT
        B    *R14
DIGIT   CLR  R4
DLOOP   C    R2,R3
        JLT  DEND
        S    R3,R2
        INC  R4
        JMP  DLOOP
DEND    MOV  R4,R0
        SLA  R0,8
        ORI  R0,>3000
        MOVB R0,@VDPWD
        RT

SETWR   SWPB R1
        MOVB R1,@VDPWA
        SWPB R1
        ORI  R1,>4000
        MOVB R1,@VDPWA
        ANDI R1,>3FFF
        RT

READACT MOV  R11,R10
        LI   R0,>0500
        BL   @KEYROW
        JEQ  IS1
        LI   R0,>0100
        BL   @KEYROW
        JEQ  IS2
        LI   R0,>0200
        BL   @KEYROW
        JEQ  IS3
        LI   R0,>0300
        BL   @KEYROW
        JEQ  IS4
        CLR  R0
        B    *R10
IS1     LI   R0,1
        B    *R10
IS2     LI   R0,2
        B    *R10
IS3     LI   R0,3
        B    *R10
IS4     LI   R0,4
        B    *R10
KEYROW  LI   R12,>0024
        LDCR R0,3
        LI   R12,>0006
        STCR R2,8
        ANDI R2,>1000
        RT

ANYKEY  CLR  R1
AKLOOP  MOV  R1,R0
        SWPB R0
        LI   R12,>0024
        LDCR R0,3
        LI   R12,>0006
        STCR R2,8
        ANDI R2,>FF00
        CI   R2,>FF00
        JNE  AKYES
        INC  R1
        CI   R1,8
        JNE  AKLOOP
        CLR  R0
        RT
AKYES   LI   R0,1
        RT

WAITUP  MOV  R11,R14
WUP     BL   @ANYKEY
        CI   R0,0
        JNE  WUP
        B    *R14


USEWATER
        NEG  R0
        A    R0,@WATER
        JGT  UWDONE
        CLR  @WATER
UWDONE  RT
USEFOOD NEG  R0
        A    R0,@FOOD
        JGT  UFDONE
        CLR  @FOOD
UFDONE  RT

REGTAB  BYTE >00,>80,>C0,>81,>00,>82,>0C,>83
        BYTE >01,>84,>0F,>85,>03,>86,>17,>87
TITLETAB
        DATA >0085,TTITLE
        DATA >00C4,TSUB
        DATA >0144,TTEXAS
        DATA >0184,TTSTART
        DATA >01E4,TTHELP
        DATA -1
HELPTAB
        DATA >0046,HTITLE
        DATA >00A2,H1
        DATA >00C2,H2
        DATA >00E2,H3
        DATA >0102,H4
        DATA >0142,H5
        DATA >0182,H6
        DATA >01E2,HPRESS
        DATA -1
ROUTETAB
        DATA >0048,RTITLE
        DATA >00A2,R1TXT
        DATA >00E2,R2TXT
        DATA >0162,R3TXT
        DATA >01E2,HPRESS
        DATA -1

MAPTITLE TEXT 'TEXAS TRAIL JOURNAL'
        BYTE 0
DAYTXT   TEXT 'DAY:'
        BYTE 0
MILETXT  TEXT 'MILES:'
        BYTE 0
FOODTXT  TEXT 'FOOD:'
        BYTE 0
WATERTXT TEXT 'WATER:'
        BYTE 0
CASHTXT  TEXT 'CASH:$'
        BYTE 0
HEALTHTXT TEXT 'HEALTH:'
        BYTE 0
WAGONTXT TEXT 'WAGON:'
        BYTE 0
REG0     TEXT 'NORTH TEXAS PRAIRIE'
        BYTE 0
REG1     TEXT 'HILL COUNTRY'
        BYTE 0
REG2     TEXT 'WEST TEXAS DRYLAND'
        BYTE 0
REG3     TEXT 'PECOS CROSSING'
        BYTE 0
ACTTXT1  TEXT '1 TRAVEL  2 REST'
        BYTE 0
ACTTXT2  TEXT '3 REPAIR  4 TRADE'
        BYTE 0
ROWRULE  TEXT 'H HELP  TARGET: EL PASO'
        BYTE 0
EVENT0   TEXT 'THE ROAD OPENS WEST.'
        BYTE 0
EVENT1   TEXT 'COOL PRAIRIE WIND.'
        BYTE 0
EVENT2   TEXT 'THE CUT IS ROUGH.'
        BYTE 0
EVENT3   TEXT 'DUST! WATER AND WAGON LOST.'
        BYTE 0
EVENT4   TEXT 'RAIN FILLS THE BARRELS.'
        BYTE 0
EVENT5   TEXT 'A QUIET CAMP RESTORES SPIRITS.'
        BYTE 0
EVENT6   TEXT 'THE AXLE HOLDS AGAIN.'
        BYTE 0
EVENT7   TEXT 'SUPPLIES BOUGHT AT POST.'
        BYTE 0
EVENT8   TEXT 'TOO LITTLE CASH TO TRADE.'
        BYTE 0
EVENT9   TEXT 'EL PASO! YOU REACH THE PASS.'
        BYTE 0
EVENT10  TEXT 'NO FOOD. THE CREW TURNS BACK.'
        BYTE 0
EVENT11  TEXT 'NO WATER. THE CREW TURNS BACK.'
        BYTE 0
EVENT12  TEXT 'CREW CANNOT TRAVEL FARTHER.'
        BYTE 0
EVENT13  TEXT 'THE WAGON BREAKS DOWN.'
        BYTE 0
TTITLE   TEXT 'THE TEXAS TRAIL'
        BYTE 0
TSUB     TEXT 'FORT WORTH TO EL PASO'
        BYTE 0
TTEXAS   TEXT 'PRAIRIE  HILL  DESERT  PECOS'
        BYTE 0
TTSTART  TEXT 'ANY KEY OPENS THE GUIDE'
        BYTE 0
TTHELP   TEXT 'PRESS AGAIN TO RIDE WEST'
        BYTE 0
HTITLE   TEXT 'FIELD GUIDE'
        BYTE 0
H1       TEXT '1 TRAVEL. PICK A ROAD.'
        BYTE 0
H2       TEXT '2 REST TO RECOVER HEALTH.'
        BYTE 0
H3       TEXT '3 REPAIR WAGON. COSTS $8.'
        BYTE 0
H4       TEXT '4 TRADE $10 FOR SUPPLIES.'
        BYTE 0
H5       TEXT 'WATCH SUPPLIES, HEALTH, WAGON.'
        BYTE 0
H6       TEXT 'CROSS PECOS AT 320 MILES.'
        BYTE 0
HPRESS   TEXT 'PRESS ANY KEY TO START'
        BYTE 0
RTITLE   TEXT 'CHOOSE YOUR ROUTE'
        BYTE 0
R1TXT    TEXT '1 PRAIRIE ROAD. LESS RISK.'
        BYTE 0
R2TXT    TEXT '2 WESTERN CUT. MORE MILES.'
        BYTE 0
R3TXT    TEXT 'ROUGH CUT HURTS THE WAGON.'
        BYTE 0
WIN1     TEXT 'YOU REACH THE PECOS PASS!'
        BYTE 0
WIN2     TEXT 'YOU REACH EL PASO.'
        BYTE 0
LOSE1    TEXT 'THE PARTY TURNS BACK.'
        BYTE 0
LOSE2    TEXT 'PREPARE BETTER. TRY AGAIN.'
        BYTE 0
ENDSTAT  TEXT 'DAYS ON THE TRAIL:'
        BYTE 0
AGAIN    TEXT 'PRESS 1-4 TO RIDE AGAIN'
        BYTE 0
* @FONT_TABLE_FROM_BUILD
