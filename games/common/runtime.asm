* Shared TI-99/4A cartridge routines. The font table is injected separately at
* build time from the attributed Libre99/Titris source.
VDPWD   EQU  >8C00
VDPWA   EQU  >8C02

INITVDP MOV  R11,R14
        LI   R1,REGTAB
        LI   R2,16
RGL     MOVB *R1+,@VDPWA
        DEC  R2
        JNE  RGL
        LI   R1,>0780
        BL   @SETWR
        LI   R0,>D000
        MOVB R0,@VDPWD
        B    *R14

INITFONT
        MOV  R11,R14
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

REGTAB  BYTE >00,>80,>C0,>81,>00,>82,>0C,>83
        BYTE >01,>84,>0F,>85,>03,>86,>17,>87
* @HOMEBREW_FONT
        END  START
